package de.wannerit.loadshift.core

/*
 * The engineering track (docs/CONCEPT.md 9.2): a backlog of improvements and a team with a fixed capacity per sprint.
 * Each item takes away a share of the CPU work per request. The team estimates the shares; profiling measures them.
 */

/** Where the CPU time per request goes (measured by profiling), per mille. */
data class HotSpot(val title: String, val share: Int)

data class BacklogItem(
    val title: String,
    /** Story points. */
    val effort: Int,
    /** The team's estimate of the work it saves, per mille of the original work per request. */
    val estimate: Int,
    /** What it really saves, per mille (the profile shows it). */
    val effect: Int,
    /** The hot spot it works on (index into the scenario's hot spots), if any. */
    val hotSpot: Int? = null,
    /** Profiling: saves nothing, but measures where the time goes. */
    val profiling: Boolean = false,
)

/** A sprint plan per day of the scenario: one game day stands for the week after a sprint. */
object Engineering {
    /** Work per request after the items done, per mille of the original. */
    fun work(backlog: List<BacklogItem>, done: Set<Int>): Int = 1_000 - done.sumOf { backlog[it].effect }

    /**
     * A team that picks by value per story point: by the estimates until profiling has been done, by the measured
     * effects after it. [profileFirst]: profiling goes into the first sprint. Returns the items per sprint.
     */
    fun greedy(backlog: List<BacklogItem>, points: Int, sprints: Int, profileFirst: Boolean): List<Set<Int>> {
        val done = mutableSetOf<Int>()
        var measured = false
        val plans = mutableListOf<Set<Int>>()
        for (sprint in 0 until sprints) {
            val plan = mutableSetOf<Int>()
            var left = points
            if (profileFirst && sprint == 0) {
                val p = backlog.indexOfFirst { it.profiling }
                if (p >= 0) { plan += p; left -= backlog[p].effort }
            }
            val value = { i: Int -> (if (measured) backlog[i].effect else backlog[i].estimate) * 1_000 / backlog[i].effort }
            for (i in backlog.indices.filter { it !in done && it !in plan && !backlog[it].profiling }.sortedByDescending(value)) {
                if (backlog[i].effort <= left) { plan += i; left -= backlog[i].effort }
            }
            done += plan
            if (plan.any { backlog[it].profiling }) measured = true
            plans += plan
        }
        return plans
    }

    /** Policies per day for a list of sprint plans (a sprint's items count from its day on). */
    fun changes(scenario: Scenario, plans: List<Set<Int>>): Map<Int, Policy> {
        val done = mutableSetOf<Int>()
        return plans.mapIndexed { day, plan ->
            done += plan
            GameTime.at(day, 0).tick to scenario.initialPolicy.copy(workPermille = work(scenario.backlog, done))
        }.toMap()
    }
}
