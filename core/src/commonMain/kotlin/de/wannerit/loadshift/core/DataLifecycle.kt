package de.wannerit.loadshift.core

import kotlin.math.roundToLong

/*
 * Engineering track, "Keep everything?" (docs/CONCEPT.md 9.2): an online retailer's data – orders and invoices,
 * product images, customer accounts, logs, backups, click events. Today the data stays on the fast SSD storage for a
 * year, then on the HDD storage, and nothing is deleted. Per kind of data a storage plan for data older than 90 days
 * (hot SSD, warm HDD, cold archive) and how long it is kept; requirements from law, privacy and the departments set
 * the limits. Counted for a year in the steady state: twelve years of history, location-based, with the making of
 * the storage media.
 */

enum class StorageTier { HOT, WARM, COLD }

/** Where data lives by its age: tier until that many years (null: from then on). */
data class TierPlan(val id: String, val title: String, val segments: List<Pair<StorageTier, Double?>>) {
    fun tierAt(age: Double): StorageTier = segments.first { it.second == null || age < it.second!! }.first
}

/** How long data is kept: parts of it (share) for so many years (null: for good). */
data class KeepPlan(val id: String, val title: String, val parts: List<Pair<Double?, Double>>)

/** A requirement: this share of the data kept this many years (null: all years), readable within a second while kept if fast. */
data class DataRequirement(val text: String, val years: Double?, val share: Double = 1.0, val fast: Boolean = false)

data class DataKind(
    val name: String,
    val description: String,
    /** New data a year, TB (older than 90 days). */
    val yearlyTb: Double,
    val keepOptions: List<KeepPlan>,
    val requirements: List<DataRequirement> = emptyList(),
    /** Privacy: nothing of it older than this, years (null: no limit). */
    val maxYears: Double? = null,
    val maxText: String = "",
)

data class DataPlan(val tiers: Map<String, String> = emptyMap(), val keep: Map<String, String> = emptyMap())

data class DataMiss(val kind: String, val text: String)

data class DataResult(
    val referenceKg: Long,
    val planKg: Long,
    /** TB per tier, all kinds and the recent data together. */
    val tb: Map<StorageTier, Double>,
    val perKind: Map<String, Map<StorageTier, Double>>,
    val missed: List<DataMiss>,
) {
    val reductionPermille: Long get() = (referenceKg - planKg) * 1_000 / referenceKg
}

data class DataLifecycleModel(
    val kinds: List<DataKind>,
    val tierOptions: List<TierPlan>,
    /** Years of history the retailer has. */
    val historyYears: Double = 12.0,
    /** The live databases and the images of the current range, always hot, TB. */
    val recentTb: Double = 100.0,
    /** kg CO₂ per TB and year: power of the storage with its copies, and the making of the media spread over their life. */
    val kgPerTbYear: Map<StorageTier, Double> = mapOf(StorageTier.HOT to 40.0, StorageTier.WARM to 10.0, StorageTier.COLD to 3.0),
    /** Reading archived data takes about this long, hours. */
    val coldHours: Int = 12,
    val reference: DataPlan = DataPlan(),
    val example: DataPlan = DataPlan(),
) {
    fun tier(p: DataPlan, k: DataKind): TierPlan = tierOptions.first { it.id == (p.tiers[k.name] ?: reference.tiers[k.name]) }
    fun keep(p: DataPlan, k: DataKind): KeepPlan = k.keepOptions.first { it.id == (p.keep[k.name] ?: reference.keep[k.name]) }

    private fun years(y: Double?) = minOf(y ?: historyYears, historyYears)

    /** TB per tier of one kind in the steady state. */
    fun stored(p: DataPlan, k: DataKind): Map<StorageTier, Double> {
        val out = StorageTier.entries.associateWith { 0.0 }.toMutableMap()
        val tiers = tier(p, k)
        for ((kept, share) in keep(p, k).parts) {
            val y = years(kept)
            var from = 0.0
            for ((t, until) in tiers.segments) {
                val to = minOf(until ?: y, y)
                if (to > from) { out[t] = out.getValue(t) + k.yearlyTb * share * (to - from); from = to }
            }
        }
        return out
    }

    fun misses(p: DataPlan): List<DataMiss> {
        val out = mutableListOf<DataMiss>()
        for (k in kinds) {
            val parts = keep(p, k).parts
            val tiers = tier(p, k)
            for (r in k.requirements) {
                val need = years(r.years)
                val kept = parts.any { (y, share) -> years(y) >= need - 1e-9 && share >= r.share - 1e-9 }
                // Fast: hot or warm for every age the data must be kept.
                var fast = true
                var start = 0.0
                for ((t, until) in tiers.segments) {
                    if (r.fast && t == StorageTier.COLD && start < need - 1e-9) fast = false
                    start = until ?: Double.MAX_VALUE
                }
                if (!kept || !fast) out += DataMiss(k.name, r.text)
            }
            if (k.maxYears != null && parts.any { (y, _) -> years(y) > k.maxYears + 1e-9 }) out += DataMiss(k.name, k.maxText)
        }
        return out
    }

    private fun kg(p: DataPlan): Double {
        var kg = recentTb * kgPerTbYear.getValue(StorageTier.HOT)
        for (k in kinds) for ((t, tb) in stored(p, k)) kg += tb * kgPerTbYear.getValue(t)
        return kg
    }

    fun result(p: DataPlan): DataResult {
        val perKind = kinds.associate { it.name to stored(p, it) }
        val tb = StorageTier.entries.associateWith { t -> perKind.values.sumOf { it.getValue(t) } + if (t == StorageTier.HOT) recentTb else 0.0 }
        return DataResult(kg(reference).roundToLong(), kg(p).roundToLong(), tb, perKind, misses(p))
    }

    fun goalsMet(r: DataResult, goals: StarGoals): GoalsMet {
        val rules = r.missed.isEmpty()
        val special = (goals.special as? SpecialGoal.Reduction)?.permille ?: 1_000
        return GoalsMet(rules, rules && r.reductionPermille >= goals.reduction, rules && r.reductionPermille >= special)
    }

    fun stars(r: DataResult, goals: StarGoals): Int {
        val met = goalsMet(r, goals)
        if (!met.rules) return 0
        return 1 + (if (met.reduction) 1 else 0) + (if (met.special) 1 else 0)
    }
}
