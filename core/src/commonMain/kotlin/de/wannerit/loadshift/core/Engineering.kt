package de.wannerit.loadshift.core

import kotlin.math.ln
import kotlin.math.roundToInt

/*
 * The engineering track (docs/CONCEPT.md 9.2): a backlog of improvements and a team with a fixed capacity per sprint.
 * E2: each item takes away a share of the CPU work per request; the team estimates the shares, profiling measures
 * them. E3: the data grows every month, and parts of the work grow with it – by their algorithmic complexity.
 */

/** Where the CPU time per request goes (measured by profiling), per mille. */
data class HotSpot(val title: String, val share: Int)

data class BacklogItem(
    val title: String,
    /** Story points. */
    val effort: Int,
    /** The team's estimate of the work it saves, per mille of the original work per request (E2). */
    val estimate: Int = 0,
    /** What it really saves, per mille: E2 of the work per request, E3 of the constant part. */
    val effect: Int = 0,
    /** The hot spot it works on (E2), if any. */
    val hotSpot: Int? = null,
    /** Profiling: saves nothing, but measures where the time goes. */
    val profiling: Boolean = false,
    /** E3: the growing part it fixes (index into the growth model's parts), if any. */
    val fixes: Int? = null,
)

/** How a part of the work grows with the amount of data n. */
enum class Complexity(val label: String) {
    CONSTANT("O(1)"), LOGARITHMIC("O(log n)"), LINEAR("O(n)"), N_LOG_N("O(n log n)"), QUADRATIC("O(n²)");

    /** Factor for n (per mille, n ≥ 1,000), per mille: 1,000 at n = 1. */

    fun factor(n: Int): Int {
        val x = n / 1_000.0
        val f = when (this) {
            CONSTANT -> 1.0
            LOGARITHMIC -> 1.0 + ln(x)
            LINEAR -> x
            N_LOG_N -> x * (1.0 + ln(x))
            QUADRATIC -> x * x
        }
        return (f * 1_000).roundToInt()
    }
}

/**
 * A part of the work that grows with the data: per request (per mille of the original work per request at n = 1),
 * or – [night] – a nightly job (work units per tick from 01:00 to 05:00 at n = 1).
 */
data class GrowthPart(
    val title: String, val base: Int, val from: Complexity,
    /** After the fix: the new complexity and base. */
    val to: Complexity, val toBase: Int, val night: Boolean = false,
)

/**
 * E3: the data grows by [ratePermille] every game day (one day stands for a month). The work per request is a
 * constant part plus parts that grow; the backlog items fix parts (a better algorithm) or cut the constant part.
 */
data class GrowthModel(val ratePermille: Int, val constant: Int, val parts: List<GrowthPart>) {
    /** Amount of data on a game day, per mille of the start. */
    fun n(day: Int): Int {
        var n = 1_000L
        repeat(day) { n = n * (1_000 + ratePermille) / 1_000 }
        return n.toInt()
    }

    private fun complexity(part: Int, fixed: Set<Int>) = if (part in fixed) parts[part].to else parts[part].from
    private fun base(part: Int, fixed: Set<Int>) = if (part in fixed) parts[part].toBase else parts[part].base

    /** Work per request on a day, per mille of the original. */
    fun perRequest(day: Int, backlog: List<BacklogItem>, done: Set<Int>): Int {
        val fixed = done.mapNotNull { backlog[it].fixes }.toSet()
        val constantNow = constant - done.sumOf { backlog[it].effect }
        val n = n(day)
        return constantNow + parts.indices.filter { !parts[it].night }.sumOf { base(it, fixed) * complexity(it, fixed).factor(n) / 1_000 }
    }

    /** Work of the nightly jobs at a tick, work units. */
    fun night(tick: Int, backlog: List<BacklogItem>, done: Set<Int>): Int {
        if (GameTime(tick).hour !in NIGHT_HOURS) return 0
        val fixed = done.mapNotNull { backlog[it].fixes }.toSet()
        val n = n((tick - GameTime.START.tick) / GameTime.TICKS_PER_DAY)
        return parts.indices.filter { parts[it].night }.sumOf { base(it, fixed) * complexity(it, fixed).factor(n) / 1_000 }
    }

    /**
     * The day's work by part, work units (for the profile): the constant part, each growing part; [demand] is
     * the expected requests per tick.
     */
    fun profile(day: Int, backlog: List<BacklogItem>, done: Set<Int>, demand: (Int) -> Int): List<Pair<String, Long>> {
        val fixed = done.mapNotNull { backlog[it].fixes }.toSet()
        val n = n(day)
        val start = GameTime.at(day, 0).tick
        val ticks = start until start + GameTime.TICKS_PER_DAY
        val requests = ticks.sumOf { demand(it).toLong() }
        val nightTicks = ticks.count { GameTime(it).hour in NIGHT_HOURS }
        val constantNow = constant - done.sumOf { backlog[it].effect }
        return listOf("" to requests * constantNow / 1_000) + parts.indices.map { i ->
            val f = base(i, fixed).toLong() * complexity(i, fixed).factor(n) / 1_000
            parts[i].title to (if (parts[i].night) f * nightTicks else requests * f / 1_000)
        }
    }

    companion object {
        /** The nightly jobs run from 01:00 to 05:00. */
        val NIGHT_HOURS = 1 until 5
    }
}

/**
 * A team working through the backlog sprint by sprint: items started and not finished continue first (carry-over),
 * then the plan in its order. An item takes effect from the day of the sprint it is finished in.
 */
class Team(private val backlog: List<BacklogItem>, val points: Int) {
    private val remaining = IntArray(backlog.size) { backlog[it].effort }

    /** Items finished, with the sprint they were finished in. */
    val done = mutableMapOf<Int, Int>()

    fun remaining(item: Int): Int = remaining[item]
    fun isStarted(item: Int): Boolean = item !in done && remaining[item] < backlog[item].effort
    val started: List<Int> get() = backlog.indices.filter { isStarted(it) }

    /** The order the team works in: started items first, then the plan. */
    fun order(plan: List<Int>): List<Int> = started + plan.filter { it !in done && !isStarted(it) }

    /** Runs a sprint; returns the items finished in it. */
    fun sprint(index: Int, plan: List<Int>): List<Int> {
        var left = points
        val finished = mutableListOf<Int>()
        for (i in order(plan)) {
            if (left <= 0) break
            val work = minOf(left, remaining[i])
            remaining[i] -= work
            left -= work
            if (remaining[i] == 0) { done[i] = index; finished += i }
        }
        return finished
    }

    fun copy(): Team = Team(backlog, points).also { t ->
        remaining.copyInto(t.remaining)
        t.done.putAll(done)
    }
}

object Engineering {
    /** E2: work per request after the items done, per mille of the original. */
    fun work(backlog: List<BacklogItem>, done: Set<Int>): Int = 1_000 - done.sumOf { backlog[it].effect }

    /**
     * A team that picks by [value] per story point the items that fit into the sprint; an item bigger than a
     * sprint is started in an otherwise empty sprint and carries over.
     * [profileFirst]: profiling goes into the first sprint (E2); after profiling the measured effects count.
     */
    fun greedy(
        scenario: Scenario, profileFirst: Boolean = false,
        value: (item: Int, sprint: Int, done: Set<Int>, measured: Boolean) -> Int,
    ): List<List<Int>> {
        val backlog = scenario.backlog
        val team = Team(backlog, scenario.sprintPoints)
        var measured = false
        val plans = mutableListOf<List<Int>>()
        for (sprint in 0 until scenario.days) {
            val plan = mutableListOf<Int>()
            var left = scenario.sprintPoints - team.started.sumOf { team.remaining(it) }
            if (profileFirst && sprint == 0) {
                val p = backlog.indexOfFirst { it.profiling }
                if (p >= 0) { plan += p; left -= backlog[p].effort }
            }
            val open = backlog.indices.filter { it !in team.done && !team.isStarted(it) && it !in plan && !backlog[it].profiling }
            for (i in open.sortedByDescending { value(it, sprint, team.done.keys, measured) * 1_000 / backlog[it].effort }) {
                if (backlog[i].effort <= left) { plan += i; left -= backlog[i].effort }
            }
            // A big item that fits no sprint is started when a sprint has nothing else left (it carries over).
            if (left == scenario.sprintPoints && open.isNotEmpty() && plan.isEmpty()) {
                plan += open.maxBy { value(it, sprint, team.done.keys, measured) * 1_000 / backlog[it].effort }
            }
            team.sprint(sprint, plan)
            if (team.done.keys.any { backlog[it].profiling }) measured = true
            plans += plan
        }
        return plans
    }

    /** Policies per day for a list of sprint plans: the items finished by a sprint count from its day on. */
    fun changes(scenario: Scenario, plans: List<List<Int>>): Map<Int, Policy> {
        val team = Team(scenario.backlog, scenario.sprintPoints)
        return plans.mapIndexed { day, plan ->
            team.sprint(day, plan)
            GameTime.at(day, 0).tick to scenario.initialPolicy.copy(done = team.done.keys.toSet())
        }.toMap()
    }

    /** E2: the team's estimates until profiling has been done, the measured effects after it. */
    fun byEstimates(scenario: Scenario): (Int, Int, Set<Int>, Boolean) -> Int =
        { i, _, _, measured -> if (measured) scenario.backlog[i].effect else scenario.backlog[i].estimate }

    /** E3: what an item saves on the day of the sprint ("measured today"). */
    fun byToday(scenario: Scenario): (Int, Int, Set<Int>, Boolean) -> Int =
        { i, sprint, done, _ -> (dailyWork(scenario, sprint, done) - dailyWork(scenario, sprint, done + i)).toInt() }

    /** E3: what an item will save on the last day (a team that thinks of the growth). */
    fun byLastDay(scenario: Scenario): (Int, Int, Set<Int>, Boolean) -> Int =
        { i, _, done, _ -> (dailyWork(scenario, scenario.days - 1, done) - dailyWork(scenario, scenario.days - 1, done + i)).toInt() }

    /** E3: the day's expected work (units) with the items done – to show what an item saves "today" or later. */
    fun dailyWork(scenario: Scenario, day: Int, done: Set<Int>): Long {
        val growth = scenario.growth ?: return 0
        val start = GameTime.at(day, 0).tick
        val perRequest = growth.perRequest(day, scenario.backlog, done)
        return (start until start + GameTime.TICKS_PER_DAY).sumOf { t ->
            scenario.interactive.expected(t).toLong() * perRequest / 1_000 + growth.night(t, scenario.backlog, done)
        }
    }
}
