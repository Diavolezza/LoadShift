package de.wannerit.loadshift.core

import java.io.File
import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Every scenario must teach its idea (docs/CONCEPT.md, section 11): the reference operator meets the rules, a good
 * operator is clearly better in CO₂, and a deliberately bad policy shows the expected failure. The numbers go to
 * build/lesson-report.txt for balancing.
 */
class LessonTest {
    /** A careful operator switching by hand: every tick enough servers for the next 90 minutes at 80 % utilization. */
    private fun carefulByHand(scenario: Scenario, utilization: Int = 800): Simulation {
        val simulation = Simulation(scenario)
        val perServer = ServerKind.CPU_CURRENT.capacity
        while (!simulation.isFinished) {
            val t = simulation.tick
            val expected = (t..t + ServerKind.BOOT_TICKS + GameTime.TICKS_PER_HOUR).maxOf { scenario.interactive.expected(it) }
            val servers = (expected * 1_000 + perServer * utilization - 1) / (perServer * utilization)
            simulation.policy = Policy(CapacityMode.Manual(maxOf(1, servers)))
            simulation.step()
        }
        return simulation
    }

    /** A player who switches twice: few servers for the night, many for the day. */
    private fun twoSettings(scenario: Scenario, night: Int, day: Int, dayFrom: Int): Simulation {
        val simulation = Simulation(scenario, Policy(CapacityMode.Manual(night)))
        while (!simulation.isFinished) {
            if (simulation.time.hour == dayFrom && simulation.time.minute == 0) simulation.policy = Policy(CapacityMode.Manual(day))
            simulation.step()
        }
        return simulation
    }

    @Test fun scenario1NightShift() {
        val scenario = Scenario.NIGHT_SHIFT
        val reference = Simulation.reference(scenario)
        val careful = carefulByHand(scenario)
        val rough = twoSettings(scenario, 8, 16, 6)
        val fixedHalf = Simulation.replay(scenario, Policy(CapacityMode.Manual(10)))
        val autoscale = Simulation.replay(scenario, Policy(CapacityMode.Autoscale(700)))

        val lines = mutableListOf(title(scenario), HEADER)
        for ((name, simulation) in listOf(
            "reference: all on" to reference, "by hand, careful" to careful, "by hand, 8 / 16 from 6" to rough,
            "fixed 10 servers" to fixedHalf, "autoscale 70 % (sc. 4)" to autoscale,
        )) {
            lines += line(name, simulation.totals, reference.totals) + "  " + "★".repeat(scenario.stars(simulation.totals, reference.totals))
        }
        lines += ""
        lines += "Hour  g/kWh  demand  servers on (reference / careful)"
        for (hour in 0 until 24) {
            val tick = scenario.startTick + hour * GameTime.TICKS_PER_HOUR
            val r = reference.history.first { it.tick == tick }
            val g = careful.history.first { it.tick == tick }
            lines += String.format(Locale.ROOT, "%s  %5d  %6d  %2d / %2d", GameTime(tick).clockText,
                (r.intensityTenths + 5) / 10, r.demand, r.serversOn, g.serversOn)
        }
        report(1, lines)

        // The scenario is fair: the reference operator meets the SLA – and earns no star.
        assertTrue(reference.totals.sla >= scenario.slaTarget)
        assertEquals(0, scenario.stars(reference.totals, reference.totals))
        // The lesson works: a careful player earns all three stars.
        assertEquals(3, scenario.stars(careful.totals, reference.totals))
        // Two switches a day are a good start, but not the full result.
        assertTrue(scenario.stars(rough.totals, reference.totals) in 1..2)
        // A fixed half of the fleet is not enough at the evening peak.
        assertTrue(fixedHalf.totals.sla < scenario.slaTarget)
        assertEquals(0, scenario.stars(fixedHalf.totals, reference.totals))
    }

    @Test fun scenario2WaitForTheSun() {
        val scenario = Scenario.WAIT_FOR_THE_SUN
        val off = CapacityMode.Manual(0)
        val reference = Simulation.reference(scenario)
        val greenest = Simulation.replay(scenario, Policy(off, BatchMode.Greenest))
        val night = Simulation.replay(scenario, Policy(off, BatchMode.AtHour(1)))
        val noon = Simulation.replay(scenario, Policy(off, BatchMode.AtHour(11)))
        val nothing = Simulation.replay(scenario, scenario.initialPolicy)

        val lines = mutableListOf(title(scenario), "$HEADER  batch g/kWh  missed")
        for ((name, simulation) in listOf(
            "reference: as soon as" to reference, "greenest (forecast)" to greenest, "every job at 01:00" to night,
            "every job at 11:00" to noon, "nothing planned" to nothing,
        )) {
            val t = simulation.totals
            lines += line(name, t, reference.totals) +
                String.format(Locale.ROOT, "  %11d  %6d", (t.meanBatchIntensityTenths + 5) / 10, t.deadlinesMissed) +
                "  " + "★".repeat(scenario.stars(t, reference.totals))
        }
        for (job in greenest.jobs) {
            val start = job.startedAt?.let { GameTime(it) }
            lines += "  greenest job ${job.id} ${job.spec.name}: start day ${start?.day ?: -1} ${start?.clockText ?: "–"}"
        }
        report(2, lines)

        assertEquals(0, reference.totals.deadlinesMissed, "the reference meets every deadline")
        assertEquals(0, scenario.stars(reference.totals, reference.totals))
        assertEquals(0, greenest.totals.deadlinesMissed)
        assertEquals(3, scenario.stars(greenest.totals, reference.totals), "planning by the forecast earns three stars")
        assertTrue(nothing.totals.deadlinesMissed > 0)
        assertEquals(0, scenario.stars(nothing.totals, reference.totals))
        // The night is not clean in a solar grid: the nightly batch is hardly better than the reference.
        assertTrue(scenario.stars(night.totals, reference.totals) <= 1)
    }

    @Test fun scenario3WhenTheWindDrops() {
        val scenario = Scenario.WHEN_THE_WIND_DROPS
        val off = CapacityMode.Manual(0)
        val reference = Simulation.reference(scenario)
        val runs = mutableListOf<Pair<String, Simulation>>("reference: as soon as" to reference)
        val update = scenario.forecastMiss!!.revisedAt
        runs += "greenest, planned once" to Simulation.replay(scenario, Policy(off, BatchMode.Greenest))
        runs += "greenest, again at update" to Simulation.replay(scenario, Policy(off, BatchMode.Greenest), setOf(update))
        runs += "greenest, again every 6 h" to Simulation.replay(scenario, Policy(off, BatchMode.Greenest),
            (0 until scenario.endTick step 24).toSet())
        runs += "greenest, again late" to Simulation.replay(scenario, Policy(off, BatchMode.Greenest), setOf(update + 16))
        for (h in listOf(3, 13)) {
            runs += "at %02d:00, safety".format(h) to Simulation.replay(scenario, Policy(off, BatchMode.AtHour(h)))
            runs += "at %02d:00, no safety".format(h) to Simulation.replay(scenario, Policy(off, BatchMode.AtHour(h), false))
        }
        for (g in listOf(100, 120, 140, 160, 180, 200)) {
            runs += "below $g, safety" to Simulation.replay(scenario, Policy(off, BatchMode.Threshold(g)))
            runs += "below $g, no safety" to Simulation.replay(scenario, Policy(off, BatchMode.Threshold(g), false))
        }
        val lines = mutableListOf(title(scenario), "$HEADER  batch g/kWh  missed")
        for ((name, simulation) in runs) {
            val t = simulation.totals
            lines += line(name, t, reference.totals) +
                String.format(Locale.ROOT, "  %11d  %6d", (t.meanBatchIntensityTenths + 5) / 10, t.deadlinesMissed) +
                "  " + "★".repeat(scenario.stars(t, reference.totals))
        }
        lines += ""
        lines += "Hour   " + (0 until scenario.days).joinToString("") { "day ${it + 1} (fc)    " }
        val grid = Grid(scenario.setting, scenario.seed, scenario.weather)
        for (hour in 0 until 24 step 2) {
            lines += "%02d:00  ".format(hour) + (0 until scenario.days).joinToString("") { day ->
                val tick = day * GameTime.TICKS_PER_DAY + hour * GameTime.TICKS_PER_HOUR
                "%4d (%4d)    ".format(grid.intensity(tick), grid.forecast(tick, maxOf(0, tick - GameTime.TICKS_PER_DAY)))
            }
        }
        for ((name, simulation) in runs.take(3) + runs.filter { it.first == "below 140, safety" }) {
            lines += "  $name: " + simulation.jobs.joinToString("  ") { job ->
                val start = job.startedAt?.let { GameTime(it) }
                "${job.id}@${start?.let { "d${it.day + 1} ${it.clockText}" } ?: "–"}"
            }
        }
        report(3, lines)

        assertEquals(0, reference.totals.deadlinesMissed, "the reference meets every deadline")
        assertEquals(0, scenario.stars(reference.totals, reference.totals))
        val run = runs.toMap()
        fun stars(name: String) = scenario.stars(run.getValue(name).totals, reference.totals)
        fun missed(name: String) = run.getValue(name).totals.deadlinesMissed
        // The rule from the forecast is good; planning again after the forecast update earns all three stars.
        assertEquals(0, missed("greenest, planned once"))
        assertEquals(0, missed("greenest, again at update"))
        assertEquals(3, stars("greenest, again at update"), "planning again after the forecast update earns three stars")
        assertTrue(stars("greenest, planned once") <= 2, "a plan from the old forecast is not enough for the third star")
        // Waiting without the deadline safety misses deadlines once the wind has dropped: no star.
        assertTrue(missed("at 03:00, no safety") > 0)
        assertTrue(missed("below 140, no safety") > 0)
        assertEquals(0, stars("below 140, no safety"))
        // With the safety the deadlines hold, but a fixed hour or threshold is not much better than the reference.
        for (name in listOf("at 03:00, safety", "below 140, safety", "below 200, safety")) {
            assertEquals(0, missed(name), name)
            assertTrue(stars(name) <= 1, name)
        }
    }

    /** Runs a scenario and changes the policy at given ticks (a player who acts before the campaign). */
    private fun withChanges(scenario: Scenario, start: Policy, changes: Map<Int, Policy>): Simulation {
        val simulation = Simulation(scenario, start)
        while (!simulation.isFinished) {
            changes[simulation.tick]?.let { simulation.policy = it; simulation.applyPolicy() }
            simulation.step()
        }
        return simulation
    }

    @Test fun scenario4Headroom() {
        val scenario = Scenario.HEADROOM
        val reference = Simulation.reference(scenario)
        val day2noon = GameTime.at(1, 12).tick
        fun auto(target: Int, minimum: ScheduledMinimum? = null) = Policy(CapacityMode.Autoscale(target, minimum))
        val runs = mutableListOf<Pair<String, Simulation>>("reference: all on" to reference)
        for (target in listOf(500, 600, 700, 800, 900)) runs += "autoscale $target" to Simulation.replay(scenario, auto(target))
        for (n in listOf(24, 26, 28)) {
            runs += "as 800 + min $n 18-22 daily" to Simulation.replay(scenario, auto(800, ScheduledMinimum(n, 18, 22)))
            runs += "as 800 + min $n 18-22 day 2" to withChanges(scenario, auto(800), mapOf(day2noon to auto(800, ScheduledMinimum(n, 18, 22))))
            runs += "as 700 + min $n 18-22 day 2" to withChanges(scenario, auto(700), mapOf(day2noon to auto(700, ScheduledMinimum(n, 18, 22))))
            runs += "as 900 + min $n 18-22 day 2" to withChanges(scenario, auto(900), mapOf(day2noon to auto(900, ScheduledMinimum(n, 18, 22))))
        }
        runs += "as 800, 500 from d2 12" to withChanges(scenario, auto(800), mapOf(day2noon to auto(500)))
        runs += "as 800, all on d2 18:30" to withChanges(scenario, auto(800), mapOf(GameTime.at(1, 18, 30).tick to Policy(CapacityMode.AllOn)))
        runs += "fixed 28" to Simulation.replay(scenario, Policy(CapacityMode.Manual(28)))
        val lines = mutableListOf(title(scenario), HEADER)
        for ((name, simulation) in runs) {
            lines += line(name, simulation.totals, reference.totals) + "  " + "★".repeat(scenario.stars(simulation.totals, reference.totals))
        }
        lines += ""
        lines += "Day 2  demand  servers on (autoscale 800 / + min 26 day 2)"
        val plain = runs.first { it.first == "autoscale 800" }.second
        val prepared = runs.first { it.first == "as 800 + min 26 18-22 day 2" }.second
        for (t in GameTime.at(1, 17).tick until GameTime.at(1, 23).tick step 2) {
            val a = plain.history.first { it.tick == t }
            val b = prepared.history.first { it.tick == t }
            lines += String.format(Locale.ROOT, "%s  %6d  %2d (cap %3d, in time %3d) / %2d (cap %3d, in time %3d)", GameTime(t).clockText,
                a.demand, a.serversOn, a.capacity, a.inTime, b.serversOn, b.capacity, b.inTime)
        }
        report(4, lines)

        assertTrue(reference.totals.sla >= scenario.slaTarget, "the reference meets the SLA")
        assertEquals(0, scenario.stars(reference.totals, reference.totals))
        val run = runs.toMap()
        fun stars(name: String) = scenario.stars(run.getValue(name).totals, reference.totals)
        // Left as it was found, autoscaling at 80 % misses the SLA in the campaign.
        assertEquals(0, stars("autoscale 800"))
        // Headroom all the time holds the SLA but costs energy: no more than one star.
        assertTrue(stars("autoscale 600") <= 1)
        assertTrue(stars("autoscale 500") <= 1)
        // A minimum for the campaign window on day 2 earns all three stars; every day it costs the third.
        assertEquals(3, stars("as 800 + min 26 18-22 day 2"))
        assertTrue(stars("as 800 + min 26 18-22 daily") <= 2)
    }

    @Test fun scenario5Consolidation() {
        val scenario = Scenario.CONSOLIDATION
        val vms = scenario.vms
        val host = ServerKind.CPU_CURRENT
        val reference = Simulation.reference(scenario)
        fun placed(p: List<Int>) = Simulation.replay(scenario, Policy(CapacityMode.Placed(p)))
        val fleet = scenario.serverCount
        val both = { k: VmKind -> k.ramGb * 1_000 / host.ramGb + k.peak * 1_000 / host.capacity }
        val heuristics = listOf(
            "FFD by RAM, RAM only" to Packing.firstFitDecreasing(vms, host, fleet, Packing.Check.RAM_ONLY) { it.ramGb },
            "FFD by RAM, peak sum" to Packing.firstFitDecreasing(vms, host, fleet, Packing.Check.PEAK_SUM) { it.ramGb },
            "FFD both, profiles 85" to Packing.firstFitDecreasing(vms, host, fleet, Packing.Check.PROFILES, key = both),
            "FFD both, profiles 90" to Packing.firstFitDecreasing(vms, host, fleet, Packing.Check.PROFILES, 900, both),
            "FFD both, profiles 100" to Packing.firstFitDecreasing(vms, host, fleet, Packing.Check.PROFILES, 1_000, both),
            // By hand: one batch worker per host, the peaks of the others spread (found by a local search).
            "by hand, 6 hosts" to listOf(0, 0, 1, 2, 3, 4, 4, 5, 0, 1, 2, 3, 4, 5, 0, 2, 4, 5, 1, 3, 2, 2, 5, 5),
            "by hand, 5 hosts" to listOf(0, 0, 1, 2, 3, 4, 4, 0, 0, 1, 2, 3, 4, 1, 0, 2, 0, 3, 1, 3, 2, 2, 4, 4),
        )
        val runs = mutableListOf("reference: spread" to reference)
        for ((name, p) in heuristics) runs += name to placed(p)
        val lines = mutableListOf(title(scenario), HEADER + "  hosts  peak/host")
        for ((name, simulation) in runs) {
            val p = (simulation.policy.capacity as CapacityMode.Placed).hosts
            val hosts = p.toSet().size
            val peak = p.toSet().maxOf { Packing.expectedPeakMilli(vms, p, it) } / 1_000.0
            lines += line(name, simulation.totals, reference.totals) + String.format(Locale.ROOT, "  %5d  %9.1f", hosts, peak) +
                "  " + "★".repeat(scenario.stars(simulation.totals, reference.totals))
        }
        report(5, lines)

        assertTrue(reference.totals.sla >= scenario.slaTarget, "the reference meets the SLA")
        assertEquals(0, scenario.stars(reference.totals, reference.totals))
        val run = runs.toMap()
        fun stars(name: String) = scenario.stars(run.getValue(name).totals, reference.totals)
        // Packed by RAM alone, the peaks collide: requests get slow, no star.
        assertEquals(0, stars("FFD by RAM, RAM only"))
        // Six hosts by hand earn three stars; five fit by RAM, but not by CPU.
        assertEquals(3, stars("by hand, 6 hosts"))
        assertEquals(0, stars("by hand, 5 hosts"))
    }

    @Test fun e1MeasureFirst() {
        val scenario = Scenario.MEASURE_FIRST
        val reference = Simulation.reference(scenario)
        val backlog = scenario.backlog
        fun run(plans: List<List<Int>>) = Simulation.replay(scenario, scenario.initialPolicy, changes = Engineering.changes(scenario, plans))
        val byEstimate = Engineering.greedy(scenario, profileFirst = false, value = Engineering.byEstimates(scenario))
        val profileFirst = Engineering.greedy(scenario, profileFirst = true, value = Engineering.byEstimates(scenario))
        val i = { title: String -> backlog.indexOfFirst { it.title == title } }
        val lucky = listOf(listOf(i("Fix the N+1 queries in the product list"), i("Log at INFO instead of DEBUG in production"), i("Cache the catalog responses")),
            listOf(), listOf(i("Cache the resized images"), i("Upgrade the web framework")),
            listOf(i("Rewrite the checkout in a compiled language")))
        val runs = listOf("reference: nothing" to reference, "by the estimates" to run(byEstimate),
            "profile first" to run(profileFirst), "lucky guess, no profile" to run(lucky))
        val lines = mutableListOf(title(scenario), HEADER)
        for ((name, simulation) in runs) {
            lines += line(name, simulation.totals, reference.totals) + "  " + "★".repeat(scenario.stars(simulation.totals, reference.totals))
        }
        for ((name, plans) in listOf("by the estimates" to byEstimate, "profile first" to profileFirst)) {
            lines += "  $name: " + plans.joinToString("  |  ") { p -> p.joinToString(", ") { backlog[it].title.take(24) } }
        }
        report(101, lines)

        assertTrue(reference.totals.sla >= scenario.slaTarget)
        assertEquals(0, scenario.stars(reference.totals, reference.totals))
        val byName = runs.toMap()
        fun stars(name: String) = scenario.stars(byName.getValue(name).totals, reference.totals)
        // Optimizing by the team's estimates saves something, but the big items hardly matter: one star.
        assertEquals(1, stars("by the estimates"))
        // Profiling first, then the measured hot spots: three stars.
        assertEquals(3, stars("profile first"))
    }

    @Test fun e2GrowingData() {
        val scenario = Scenario.GROWING_DATA
        val reference = Simulation.reference(scenario)
        val backlog = scenario.backlog
        fun run(plans: List<List<Int>>) = Simulation.replay(scenario, scenario.initialPolicy, changes = Engineering.changes(scenario, plans))
        val today = Engineering.greedy(scenario, value = Engineering.byToday(scenario))
        val later = Engineering.greedy(scenario, value = Engineering.byLastDay(scenario))
        // A player who starts the quadratic job first, then the linear ones, then the constant cuts.
        val growthFirst = listOf(listOf(2), listOf(), listOf(), listOf(), listOf(0), listOf(1), listOf(), listOf(4), listOf(3), listOf(), listOf(5), listOf())
        val constantFirst = Engineering.greedy(scenario) { item, _, _, _ -> backlog[item].effect }
        val runs = listOf("reference: nothing" to reference, "by today's savings" to run(today),
            "by the savings in a year" to run(later), "growth first" to run(growthFirst), "constant cuts first" to run(constantFirst))
        val lines = mutableListOf(title(scenario), HEADER)
        for ((name, simulation) in runs) {
            lines += line(name, simulation.totals, reference.totals) + "  " + "★".repeat(scenario.stars(simulation.totals, reference.totals))
        }
        for ((name, plans) in listOf("by today's savings" to today, "by the savings in a year" to later)) {
            lines += "  $name: " + plans.joinToString(" | ") { p -> p.joinToString(",") }
        }
        val growth = scenario.growth!!
        lines += "  n by day: " + (0 until scenario.days).joinToString(" ") { "%.2f".format(Locale.ROOT, growth.n(it) / 1_000.0) }
        lines += "  per request (nothing / all fixed): " + (0 until scenario.days).joinToString(" ") { d ->
            "${growth.perRequest(d, backlog, emptySet())}/${growth.perRequest(d, backlog, backlog.indices.toSet())}"
        }
        lines += "  servers on per day (reference): " + (0 until scenario.days).joinToString(" ") { d ->
            val h = reference.history.filter { it.tick / GameTime.TICKS_PER_DAY == d }
            "%.1f".format(Locale.ROOT, h.sumOf { it.serversOn } / h.size.toDouble())
        }
        report(102, lines)
        assertTrue(reference.totals.sla >= scenario.slaTarget, "the reference meets the SLA")
        assertEquals(0, scenario.stars(reference.totals, reference.totals))
        val byName = runs.toMap()
        fun stars(name: String) = scenario.stars(byName.getValue(name).totals, reference.totals)
        // The biggest saving today first: the constant cuts block the team while the quadratic job grows.
        assertTrue(stars("constant cuts first") <= 1)
        // By today's savings per story point: good, but the quadratic job comes late.
        assertEquals(2, stars("by today's savings"))
        // The growing parts first, the quadratic one before all: three stars.
        assertEquals(3, stars("growth first"))
    }

    @Test fun e3HitOrMiss() {
        val scenario = Scenario.HIT_OR_MISS
        val reference = Simulation.reference(scenario)
        val model = scenario.caching!!
        fun run(c: CacheConfig) = Simulation.replay(scenario, scenario.initialPolicy.copy(cache = c))
        val careful = CacheConfig(32, listOf(1_440, 15, 1, 0))
        val configs = listOf(
            "everything for a day, 64" to CacheConfig(64, listOf(1_440, 1_440, 1_440, 1_440)),
            "careful 8 GB" to careful.copy(sizeGb = 8),
            "careful 32 GB" to careful,
            "careful 256 GB" to careful.copy(sizeGb = 256),
            "rates 15 min (stale)" to careful.copy(ttlMinutes = listOf(1_440, 15, 15, 0)),
            "catalog 60 min (stale)" to careful.copy(ttlMinutes = listOf(1_440, 60, 1, 0)),
            "careful + HLL + view" to careful.copy(approximateCount = true, materializedView = true),
            "careful 8 + HLL + view" to careful.copy(sizeGb = 8, approximateCount = true, materializedView = true),
            "no cache, HLL + view" to CacheConfig(approximateCount = true, materializedView = true),
        )
        val runs = listOf("reference: no cache" to reference) + configs.map { (n, c) -> n to run(c) }
        val lines = mutableListOf(title(scenario), "$HEADER  stale ‰")
        for ((name, simulation) in runs) {
            lines += line(name, simulation.totals, reference.totals) +
                String.format(Locale.ROOT, "  %6.2f", simulation.totals.stale * 1_000.0 / simulation.totals.requests) +
                "  " + "★".repeat(scenario.stars(simulation.totals, reference.totals))
        }
        for ((name, c) in configs.take(4)) {
            lines += "  hit rates $name: peak " + model.hitRates(c, 240) + ", night " + model.hitRates(c, 48) +
                ", work peak " + model.work(c, 240) + " night " + model.work(c, 48) + ", cache power " + model.power(c) + " W"
        }
        report(103, lines)
        assertTrue(reference.totals.sla >= scenario.slaTarget)
        assertEquals(0, scenario.stars(reference.totals, reference.totals))
        val byName = runs.toMap()
        fun stars(name: String) = scenario.stars(byName.getValue(name).totals, reference.totals)
        // Stale answers break the first rule, however much they save.
        assertEquals(0, stars("everything for a day, 64"))
        assertEquals(0, stars("rates 15 min (stale)"))
        // Cached with the right TTLs: two stars; four cache servers for 256 GB cost one of them.
        assertEquals(2, stars("careful 32 GB"))
        assertEquals(1, stars("careful 256 GB"))
        // With approximate counting and the materialized view: three.
        assertEquals(3, stars("careful + HLL + view"))
    }

    private companion object {
        val sections = sortedMapOf<Int, List<String>>()

        const val HEADER =
            "policy                  CO₂ kg  oper.  embod.    kWh  g/kWh   PUE   SLA %  servers  util. %  SCI g/1000 req  vs. ref"

        fun title(s: Scenario) = "Scenario ${s.number} – ${s.title} (${s.setting.region.title}, ${s.setting.season.title}, " +
            "${s.days} day${if (s.days == 1) "" else "s"})"

        fun line(name: String, t: Totals, reference: Totals): String {
            val diff = if (reference.emissions == 0L) 0L else (t.emissions - reference.emissions) * 1_000 / reference.emissions
            return name.padEnd(22) + String.format(
                Locale.ROOT, " %7.1f %6.1f %6.1f %7.1f  %5d  %4.2f  %6.1f  %7.1f  %7.1f  %14.2f  %+6.1f %%",
                t.emissions / 1e9, t.operational / 1e9, t.embodied / 1e9, t.facilityEnergy / 1e6,
                (t.meanIntensityTenths + 5) / 10, t.pue / 1_000.0, t.sla / 10.0, t.meanServersOn / 1_000.0,
                t.meanUtilization / 10.0, t.sciPerUnit / 1e6, diff / 10.0,
            )
        }

        /** Keeps the section of a scenario and writes the whole report again (tests run in any order). */
        @Synchronized
        fun report(number: Int, lines: List<String>) {
            sections[number] = lines + ""
            val path = System.getProperty("lessonReport") ?: return
            File(path).apply { parentFile.mkdirs() }.writeText(sections.values.flatten().joinToString("\n") + "\n")
        }
    }
}
