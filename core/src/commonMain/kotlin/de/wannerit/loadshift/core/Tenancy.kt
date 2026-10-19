package de.wannerit.loadshift.core

import kotlin.math.ceil
import kotlin.math.roundToLong
import kotlin.math.sqrt

/*
 * Engineering track, "One per customer?" (docs/CONCEPT.md 9.2): an HR software as a service for 40 customer
 * companies, today each with its own instance of every building block (silo). Per building block and group of
 * customers a tenancy model: each customer its own instance, a shared application with one database per customer on
 * shared servers (bridge, databases only), or everything shared, the customers told apart by a tenant ID (pool).
 * An instance needs servers for the peak of its customers – peaks of many customers rarely come together – at 70 %,
 * at least a minimum for availability, plus one server's worth for its load balancer, monitoring and backups.
 * Three quality scenarios set the limits. Counted for a year, location-based.
 */

enum class Tenancy { OWN, BRIDGE, SHARED }

data class TenantGroup(val name: String, val description: String, val count: Int)

data class TenancyComponent(
    val name: String,
    val description: String,
    /** Peak load per customer of each group, in servers at full load. */
    val peaks: Map<String, Double>,
    /** Average load as a share of the peak. */
    val averageShare: Double,
    /** Servers an instance needs at least (availability). */
    val minServers: Int,
    val options: List<Tenancy>,
    /** The regulated customers' contract covers this building block (their data). */
    val contract: Boolean = false,
    /** The large customer's month-end payroll run loads this building block. */
    val monthEnd: Boolean = false,
    /** A single customer's data must be restorable on its own (databases). */
    val restore: Boolean = false,
)

data class TenancyPlan(val choices: Map<String, Map<String, Tenancy>> = emptyMap())

enum class TenancyRule { CONTRACT, MONTH_END, RESTORE }

data class TenancyMiss(val rule: TenancyRule, val component: String)

data class TenancyResult(
    val referenceKg: Long,
    val planKg: Long,
    val referenceServers: Int,
    val servers: Int,
    /** Servers per building block. */
    val perComponent: Map<String, Int>,
    val missed: List<TenancyMiss>,
) {
    val reductionPermille: Long get() = (referenceKg - planKg) * 1_000 / referenceKg
    fun broken(rule: TenancyRule) = missed.any { it.rule == rule }
}

data class TenancyModel(
    val groups: List<TenantGroup>,
    val components: List<TenancyComponent>,
    /** The group whose contract asks for its own servers, and the group with the month-end run. */
    val regulated: String,
    val large: String,
    val targetPercent: Int = 70,
    val idleWatts: Int = 150,
    /** Extra watts of a fully busy server. */
    val loadWatts: Int = 250,
    /** Load balancer, monitoring and backups of an instance, servers. */
    val overheadServers: Int = 1,
    /** A database of its own on a shared server, extra load per customer. */
    val bridgeExtra: Double = 0.1,
    val pueHundredths: Int = 130,
    val gramsPerKwh: Int = 380,
    val reference: TenancyPlan = TenancyPlan(),
    val example: TenancyPlan = TenancyPlan(),
) {
    fun choice(p: TenancyPlan, c: TenancyComponent, g: TenantGroup): Tenancy = p.choices[c.name]?.get(g.name) ?: Tenancy.OWN

    /** How much the peaks of n customers add up: one customer 100 %, many about 60 % of the sum. */
    fun diversity(n: Int): Double = if (n <= 1) 1.0 else 0.6 + 0.4 / sqrt(n.toDouble())

    private fun instanceServers(c: TenancyComponent, peaks: List<Double>): Int {
        val peak = peaks.sum() * diversity(peaks.size)
        return maxOf(c.minServers, ceil(peak * 100 / targetPercent - 1e-9).toInt()) + overheadServers
    }

    fun servers(p: TenancyPlan, c: TenancyComponent): Int {
        var n = 0
        val shared = mutableListOf<Double>()
        val bridged = mutableListOf<Double>()
        for (g in groups) {
            val peak = c.peaks.getValue(g.name)
            when (choice(p, c, g)) {
                Tenancy.OWN -> n += g.count * instanceServers(c, listOf(peak))
                Tenancy.SHARED -> repeat(g.count) { shared += peak }
                Tenancy.BRIDGE -> repeat(g.count) { bridged += peak + bridgeExtra }
            }
        }
        if (shared.isNotEmpty()) n += instanceServers(c, shared)
        if (bridged.isNotEmpty()) n += instanceServers(c, bridged)
        return n
    }

    fun misses(p: TenancyPlan): List<TenancyMiss> {
        val out = mutableListOf<TenancyMiss>()
        val reg = groups.first { it.name == regulated }
        val big = groups.first { it.name == large }
        for (c in components) {
            if (c.contract && choice(p, c, reg) != Tenancy.OWN) out += TenancyMiss(TenancyRule.CONTRACT, c.name)
            if (c.monthEnd && choice(p, c, big) != Tenancy.OWN &&
                groups.any { it != big && choice(p, c, it) == choice(p, c, big) }) out += TenancyMiss(TenancyRule.MONTH_END, c.name)
            if (c.restore && groups.any { choice(p, c, it) == Tenancy.SHARED }) out += TenancyMiss(TenancyRule.RESTORE, c.name)
        }
        return out
    }

    private fun kg(p: TenancyPlan): Double {
        var servers = 0
        var load = 0.0
        for (c in components) {
            servers += servers(p, c)
            for (g in groups) load += g.count * c.peaks.getValue(g.name) * c.averageShare
        }
        val kwh = (servers * idleWatts + load * loadWatts) * 8_760 / 1_000.0 * pueHundredths / 100
        return kwh * gramsPerKwh / 1_000
    }

    fun result(p: TenancyPlan) = TenancyResult(
        referenceKg = kg(reference).roundToLong(),
        planKg = kg(p).roundToLong(),
        referenceServers = components.sumOf { servers(reference, it) },
        servers = components.sumOf { servers(p, it) },
        perComponent = components.associate { it.name to servers(p, it) },
        missed = misses(p),
    )

    fun goalsMet(r: TenancyResult, goals: StarGoals): GoalsMet {
        val rules = r.missed.isEmpty()
        val special = (goals.special as? SpecialGoal.Reduction)?.permille ?: 1_000
        return GoalsMet(rules, rules && r.reductionPermille >= goals.reduction, rules && r.reductionPermille >= special)
    }

    fun stars(r: TenancyResult, goals: StarGoals): Int {
        val met = goalsMet(r, goals)
        if (!met.rules) return 0
        return 1 + (if (met.reduction) 1 else 0) + (if (met.special) 1 else 0)
    }
}
