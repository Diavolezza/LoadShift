package de.wannerit.loadshift.core

/*
 * Portfolio track, P3 "Tidy up with TIME" (docs/CONCEPT.md 9.3): the applications by business value and technical
 * condition, and per application one decision – tolerate, invest, migrate, eliminate – with a start quarter. The team
 * runs at most three projects at a time. A migration runs the new system next to the old one; the old one is off only
 * when it is switched off. Counted for three years in quarters, location-based.
 */

enum class Decision { TOLERATE, INVEST, MIGRATE, ELIMINATE }

/** What a decision does: how many quarters the project takes and what is left of the footprint afterwards, percent. */
data class Option(val title: String, val quarters: Int, val leftPercent: Int)

data class TidyApp(
    val name: String,
    val description: String,
    val highValue: Boolean,
    val goodCondition: Boolean,
    /** Place in the matrix: technical condition (x) and business value (y), per mille. */
    val x: Int,
    val y: Int,
    val kgPerYear: Long,
    val invest: Option,
    val migrate: Option,
    /** Quarters to move the users and data elsewhere and switch it off. */
    val eliminateQuarters: Int,
    /** Why it must stay, whatever its value (null: it may go if its value is low). */
    val mustKeep: String? = null,
)

/** The player's choice for an application. */
data class Choice(val decision: Decision = Decision.TOLERATE, val start: Int = 0, val decommission: Boolean = false)

data class TidyResult(
    val baselineKg: Long,
    val planKg: Long,
    /** Quarters with more projects than the team can run. */
    val overloaded: List<Int>,
    /** Applications that may not be eliminated, eliminated anyway. */
    val wronglyEliminated: List<String>,
    /** Old systems still running after their migration. */
    val stillRunning: List<String>,
) {
    val reductionPermille: Long get() = (baselineKg - planKg) * 1_000 / baselineKg
}

data class TidyModel(
    val apps: List<TidyApp>,
    val quarters: Int = 12,
    /** Projects the team runs at a time. */
    val capacity: Int = 3,
    /** A good plan (the debrief's "what if"; the tests check its stars). */
    val example: Map<String, Choice> = emptyMap(),
) {
    val baselineKg: Long get() = apps.sumOf { it.kgPerYear } * quarters / 4

    /** Quarters a choice keeps the team busy (a migration with switching off the old system one more). */
    fun projectQuarters(app: TidyApp, c: Choice): Int = when (c.decision) {
        Decision.TOLERATE -> 0
        Decision.INVEST -> app.invest.quarters
        Decision.MIGRATE -> app.migrate.quarters + if (c.decommission) 1 else 0
        Decision.ELIMINATE -> app.eliminateQuarters
    }

    /** The footprint of an application in a quarter, kg a year. */
    fun rate(app: TidyApp, c: Choice, q: Int): Double {
        val t = app.kgPerYear.toDouble()
        return when (c.decision) {
            Decision.TOLERATE -> t
            Decision.INVEST -> if (q >= c.start + app.invest.quarters) t * app.invest.leftPercent / 100 else t
            Decision.MIGRATE -> {
                val newOne = t * app.migrate.leftPercent / 100
                val done = c.start + app.migrate.quarters
                when {
                    q < c.start -> t
                    q < done -> t + newOne // both run
                    c.decommission && q >= done + 1 -> newOne
                    else -> t + newOne
                }
            }
            Decision.ELIMINATE -> if (q >= c.start + app.eliminateQuarters) 0.0 else t
        }
    }

    /** The whole portfolio per quarter, kg a year. */
    fun byQuarter(plan: Map<String, Choice>): List<Double> =
        (0 until quarters).map { q -> apps.sumOf { rate(it, plan[it.name] ?: Choice(), q) } }

    /** Projects running in each quarter. */
    fun load(plan: Map<String, Choice>): IntArray {
        val l = IntArray(quarters)
        for (app in apps) {
            val c = plan[app.name] ?: continue
            for (q in c.start until minOf(quarters, c.start + projectQuarters(app, c))) l[q]++
        }
        return l
    }

    fun mayEliminate(app: TidyApp) = !app.highValue && app.mustKeep == null

    fun result(plan: Map<String, Choice>) = TidyResult(
        baselineKg = baselineKg,
        planKg = kotlin.math.round(byQuarter(plan).sum() / 4).toLong(),
        overloaded = load(plan).withIndex().filter { it.value > capacity }.map { it.index },
        wronglyEliminated = apps.filter { plan[it.name]?.decision == Decision.ELIMINATE && !mayEliminate(it) }.map { it.name },
        stillRunning = apps.filter { plan[it.name]?.let { c -> c.decision == Decision.MIGRATE && !c.decommission } == true }.map { it.name },
    )

    fun goalsMet(r: TidyResult, goals: StarGoals): GoalsMet {
        val rules = r.overloaded.isEmpty() && r.wronglyEliminated.isEmpty() && r.planKg < r.baselineKg
        val special = (goals.special as? SpecialGoal.Reduction)?.permille ?: 1_000
        return GoalsMet(rules, rules && r.reductionPermille >= goals.reduction, rules && r.reductionPermille >= special)
    }

    fun stars(r: TidyResult, goals: StarGoals): Int {
        val met = goalsMet(r, goals)
        if (!met.rules) return 0
        return 1 + (if (met.reduction) 1 else 0) + (if (met.special) 1 else 0)
    }

    /** The decision the quadrant suggests: invest where good and valuable, migrate where poor and valuable, eliminate where poor and of little value. */
    fun quadrant(app: TidyApp): Decision = when {
        app.highValue && app.goodCondition -> Decision.INVEST
        app.highValue -> Decision.MIGRATE
        !app.goodCondition && mayEliminate(app) -> Decision.ELIMINATE
        else -> Decision.TOLERATE
    }

    /** The quadrants' decisions, each started as early as the team can, in the order of the list. */
    fun byQuadrant(decommission: Boolean): Map<String, Choice> {
        val plan = mutableMapOf<String, Choice>()
        val l = IntArray(quarters)
        for (app in apps) {
            val d = quadrant(app)
            if (d == Decision.TOLERATE) continue
            val c0 = Choice(d, 0, decommission)
            val n = projectQuarters(app, c0)
            val s = (0 until quarters).firstOrNull { s -> (s until minOf(quarters, s + n)).all { l[it] < capacity } } ?: continue
            for (q in s until minOf(quarters, s + n)) l[q]++
            plan[app.name] = c0.copy(start = s)
        }
        return plan
    }
}
