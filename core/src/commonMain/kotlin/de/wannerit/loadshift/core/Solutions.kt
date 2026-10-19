package de.wannerit.loadshift.core

/*
 * The solution of each scenario with a clock (D31): what a careful player sets, so that the debrief can show it and
 * the app can put it into the task. LessonTest checks that every solution earns three stars.
 */

data class Solution(
    /** Rules from the start. */
    val policy: Policy? = null,
    /** Rules that change later in the run: tick → rules. */
    val changes: Map<Int, Policy> = emptyMap(),
    /** Planning again with the latest forecast at these ticks. */
    val replanAt: Set<Int> = emptySet(),
    /** Sprint plans, one list of backlog items per sprint. */
    val sprints: List<List<Int>> = emptyList(),
    /** O10: jobs moved to the other site, and the batch rule there. */
    val moved: Set<Int> = emptySet(),
    /** O7: new servers per site in total, bought by the given day. */
    val renewal: List<Map<Int, Int>> = emptyList(),
    /** Servers by hand: tick → servers on. */
    val servers: Map<Int, Int> = emptyMap(),
)

object Solutions {
    /** Servers by hand: enough for the expected load of the next 90 minutes at 80 % (changes only). */
    fun serverPlan(scenario: Scenario): Map<Int, Int> {
        val perServer = (scenario.fleet.firstOrNull()?.kind?.capacity ?: 20) * 800
        val plan = linkedMapOf<Int, Int>()
        var last = -1
        for (tick in scenario.startTick until scenario.endTick) {
            val expected = (tick..tick + ServerKind.BOOT_TICKS + GameTime.TICKS_PER_HOUR).maxOf { scenario.interactive.expected(it) }
            val n = ((expected * 1_000 + perServer - 1) / perServer).coerceAtMost(scenario.serverCount)
            if (n != last) { plan[tick] = n; last = n }
        }
        return plan
    }

    fun of(s: Scenario): Solution? {
        val c = s.controls
        val start = s.initialPolicy
        val revised = setOfNotNull(s.forecastMiss?.revisedAt)
        return when {
            Control.MANUAL_CAPACITY in c -> Solution(servers = serverPlan(s))
            Control.SPRINTS in c -> Solution(sprints =
                if (s.growth != null) Engineering.greedy(s, value = Engineering.byLastDay(s))
                else Engineering.greedy(s, profileFirst = true, value = Engineering.byEstimates(s)))
            Control.LOCATION in c -> Solution(policy = start.copy(batch = BatchMode.Greenest), moved = setOf(0, 1, 5, 6, 7, 8))
            Control.RENEWAL in c -> Solution(renewal = listOf(mapOf(0 to 4, 4 to 8, 8 to 10), mapOf(10 to 2)))
            s.contract != null -> Solution(s.referencePolicy.copy(contract = Contract.HOURLY, capacity = CapacityMode.Autoscale(800), batch = BatchMode.Greenest))
            s.services != null -> Solution(start.copy(services = ServiceConfig(ServiceCut.MICROSERVICES, Protocol.GRPC, MasterData.EVENTS, 60, 300, true)))
            s.pipeline != null -> Solution(start.copy(pipeline = PipelineConfig(Runners.ON_DEMAND, true, true, HeavyTests.NIGHTLY, Trigger.EVERY_COMMIT, Environments.OFFICE_HOURS, 1)))
            s.web != null -> Solution(start.copy(pages = PageConfig(ImageFormat.AVIF, true, LazyLoading.BELOW_FOLD, false, false, Tracking.ONE_TAG)))
            s.inference != null -> Answerer.entries.let { a -> Solution(start.copy(inference = InferenceConfig(listOf(a[0], a[0], a[1], a[2], a[2]), 1_000))) }
            c.size > 1 && s.training != null -> Solution(Policy(CapacityMode.Autoscale(800), BatchMode.Greenest, training = TrainingConfig(4, 600, true)), replanAt = revised)
            Control.STREAMING in c -> Solution(start.copy(streaming = StreamingConfig(listOf(Quality.HD, Quality.HD, Quality.UHD), eco = true, ecoAbove = 550)))
            Control.TRAINING in c -> Solution(start.copy(training = TrainingConfig(6, 600, true)))
            Control.COOLING in c -> Solution(start.copy(capacity = CapacityMode.Autoscale(800), setpoint = 25))
            Control.CACHING in c -> Solution(start.copy(cache = CacheConfig(1, listOf(1_440, 15, 1, 0, 0), approximateCount = true, materializedView = true, invalidate = true)))
            Control.PLACEMENT in c -> Solution(Policy(CapacityMode.Placed(listOf(0, 0, 1, 2, 3, 4, 4, 5, 0, 1, 2, 3, 4, 5, 0, 2, 4, 5, 1, 3, 2, 2, 5, 5))))
            Control.BATCH_BY_HAND in c || Control.BATCH_RULES in c -> Solution(Policy(start.capacity, BatchMode.Greenest), replanAt = revised)
            Control.CAPACITY_RULES in c -> {
                // The campaign is announced for day 2 only: the minimum on that evening, not every evening.
                val target = (start.capacity as? CapacityMode.Autoscale)?.targetUtilization ?: 800
                Solution(start, changes = mapOf(GameTime.at(1, 12).tick to Policy(CapacityMode.Autoscale(target, ScheduledMinimum(26, 18, 22)))))
            }
            else -> null
        }
    }

    /** The whole run with the solution, all sites together. */
    fun totals(s: Scenario, sol: Solution): Totals {
        s.location?.let { return s.runLocation(sol.moved, sol.policy ?: s.initialPolicy).map { it.totals }.reduce { a, b -> a + b } }
        if (s.renewal != null) return sol.renewal.mapIndexed { site, plan ->
            val sc = s.forSite(site)
            Simulation.replay(sc, sc.initialPolicy, changes = plan.entries.associate { (day, n) -> GameTime.at(day, 0).tick to sc.initialPolicy.copy(renewed = n) }).totals
        }.reduce { a, b -> a + b }
        if (sol.sprints.isNotEmpty()) return Simulation.replay(s, s.initialPolicy, changes = Engineering.changes(s, sol.sprints)).totals
        if (sol.servers.isNotEmpty()) {
            val changes = sol.servers.mapValues { (_, n) -> s.initialPolicy.copy(capacity = CapacityMode.Manual(n)) }
            return Simulation.replay(s, changes[s.startTick] ?: s.initialPolicy, changes = changes).totals
        }
        return Simulation.replay(s, sol.policy ?: s.initialPolicy, sol.replanAt, sol.changes).totals
    }

    fun referenceTotals(s: Scenario): Totals = when {
        s.location != null -> s.location.sites.indices.map { Simulation.reference(s.forLocation(it, emptySet())).totals }.reduce { a, b -> a + b }
        s.renewal != null -> s.renewal.sites.indices.map { Simulation.reference(s.forSite(it)).totals }.reduce { a, b -> a + b }
        else -> Simulation.reference(s).totals
    }

    fun stars(s: Scenario): Int = of(s)?.let { s.stars(totals(s, it), referenceTotals(s)) } ?: 0
}
