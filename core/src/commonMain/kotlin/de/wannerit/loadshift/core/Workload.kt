package de.wannerit.loadshift.core

import kotlin.math.pow
import kotlin.math.roundToInt

/** Workloads (docs/CONCEPT.md, section 5). */
enum class WorkloadKind(val title: String) {
    INTERACTIVE("Interactive"), BATCH("Batch"), AI_TRAINING("AI training"), STREAMING("Streaming"),
}

/**
 * Demand of interactive work (web shop, app backend): a daily curve with an evening peak, scaled to the
 * scenario's peak, with a small random noise per tick. One work unit = 1,000 requests.
 */
/**
 * A marketing campaign: from [start] to [end] (ticks) the demand is [factor] per mille of the usual. It is not in
 * the expected demand – a planner that only knows the usual day does not see it coming.
 */
data class Campaign(val start: Int, val end: Int, val factor: Int)

data class InteractiveLoad(
    /** Work units per tick at the evening peak (curve value 1,000, before noise). */
    val peak: Int,
    /** Half width of the random noise per tick, per mille. */
    val noise: Int = 50,
    val campaigns: List<Campaign> = emptyList(),
    /** O7: the demand grows by this much a year, per mille; [daysPerYear] game days make a year. */
    val growthPerYear: Int = 0,
    val daysPerYear: Int = 4,
) {
    /** Growth of the demand on a day, per mille of the first day (compound, a step per game day). */
    fun growth(day: Int): Int =
        if (growthPerYear == 0) 1_000 else (1_000 * (1 + growthPerYear / 1_000.0).pow(day.toDouble() / daysPerYear)).roundToInt()

    /** Expected demand at a tick, without noise (what the operator can plan with). */
    fun expected(tick: Int): Int {
        val perHour = GameTime.TICKS_PER_HOUR
        val inDay = tick % GameTime.TICKS_PER_DAY
        val hour = inDay / perHour
        val r = inDay % perHour
        val share = (DAILY_CURVE[hour] * (perHour - r) + DAILY_CURVE[(hour + 1) % 24] * r) / perHour
        val base = peak * share / 1_000
        return if (growthPerYear == 0) base else base * growth(tick / GameTime.TICKS_PER_DAY) / 1_000
    }

    /** Actual demand at a tick: the expected demand with the noise of that tick, times a running campaign. */
    fun actual(tick: Int, seed: Long): Int {
        val e = SeededRandom.symmetric(seed, 10, tick, noise)
        val campaign = campaigns.firstOrNull { tick >= it.start && tick < it.end }?.factor ?: 1_000
        return maxOf(0, expected(tick) * (1_000 + e) / 1_000 * campaign / 1_000)
    }

    companion object {
        /**
         * Share of the peak at the full hours, per mille (game values: night about 20 % of the evening peak,
         * morning rise, a small bump at lunch, peak at 20:00).
         */
        val DAILY_CURVE = listOf(
            380, 300, 240, 210, 200, 220, 300, 420, 540, 620, 660, 690,
            720, 700, 670, 660, 680, 730, 800, 900, 1_000, 960, 780, 560,
        )

        /**
         * Share of the capacity up to which requests are served in time, per mille: above it they are slow and
         * count only half towards the SLA (simple latency model, game value).
         */
        const val SLOW_ABOVE = 850
    }
}
