package de.wannerit.loadshift.core

/**
 * Game time. One tick is 15 game minutes, one game day 96 ticks (docs/CONCEPT.md, section 3.1).
 * Tick 0 is 00:00 on day 0; every scenario starts there (decision D10).
 */
data class GameTime(val tick: Int) : Comparable<GameTime> {
    init {
        require(tick >= 0) { "game time cannot be negative" }
    }

    /** Day number, starting with 0. */
    val day: Int get() = tick / TICKS_PER_DAY

    /** Tick within the day, 0 until 96. */
    val tickOfDay: Int get() = tick % TICKS_PER_DAY
    val hour: Int get() = tickOfDay / TICKS_PER_HOUR
    val minute: Int get() = (tickOfDay % TICKS_PER_HOUR) * MINUTES_PER_TICK

    /** "14:15" */
    val clockText: String get() = hour.toString().padStart(2, '0') + ":" + minute.toString().padStart(2, '0')

    fun advanced(by: Int) = GameTime(tick + by)

    override fun compareTo(other: GameTime) = tick.compareTo(other.tick)

    companion object {
        const val MINUTES_PER_TICK = 15
        const val TICKS_PER_HOUR = 60 / MINUTES_PER_TICK
        const val TICKS_PER_DAY = 24 * TICKS_PER_HOUR

        /** Tick at which a scenario starts: 00:00 on day 0. */
        val START = GameTime(0)

        fun at(day: Int, hour: Int, minute: Int = 0): GameTime {
            require(hour in 0..23 && minute in 0..59 && minute % MINUTES_PER_TICK == 0)
            return GameTime(day * TICKS_PER_DAY + hour * TICKS_PER_HOUR + minute / MINUTES_PER_TICK)
        }
    }
}
