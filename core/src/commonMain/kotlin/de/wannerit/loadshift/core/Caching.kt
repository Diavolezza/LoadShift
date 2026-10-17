package de.wannerit.loadshift.core

import kotlin.math.ln
import kotlin.math.roundToInt

/*
 * E3, caching (docs/CONCEPT.md 9.2; Green IT LE06): kinds of data the web shop serves, each with a share of the
 * requests, a cost when it is not in the cache, a number of items of a size, and how old an answer may be. A cache
 * keeps the most popular items (Zipf: a small hot set gets most of the requests); a time to live (TTL) limits how long
 * an entry counts – longer saves more, but an answer older than allowed is a stale answer.
 */

data class CacheKind(
    val title: String,
    /** Share of the requests, per mille. */
    val share: Int,
    /** Work per request without the cache, thousandths of a work unit per 1,000 requests. */
    val cost: Int,
    /** Number of items and size of one, KB. */
    val items: Int,
    val itemKb: Int,
    /** How old an answer may be (business requirement), minutes; 0 = must be current. */
    val allowedMinutes: Int,
)

/** The player's caching settings: cache size, TTL per kind (0 = not cached), approximate counting, materialized view. */
data class CacheConfig(
    val sizeGb: Int = 0,
    val ttlMinutes: List<Int> = emptyList(),
    val approximateCount: Boolean = false,
    val materializedView: Boolean = false,
) {
    val anyCached: Boolean get() = sizeGb > 0 && ttlMinutes.any { it > 0 }
}

data class CacheModel(
    val kinds: List<CacheKind>,
    /** Exact count of unique visitors, per request (thousandths of a unit per 1,000 requests); with HyperLogLog. */
    val exactCount: Int,
    val approximateCount: Int,
    /** The sales dashboard, recomputed every 15 minutes: work units per tick; with a materialized view. */
    val report: Int,
    val reportWithView: Int,
    /** RAM per cache server, GB; idle power of a cache server, W; RAM power, mW per GB (3 W per 8 GB). */
    val gbPerServer: Int = 64,
    val serverPower: Int = 156,
    val ramMilliwattPerGb: Int = 375,
    /** Hit rate of a cached item that is served from the cache, work per request left, per mille of the cost. */
    val hitCost: Int = 20,
) {
    /** Cache servers for a size. */
    fun servers(config: CacheConfig): Int = if (!config.anyCached) 0 else (config.sizeGb + gbPerServer - 1) / gbPerServer

    /** Power of the cache servers, W. */
    fun power(config: CacheConfig): Int =
        if (!config.anyCached) 0 else servers(config) * serverPower + config.sizeGb * ramMilliwattPerGb / 1_000

    /** Size of all items of a kind, GB. */
    private fun fullGb(k: Int): Double = kinds[k].items.toDouble() * kinds[k].itemKb / 1e6

    /**
     * The space each cached kind gets: kinds that are small against the cache keep all their items, the others
     * share the rest in proportion to the size of all their items.
     */
    private fun space(config: CacheConfig): List<Double> {
        val cached = kinds.indices.filter { config.ttlMinutes.getOrElse(it) { 0 } > 0 }
        val small = cached.filter { fullGb(it) <= config.sizeGb * 0.01 }
        val rest = config.sizeGb - small.sumOf { fullGb(it) }
        val large = cached - small.toSet()
        val full = large.sumOf { fullGb(it) }
        return kinds.indices.map { k ->
            when {
                k in small -> fullGb(k)
                k in large && full > 0.0 -> minOf(fullGb(k), rest * fullGb(k) / full)
                else -> 0.0
            }
        }
    }

    /**
     * Hit rate of each kind, per mille, at a load (requests per tick, units of 1,000). Item i is asked for in
     * proportion to 1/i (Zipf); the cache keeps the m most popular items. An entry lives for the TTL T, so item i
     * hits with r_i·T / (r_i·T + 1), r_i its requests per minute. Summed over the items kept:
     * hit = (H(m) − H(m + a) + H(a)) / H(n) with a = R·T / H(n), R the requests per minute to the kind, H the
     * harmonic number. More memory never lowers the hit rate; a longer TTL raises it; less load lowers it.
     */
    fun hitRates(config: CacheConfig, requests: Int): List<Int> {
        val space = space(config)
        return kinds.mapIndexed { k, kind ->
            val ttl = config.ttlMinutes.getOrElse(k) { 0 }
            if (ttl <= 0 || space[k] <= 0.0) return@mapIndexed 0
            val n = kind.items.toDouble()
            val m = maxOf(1.0, minOf(n, space[k] * 1e6 / kind.itemKb))
            val perMinute = requests * 1_000.0 * kind.share / 1_000 / 15
            val a = perMinute * ttl / harmonic(n)
            val hit = (harmonic(m) - harmonic(m + a) + harmonic(a)) / harmonic(n)
            (hit.coerceIn(0.0, 1.0) * 1_000).roundToInt()
        }
    }

    /** Harmonic number H(x) ≈ ln x + γ + 1/(2x), H(0) = 0. */
    private fun harmonic(x: Double): Double = if (x < 1.0) x else ln(x) + 0.5772156649 + 1.0 / (2 * x)

    /** Work per 1,000 requests at a load, thousandths of a unit. */
    fun work(config: CacheConfig, requests: Int): Int {
        val hits = hitRates(config, requests)
        val count = if (config.approximateCount) approximateCount else exactCount
        return count + kinds.indices.sumOf { k ->
            val cost = kinds[k].cost
            kinds[k].share * (cost - (cost - cost * hitCost / 1_000) * hits[k] / 1_000) / 1_000
        }
    }

    /** Stale answers per 1,000 requests: hits older than allowed (TTL above the allowed age). */
    fun stale(config: CacheConfig, requests: Int): Int {
        val hits = hitRates(config, requests)
        return kinds.indices.sumOf { k ->
            val ttl = config.ttlMinutes.getOrElse(k) { 0 }
            if (ttl <= kinds[k].allowedMinutes) 0
            else kinds[k].share * hits[k] / 1_000 * (ttl - kinds[k].allowedMinutes) / ttl
        }
    }

    fun extraWork(config: CacheConfig): Int = if (config.materializedView) reportWithView else report

    companion object {
        /** TTL choices, minutes. */
        val TTLS = listOf(1, 15, 60, 1_440)
        /** Cache sizes, GB. */
        val SIZES = listOf(8, 16, 32, 64, 128, 256)
    }
}
