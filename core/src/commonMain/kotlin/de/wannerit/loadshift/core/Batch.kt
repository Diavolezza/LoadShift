package de.wannerit.loadshift.core

/*
 * Batch work (docs/CONCEPT.md 5.2): jobs with a size, an arrival time and a deadline. A job needs a number of
 * servers for a number of ticks; once started it runs to the end. Its servers are switched on when it starts and
 * boot first, so a job takes boot time plus its duration.
 */

data class BatchJobSpec(
    val name: String,
    /** Servers the job needs at the same time. */
    val servers: Int,
    /** Work time in ticks (after booting). */
    val duration: Int,
    /** Tick from which the job can start. */
    val arrival: Int,
    /** The job must be finished at this tick at the latest. */
    val deadline: Int,
) {
    /** The last tick at which the job can be started and still meet its deadline (boot time included). */
    val latestStart: Int get() = deadline - duration - ServerKind.BOOT_TICKS

    /** Ticks from start to finish. */
    val span: Int get() = ServerKind.BOOT_TICKS + duration

    companion object {
        fun of(name: String, servers: Int, hours: Int, arrival: GameTime, deadline: GameTime) =
            BatchJobSpec(name, servers, hours * GameTime.TICKS_PER_HOUR, arrival.tick, deadline.tick)
    }
}

sealed interface BatchJobState {
    /** Not planned yet. */
    data object Waiting : BatchJobState

    /** Planned to start at the tick (waits there if no servers are free). */
    data class Planned(val start: Int) : BatchJobState

    /** Started at the tick; its servers boot, the work begins at [workFrom]. */
    data class Started(val at: Int, val workFrom: Int) : BatchJobState

    /** Working; finished at [endsAt]. */
    data class Running(val startedAt: Int, val endsAt: Int) : BatchJobState

    /** Finished at the tick. */
    data class Done(val startedAt: Int, val at: Int) : BatchJobState

    val isStarted: Boolean get() = this is Started || this is Running || this is Done
    val isDone: Boolean get() = this is Done
}

data class BatchJob(val id: Int, val spec: BatchJobSpec, val state: BatchJobState = BatchJobState.Waiting) {
    /** Tick at which the job started (switched on its servers), if it has. */
    val startedAt: Int?
        get() = when (state) {
            is BatchJobState.Started -> state.at
            is BatchJobState.Running -> state.startedAt
            is BatchJobState.Done -> state.startedAt
            else -> null
        }

    /** Tick at which the job finished, if it has. */
    val finishedAt: Int? get() = (state as? BatchJobState.Done)?.at

    /** Planned start, if planned and not started yet. */
    val plannedStart: Int? get() = (state as? BatchJobState.Planned)?.start

    /** The job finished after its deadline, or has not finished and the deadline has passed. */
    fun missedDeadline(at: Int): Boolean = finishedAt?.let { it > spec.deadline } ?: (at >= spec.deadline)
}

/** Planning help: start ticks with the lowest forecast carbon intensity (CONCEPT.md 5.2, "greenest hours"). */
object BatchPlanner {
    /** Mean forecast intensity (tenths of g/kWh) over the work time of a job started at [start]. */
    fun meanForecast(spec: BatchJobSpec, start: Int, grid: Grid, madeAt: Int): Int {
        val from = start + ServerKind.BOOT_TICKS
        val sum = (from until from + spec.duration).sumOf { grid.forecastTenths(it, madeAt) }
        return sum / maxOf(1, spec.duration)
    }

    /**
     * Plans jobs one after another into the start with the lowest mean forecast intensity before their latest
     * start, so that at no tick more servers are used than [servers]. [busy] gives servers already taken per tick
     * (running jobs). Jobs that fit nowhere get their latest start.
     */
    fun plan(specs: List<Pair<Int, BatchJobSpec>>, servers: Int, busy: Map<Int, Int>, grid: Grid, madeAt: Int): Map<Int, Int> {
        val used = busy.toMutableMap()
        val result = mutableMapOf<Int, Int>()
        // Tightest deadlines first.
        for ((id, spec) in specs.sortedBy { it.second.latestStart }) {
            val earliest = maxOf(madeAt, spec.arrival)
            val latest = maxOf(earliest, spec.latestStart)
            var bestStart: Int? = null
            var bestMean = Int.MAX_VALUE
            for (start in earliest..latest) {
                val fits = (start until start + spec.span).all { (used[it] ?: 0) + spec.servers <= servers }
                if (!fits) continue
                val mean = meanForecast(spec, start, grid, madeAt)
                if (bestStart == null || mean < bestMean) {
                    bestStart = start
                    bestMean = mean
                }
            }
            val start = bestStart ?: latest
            result[id] = start
            for (t in start until start + spec.span) used[t] = (used[t] ?: 0) + spec.servers
        }
        return result
    }
}
