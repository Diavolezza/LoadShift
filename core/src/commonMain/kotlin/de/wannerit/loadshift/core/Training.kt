package de.wannerit.loadshift.core

/*
 * Scenario O8, an AI training run (docs/CONCEPT.md 5.3, 9.1): a model needs a fixed amount of GPU work by a
 * deadline. The player chooses how many GPU servers train in parallel (more are faster, but each one does less:
 * they wait for each other), a power cap (less power per GPU, a little slower – MIT Lincoln Laboratory measured
 * 12–15 % less energy for about 3 % more time at a cap of 150 W [S7]) and when to train: continuously or in the
 * greenest hours of the forecast. Pausing is possible at a checkpoint; resuming costs a quarter of an hour of
 * loading without progress.
 */

/** The player's training settings. */
data class TrainingConfig(
    /** GPU servers that train in parallel. */
    val servers: Int = 8,
    /** Power cap, per mille of the maximum power. */
    val cap: Int = 1_000,
    /** Train in the greenest hours of the forecast (else continuously from now on). */
    val greenest: Boolean = false,
)

data class TrainingModel(
    /** The work, thousandths of the work one server does in an hour at full speed. */
    val work: Int,
    /** Share of the GPU power drawn while training without a cap, per mille (GPUs rarely reach their maximum). */
    val uncappedDraw: Int = 900,
    /** Throughput lost per additional server (they wait for each other), per mille. */
    val scalingLoss: Int = 40,
    /** Ticks without progress after a pause (loading the checkpoint). */
    val restartTicks: Int = 1,
) {
    /** Power drawn by the GPUs at a cap, per mille of the dynamic range. */
    fun draw(cap: Int): Int = minOf(cap, uncappedDraw)

    /** Speed of one server at a cap, per mille of full speed (game values after [S7]). */
    fun speed(cap: Int): Int {
        val points = listOf(600 to 870, 700 to 930, 800 to 970, 900 to 1_000, 1_000 to 1_000)
        val c = cap.coerceIn(600, 1_000)
        val (a, b) = points.zipWithNext().first { c <= it.second.first }
        return a.second + (b.second - a.second) * (c - a.first) / (b.first - a.first)
    }

    /** Work of all servers in a tick, thousandths of a server-hour. */
    fun perTick(config: TrainingConfig): Int {
        val n = config.servers.coerceAtLeast(1)
        val efficiency = 1_000 - scalingLoss * (n - 1)
        return (n.toLong() * efficiency * speed(config.cap) / 1_000 / GameTime.TICKS_PER_HOUR).toInt()
    }

    companion object {
        val CAPS = listOf(1_000, 900, 800, 700, 600)
        val SERVERS = 2..8
    }
}
