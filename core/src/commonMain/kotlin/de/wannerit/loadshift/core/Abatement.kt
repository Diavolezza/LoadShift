package de.wannerit.loadshift.core

/*
 * Portfolio track, P2 "Where to start" (docs/CONCEPT.md 9.3): a consultant's list of measures as a marginal abatement
 * cost curve – euros per tonne of CO₂ avoided, from each measure's saving per year. The curve ranks the measures one
 * by one; what it leaves out decides the real result: measures on the same servers do not add up, a long project saves
 * only once it is done, new hardware is made with CO₂, and certificates change the report, not the grid. Counted over
 * three years in quarters, location-based.
 */

/** One measure of the consultant's list. */
data class AbatementMeasure(
    val title: String,
    /** What it works on, and the facts the list gives. */
    val application: String,
    val facts: String,
    val costEuro: Int,
    /** The consultant's number: CO₂ it saves a year once done, kg. */
    val grossKgPerYear: Long,
    /** Quarters until it works. */
    val leadQuarters: Int,
    /** The emissions it works on (a key of [AbatementModel.targets]) and how much of them it removes, percent. */
    val target: String? = null,
    val effectPercent: Int = 0,
    /** Made once: the CO₂ of new hardware, kg. */
    val embodiedKg: Long = 0,
    /** Clean energy produced on site and used there, kg CO₂ a year it saves once done. */
    val onSiteKgPerYear: Long = 0,
)

data class AbatementResult(val baselineKg: Long, val savedKg: Long, val costEuro: Int) {
    val reductionPermille: Long get() = savedKg * 1_000 / baselineKg
}

data class AbatementModel(
    val measures: List<AbatementMeasure>,
    /** The emissions the measures work on, kg a year. */
    val targets: Map<String, Long>,
    /** The whole footprint, kg a year. */
    val footprintKgPerYear: Long,
    val budget: Int,
    val quarters: Int = 12,
) {
    val years: Int get() = quarters / 4
    val baselineKg: Long get() = footprintKgPerYear * quarters / 4

    /** The consultant's curve: € per tonne avoided over the horizon, from the gross saving a year. */
    fun consultantEuroPerTonne(i: Int): Long = measures[i].let { it.costEuro * 1_000L / maxOf(1L, it.grossKgPerYear * years) }
    fun consultantKg(i: Int): Long = measures[i].grossKgPerYear * years
    val consultantOrder: List<Int> get() = measures.indices.sortedBy { consultantEuroPerTonne(it) }

    /** CO₂ the plan really saves over the horizon, kg: per quarter and target what is left after all measures that work by then. */
    fun savedKg(plan: Set<Int>): Long {
        var saved = 0.0
        for ((target, perYear) in targets) {
            val ms = plan.filter { measures[it].target == target }
            for (q in 0 until quarters) {
                var left = 1.0
                for (m in ms) if (q >= measures[m].leadQuarters) left *= 1.0 - measures[m].effectPercent / 100.0
                saved += perYear / 4.0 * (1.0 - left)
            }
        }
        for (m in plan) {
            saved -= measures[m].embodiedKg
            saved += measures[m].onSiteKgPerYear / 4.0 * maxOf(0, quarters - measures[m].leadQuarters)
        }
        return kotlin.math.round(saved).toLong()
    }

    fun cost(plan: Set<Int>): Int = plan.sumOf { measures[it].costEuro }
    fun result(plan: Set<Int>) = AbatementResult(baselineKg, savedKg(plan), cost(plan))

    fun goalsMet(r: AbatementResult, goals: StarGoals): GoalsMet {
        val rules = r.costEuro <= budget && r.savedKg > 0
        val special = (goals.special as? SpecialGoal.Reduction)?.permille ?: 1_000
        return GoalsMet(rules, rules && r.reductionPermille >= goals.reduction, rules && r.reductionPermille >= special)
    }

    fun stars(r: AbatementResult, goals: StarGoals): Int {
        val met = goalsMet(r, goals)
        if (!met.rules) return 0
        return 1 + (if (met.reduction) 1 else 0) + (if (met.special) 1 else 0)
    }

    /** The consultant's way: along the curve from the left, as long as the budget lasts. */
    fun alongTheCurve(): Set<Int> {
        var left = budget
        val plan = mutableSetOf<Int>()
        for (i in consultantOrder) if (measures[i].costEuro <= left) { plan += i; left -= measures[i].costEuro }
        return plan
    }

    /** The best plan within the budget (all subsets – the list is short). */
    fun best(): Set<Int> {
        var best = emptySet<Int>()
        var bestKg = Long.MIN_VALUE
        for (mask in 0 until (1 shl measures.size)) {
            val plan = measures.indices.filter { mask shr it and 1 == 1 }.toSet()
            if (cost(plan) > budget) continue
            val kg = savedKg(plan)
            if (kg > bestKg) { bestKg = kg; best = plan }
        }
        return best
    }

    fun indexOf(title: String): Int = measures.indexOfFirst { it.title == title }
}
