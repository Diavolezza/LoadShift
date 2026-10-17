package de.wannerit.loadshift.app

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import de.wannerit.loadshift.core.BatchMode
import de.wannerit.loadshift.core.CacheConfig
import de.wannerit.loadshift.core.CapacityMode
import de.wannerit.loadshift.core.Control
import de.wannerit.loadshift.core.Engineering
import de.wannerit.loadshift.core.Team
import de.wannerit.loadshift.core.GameTime
import de.wannerit.loadshift.core.Packing
import de.wannerit.loadshift.core.Policy
import de.wannerit.loadshift.core.ScheduledMinimum
import de.wannerit.loadshift.core.Scenario
import de.wannerit.loadshift.core.Simulation
import de.wannerit.loadshift.core.Totals
import de.wannerit.loadshift.core.VmKind

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
    val hasInteractive: Boolean get() = scenario.interactive.peak > 0 || scenario.vms.isNotEmpty()

    private fun changed() {
        version++
    }

    fun changeSpeed(newSpeed: Speed) {
        if (isFinished && newSpeed != Speed.PAUSED) return
        speed = newSpeed
    }

    /** The rules in force when this run's first tick ran (the next run starts with them). */
    private var startPolicy: Policy? = null

    /** One tick; called by the clock. */
    fun step() {
        if (simulation.isFinished) return
        if (current.history.isEmpty()) startPolicy = current.policy
        simulation.step()
        if (simulation.isFinished) finish() else {
            checkUrgentJobs()
            checkForecast()
            checkSprint()
        }
        changed()
    }

    // Sprints (engineering track): one sprint plan at the start of every day

    /** The team and its progress through the backlog (carry-over of started items). */
    private var team = Team(scenario.backlog, scenario.sprintPoints)

    /** Items done so far, with the sprint (day index) they were done in. */
    val sprintDone: Map<Int, Int> get() = version.let { team.done }
    /** The items the player has picked for the coming sprint, in the order the team works on them. */
    var sprintDraft by mutableStateOf(listOf<Int>())
        private set
    /** Day of the last sprint started (-1: none yet). */
    private var sprintStartedDay = -1
    /** Plans of this run by day, kept for the next run. */
    private val sprintPlans = mutableMapOf<Int, List<Int>>()

    val hasSprints: Boolean get() = Control.SPRINTS in scenario.controls
    val currentDay: Int get() = (current.tick - scenario.startTick) / GameTime.TICKS_PER_DAY
    val canPlanSprint: Boolean
        get() = hasSprints && !isFinished && (current.tick - scenario.startTick) % GameTime.TICKS_PER_DAY == 0 && sprintStartedDay < currentDay
    val profiled: Boolean get() = scenario.growth != null || sprintDone.keys.any { scenario.backlog[it].profiling }

    /** Items started in an earlier sprint and not finished: the team continues them first. */
    val sprintCarried: List<Int> get() = version.let { team.started }
    fun remaining(item: Int): Int = version.let { team.remaining(item) }

    /** Story points of the carried items and the draft. */
    val sprintPointsPicked: Int get() = sprintCarried.sumOf { team.remaining(it) } + sprintDraft.sumOf { scenario.backlog[it].effort }

    /** Launch options: a whole plan at once. */
    fun planSprint(items: List<Int>) {
        if (canPlanSprint) sprintDraft = items.filter { it !in sprintDone && it !in sprintCarried }
    }

    fun toggleSprintItem(item: Int) {
        if (!canPlanSprint || item in sprintDone || item in sprintCarried) return
        sprintDraft = if (item in sprintDraft) sprintDraft - item else sprintDraft + item
    }

    /** Starts the sprint and lets the week run (fast: a sprint shows its effect over a whole day). */
    fun startSprint() {
        if (!canPlanSprint) return
        val day = currentDay
        sprintPlans[day] = sprintDraft
        team.sprint(day, sprintDraft)
        sprintStartedDay = day
        sprintDraft = emptyList()
        setPolicy(current.policy.copy(done = team.done.keys.toSet()))
        speed = Speed.X16
    }

    /** E2: what an item saves today (per mille of today's work) – "measured" with today's data. */
    fun savingToday(item: Int): Int {
        val done = sprintDone.keys
        val before = Engineering.dailyWork(scenario, currentDay.coerceAtMost(scenario.days - 1), done)
        val after = Engineering.dailyWork(scenario, currentDay.coerceAtMost(scenario.days - 1), done + item)
        return if (before == 0L) 0 else ((before - after) * 1_000 / before).toInt()
    }

    /** Pauses at the start of every sprint, with the last run's plan for that day ready. */
    private fun checkSprint() {
        if (!hasSprints || (current.tick - scenario.startTick) % GameTime.TICKS_PER_DAY != 0) return
        speed = Speed.PAUSED
        prefillSprint()
    }

    private fun prefillSprint() {
        // Not in E2: twelve sprints, a plan that appears by itself would surprise more than help.
        if (scenario.growth != null) return
        val last = lastSprintPlans[scenario.number]?.get(currentDay) ?: return
        sprintDraft = last.filter { it !in sprintDone && it !in sprintCarried }
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
        if (ticks > 0 && current.history.isEmpty()) startPolicy = current.policy
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

    // Caching (E3)

    var cacheServers by mutableIntStateOf(0)
    var cacheTtl by mutableStateOf(listOf<Int>())
    var cacheCount by mutableStateOf(false)
    var cacheView by mutableStateOf(false)

    val draftCache: CacheConfig
        get() = CacheConfig(cacheServers, cacheTtl, cacheCount, cacheView)

    fun setTtl(kind: Int, minutes: Int) {
        cacheTtl = cacheTtl.toMutableList().also { while (it.size <= kind) it += 0; it[kind] = minutes }
    }

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
            if (Control.CACHING in scenario.controls) policy = policy.copy(cache = draftCache)
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
            is CapacityMode.Placed -> {}
        }
        draftSafety = policy.deadlineSafety
        scenario.caching?.let { model ->
            val c = policy.cache
            cacheServers = c.servers
            cacheTtl = model.kinds.indices.map { c.ttlMinutes.getOrElse(it) { 0 } }
            cacheCount = c.approximateCount
            cacheView = c.materializedView
        }
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
        if (hasSprints) prefillSprint()
    }

    /** The draft of the last run of this scenario (kept while the app runs). */
    private class Draft(
        val kind: RuleKind, val hour: Int, val threshold: Int, val safety: Boolean,
        val capKind: CapacityKind, val capFixed: Int, val capTarget: Int, val capMinimumOn: Boolean, val capMinimum: Int,
        val capFrom: Int, val capTo: Int, val startPolicy: Policy?, val cache: CacheConfig,
    )

    private companion object {
        val drafts = mutableMapOf<Int, Draft>()
        val lastSprintPlans = mutableMapOf<Int, Map<Int, List<Int>>>()
    }

    /** Keeps the panel's settings for the next run (a new run starts with the scenario's rules, the panel keeps yours). */
    fun saveDraft() {
        if (hasSprints && sprintPlans.isNotEmpty()) lastSprintPlans[scenario.number] = sprintPlans.toMap()
        drafts[scenario.number] = Draft(draftKind, draftHour, draftThreshold, draftSafety, capKind, capFixed, capTarget,
            capMinimumOn, capMinimum, capFrom, capTo, startPolicy ?: current.policy.takeIf { current.history.isEmpty() }, draftCache)
    }

    private fun restoreDraft() {
        val d = drafts[scenario.number] ?: return
        draftKind = d.kind; draftHour = d.hour; draftThreshold = d.threshold; draftSafety = d.safety
        capKind = d.capKind; capFixed = d.capFixed; capTarget = d.capTarget; capMinimumOn = d.capMinimumOn
        capMinimum = d.capMinimum; capFrom = d.capFrom; capTo = d.capTo
        cacheServers = d.cache.servers
        if (d.cache.ttlMinutes.isNotEmpty()) cacheTtl = d.cache.ttlMinutes
        cacheCount = d.cache.approximateCount; cacheView = d.cache.materializedView
        // A new run starts with the rules the last run started with (the panel shows the last run's final ones).
        if (d.startPolicy != null && current.history.isEmpty() && !hasSprints) {
            current.policy = d.startPolicy
            current.applyPolicy()
        }
    }

    // Placing VMs (scenario 5)

    /** The VM the player has picked up to move, if any. */
    var selectedVm by mutableStateOf<Int?>(null)
        private set
    var placementMessage by mutableStateOf<String?>(null)
        private set

    /** The host of every VM, in the placement scenario. */
    val placement: List<Int>? get() = (simulation.policy.capacity as? CapacityMode.Placed)?.hosts

    /** VMs can be moved until the day starts. */
    val canPlace: Boolean get() = simulation.history.isEmpty() && placement != null

    /** Picks a VM up for dragging (always selects it). */
    fun pickVm(vm: Int) {
        if (!canPlace) return
        selectedVm = vm
        placementMessage = null
    }

    fun dropVm() {
        selectedVm = null
    }

    fun selectVm(vm: Int) {
        if (!canPlace) return
        selectedVm = if (selectedVm == vm) null else vm
        placementMessage = null
    }

    /** Moves the picked-up VM to a host, if its RAM fits there. */
    fun moveSelectedTo(host: Int) {
        val vm = selectedVm ?: return
        val p = placement ?: return
        if (!canPlace) return
        if (p[vm] == host) { selectedVm = null; return }
        val kind = scenario.vms[vm]
        val free = simulation.servers[host].kind.ramGb - Packing.ramUsed(scenario.vms, p, host)
        if (kind.ramGb > free) {
            placementMessage = t("%@ needs %lld GB – host %lld has only %lld GB free.", t(kind.title), kind.ramGb, host + 1, free)
            return
        }
        setPolicy(Policy(CapacityMode.Placed(p.toMutableList().also { it[vm] = host })))
        selectedVm = null
        placementMessage = null
    }

    fun spreadAgain() {
        if (!canPlace) return
        setPolicy(scenario.initialPolicy)
        selectedVm = null
        placementMessage = null
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
        if (scenario.caching != null) {
            fun run(c: CacheConfig) = Simulation.replay(scenario, scenario.initialPolicy.copy(cache = c)).totals
            val allowed = CacheConfig(1, listOf(1_440, 15, 1, 0, 0))
            return listOf(
                t("No cache (reference)") to reference.totals,
                t("Everything allowed, search too, 4 cache servers (the team's plan)") to run(allowed.copy(servers = 4, ttlMinutes = listOf(1_440, 15, 1, 60, 0))),
                t("As allowed, without search, 2 cache servers") to run(allowed.copy(servers = 2)),
                t("As allowed, without search, 1 cache server") to run(allowed),
                t("The same with HyperLogLog and materialized view") to run(allowed.copy(approximateCount = true, materializedView = true)),
            )
        }
        if (hasSprints) {
            fun run(plans: List<List<Int>>) = Simulation.replay(scenario, scenario.initialPolicy, changes = Engineering.changes(scenario, plans)).totals
            if (scenario.growth != null) {
                val backlog = scenario.backlog
                return listOf(
                    t("The team changes nothing (reference)") to reference.totals,
                    t("The biggest saving today first") to run(Engineering.greedy(scenario) { i, _, _, _ -> backlog[i].effect }),
                    t("By today's saving per story point") to run(Engineering.greedy(scenario, value = Engineering.byToday(scenario))),
                    t("By the saving in a year per story point") to run(Engineering.greedy(scenario, value = Engineering.byLastDay(scenario))),
                )
            }
            return listOf(
                t("The team changes nothing (reference)") to reference.totals,
                t("By the team's estimates") to run(Engineering.greedy(scenario, value = Engineering.byEstimates(scenario))),
                t("Profiling first, then by the measurements") to run(Engineering.greedy(scenario, profileFirst = true, value = Engineering.byEstimates(scenario))),
            )
        }
        if (Control.PLACEMENT in scenario.controls) {
            val vms = scenario.vms
            val host = scenario.fleet.first().kind
            val fleet = scenario.serverCount
            val both = { k: VmKind -> k.ramGb * 1_000 / host.ramGb + k.peak * 1_000 / host.capacity }
            fun replay(p: List<Int>) = Simulation.replay(scenario, Policy(CapacityMode.Placed(p))).totals
            return listOf(
                t("Spread over all hosts (reference)") to reference.totals,
                t("First Fit Decreasing, RAM only") to replay(Packing.firstFitDecreasing(vms, host, fleet, Packing.Check.RAM_ONLY) { it.ramGb }),
                t("First Fit Decreasing, sum of the peaks") to replay(Packing.firstFitDecreasing(vms, host, fleet, Packing.Check.PEAK_SUM) { it.ramGb }),
                t("First Fit Decreasing, daily profiles up to 85 %") to replay(Packing.firstFitDecreasing(vms, host, fleet, Packing.Check.PROFILES, key = both)),
                t("First Fit Decreasing, daily profiles up to 100 %") to replay(Packing.firstFitDecreasing(vms, host, fleet, Packing.Check.PROFILES, 1_000, both)),
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
        startPolicy = null
        team = Team(scenario.backlog, scenario.sprintPoints)
        sprintDraft = emptyList()
        sprintStartedDay = -1
        sprintPlans.clear()
        if (hasSprints) prefillSprint()
        stars = 0
        showEnd = false
        urgentJob = null
        forecastNews = false
        warnedJobs.clear()
        changed()
    }
}
