package de.wannerit.loadshift.core

/*
 * Portfolio track, P1 "Inventory" (docs/CONCEPT.md 9.3, D26): the footprint of a company's applications in three
 * hosting paths – the own data center, a cloud provider and SaaS – each counted from its own source of data. Better
 * data costs money from the same budget as the measures; the measures are chosen by what the data show, and they work
 * on what is really there. Everything is counted for a year, in kg CO₂e, location-based.
 */

/** Where an application runs, and so where its footprint comes from. */
enum class Hosting(val title: String) {
    ON_PREM("Own data center"), CLOUD("Cloud"), SAAS("SaaS"),
}

/** How the own data center's metered energy is split among its applications. */
enum class OnPremKey(val title: String, val costEuro: Int) {
    SERVERS("By number of servers (CMDB export)", 0),
    CPU_TIME("By CPU time (monitoring)", 5_000),
    POWER("Measured power per server (metered PDUs)", 20_000),
}

/** Where a number in the inventory comes from. */
enum class Source(val title: String, val quality: Int) {
    SERVERS("share by number of servers", 1),
    CPU_TIME("share by CPU time", 2),
    POWER("measured power", 3),
    PROVIDER("provider's tool, market-based", 1),
    RECALCULATED("recalculated from usage, location-based", 3),
    SPEND("spend-based estimate", 1),
    SUPPLIER("data from the supplier", 3),
    DECLINED("supplier sends no data – spend-based", 1),
}

/** A measure for one application: what it costs once and how much of the application's footprint it removes. */
data class Measure(val title: String, val costEuro: Int, val effectPercent: Int)

data class PortfolioApp(
    val name: String,
    val hosting: Hosting,
    val description: String,
    /** What the application costs a year (licences, cloud bill, operation), €. */
    val costPerYear: Int,
    /** The real footprint, location-based, kg CO₂e a year. */
    val trueKg: Long,
    val measure: Measure,
    /** Own data center: servers in the CMDB and the share of the monitored CPU time, per mille. */
    val servers: Int = 0,
    val cpuPermille: Int = 0,
    /** SaaS: whether the supplier answers a request for data. */
    val supplierAnswers: Boolean = true,
)

/** What the player knows: the key for the own data center (and the keys bought), the cloud recalculated, the suppliers asked. */
data class Inventory(
    val key: OnPremKey = OnPremKey.SERVERS,
    val bought: Set<OnPremKey> = setOf(OnPremKey.SERVERS),
    val recalculated: Boolean = false,
    val asked: Set<Int> = emptySet(),
)

/** The year: what the inventory said, what was really there, and what the plan changed. */
data class PortfolioResult(
    val inventoryKg: Long,
    val trueKg: Long,
    val afterKg: Long,
    val inventoryCost: Int,
    val planCost: Int,
) {
    /** How far the inventory is from the real footprint, per mille (signed). */
    val inventoryError: Long get() = (inventoryKg - trueKg) * 1_000 / trueKg
    val reductionPermille: Long get() = (trueKg - afterKg) * 1_000 / trueKg
}

data class PortfolioModel(
    val apps: List<PortfolioApp>,
    /** For data and measures together, €. */
    val budget: Int,
    /** The own data center: metered energy a year (cooling included), kWh, and the grid's mean, g/kWh. */
    val meterKWh: Long,
    val gridGrams: Int,
    /** Spend-based factor for SaaS, g CO₂e per €. */
    val spendGrams: Int = 120,
    /** The cloud provider's tool counts its certificates: this share of the location-based footprint is left, per mille. */
    val providerPermille: Int = 30,
    val recalculateCost: Int = 8_000,
    val askCost: Int = 2_000,
    /** First star: the inventory at most this far from the real footprint, per mille. */
    val inventoryLimit: Int = 150,
) {
    val onPremKg: Long get() = meterKWh * gridGrams / 1_000
    private val servers: Int get() = apps.sumOf { it.servers }

    fun source(i: Int, inv: Inventory): Source {
        val app = apps[i]
        return when (app.hosting) {
            Hosting.ON_PREM -> when (inv.key) {
                OnPremKey.SERVERS -> Source.SERVERS
                OnPremKey.CPU_TIME -> Source.CPU_TIME
                OnPremKey.POWER -> Source.POWER
            }
            Hosting.CLOUD -> if (inv.recalculated) Source.RECALCULATED else Source.PROVIDER
            Hosting.SAAS -> if (i !in inv.asked) Source.SPEND else if (app.supplierAnswers) Source.SUPPLIER else Source.DECLINED
        }
    }

    /** What the inventory shows for an application, kg CO₂e a year. */
    fun estimate(i: Int, inv: Inventory): Long {
        val app = apps[i]
        return when (source(i, inv)) {
            Source.SERVERS -> onPremKg * app.servers / servers
            Source.CPU_TIME -> onPremKg * app.cpuPermille / 1_000
            Source.POWER, Source.RECALCULATED, Source.SUPPLIER -> app.trueKg
            Source.PROVIDER -> app.trueKg * providerPermille / 1_000
            Source.SPEND, Source.DECLINED -> app.costPerYear.toLong() * spendGrams / 1_000
        }
    }

    fun inventoryKg(inv: Inventory): Long = apps.indices.sumOf { estimate(it, inv) }
    val trueKg: Long get() = apps.sumOf { it.trueKg }

    fun inventoryCost(inv: Inventory): Int =
        inv.bought.sumOf { it.costEuro } + (if (inv.recalculated) recalculateCost else 0) + inv.asked.size * askCost

    fun planCost(plan: Set<Int>): Int = plan.sumOf { apps[it].measure.costEuro }

    /** What a measure saves by the inventory's numbers, and really. */
    fun expectedSaving(i: Int, inv: Inventory): Long = estimate(i, inv) * apps[i].measure.effectPercent / 100
    fun realSaving(i: Int): Long = apps[i].trueKg * apps[i].measure.effectPercent / 100

    fun result(inv: Inventory, plan: Set<Int>) = PortfolioResult(
        inventoryKg = inventoryKg(inv), trueKg = trueKg, afterKg = trueKg - plan.sumOf { realSaving(it) },
        inventoryCost = inventoryCost(inv), planCost = planCost(plan),
    )

    fun goalsMet(r: PortfolioResult, goals: StarGoals): GoalsMet {
        val rules = r.inventoryCost + r.planCost <= budget && kotlin.math.abs(r.inventoryError) <= inventoryLimit && r.afterKg < r.trueKg
        val special = (goals.special as? SpecialGoal.Reduction)?.permille ?: 1_000
        return GoalsMet(rules, rules && r.reductionPermille >= goals.reduction, rules && r.reductionPermille >= special)
    }

    fun stars(r: PortfolioResult, goals: StarGoals): Int {
        val met = goalsMet(r, goals)
        if (!met.rules) return 0
        return 1 + (if (met.reduction) 1 else 0) + (if (met.special) 1 else 0)
    }

    /**
     * The plan a careful manager makes from an inventory: the measures with the most expected saving per euro first,
     * as long as the budget lasts.
     */
    fun greedyPlan(inv: Inventory): Set<Int> {
        var left = budget - inventoryCost(inv)
        val plan = mutableSetOf<Int>()
        for (i in apps.indices.sortedByDescending { expectedSaving(it, inv).toDouble() / apps[it].measure.costEuro }) {
            if (apps[i].measure.costEuro <= left && expectedSaving(i, inv) > 0) {
                plan += i
                left -= apps[i].measure.costEuro
            }
        }
        return plan
    }

    fun indexOf(name: String): Int = apps.indexOfFirst { it.name == name }
}
