package de.wannerit.loadshift.core

/** The player's rules (docs/CONCEPT.md, section 2: policies, not clicks): capacity and batch work. */
sealed interface CapacityMode {
    /** Every server runs all the time (the reference operator of scenario 1). */
    data object AllOn : CapacityMode

    /** A fixed number of servers for interactive work runs; the most efficient ones are used first. */
    data class Manual(val servers: Int) : CapacityMode

    /**
     * The simulation keeps enough servers on to meet the expected demand of the next hour – or the demand of the
     * last tick, if that was higher – at the target utilization (per mille, 400..900). [minimum]: at least that
     * many servers in a time window every day (scheduled scaling).
     */
    data class Autoscale(val targetUtilization: Int, val minimum: ScheduledMinimum? = null) : CapacityMode

    /** Scenario 5: the host of every VM (index into the fleet); hosts with a VM run, the others are off. */
    data class Placed(val hosts: List<Int>) : CapacityMode
}

/** At least [servers] servers every day from [fromHour] to [toHour] (exclusive), whatever the autoscaler thinks. */
data class ScheduledMinimum(val servers: Int, val fromHour: Int, val toHour: Int) {
    fun activeAt(tick: Int): Boolean = GameTime(tick).hour.let { it >= fromHour && it < toHour }
}

/** When batch jobs start (CONCEPT.md 5.2). */
sealed interface BatchMode {
    /** The player plans every job by hand (start time per job); unplanned jobs wait. */
    data object ByHand : BatchMode

    /** Every job starts as soon as it has arrived and servers are free (the reference operator). */
    data object Asap : BatchMode

    /**
     * Every job starts at the next given full hour after its arrival ("nightly batch at 01:00"); with the deadline
     * safety at its latest start if that comes first.
     */
    data class AtHour(val hour: Int) : BatchMode

    /**
     * Every job is planned into the hours with the lowest forecast carbon intensity before its deadline, taking the
     * free servers into account; plans are made again whenever a job arrives.
     */
    data object Greenest : BatchMode

    /**
     * A job starts as soon as the carbon intensity of the grid is at or below the threshold (g/kWh) – the value
     * now, not the forecast; with the deadline safety at its latest start at the latest.
     */
    data class Threshold(val gramsPerKWh: Int) : BatchMode

    /** A rule that the simulation follows (everything but planning by hand). */
    val isRule: Boolean get() = this != ByHand
}

/**
 * The rules for capacity and batch work. [deadlineSafety] (CONCEPT.md 5.2): whatever the batch rule, a job starts
 * when waiting longer would miss its deadline (fixed hour and threshold; the other rules never wait that long).
 */
data class Policy(
    val capacity: CapacityMode,
    val batch: BatchMode = BatchMode.Asap,
    val deadlineSafety: Boolean = true,
    /** Engineering track: the backlog items finished (they change the work per request). */
    val done: Set<Int> = emptySet(),
    /** E4: what is cached, for how long, and in how much memory. */
    val cache: CacheConfig = CacheConfig(),
    /** Scenario 6: the inlet temperature the cooling keeps, °C. */
    val setpoint: Int = 20,
    /** O7: new servers installed at the site so far. */
    val renewed: Int = 0,
    /** O7, the reference operator: buys new servers whenever the coming quarter would be short of capacity. */
    val buyWhenShort: Boolean = false,
    /** O8: how the training runs. */
    val training: TrainingConfig = TrainingConfig(),
    /** O9: qualities and eco mode of the streaming service. */
    val streaming: StreamingConfig = StreamingConfig(),
    /** E10: who answers each kind of question, and the batching window. */
    val inference: InferenceConfig = InferenceConfig(),
    /** E5: how heavy the shop's pages are. */
    val pages: PageConfig = PageConfig(),
    /** E11: the CI/CD pipeline and the test environments. */
    val pipeline: PipelineConfig = PipelineConfig(),
    /** E7: how the logistics system's services are cut and talk to each other. */
    val services: ServiceConfig = ServiceConfig(),
    /** B12: the electricity contract. */
    val contract: Contract = Contract.GREY,
) {
    companion object {
        /**
         * What the reference operator does unless a scenario says otherwise: all servers on, batch jobs as soon as
         * possible (CONCEPT.md 8.2).
         */
        val REFERENCE = Policy(CapacityMode.AllOn, BatchMode.Asap)

        /** Allowed range of the target utilization, per mille. */
        val TARGET_RANGE = 400..900

        /** Thresholds the player can choose for batch work, g/kWh. */
        val THRESHOLD_RANGE = 50..400

        /** Ticks the autoscaler looks ahead beyond the boot time: one hour. */
        const val LOOKAHEAD = GameTime.TICKS_PER_HOUR
    }
}
