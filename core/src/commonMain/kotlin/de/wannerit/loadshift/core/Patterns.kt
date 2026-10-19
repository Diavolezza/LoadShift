package de.wannerit.loadshift.core

import kotlin.math.roundToLong

/*
 * Engineering track, E12 "One pattern for everything?" (docs/CONCEPT.md 9.2): a transport association's mobility
 * platform, built with the same modern patterns everywhere. Per subsystem one architecture pattern – Layers, CQRS,
 * Event Sourcing, Publish/Subscribe, Request/Reply, Lambda, Kappa, Pipes and Filters as services or as a batch. Each
 * option needs servers and storage (and, for the notifications, energy on the phones and in the network); a batch
 * can run in the greenest hours. Quality scenarios set the limits. Counted for a year, location-based.
 */

data class PatternOption(
    val id: String,
    val title: String,
    val text: String,
    /** Servers on, on average over the year. */
    val servers: Double,
    val storageTb: Double = 0.0,
    /** Phones and network, kg CO₂ a year. */
    val otherKg: Long = 0,
    /** Runs as a batch in the greenest hours. */
    val shifted: Boolean = false,
    /** Quality scenarios this option does not meet. */
    val fails: Set<String> = emptySet(),
)

data class Subsystem(val name: String, val description: String, val options: List<PatternOption>)

data class PatternQuality(val id: String, val name: String, val text: String)

data class PatternPlan(val choices: Map<String, String> = emptyMap())

data class PatternResult(
    val referenceKg: Long,
    val planKg: Long,
    val servers: Double,
    val storageTb: Double,
    val perSubsystemKg: Map<String, Long>,
    /** Quality scenarios not met. */
    val missed: List<String>,
) {
    val reductionPermille: Long get() = (referenceKg - planKg) * 1_000 / referenceKg
}

data class PatternModel(
    val subsystems: List<Subsystem>,
    val qualities: List<PatternQuality>,
    /** A server for a year: kWh, PUE, grid. */
    val serverKwh: Int = 1_750,
    val pueHundredths: Int = 130,
    val gramsPerKwh: Int = 380,
    /** The greenest hours: CO₂ per kWh compared with the mean, per mille. */
    val greenestPermille: Int = 600,
    /** A TB of storage for a year, kg CO₂ (power with copies, making of the media). */
    val kgPerTb: Int = 40,
    val reference: PatternPlan = PatternPlan(),
    val example: PatternPlan = PatternPlan(),
) {
    val serverKg: Double get() = serverKwh * pueHundredths / 100.0 * gramsPerKwh / 1_000

    fun option(p: PatternPlan, s: Subsystem): PatternOption =
        s.options.first { it.id == (p.choices[s.name] ?: reference.choices.getValue(s.name)) }

    fun kg(o: PatternOption): Double =
        o.servers * serverKg * (if (o.shifted) greenestPermille / 1_000.0 else 1.0) + o.storageTb * kgPerTb + o.otherKg

    private fun total(p: PatternPlan) = subsystems.sumOf { kg(option(p, it)) }

    fun result(p: PatternPlan) = PatternResult(
        referenceKg = total(reference).roundToLong(),
        planKg = total(p).roundToLong(),
        servers = subsystems.sumOf { option(p, it).servers },
        storageTb = subsystems.sumOf { option(p, it).storageTb },
        perSubsystemKg = subsystems.associate { it.name to kg(option(p, it)).roundToLong() },
        missed = qualities.map { it.id }.filter { q -> subsystems.any { q in option(p, it).fails } },
    )

    fun goalsMet(r: PatternResult, goals: StarGoals): GoalsMet {
        val rules = r.missed.isEmpty()
        val special = (goals.special as? SpecialGoal.Reduction)?.permille ?: 1_000
        return GoalsMet(rules, rules && r.reductionPermille >= goals.reduction, rules && r.reductionPermille >= special)
    }

    fun stars(r: PatternResult, goals: StarGoals): Int {
        val met = goalsMet(r, goals)
        if (!met.rules) return 0
        return 1 + (if (met.reduction) 1 else 0) + (if (met.special) 1 else 0)
    }
}
