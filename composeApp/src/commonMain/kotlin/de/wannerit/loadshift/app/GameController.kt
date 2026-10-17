package de.wannerit.loadshift.app

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import de.wannerit.loadshift.core.BatchMode
import de.wannerit.loadshift.core.CapacityMode
import de.wannerit.loadshift.core.Control
import de.wannerit.loadshift.core.GameTime
import de.wannerit.loadshift.core.Policy
import de.wannerit.loadshift.core.ScheduledMinimum
import de.wannerit.loadshift.core.Scenario
import de.wannerit.loadshift.core.Simulation
import de.wannerit.loadshift.core.Totals

/** Speed of the game clock: ticks (15 game minutes) per real second. */
enum class Speed(val ticksPerSecond: Int) { PAUSED(0), X1(1), X4(4), X16(16) }

/**
 * Runs a scenario for the screen: the simulation, the clock and the player's controls. The simulation is mutable;
 * [version] changes with every change. Reading [simulation] reads [version], so every composable that looks at the
 * simulation redraws after a change – Compose skips a composable whose arguments are the same objects as before.
 */
class GameController(val scenario: Scenario) {
    private var current = Simulation(scenario)
    val simulation: Simulation get() = version.let { current }

    /** The reference operator's run of the same scenario, for the comparison. */
    val reference: Simulation = Simulation.reference(scenario)

    var version by mutableIntStateOf(0)
        private set
    var speed by mutableStateOf(Speed.PAUSED)
        private set
    var showEnd by mutableStateOf(false)
        private set

    /** Stars of the finished run (set once at the end). */
    var stars by mutableIntStateOf(0)
        private set

    /** The forecast has been revised (scenario 3): the game paused for it. */
    var forecastNews by mutableStateOf(false)
        private set

    /** A batch job that must start soon or will miss its deadline: the game paused for it. */
    var urgentJob by mutableStateOf<Int?>(null)
        private set
    private val warnedJobs = mutableSetOf<Int>()

    val isFinished: Boolean get() = simulation.isFinished
    val hasBatch: Boolean get() = scenario.batchJobs.isNotEmpty()
    val hasInteractive: Boolean get() = scenario.interactive.peak > 0

    private fun changed() {
        version++
    }

    fun changeSpeed(newSpeed: Speed) {
        if (isFinished && newSpeed != Speed.PAUSED) return
        speed = newSpeed
    }

    /** One tick; called by the clock. */
    fun step() {
        if (simulation.isFinished) return
        simulation.step()
        if (simulation.isFinished) finish() else {
            checkUrgentJobs()
            checkForecast()
        }
        changed()
    }

    /** Pauses when the forecast is revised (CONCEPT.md 3.3). */
    private fun checkForecast() {
        if (scenario.forecastMiss?.revisedAt == current.tick) {
            forecastNews = true
            speed = Speed.PAUSED
        }
    }

    fun dismissForecastNews() {
        forecastNews = false
    }

    /** Plans the open jobs again with the latest forecast (greenest-hours rule). */
    fun replan() {
        current.replan()
        forecastNews = false
        changed()
    }

    /** Runs a number of ticks at once (launch options). */
    fun advance(ticks: Int) {
        repeat(ticks) { if (!simulation.isFinished) simulation.step() }
        if (simulation.isFinished) finish()
        changed()
    }

    private fun finish() {
        saveDraft()
        speed = Speed.PAUSED
        stars = scenario.stars(simulation.totals, reference.totals)
        Progress.record(stars, scenario.number)
        showEnd = true
    }

    /** Pauses once per job when an unplanned job has to start within the next hour (CONCEPT.md 3.3). */
    private fun checkUrgentJobs() {
        if (Control.BATCH_BY_HAND !in scenario.controls) return
        for (job in simulation.jobs) {
            if (!job.state.isStarted && job.plannedStart == null && job.id !in warnedJobs &&
                simulation.tick >= job.spec.latestStart - GameTime.TICKS_PER_HOUR
            ) {
                warnedJobs += job.id
                urgentJob = job.id
                speed = Speed.PAUSED
                return
            }
        }
    }

    fun dismissUrgent() {
        urgentJob = null
    }

    /** Changes the policy at once (switching by hand, launch options). */
    fun setPolicy(policy: Policy) {
        current.policy = policy
        current.applyPolicy()
        resetDraft()
        changed()
    }

    // Servers by hand

    val wantedServers: Int
        get() = (simulation.policy.capacity as? CapacityMode.Manual)?.servers
            ?: simulation.servers.count { it.state.isPowered }

    fun setWantedServers(n: Int) {
        setPolicy(simulation.policy.copy(capacity = CapacityMode.Manual(n.coerceIn(0, scenario.serverCount))))
    }

    // Batch rules (scenario 3): the player edits a draft and applies it

    /** The kinds of batch rule the panel offers. */
    enum class RuleKind { ASAP, HOUR, GREENEST, THRESHOLD }

    var draftKind by mutableStateOf(RuleKind.ASAP)
    var draftHour by mutableIntStateOf(3)
    var draftThreshold by mutableIntStateOf(150)
    var draftSafety by mutableStateOf(true)

    /** The batch rule of the draft. */
    val draftBatch: BatchMode
        get() = when (draftKind) {
            RuleKind.ASAP -> BatchMode.Asap
            RuleKind.HOUR -> BatchMode.AtHour(draftHour)
            RuleKind.GREENEST -> BatchMode.Greenest
            RuleKind.THRESHOLD -> BatchMode.Threshold(draftThreshold)
        }

    // Capacity rules (scenario 4)

    enum class CapacityKind { ALL_ON, FIXED, AUTOSCALE }

    var capKind by mutableStateOf(CapacityKind.AUTOSCALE)
    var capFixed by mutableIntStateOf(15)
    var capTarget by mutableIntStateOf(800)
    var capMinimumOn by mutableStateOf(false)
    var capMinimum by mutableIntStateOf(24)
    var capFrom by mutableIntStateOf(18)
    var capTo by mutableIntStateOf(22)

    /** The capacity rule of the draft. */
    val draftCapacity: CapacityMode
        get() = when (capKind) {
            CapacityKind.ALL_ON -> CapacityMode.AllOn
            CapacityKind.FIXED -> CapacityMode.Manual(capFixed)
            CapacityKind.AUTOSCALE -> CapacityMode.Autoscale(capTarget, if (capMinimumOn) ScheduledMinimum(capMinimum, capFrom, capTo) else null)
        }

    /** The whole draft: the rules the scenario lets the player edit, the rest as it runs. */
    val draftPolicy: Policy
        get() {
            var policy = simulation.policy
            if (Control.BATCH_RULES in scenario.controls) policy = policy.copy(batch = draftBatch, deadlineSafety = draftSafety)
            if (Control.CAPACITY_RULES in scenario.controls) policy = policy.copy(capacity = draftCapacity)
            return policy
        }

    /** The draft is what runs now. */
    val draftIsActive: Boolean get() = simulation.policy == draftPolicy

    fun applyDraft() {
        setPolicy(draftPolicy)
    }

    /** Sets the draft to the rules that run now. */
    private fun resetDraft() {
        val policy = current.policy
        when (val capacity = policy.capacity) {
            CapacityMode.AllOn -> capKind = CapacityKind.ALL_ON
            is CapacityMode.Manual -> { capKind = CapacityKind.FIXED; capFixed = capacity.servers }
            is CapacityMode.Autoscale -> {
                capKind = CapacityKind.AUTOSCALE
                capTarget = capacity.targetUtilization
                capMinimumOn = capacity.minimum != null
                capacity.minimum?.let { capMinimum = it.servers; capFrom = it.fromHour; capTo = it.toHour }
            }
        }
        draftSafety = policy.deadlineSafety
        draftKind = when (val mode = policy.batch) {
            is BatchMode.AtHour -> RuleKind.HOUR.also { draftHour = mode.hour }
            BatchMode.Greenest -> RuleKind.GREENEST
            is BatchMode.Threshold -> RuleKind.THRESHOLD.also { draftThreshold = mode.gramsPerKWh }
            else -> RuleKind.ASAP
        }
    }

    init {
        resetDraft()
        restoreDraft()
    }

    /** The draft of the last run of this scenario (kept while the app runs). */
    private class Draft(
        val kind: RuleKind, val hour: Int, val threshold: Int, val safety: Boolean,
        val capKind: CapacityKind, val capFixed: Int, val capTarget: Int, val capMinimumOn: Boolean, val capMinimum: Int,
        val capFrom: Int, val capTo: Int,
    )

    private companion object {
        val drafts = mutableMapOf<Int, Draft>()
    }

    /** Keeps the panel's settings for the next run (a new run starts with the scenario's rules, the panel keeps yours). */
    fun saveDraft() {
        drafts[scenario.number] = Draft(draftKind, draftHour, draftThreshold, draftSafety, capKind, capFixed, capTarget,
            capMinimumOn, capMinimum, capFrom, capTo)
    }

    private fun restoreDraft() {
        val d = drafts[scenario.number] ?: return
        draftKind = d.kind; draftHour = d.hour; draftThreshold = d.threshold; draftSafety = d.safety
        capKind = d.capKind; capFixed = d.capFixed; capTarget = d.capTarget; capMinimumOn = d.capMinimumOn
        capMinimum = d.capMinimum; capFrom = d.capFrom; capTo = d.capTo
    }

    // Batch jobs by hand

    fun plan(job: Int, start: Int) {
        simulation.plan(job, start)
        if (urgentJob == job) urgentJob = null
        changed()
    }

    fun unplan(job: Int) {
        simulation.unplan(job)
        changed()
    }

    /** Default start when the player plans a job: the next full hour, not before its arrival. */
    fun suggestedStart(job: Int): Int {
        val earliest = maxOf(simulation.tick, simulation.jobs[job].spec.arrival)
        val perHour = GameTime.TICKS_PER_HOUR
        return (earliest + perHour - 1) / perHour * perHour
    }

    /** The player's emissions so far against the reference's up to the same moment, per mille (null before the first tick). */
    val changeSoFar: Long?
        get() {
            val last = simulation.history.lastOrNull() ?: return null
            val ref = reference.emissions(last.tick)
            if (ref <= 0) return null
            return (last.emissionsSoFar - ref) * 1_000 / ref
        }

    /** "What if" for the debrief: the whole scenario replayed with other rules, with the same load and grid mix. */
    fun whatIf(): List<Pair<String, Totals>> {
        if (hasBatch && Control.BATCH_RULES in scenario.controls) {
            val capacity = scenario.referencePolicy.capacity
            fun replay(mode: BatchMode, safety: Boolean = true) = Simulation.replay(scenario, Policy(capacity, mode, safety)).totals
            return listOf(
                t("Every job as soon as it arrives (reference)") to reference.totals,
                t("Every job at night, at 03:00") to replay(BatchMode.AtHour(3)),
                t("Below 150 g/kWh, with deadline safety") to replay(BatchMode.Threshold(150)),
                t("Below 150 g/kWh, without deadline safety") to replay(BatchMode.Threshold(150), false),
                t("Greenest hours, planned once") to replay(BatchMode.Greenest),
                t("Greenest hours, planned again after the forecast update") to
                    scenario.forecastMiss.let { miss ->
                        Simulation.replay(scenario, Policy(capacity, BatchMode.Greenest), setOfNotNull(miss?.revisedAt)).totals
                    },
            )
        }
        if (hasBatch) {
            val capacity = scenario.referencePolicy.capacity
            fun replay(mode: BatchMode) = Simulation.replay(scenario, Policy(capacity, mode)).totals
            return listOf(
                t("Every job as soon as it arrives (reference)") to reference.totals,
                t("Every job at night, at 01:00") to replay(BatchMode.AtHour(1)),
                t("Every job at 11:00") to replay(BatchMode.AtHour(11)),
                t("Planned by the forecast (from scenario 3)") to replay(BatchMode.Greenest),
            )
        }
        if (Control.CAPACITY_RULES in scenario.controls) {
            fun replay(policy: Policy, changes: Map<Int, Policy> = emptyMap()) = Simulation.replay(scenario, policy, changes = changes).totals
            val start = scenario.initialPolicy
            val target = (start.capacity as? CapacityMode.Autoscale)?.targetUtilization ?: 800
            val day2 = GameTime.at(1, 12).tick
            val prepared = Policy(CapacityMode.Autoscale(target, ScheduledMinimum(26, 18, 22)))
            return listOf(
                t("All servers on (reference)") to reference.totals,
                t("Autoscaling at %@ (as at the start)", Format.wholePercent(target)) to replay(start),
                t("Autoscaling at %@", Format.wholePercent(600)) to replay(Policy(CapacityMode.Autoscale(600))),
                t("Autoscaling at %@, every day at least %lld servers 18:00–22:00", Format.wholePercent(target), 26) to replay(prepared),
                t("Autoscaling at %@, on day 2 at least %lld servers 18:00–22:00", Format.wholePercent(target), 26) to replay(start, mapOf(day2 to prepared)),
            )
        }
        val half = scenario.serverCount / 2
        val most = scenario.serverCount * 7 / 10
        fun replay(mode: CapacityMode) = Simulation.replay(scenario, Policy(mode)).totals
        return listOf(
            t("All servers on (reference)") to reference.totals,
            t("Fixed: %lld servers all day", half) to replay(CapacityMode.Manual(half)),
            t("Fixed: %lld servers all day", most) to replay(CapacityMode.Manual(most)),
            t("Autoscaling at 70 % (from scenario 4)") to replay(CapacityMode.Autoscale(700)),
        )
    }

    fun restart() {
        saveDraft()
        speed = Speed.PAUSED
        current = Simulation(scenario)
        restoreDraft()
        stars = 0
        showEnd = false
        urgentJob = null
        forecastNews = false
        warnedJobs.clear()
        changed()
    }
}
