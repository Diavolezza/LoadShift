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
        if (simulation.isFinished) finish() else checkUrgentJobs()
        changed()
    }

    /** Runs a number of ticks at once (launch options). */
    fun advance(ticks: Int) {
        repeat(ticks) { if (!simulation.isFinished) simulation.step() }
        if (simulation.isFinished) finish()
        changed()
    }

    private fun finish() {
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
        changed()
    }

    // Servers by hand

    val wantedServers: Int
        get() = (simulation.policy.capacity as? CapacityMode.Manual)?.servers
            ?: simulation.servers.count { it.state.isPowered }

    fun setWantedServers(n: Int) {
        setPolicy(simulation.policy.copy(capacity = CapacityMode.Manual(n.coerceIn(0, scenario.serverCount))))
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
        speed = Speed.PAUSED
        current = Simulation(scenario)
        stars = 0
        showEnd = false
        urgentJob = null
        warnedJobs.clear()
        changed()
    }
}
