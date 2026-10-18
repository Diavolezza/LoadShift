package de.wannerit.loadshift.core

import kotlin.math.ceil
import kotlin.math.pow

/*
 * E6, "Right-sized AI" (docs/CONCEPT.md 9.2): a shop assistant answers the customers' questions. The team's prototype
 * sends every question to a large language model, one at a time. The player chooses per kind of question who answers
 * it – no AI at all (a status page, a form, a canned reply), a small model, the large model, or the large model with
 * reasoning – and how long the servers may wait to answer several questions together (batching).
 *
 * Game values in the range of the literature: Google measured 0.24 Wh for the median Gemini text prompt, with idle
 * machines and overhead [S25]; Epoch AI estimates about 0.3 Wh for a typical GPT-4o answer, reasoning models write
 * about 2.5 times as many tokens [S26]; serving with batching cut the energy per request of an 8B model by 12.5 to 100
 * times compared with answering one request at a time [S27].
 */

/** Who answers a kind of question: energy per answer without batching (mWh) and the time to an answer (tenths of s). */
enum class Answerer(val label: String, val milliWattHours: Int, val tenths: Int, val gpu: Boolean) {
    NONE("Without AI", 2, 3, false),
    SMALL("Small model", 120, 8, true),
    LARGE("Large model", 1_000, 20, true),
    REASONING("Large model with reasoning", 2_500, 90, true),
}

/** A kind of question: its share of all questions, the quality of the answers per answerer (per mille), a note. */
data class QuestionKind(val title: String, val share: Int, val quality: List<Int>, val note: String)

/** The player's settings: who answers each kind, and how long the servers wait to fill a batch (ms). */
data class InferenceConfig(val answerers: List<Answerer> = emptyList(), val windowMs: Int = 0)

data class InferenceModel(
    val kinds: List<QuestionKind>,
    /** Questions per second at the evening peak; the rest of the day follows the shop's daily curve. */
    val peakRate: Int,
    /** Mean quality of the answers the shop must keep, per mille. */
    val qualityTarget: Int,
    /** Share of the answers that must come within [limitTenths], per mille. */
    val onTimeTarget: Int,
    val limitTenths: Int = 30,
    /** Questions in one batch at most. */
    val maxBatch: Int = 32,
    /** GPU servers available for the models. */
    val gpuServers: Int,
) {
    fun answerer(config: InferenceConfig, kind: Int): Answerer = config.answerers.getOrElse(kind) { Answerer.LARGE }

    /** Questions per second at a point of the daily curve (per mille of the peak). */
    fun rate(curvePermille: Int): Double = peakRate * curvePermille / 1_000.0

    /**
     * Energy per answer at a batch size, share of the energy without batching (game values after [S27]): the GPU
     * reads the model's weights once per step for the whole batch, so a fuller batch shares that cost.
     */
    fun batchFactor(batch: Double): Double = 0.1 + 0.9 / batch.pow(0.8)

    /** Mean batch size of an answerer: the questions that arrive while the servers wait, at most [maxBatch]. */
    fun batchSize(config: InferenceConfig, answerer: Answerer, curvePermille: Int): Double {
        if (!answerer.gpu || config.windowMs == 0) return 1.0
        val perSecond = kinds.indices.filter { answerer(config, it) == answerer }.sumOf { rate(curvePermille) * kinds[it].share / 1_000.0 }
        return minOf(maxBatch.toDouble(), 1.0 + perSecond * config.windowMs / 1_000.0)
    }

    /** IT energy of one tick per kind of question, mWh. */
    fun energy(config: InferenceConfig, curvePermille: Int): List<Double> = kinds.indices.map { k ->
        val a = answerer(config, k)
        val answers = rate(curvePermille) * kinds[k].share / 1_000.0 * SECONDS_PER_TICK
        answers * a.milliWattHours * batchFactor(batchSize(config, a, curvePermille))
    }

    /** Energy per answer of a kind at a point of the daily curve, mWh. */
    fun energyPerAnswer(config: InferenceConfig, kind: Int, curvePermille: Int): Double {
        val a = answerer(config, kind)
        return a.milliWattHours * batchFactor(batchSize(config, a, curvePermille))
    }

    /** Mean quality of the answers, per mille. */
    fun quality(config: InferenceConfig): Int = kinds.indices.sumOf { kinds[it].share * kinds[it].quality[answerer(config, it).ordinal] } / 1_000

    /** Time to an answer of a kind, tenths of a second: the answerer's time plus the wait for the batch. */
    fun timeTenths(config: InferenceConfig, kind: Int): Int {
        val a = answerer(config, kind)
        return a.tenths + if (a.gpu) config.windowMs / 100 else 0
    }

    /** Share of the answers within the limit, per mille. */
    fun onTime(config: InferenceConfig): Int = kinds.indices.filter { timeTenths(config, it) <= limitTenths }.sumOf { kinds[it].share }

    /** GPU servers needed for a power, at least one while a model answers. */
    fun servers(gpuWatts: Double): Int = if (gpuWatts <= 0.0) 0 else minOf(gpuServers, ceil(gpuWatts / SERVER_WATTS).toInt()).coerceAtLeast(1)

    companion object {
        /** Power a GPU server draws while it answers at full load, W (DGX H100 at about 90 % [S12]). */
        const val SERVER_WATTS = 9_300.0
        val WINDOWS = listOf(0, 250, 1_000, 2_000)
        const val SECONDS_PER_TICK = 3_600 / GameTime.TICKS_PER_HOUR
    }
}

/** E6: what the models drew in a tick – W per kind of question, the idle rest of the running GPU servers, the servers. */
data class InferenceRecord(val kindWatts: List<Int>, val idleWatts: Int, val servers: Int)
