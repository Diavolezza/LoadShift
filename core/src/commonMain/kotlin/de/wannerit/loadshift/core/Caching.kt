package de.wannerit.loadshift.core

import kotlin.math.ceil
import kotlin.math.ln
import kotlin.math.roundToInt

/*
 * E3, caching (docs/CONCEPT.md 9.2; Green IT LE06): kinds of data the web shop serves, each with a share of the
 * requests, a cost when it is not in the cache, a number of items of a size, how old an answer may be, and the share
 * of requests that are asked only once. All cached kinds share the memory of the cache servers. An entry stays until
 * its time to live (TTL) ends – or until the cache is full and it is the one used least recently (LRU).
 */

data class CacheKind(
    val title: String,
    /** Share of the requests, per mille. */
    val share: Int,
    /** Work per request without the cache, thousandths of a work unit per 1,000 requests. */
    val cost: Int,
    /** Number of items that are asked for again and again, and the size of one, KB. */
    val items: Int,
    val itemKb: Int,
    /** How old an answer may be (business requirement), minutes; 0 = must be current. */
    val allowedMinutes: Int,
    /** Requests that are asked only once (a search nobody else types), per mille of the kind's requests. */
    val unique: Int = 0,
    /** What the player is told about the kind (size, number, how it is built). */
    val note: String = "",
)

/** The player's caching settings: cache servers, TTL per kind (0 = not cached), approximate counting, materialized view. */
data class CacheConfig(
    val servers: Int = 0,
    val ttlMinutes: List<Int> = emptyList(),
    val approximateCount: Boolean = false,
    val materializedView: Boolean = false,
) {
    val anyCached: Boolean get() = servers > 0 && ttlMinutes.any { it > 0 }
}

/** What the cache holds at a load: per kind how long an entry stays (minutes), its hit rate (‰) and memory (GB). */
class CacheState(val stay: List<Double>, val hits: List<Int>, val memoryGb: List<Double>, val capacityGb: Int) {
    val usedGb: Double get() = memoryGb.sum()
    /** Entries are pushed out before their TTL ends. */
    val full: Boolean get() = capacityGb > 0 && usedGb >= capacityGb - 0.5
}

data class CacheModel(
    val kinds: List<CacheKind>,
    /** Work per request that no cache can save (cart, checkout, login), thousandths of a unit per 1,000 requests. */
    val other: Int,
    /** Exact count of unique visitors, per request (thousandths of a unit per 1,000 requests); with HyperLogLog. */
    val exactCount: Int,
    val approximateCount: Int,
    /** The sales dashboard, recomputed every 15 minutes: work units per tick; with a materialized view. */
    val report: Int,
    val reportWithView: Int,
    /** Memory of a cache server, GB; its power, W (156 W idle and 24 W for 64 GB of RAM – it hardly computes). */
    val gbPerServer: Int = 64,
    val serverPower: Int = 180,
    /** Work left on a hit, and extra work on a miss (look up, serialize, store), per mille of the cost. */
    val hitCost: Int = 20,
    val missCost: Int = 80,
) {
    /** Cache servers that run. */
    fun servers(config: CacheConfig): Int = if (config.anyCached) config.servers else 0

    /** Power of the cache servers, W. */
    fun power(config: CacheConfig): Int = servers(config) * serverPower

    /**
     * Memory and hit rate of a kind when an entry stays T minutes, at R requests per minute to it. Item i is asked
     * for in proportion to 1/i (Zipf [S21]); it is in the cache with probability p_i = r_i·T / (r_i·T + 1) = a / (a + i)
     * with a = R·T / H(n). Summed: the cache holds a·(H(n + a) − H(a)) items, and the hit rate is
     * (H(n) − H(n + a) + H(a)) / H(n). Requests asked only once never hit, but are stored for T minutes too.
     */
    private fun kindAt(k: Int, minutes: Double, perMinute: Double): Pair<Double, Double> {
        val kind = kinds[k]
        if (minutes <= 0.0) return 0.0 to 0.0
        val n = kind.items.toDouble()
        val repeated = perMinute * (1_000 - kind.unique) / 1_000
        val a = repeated * minutes / harmonic(n)
        val items = a * (harmonic(n + a) - harmonic(a)) + perMinute * kind.unique / 1_000 * minutes
        val hit = (harmonic(n) - harmonic(n + a) + harmonic(a)) / harmonic(n) * (1_000 - kind.unique) / 1_000
        return hit.coerceIn(0.0, 1.0) to items * kind.itemKb / 1e6
    }

    /**
     * The cache at a load (requests per tick, units of 1,000). If everything fits, an entry stays for its TTL. If not,
     * the cache keeps what was used last: all entries stay the same time t (LRU, Che's approximation), or their TTL if
     * that is shorter – t is the time at which the memory is just full.
     */
    fun state(config: CacheConfig, requests: Int): CacheState {
        val capacity = servers(config) * gbPerServer
        val ttl = kinds.indices.map { if (capacity > 0) config.ttlMinutes.getOrElse(it) { 0 }.toDouble() else 0.0 }
        val perMinute = kinds.map { requests * 1_000.0 * it.share / 1_000 / 15 }
        fun memory(t: Double) = kinds.indices.sumOf { kindAt(it, minOf(ttl[it], t), perMinute[it]).second }
        var stay = ttl
        if (memory(Double.MAX_VALUE) > capacity) {
            var low = 0.0
            var high = ttl.max()
            repeat(60) {
                val mid = (low + high) / 2
                if (memory(mid) > capacity) high = mid else low = mid
            }
            stay = ttl.map { minOf(it, low) }
        }
        val each = kinds.indices.map { kindAt(it, stay[it], perMinute[it]) }
        return CacheState(stay, each.map { (it.first * 1_000).roundToInt() }, each.map { it.second }, capacity)
    }

    /** Hit rate of each kind, per mille. */
    fun hitRates(config: CacheConfig, requests: Int): List<Int> = state(config, requests).hits

    /** Harmonic number H(x) ≈ ln x + γ + 1/(2x), H(x) = x below 1. */
    private fun harmonic(x: Double): Double = if (x < 1.0) x else ln(x) + 0.5772156649 + 1.0 / (2 * x)

    /** Work per 1,000 requests at a load, thousandths of a unit. */
    fun work(config: CacheConfig, requests: Int): Int {
        val state = state(config, requests)
        val count = if (config.approximateCount) approximateCount else exactCount
        return other + count + kinds.indices.sumOf { k ->
            val cost = kinds[k].cost.toDouble()
            val per = if (state.stay[k] <= 0.0) cost else {
                val hit = state.hits[k] / 1_000.0
                hit * cost * hitCost / 1_000 + (1 - hit) * cost * (1_000 + missCost) / 1_000
            }
            (kinds[k].share * per / 1_000).roundToInt()
        }
    }

    /** Stale answers per 1,000 requests: hits on entries older than allowed (an entry that stays longer than allowed). */
    fun stale(config: CacheConfig, requests: Int): Int {
        val state = state(config, requests)
        return ceil(kinds.indices.sumOf { k ->
            val stay = state.stay[k]
            val allowed = kinds[k].allowedMinutes
            if (stay <= allowed) 0.0 else kinds[k].share * state.hits[k] / 1_000.0 * (stay - allowed) / stay
        }).toInt()
    }

    fun extraWork(config: CacheConfig): Int = if (config.materializedView) reportWithView else report

    companion object {
        /** TTL choices, minutes. */
        val TTLS = listOf(1, 15, 60, 1_440)
        /** At most this many cache servers. */
        const val MAX_SERVERS = 4
    }
}
