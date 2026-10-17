package de.wannerit.loadshift.core

import java.io.File
import java.util.Locale
import kotlin.math.roundToInt
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

    @Test fun scenario6HeatWave() {
        val scenario = Scenario.HEAT_WAVE
        val reference = Simulation.reference(scenario)
        fun run(target: Int, setpoint: Int, all: Boolean = false) = Simulation.replay(
            scenario, Policy(if (all) CapacityMode.AllOn else CapacityMode.Autoscale(target), setpoint = setpoint))
        val runs = listOf(
            "reference: 50 %, 20 °C" to reference,
            "50 %, 18 °C" to run(500, 18),
            "50 %, 24 °C" to run(500, 24),
            "50 %, 25 °C" to run(500, 25),
            "50 %, 26 °C" to run(500, 26),
            "50 %, 27 °C" to run(500, 27),
            "70 %, 20 °C" to run(700, 20),
            "80 %, 20 °C" to run(800, 20),
            "70 %, 24 °C" to run(700, 24),
            "80 %, 24 °C" to run(800, 24),
            "80 %, 25 °C" to run(800, 25),
            "80 %, 26 °C" to run(800, 26),
            "80 %, 27 °C" to run(800, 27),
            "70 %, 25 °C" to run(700, 25),
            "90 %, 25 °C" to run(900, 25),
            "all on, 27 °C" to run(0, 27, all = true),
            "40 %, 27 °C" to run(400, 27),
        )
        val lines = mutableListOf(title(scenario), HEADER)
        for ((name, simulation) in runs) {
            lines += line(name, simulation.totals, reference.totals) + "  " + "★".repeat(scenario.stars(simulation.totals, reference.totals))
        }
        val cooling = scenario.cooling!!
        for (day in 0 until scenario.days) {
            lines += "  day ${day + 1}: " + listOf(5, 10, 15, 20).joinToString("  ") { h ->
                val tick = GameTime.at(day, h).tick
                "%02d:00 %.1f °C mech 20/27: %d/%d ‰".format(Locale.ROOT, h, cooling.outsideTenths(tick) / 10.0,
                    cooling.mechanicalPermille(tick, 20), cooling.mechanicalPermille(tick, 27))
            }
        }
        report(6, lines)
        assertTrue(reference.totals.sla >= scenario.slaTarget)
        assertEquals(0, scenario.stars(reference.totals, reference.totals))
        val byName = runs.toMap()
        fun stars(name: String) = scenario.stars(byName.getValue(name).totals, reference.totals)
        fun pue(name: String) = byName.getValue(name).totals.pue
        // The setpoint alone: one star; its sweet spot is 25 °C – above it the fans eat the saving.
        assertEquals(1, stars("50 %, 25 °C"))
        assertTrue(byName.getValue("50 %, 25 °C").totals.emissions < byName.getValue("50 %, 27 °C").totals.emissions)
        // Fewer servers alone: one star.
        assertEquals(1, stars("80 %, 20 °C"))
        // Both: two stars; at the sweet spot three.
        assertEquals(2, stars("80 %, 27 °C"))
        assertEquals(3, stars("80 %, 25 °C"))
        // The PUE trap: fewer servers raise the PUE, the warmest setpoint and all servers on lower it.
        assertTrue(pue("80 %, 20 °C") > pue("reference: 50 %, 20 °C"))
        assertTrue(pue("50 %, 27 °C") < pue("50 %, 25 °C"))
        assertTrue(pue("all on, 27 °C") < pue("80 %, 25 °C"))
        assertEquals(0, stars("all on, 27 °C"))
    }

    @Test fun scenario7OldIron() {
        val scenario = Scenario.OLD_IRON
        val sites = scenario.renewal!!.sites.indices
        val references = sites.map { Simulation.reference(scenario.forSite(it)) }
        val reference = references.map { it.totals }.reduce { a, b -> a + b }
        /** A site's run: [plan] = new servers installed by the start of a quarter (day), in total. */
        fun site(site: Int, plan: Map<Int, Int>): Simulation {
            val s = scenario.forSite(site)
            val changes = plan.entries.associate { (day, n) -> GameTime.at(day, 0).tick to s.initialPolicy.copy(renewed = n) }
            return Simulation.replay(s, s.initialPolicy, changes = changes)
        }
        val runs = listOf(
            "buy nothing" to listOf(emptyMap(), emptyMap()),
            "Warsaw first" to listOf(mapOf(0 to 4, 4 to 8), mapOf(8 to 4)),
            "Warsaw first, P at Q12" to listOf(mapOf(0 to 4, 4 to 8, 8 to 10), mapOf(11 to 2)),
            "Paris first" to listOf(mapOf(8 to 4), mapOf(0 to 4, 4 to 8)),
            "2 and 2 every year" to listOf(mapOf(0 to 2, 4 to 4, 8 to 6), mapOf(0 to 2, 4 to 4, 8 to 6)),
            "W 4 now, then split" to listOf(mapOf(0 to 4, 4 to 6, 8 to 8), mapOf(4 to 2, 8 to 4)),
            "all in Warsaw" to listOf(mapOf(0 to 4, 4 to 8, 8 to 12), emptyMap()),
            "W first, P 2 at Q9" to listOf(mapOf(0 to 4, 4 to 8, 8 to 10), mapOf(8 to 2)),
            "W first, P 2 at Q11" to listOf(mapOf(0 to 4, 4 to 8, 8 to 10), mapOf(10 to 2)),
            "W first, P 1 at Q12" to listOf(mapOf(0 to 4, 4 to 8, 8 to 11), mapOf(11 to 1)),
        ).map { (name, plans) -> name to sites.map { site(it, plans[it]) } }
        val lines = mutableListOf(title(scenario), HEADER + "  Warsaw  Paris  failed")
        val all = listOf("reference (buys when short)" to references) + runs
        for ((name, sims) in all) {
            val total = sims.map { it.totals }.reduce { a, b -> a + b }
            val perSite = sims.mapIndexed { i, sim ->
                val r = references[i].totals.emissions
                String.format(Locale.ROOT, "%+6.1f%% %5.1f", (sim.totals.emissions - r) * 100.0 / r, sim.totals.sla / 10.0)
            }
            lines += line(name, total, reference) + "  " + perSite.joinToString("  ") + "  " + total.failedServers +
                "  " + "★".repeat(scenario.stars(total, reference))
        }
        for ((i, sim) in references.withIndex()) {
            lines += "  reference site $i: new " + sim.servers.count { it.kind == ServerKind.CPU_CURRENT } + ", old left " + sim.servers.count { it.kind == ServerKind.CPU_OLD }
        }
        report(7, lines)
        val byName = all.toMap()
        fun stars(name: String) = scenario.stars(byName.getValue(name).map { it.totals }.reduce { a, b -> a + b }, reference)
        // Without new servers the growth and the failures break the SLA; Paris must not be left without capacity.
        assertEquals(0, stars("buy nothing"))
        assertEquals(0, stars("all in Warsaw"))
        // New hardware first where the grid is dirty: two stars; in Paris only as late as the capacity allows: three.
        assertEquals(2, stars("Warsaw first"))
        assertEquals(3, stars("W first, P 2 at Q11"))
        // Paris first saves nothing against the reference.
        assertEquals(0, stars("Paris first"))
        assertEquals(1, stars("2 and 2 every year"))
    }

    @Test fun scenario8TrainingRun() {
        val scenario = Scenario.TRAINING_RUN
        val reference = Simulation.reference(scenario)
        fun run(servers: Int, cap: Int, greenest: Boolean) =
            Simulation.replay(scenario, scenario.initialPolicy.copy(training = TrainingConfig(servers, cap, greenest)))
        val runs = listOf("reference: 8, 100 %, now" to reference) + listOf(
            Triple(8, 1_000, true), Triple(8, 700, false), Triple(8, 800, false), Triple(8, 600, false),
            Triple(8, 700, true), Triple(8, 800, true), Triple(8, 600, true),
            Triple(6, 1_000, true), Triple(6, 700, true), Triple(6, 600, true),
            Triple(4, 1_000, false), Triple(4, 700, false), Triple(5, 700, true), Triple(7, 700, true),
            Triple(8, 500, false), Triple(8, 500, true), Triple(6, 500, true),
        ).map { (n, c, g) -> "$n, ${c / 10} %, ${if (g) "greenest" else "now"}" to run(n, c, g) }
        val lines = mutableListOf(title(scenario), HEADER + "  done at  restarts")
        for ((name, sim) in runs) {
            val done = sim.totals.trainingFinishedAt?.let { GameTime(it).let { t -> "d${t.day + 1} ${t.clockText}" } } ?: "NOT DONE"
            lines += line(name, sim.totals, reference.totals) + "  " + done.padStart(9) + "  " + sim.totals.trainingRestarts +
                "  " + "★".repeat(scenario.stars(sim.totals, reference.totals))
        }
        report(8, lines)
        assertTrue(reference.totals.trainingFinishedAt != null)
        val byName = runs.toMap()
        fun stars(name: String) = scenario.stars(byName.getValue(name).totals, reference.totals)
        // One lever alone: one star – the greenest hours, the power cap.
        assertEquals(1, stars("8, 100 %, greenest"))
        assertEquals(1, stars("8, 70 %, now"))
        // Both: two stars; with fewer servers that wait less for each other: three.
        assertEquals(2, stars("8, 70 %, greenest"))
        assertEquals(3, stars("6, 60 %, greenest"))
        // Too few servers at a low cap, all the time: the deadline is missed – no star.
        assertEquals(0, stars("4, 70 %, now"))
        // Below the sweet spot the cap costs more than it saves: 50 % is worse than 60 %.
        fun co2(name: String) = byName.getValue(name).totals.emissions
        assertTrue(co2("8, 50 %, now") > co2("8, 60 %, now"))
        assertTrue(co2("6, 50 %, greenest") > co2("6, 60 %, greenest"))
    }

    @Test fun scenario9EcoMode() {
        val scenario = Scenario.ECO_MODE
        val reference = Simulation.reference(scenario)
        fun run(c: StreamingConfig) = Simulation.replay(scenario, scenario.initialPolicy.copy(streaming = c))
        val U = Quality.UHD; val H = Quality.HD; val S = Quality.SD
        val byDevice = StreamingConfig(listOf(H, H, U))
        val runs = listOf("reference: 4K everywhere" to reference) + listOf(
            "by device" to byDevice,
            "HD everywhere" to StreamingConfig(listOf(H, H, H)),
            "phones SD, by device" to StreamingConfig(listOf(S, H, U)),
            "eco 600 only" to StreamingConfig(eco = true, ecoAbove = 600),
            "by device + eco 700" to byDevice.copy(eco = true, ecoAbove = 700),
            "by device + eco 650" to byDevice.copy(eco = true, ecoAbove = 650),
            "by device + eco 600" to byDevice.copy(eco = true, ecoAbove = 600),
            "by device + eco 550" to byDevice.copy(eco = true, ecoAbove = 550),
            "by device + eco 500" to byDevice.copy(eco = true, ecoAbove = 500),
            "by device + eco SD 650" to byDevice.copy(eco = true, ecoAbove = 650, ecoQuality = S),
        ).map { (n, c) -> n to run(c) }
        val lines = mutableListOf(title(scenario), HEADER + "  rating")
        for ((name, sim) in runs) {
            lines += line(name, sim.totals, reference.totals) + String.format(Locale.ROOT, "  %5.1f", sim.totals.meanRating / 10.0) +
                "  " + "★".repeat(scenario.stars(sim.totals, reference.totals))
        }
        val grid = reference.grid
        lines += "  grid day 1: " + (0 until 24 step 2).joinToString(" ") { "${it}h ${grid.intensity(GameTime.at(0, it).tick)}" }
        lines += "  grid day 2: " + (0 until 24 step 2).joinToString(" ") { "${it}h ${grid.intensity(GameTime.at(1, it).tick)}" }
        report(9, lines)
        val byName = runs.toMap()
        fun stars(name: String) = scenario.stars(byName.getValue(name).totals, reference.totals)
        // HD on phones and laptops costs hardly any rating and saves 40 %: one star.
        assertEquals(1, stars("by device"))
        // Too much: the viewers' rating falls below the target – no star, however much CO₂ it saves.
        assertEquals(0, stars("HD everywhere"))
        assertEquals(0, stars("by device + eco SD 650"))
        // With the eco mode in the dirty hours: two stars; pushed as far as the viewers accept: three.
        assertEquals(2, stars("by device + eco 650"))
        assertEquals(3, stars("by device + eco 550"))
    }

    @Test fun scenario10TwoSites() {
        val scenario = Scenario.TWO_SITES
        val ref = scenario.runLocation(emptySet(), scenario.referencePolicy)
        val reference = ref.map { it.totals }.reduce { a, b -> a + b }
        val asap = scenario.initialPolicy
        val greenest = asap.copy(batch = BatchMode.Greenest)
        val smart = setOf(0, 1, 5, 6, 7, 8)
        val runs = listOf(
            "all in Frankfurt, greenest" to (emptySet<Int>() to greenest),
            "trainings to Oslo" to (setOf(1, 7) to asap),
            "smart, asap" to (smart to asap),
            "smart, greenest" to (smart to greenest),
            "smart + videos" to (smart + 2 to asap),
            "smart + price calc" to (smart + 4 + 9 to asap),
            "smart + customer data" to (smart + 3 to asap),
            "everything movable" to ((0..9).toSet() - 3 to asap),
            "reports only" to (setOf(0, 5, 8) to asap),
            "trainings + index" to (setOf(1, 6, 7) to asap),
            "trainings + index, greenest" to (setOf(1, 6, 7) to greenest),
        ).map { (name, p) -> name to scenario.runLocation(p.first, p.second) }
        val lines = mutableListOf(title(scenario), HEADER + "  missed  transfer kWh  rules")
        for ((name, sims) in listOf("reference: all in Frankfurt, asap" to ref) + runs) {
            val total = sims.map { it.totals }.reduce { a, b -> a + b }
            lines += line(name, total, reference) + "  " + total.deadlinesMissed + "  " + (total.transferEnergy / 1_000_000) +
                "  " + total.rulesBroken + "  " + "★".repeat(scenario.stars(total, reference))
        }
        report(10, lines)
        val byName = runs.toMap()
        fun stars(name: String) = scenario.stars(byName.getValue(name).map { it.totals }.reduce { a, b -> a + b }, reference)
        // Time shifting alone in the German grid, or only the trainings moved: one star.
        assertEquals(1, stars("all in Frankfurt, greenest"))
        assertEquals(1, stars("trainings to Oslo"))
        // The jobs with much computing and little data in Oslo: two stars; with the greenest hours: three.
        assertEquals(2, stars("smart, asap"))
        assertEquals(3, stars("smart, greenest"))
        // Terabytes over the network eat the saving; the price calculation misses its deadline far from its
        // database; customer data must stay in Germany.
        assertTrue(stars("smart + videos") <= 1)
        assertEquals(0, stars("smart + price calc"))
        assertEquals(0, stars("smart + customer data"))
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
        val allowed = CacheConfig(1, listOf(1_440, 15, 1, 0, 0), invalidate = true)
        val withSearch = allowed.copy(ttlMinutes = listOf(1_440, 15, 1, 60, 0))
        val configs = listOf(
            "everything 1 day, 1" to CacheConfig(1, listOf(1_440, 1_440, 1_440, 1_440, 1_440)),
            "balance 1 min (stale)" to allowed.copy(ttlMinutes = listOf(1_440, 15, 1, 0, 1)),
            "catalog 1 h (stale)" to allowed.copy(ttlMinutes = listOf(1_440, 60, 1, 0, 0)),
            "+search, 4 (team)" to withSearch.copy(servers = 4),
            "+search, 2" to withSearch.copy(servers = 2),
            "+search, 1" to withSearch,
            "allowed, 4" to allowed.copy(servers = 4),
            "allowed, 2" to allowed.copy(servers = 2),
            "allowed, 1" to allowed,
            "images 1 h, 1" to allowed.copy(ttlMinutes = listOf(60, 15, 1, 0, 0)),
            "no images, 1" to allowed.copy(ttlMinutes = listOf(0, 15, 1, 0, 0)),
            "+search, 4, HLL+view" to withSearch.copy(servers = 4, approximateCount = true, materializedView = true),
            "allowed, 4, HLL+view" to allowed.copy(servers = 4, approximateCount = true, materializedView = true),
            "allowed, 2, HLL+view" to allowed.copy(servers = 2, approximateCount = true, materializedView = true),
            "+search, 1, HLL+view" to withSearch.copy(approximateCount = true, materializedView = true),
            "allowed, 1, HLL+view" to allowed.copy(approximateCount = true, materializedView = true),
            "only HLL+view" to CacheConfig(approximateCount = true, materializedView = true),
            "1, HLL+view, no inval." to allowed.copy(approximateCount = true, materializedView = true, invalidate = false),
            "1, HLL+view, catalog 1 min" to allowed.copy(ttlMinutes = listOf(1_440, 1, 1, 0, 0), approximateCount = true, materializedView = true, invalidate = false),
            "allowed, 1, no inval." to allowed.copy(invalidate = false),
            "1, HLL+view, warm-up" to allowed.copy(approximateCount = true, materializedView = true, warmUp = true),
        )
        val runs = listOf("reference: no cache" to reference) + configs.map { (n, c) -> n to run(c) }
        val lines = mutableListOf(title(scenario), "$HEADER  stale ‰")
        for ((name, simulation) in runs) {
            lines += line(name, simulation.totals, reference.totals) +
                String.format(Locale.ROOT, "  %6.2f", simulation.totals.stale.toDouble() / simulation.totals.requests) +
                "  " + "★".repeat(scenario.stars(simulation.totals, reference.totals))
        }
        for ((name, c) in configs.drop(3).take(8)) {
            for (load in listOf(240, 48)) {
                val s = model.state(c, load)
                lines += "  $name at $load: hits " + s.hits + ", GB " + s.memoryGb.map { it.roundToInt() } +
                    String.format(Locale.ROOT, " (%.0f of %d), stay ", s.usedGb, s.capacityGb) + s.stay.map { it.roundToInt() } +
                    ", work " + model.work(c, load) + ", cache " + model.power(c) + " W"
            }
        }
        report(103, lines)
        assertTrue(reference.totals.sla >= scenario.slaTarget)
        assertEquals(0, scenario.stars(reference.totals, reference.totals))
        val byName = runs.toMap()
        fun stars(name: String) = scenario.stars(byName.getValue(name).totals, reference.totals)
        // Stale answers break the first rule, however much they save.
        assertEquals(0, stars("everything 1 day, 1"))
        assertEquals(0, stars("balance 1 min (stale)"))
        assertEquals(0, stars("catalog 1 h (stale)"))
        // The price change: catalog pages cached before it are wrong – without invalidation no star.
        assertEquals(0, stars("1, HLL+view, no inval."))
        // Catalog pages for one minute instead: correct, but the expensive pages hit less – two stars.
        assertEquals(2, stars("1, HLL+view, catalog 1 min"))
        // Warming the cache every night computes pages that expire before anyone asks: it costs the third star.
        assertEquals(2, stars("1, HLL+view, warm-up"))
        // The team's plan – everything allowed, search too, four cache servers – saves, but only one star.
        assertEquals(1, stars("+search, 4 (team)"))
        assertEquals(1, stars("allowed, 2"))
        // One cache server holds the hot set: two stars; search results add nothing but fill the memory.
        assertEquals(2, stars("allowed, 1"))
        assertEquals(2, stars("+search, 1"))
        // The extra measures do not make up for three idle cache servers.
        assertEquals(1, stars("allowed, 4, HLL+view"))
        assertEquals(2, stars("allowed, 2, HLL+view"))
        // One cache server, with HyperLogLog and the materialized view: three.
        assertEquals(3, stars("allowed, 1, HLL+view"))
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
