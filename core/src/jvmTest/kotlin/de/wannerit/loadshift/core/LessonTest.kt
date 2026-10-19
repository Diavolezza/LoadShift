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

    @Test fun e2MeasureFirst() {
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

    @Test fun e3GrowingData() {
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

    @Test fun e4HitOrMiss() {
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

    @Test fun scenario11DarkDoldrums() {
        val scenario = Scenario.DARK_DOLDRUMS
        val reference = Simulation.reference(scenario)
        val revised = setOfNotNull(scenario.forecastMiss?.revisedAt)
        val shop = mapOf(
            "all on" to CapacityMode.AllOn,
            "auto 80" to CapacityMode.Autoscale(800),
            "auto 80+min" to CapacityMode.Autoscale(800, ScheduledMinimum(30, 19, 23)),
            "auto 70+min" to CapacityMode.Autoscale(700, ScheduledMinimum(30, 19, 23)),
            "auto 60" to CapacityMode.Autoscale(600),
            "auto 90" to CapacityMode.Autoscale(900),
            "auto 70" to CapacityMode.Autoscale(700),
        )
        val trainings = mapOf(
            "4/100/now" to TrainingConfig(4, 1_000, false),
            "4/100/green" to TrainingConfig(4, 1_000, true),
            "4/60/green" to TrainingConfig(4, 600, true),
            "3/60/green" to TrainingConfig(3, 600, true),
            "4/60/now" to TrainingConfig(4, 600, false),
            "2/60/green" to TrainingConfig(2, 600, true),
            "4/70/green" to TrainingConfig(4, 700, true),
        )
        fun run(s: String, b: String, tr: String): Simulation {
            val batch = if (b == "asap") BatchMode.Asap else BatchMode.Greenest
            val policy = Policy(shop.getValue(s), batch, training = trainings.getValue(tr))
            return Simulation.replay(scenario, policy, if (b == "green+replan") revised else emptySet())
        }
        val combos = listOf(
            Triple("auto 80", "asap", "4/100/now"), Triple("auto 80+min", "asap", "4/100/now"),
            Triple("all on", "green+replan", "4/100/now"), Triple("all on", "asap", "4/60/green"),
            Triple("all on", "asap", "4/100/green"),
            Triple("auto 80+min", "green+replan", "4/100/now"), Triple("auto 80+min", "asap", "4/60/green"),
            Triple("all on", "green+replan", "4/60/green"),
            Triple("auto 80+min", "green", "4/60/green"), Triple("auto 80+min", "green+replan", "4/60/green"),
            Triple("auto 80+min", "green+replan", "3/60/green"), Triple("auto 70+min", "green+replan", "4/60/green"),
            Triple("auto 80", "green+replan", "4/60/green"), Triple("auto 60", "green+replan", "4/60/green"),
            Triple("auto 80+min", "green+replan", "4/60/now"), Triple("auto 80+min", "green+replan", "4/100/green"),
            Triple("auto 80+min", "green+replan", "2/60/green"),
            Triple("auto 60", "asap", "4/100/now"), Triple("auto 90", "green+replan", "4/60/green"),
            Triple("auto 70", "green+replan", "4/60/green"), Triple("auto 80", "green", "4/60/green"),
            Triple("auto 80", "asap", "4/60/green"), Triple("auto 80", "green+replan", "4/100/green"),
            Triple("auto 80", "green+replan", "3/60/green"), Triple("auto 80", "green+replan", "4/70/green"),
        )
        val runs = combos.map { (s, b, tr) -> "$s, $b, $tr" to run(s, b, tr) }
        val lines = mutableListOf(title(scenario), HEADER + "  missed  training")
        for ((name, sim) in listOf("reference: all on, asap, 4/100/now" to reference) + runs) {
            val done = sim.totals.trainingFinishedAt?.let { GameTime(it).let { t -> "d${t.day + 1} ${t.clockText}" } } ?: "NOT DONE"
            lines += line(name, sim.totals, reference.totals) + "  " + sim.totals.deadlinesMissed + "  " + done +
                "  " + "★".repeat(scenario.stars(sim.totals, reference.totals))
        }
        val daily = (0 until scenario.days).map { d -> reference.history.filter { it.tick / GameTime.TICKS_PER_DAY == d }.map { it.intensityTenths }.average() / 10 }
        lines += "  grid by day: " + daily.joinToString(" ") { "%.0f".format(it) }
        report(11, lines)
        val byName = runs.toMap()
        fun stars(name: String) = scenario.stars(byName.getValue(name).totals, reference.totals)
        // One lever alone: one star – autoscaling, the training in the wind, the batch jobs in the wind.
        assertEquals(1, stars("auto 80, asap, 4/100/now"))
        assertEquals(1, stars("all on, asap, 4/60/green"))
        assertEquals(1, stars("all on, green+replan, 4/100/now"))
        // Two levers: two stars; all three: three – if the plan is made again after the forecast update, the
        // training is capped and the autoscaler leaves little headroom.
        assertEquals(2, stars("auto 80, asap, 4/60/green"))
        assertEquals(3, stars("auto 80, green+replan, 4/60/green"))
        assertEquals(3, stars("auto 70, green+replan, 4/60/green"))
        assertEquals(2, stars("auto 80, green, 4/60/green"))
        assertEquals(2, stars("auto 80, green+replan, 4/100/green"))
        assertEquals(2, stars("auto 60, green+replan, 4/60/green"))
        // The training waits for the wind and is still done in time.
        assertTrue(byName.getValue("auto 80, green+replan, 4/60/green").totals.trainingFinishedAt != null)
    }

    @Test fun e10RightSizedAi() {
        val scenario = Scenario.RIGHT_SIZED_AI
        val reference = Simulation.reference(scenario)
        fun config(code: String, window: Int) = InferenceConfig(code.split(" ").map { c ->
            when (c) { "N" -> Answerer.NONE; "S" -> Answerer.SMALL; "L" -> Answerer.LARGE; else -> Answerer.REASONING }
        }, window)
        fun run(code: String, window: Int) = Simulation.replay(scenario, scenario.initialPolicy.copy(inference = config(code, window)))
        val combos = listOf(
            "S S S S S" to 0, "N N N N N" to 0, "N L L L L" to 0, "N N L L L" to 0, "N N S L L" to 0,
            "N N S L L" to 250, "N N S L L" to 1_000, "N N S L L" to 2_000, "L L L L L" to 1_000, "L L L L L" to 250,
            "N N L L L" to 1_000, "N N S R L" to 1_000, "N S S L L" to 1_000, "N N S L S" to 1_000, "N N L L L" to 250,
        )
        val runs = combos.map { (c, w) -> "$c, ${w} ms" to run(c, w) }
        val lines = mutableListOf(title(scenario), HEADER + "  quality  on time  GPU servers")
        for ((name, sim) in listOf("reference: L L L L L, 0 ms" to reference) + runs) {
            val t = sim.totals
            val gpu = sim.history.sumOf { it.inference?.servers ?: 0 }.toDouble() / sim.history.size
            lines += line(name, t, reference.totals) + "  " + "%.1f".format(t.meanAnswerQuality / 10.0) + "  " +
                "%.1f".format(t.answersOnTime / 10.0) + "  " + "%.1f".format(gpu) + "  " + "★".repeat(scenario.stars(t, reference.totals))
        }
        report(104, lines)
        val byName = runs.toMap()
        fun stars(name: String) = scenario.stars(byName.getValue(name).totals, reference.totals)
        // One lever alone: one star – fewer large-model answers, or batching.
        assertEquals(1, stars("N N L L L, 0 ms"))
        assertEquals(1, stars("N N S L L, 0 ms"))
        assertEquals(1, stars("L L L L L, 1000 ms"))
        // No AI where none is needed plus batching: two; the small model for the product questions too: three.
        assertEquals(2, stars("N N L L L, 1000 ms"))
        assertEquals(3, stars("N N S L L, 1000 ms"))
        // The traps: small or no AI everywhere loses quality; reasoning and a long batching window make answers late.
        assertEquals(0, stars("S S S S S, 0 ms"))
        assertEquals(0, stars("N N S L S, 1000 ms"))
        assertEquals(0, stars("N N S R L, 1000 ms"))
        assertEquals(0, stars("N N S L L, 2000 ms"))
    }

    @Test fun e5DataDiet() {
        val scenario = Scenario.DATA_DIET
        val reference = Simulation.reference(scenario)
        // Code: format (J, W, A, L = AVIF live), responsive (r/-), lazy (o, b, a), video (A autoplay, C on click),
        // lists (L long, S short), tracking (6, 1, s).
        fun config(code: String): PageConfig {
            val c = code.split(" ")
            return PageConfig(
                format = when (c[0]) { "J" -> ImageFormat.JPEG; "W" -> ImageFormat.WEBP; "A" -> ImageFormat.AVIF; else -> ImageFormat.AVIF_LIVE },
                responsive = c[1] == "r",
                lazy = when (c[2]) { "o" -> LazyLoading.OFF; "b" -> LazyLoading.BELOW_FOLD; else -> LazyLoading.ALL },
                autoplay = c[3] == "A", longLists = c[4] == "L",
                tracking = when (c[5]) { "6" -> Tracking.SIX_TAGS; "1" -> Tracking.ONE_TAG; else -> Tracking.SERVER },
            )
        }
        fun run(code: String) = Simulation.replay(scenario, scenario.initialPolicy.copy(pages = config(code)))
        val codes = listOf(
            "J - o C L 6", "J - o A L 1", "J - o A S 6", "W - o A L 6", "A r b A L 6", "J - o C L 1", "J - o C S 1",
            "J - o C S s", "A r b C S 1", "A r b C S s", "W r b C S 1", "L r b C S s", "A r a C S s", "A r b C L 1",
            "A - o C S 1", "J r b C S 1", "A r o C S 1",
        )
        val runs = codes.map { it to run(it) }
        val lines = mutableListOf(title(scenario), HEADER + "  MB/visit  LCP s  DC kg  net kg  dev kg")
        for ((name, sim) in listOf("reference: J - o A L 6" to reference) + runs) {
            val t = sim.totals
            val dc = (t.operational - t.networkOperational - t.deviceOperational + t.embodied) / 1e9
            lines += line(name, t, reference.totals) + "  " + "%.1f".format(t.meanPageKb / 1_000.0) + "  " + "%.1f".format(t.meanLcp / 10.0) +
                "  " + "%.0f".format(dc) + "  " + "%.0f".format(t.networkOperational / 1e9) + "  " + "%.0f".format(t.deviceOperational / 1e9) +
                "  " + "★".repeat(scenario.stars(t, reference.totals))
        }
        report(105, lines)
        val byName = runs.toMap()
        fun stars(name: String) = scenario.stars(byName.getValue(name).totals, reference.totals)
        // The video alone halves the bytes, but six tracking tags keep the page too slow.
        assertEquals(0, stars("J - o C L 6"))
        assertEquals(1, stars("J - o C L 1"))
        assertEquals(2, stars("J - o C S 1"))
        // Lighter images in the right size on top: three stars.
        assertEquals(3, stars("A r b C S 1"))
        assertEquals(3, stars("W r b C S 1"))
        assertEquals(2, stars("A - o C S 1"))
        // The traps: AVIF converted on every request and lazy loading of the first image make the page slow.
        assertEquals(0, stars("L r b C S s"))
        assertEquals(0, stars("A r a C S s"))
    }

    @Test fun e11GreenPipeline() {
        val scenario = Scenario.GREEN_PIPELINE
        val reference = Simulation.reference(scenario)
        // Code: runners (A always on, D on demand), remote cache (c/-), test impact (t/-), heavy (E every commit,
        // N nightly, X never), trigger (1 every commit, 30, 120), environments (A always, O office hours, D on demand), replicas.
        fun config(code: String): PipelineConfig {
            val c = code.split(" ")
            return PipelineConfig(
                runners = if (c[0] == "A") Runners.ALWAYS_ON else Runners.ON_DEMAND, remoteCache = c[1] == "c", testImpact = c[2] == "t",
                heavy = when (c[3]) { "E" -> HeavyTests.EVERY_COMMIT; "N" -> HeavyTests.NIGHTLY; else -> HeavyTests.NEVER },
                trigger = when (c[4]) { "1" -> Trigger.EVERY_COMMIT; "30" -> Trigger.EVERY_30_MIN; else -> Trigger.EVERY_2_H },
                environments = when (c[5]) { "A" -> Environments.ALWAYS; "O" -> Environments.OFFICE_HOURS; else -> Environments.ON_DEMAND },
                replicas = c[6].toInt(),
            )
        }
        fun run(code: String) = Simulation.replay(scenario, scenario.initialPolicy.copy(pipeline = config(code)))
        val codes = listOf(
            "A - - N 1 A 3", "A - - E 1 O 3", "A - - E 1 A 1", "A - t E 1 A 3", "D - - E 1 A 3", "D c - E 1 A 3",
            "A - t N 1 O 1", "D c t N 1 O 1", "D c t N 1 D 1", "D - t N 1 O 1", "D c t X 1 O 1", "D c t N 30 O 1",
            "A - t N 30 O 1", "D c t N 120 O 1", "D c t E 1 O 1", "D c - N 1 O 1", "D c t N 1 O 3", "A - - N 1 O 3",
        )
        val runs = codes.map { it to run(it) }
        val lines = mutableListOf(title(scenario), HEADER + "  feedback min  releases broken  servers")
        for ((name, sim) in listOf("reference: A - - E 1 A 3" to reference) + runs) {
            val t = sim.totals
            lines += line(name, t, reference.totals) + "  " + "%.1f".format(t.meanFeedback / 10.0) + "  " + t.rulesBroken +
                "  " + "%.1f".format(t.meanServersOn / 1_000.0) + "  " + "★".repeat(scenario.stars(t, reference.totals))
        }
        report(106, lines)
        val byName = runs.toMap()
        fun stars(name: String) = scenario.stars(byName.getValue(name).totals, reference.totals)
        // One lever alone: one star.
        assertEquals(1, stars("A - - E 1 O 3"))
        assertEquals(1, stars("A - - N 1 A 3"))
        // Agents on demand with a remote cache, test impact analysis, the UI tests at night, environments only in
        // office hours with one replica: three stars; without the remote cache or with three replicas: two.
        assertEquals(3, stars("D c t N 1 O 1"))
        assertEquals(2, stars("D - t N 1 O 1"))
        assertEquals(2, stars("D c t N 1 O 3"))
        // The traps: agents on demand lose their local cache (feedback too slow); batching commits makes the
        // feedback too slow; without the UI tests the release goes out untested.
        assertEquals(0, stars("D - - E 1 A 3"))
        assertEquals(0, stars("D c t N 30 O 1"))
        assertEquals(0, stars("D c t X 1 O 1"))
    }

    @Test fun e7ChattyServices() {
        val scenario = Scenario.CHATTY_SERVICES
        val reference = Simulation.reference(scenario)
        // Code: cut (M, L modulith), protocol (J, G), master data (S, E), GPS TTL s, map TTL s, serverless (f/-).
        fun config(code: String): ServiceConfig {
            val c = code.split(" ")
            return ServiceConfig(
                cut = if (c[0] == "M") ServiceCut.MICROSERVICES else ServiceCut.MODULITH,
                protocol = if (c[1] == "J") Protocol.REST_JSON else Protocol.GRPC,
                masterData = if (c[2] == "S") MasterData.SYNC else MasterData.EVENTS,
                gpsTtl = c[3].toInt(), mapTtl = c[4].toInt(), serverless = c[5] == "f", extractOptimizer = c.getOrNull(6) == "x",
            )
        }
        fun run(code: String) = Simulation.replay(scenario, scenario.initialPolicy.copy(services = config(code)))
        val codes = listOf(
            "M J S 10 60 -", "M J S 60 300 -", "M J S 300 3600 -", "M G S 60 300 -", "M J E 60 300 -", "M G E 60 300 -",
            "M G E 60 300 f", "M J S 60 300 f", "L J S 60 300 -", "L G S 60 300 -", "L J S 0 0 -", "M J S 0 300 -",
            "M G E 10 60 f", "L J S 10 60 -", "L J S 60 300 - x", "L J S 0 0 - x",
        )
        val runs = codes.map { it to run(it) }
        val lines = mutableListOf(title(scenario), HEADER + "  latency ms  stale h  servers")
        for ((name, sim) in listOf("reference: M J S 0 0 -" to reference) + runs) {
            val t = sim.totals
            lines += line(name, t, reference.totals) + "  " + t.meanLatency + "  " + t.rulesBroken + "  " +
                "%.1f".format(t.meanServersOn / 1_000.0) + "  " + "★".repeat(scenario.stars(t, reference.totals))
        }
        report(107, lines)
        val byName = runs.toMap()
        fun stars(name: String) = scenario.stars(byName.getValue(name).totals, reference.totals)
        // The cache for the external calls makes the tracking fast enough: one star; gRPC on top: still one.
        assertEquals(1, stars("M J S 60 300 -"))
        assertEquals(1, stars("M G S 60 300 -"))
        // Events instead of synchronous calls: two; with gRPC too: two; plus serverless functions: three.
        assertEquals(2, stars("M J E 60 300 -"))
        assertEquals(2, stars("M G E 60 300 -"))
        assertEquals(3, stars("M G E 60 300 f"))
        // The modular monolith keeps capacity for the route optimization all day: two; with the optimization as a
        // service of its own: three.
        assertEquals(2, stars("L J S 60 300 -"))
        assertEquals(3, stars("L J S 60 300 - x"))
        // The traps: positions and traffic too old; serverless functions called on every request; no cache, too slow.
        assertEquals(0, stars("M J S 300 3600 -"))
        assertEquals(0, stars("M J S 60 300 f"))
        assertEquals(0, stars("L J S 0 0 -"))
    }

    @Test fun scenario12GreenOnPaper() {
        val scenario = Scenario.GREEN_ON_PAPER
        val reference = Simulation.reference(scenario)
        fun run(contract: Contract, auto: Boolean, greenest: Boolean) = Simulation.replay(scenario,
            Policy(if (auto) CapacityMode.Autoscale(800) else CapacityMode.Autoscale(500), if (greenest) BatchMode.Greenest else BatchMode.Asap, contract = contract))
        val runs = Contract.entries.flatMap { c -> listOf(false, true).flatMap { a -> listOf(false, true).map { g ->
            "${c.name.lowercase()}${if (a) ", auto 80" else ""}${if (g) ", greenest" else ""}" to run(c, a, g) } } }
        val lines = mutableListOf(title(scenario), HEADER + "  location kg  market kg  cost €  budget %")
        val refCost = reference.totals.energyCost
        for ((name, sim) in listOf("reference: grey, auto 50, asap" to reference) + runs) {
            val t = sim.totals
            lines += line(name, t, reference.totals) + "  " + "%.0f".format(t.locationOperational / 1e9) + "  " + "%.0f".format(t.marketOperational / 1e9) +
                "  " + "%.0f".format(t.energyCost / 1_000.0) + "  " + "%.0f".format(t.energyCost * 100.0 / refCost) + "  " + "★".repeat(scenario.stars(t, reference.totals))
        }
        lines += "  reference facility energy mWh: " + reference.totals.facilityEnergy
        report(12, lines)
        val byName = runs.toMap()
        fun stars(name: String) = scenario.stars(byName.getValue(name).totals, reference.totals)
        // Certificates: the report shows zero, the CO₂ in the hour does not change – no star.
        assertEquals(0, stars("certificates"))
        assertEquals(0L, byName.getValue("certificates").totals.marketOperational)
        // Efficiency alone, or the wind farm alone: one star; the wind farm with the batch jobs in the windy hours: two.
        assertEquals(1, stars("grey, auto 80"))
        assertEquals(1, stars("ppa"))
        assertEquals(2, stars("ppa, greenest"))
        assertEquals(2, stars("ppa, auto 80, greenest"))
        // 24/7 carbon-free energy: three stars – affordable only with less energy; without, over budget.
        assertEquals(3, stars("hourly, auto 80"))
        assertEquals(0, stars("hourly"))
    }

    @Test fun p1Inventory() {
        val scenario = Scenario.INVENTORY
        val model = scenario.portfolio!!
        val video = model.indexOf("Video conferencing")
        val sharing = model.indexOf("Cloud file sharing")
        val saas = model.apps.indices.filter { model.apps[it].hosting == Hosting.SAAS }.toSet()
        fun inv(key: OnPremKey, cloud: Boolean, asked: Set<Int>) = Inventory(key, setOf(OnPremKey.SERVERS, key), cloud, asked)
        val inventories = listOf(
            "servers, provider, spend" to inv(OnPremKey.SERVERS, false, emptySet()),
            "servers, recalculated, spend" to inv(OnPremKey.SERVERS, true, emptySet()),
            "cpu time, recalculated, spend" to inv(OnPremKey.CPU_TIME, true, emptySet()),
            "cpu time, recalculated, video + sharing" to inv(OnPremKey.CPU_TIME, true, setOf(video, sharing)),
            "power, provider, all suppliers" to inv(OnPremKey.POWER, false, saas),
            "power, recalculated, spend" to inv(OnPremKey.POWER, true, emptySet()),
            "power, recalculated, video + sharing" to inv(OnPremKey.POWER, true, setOf(video, sharing)),
            "power, recalculated, all suppliers" to inv(OnPremKey.POWER, true, saas),
        )
        val lines = mutableListOf("P1 Inventory: real footprint ${model.trueKg / 1_000} t", "  inventory                                inventory t  error %  data €  plan €  reduction %  stars")
        val results = inventories.associate { (name, i) ->
            val r = model.result(i, model.greedyPlan(i))
            lines += "  " + name.padEnd(40) + "%11d".format(r.inventoryKg / 1_000) + "%9.1f".format(r.inventoryError / 10.0) +
                "%8d".format(r.inventoryCost) + "%8d".format(r.planCost) + "%13.1f".format(r.reductionPermille / 10.0) + "  " + "★".repeat(model.stars(r, scenario.goals))
            name to r
        }
        val all = model.apps.indices.toList()
        var best = 0L
        for (mask in 0 until (1 shl all.size)) {
            val plan = all.filter { mask shr it and 1 == 1 }.toSet()
            if (model.planCost(plan) <= model.budget) best = maxOf(best, model.result(Inventory(), plan).reductionPermille)
        }
        lines += "  best plan with perfect knowledge and no data bought: %.1f %%".format(best / 10.0)
        report(201, lines)
        fun stars(name: String) = model.stars(results.getValue(name), scenario.goals)
        // The free data: the provider's market-based numbers make the cloud look clean – the inventory is far off.
        assertEquals(0, stars("servers, provider, spend"))
        assertEquals(0, stars("power, provider, all suppliers"))
        // Recalculating the cloud: one or two stars; CPU time hides the idle test environments.
        assertEquals(2, stars("servers, recalculated, spend"))
        assertEquals(2, stars("cpu time, recalculated, video + sharing"))
        // Measured power and the cloud recalculated: three stars.
        assertEquals(3, stars("power, recalculated, spend"))
        assertEquals(3, stars("power, recalculated, video + sharing"))
        assertEquals(3, stars("power, recalculated, all suppliers"))
    }

    @Test fun p2WhereToStart() {
        val scenario = Scenario.WHERE_TO_START
        val model = scenario.abatement!!
        val lines = mutableListOf("P2 Where to start: baseline ${model.baselineKg / 1_000} t over ${model.years} years, budget ${model.budget} €",
            "  measure                                                      consultant €/t  t/3y  real t alone")
        for (i in model.consultantOrder) {
            val m = model.measures[i]
            lines += "  " + m.title.take(60).padEnd(60) + "%8d".format(model.consultantEuroPerTonne(i)) + "%7d".format(model.consultantKg(i) / 1_000) + "%9d".format(model.savedKg(setOf(i)) / 1_000)
        }
        fun idx(vararg titles: String) = titles.map { model.indexOf(it).also { i -> check(i >= 0) { it } } }.toSet()
        val plans = listOf(
            "along the consultant's curve" to model.alongTheCurve(),
            "curve without certificates" to model.alongTheCurve() - idx("Guarantees of origin for the own data center"),
            "best plan" to model.best(),
            "without certificates and laptops, no cloud move" to idx("Switch the test environments off at night and at weekends",
                "Move the analytics platform to Stockholm", "Rightsize the analytics cluster from 120 to 60 VMs", "Smaller recommendation model, trained once a week",
                "Camera off by default in large meetings", "Retention rule for the old videos", "Build agents only on demand",
                "Shorter retention, cold storage class"),
        )
        val results = plans.associate { (name, plan) ->
            val r = model.result(plan)
            lines += "  " + name.padEnd(58) + "%8d €".format(r.costEuro) + "%7d t".format(r.savedKg / 1_000) + "%7.1f %%".format(r.reductionPermille / 10.0) + "  " + "★".repeat(model.stars(r, scenario.goals))
            lines += "    " + plan.sorted().joinToString(", ") { model.measures[it].title.take(28) }
            name to r
        }
        report(202, lines)
        fun stars(name: String) = model.stars(results.getValue(name), scenario.goals)
        // Along the curve: certificates first, the test environments and the analytics cluster twice – one star.
        assertEquals(1, stars("along the consultant's curve"))
        assertEquals(3, stars("best plan"))
        // Leaving out the second measure for the test environments costs the third star: overlapping measures still add a little.
        assertEquals(2, stars("without certificates and laptops, no cloud move"))
        // The laptops save less than making them emits within three years.
        assertTrue(model.savedKg(idx("Replace the 600 oldest desktop PCs by laptops")) < 0)
        assertEquals(0L, model.savedKg(idx("Guarantees of origin for the own data center")))
    }

    @Test fun p3TidyUp() {
        val scenario = Scenario.TIDY_UP
        val model = scenario.tidy!!
        fun c(d: Decision, start: Int, off: Boolean = true) = Choice(d, start, off)
        val careful = mapOf(
            "Old test environments" to c(Decision.ELIMINATE, 0), "Old reporting system" to c(Decision.ELIMINATE, 0), "Old intranet" to c(Decision.ELIMINATE, 0),
            "Web shop" to c(Decision.INVEST, 1), "Fax server" to c(Decision.ELIMINATE, 2), "Wiki" to c(Decision.ELIMINATE, 1),
            "Data warehouse" to c(Decision.INVEST, 2), "Time recording" to c(Decision.ELIMINATE, 3), "Warehouse management" to c(Decision.MIGRATE, 3),
            "CRM" to c(Decision.INVEST, 4), "Old customer portal" to c(Decision.MIGRATE, 5), "ERP" to c(Decision.INVEST, 5),
        )
        val plans = listOf(
            "by quadrant, old systems switched off" to model.byQuadrant(true),
            "by quadrant, old systems left running" to model.byQuadrant(false),
            "careful: quick wins first, ERP tuned" to careful,
            "careful, ERP migrated late" to careful + ("ERP" to c(Decision.MIGRATE, 5)),
            "careful, old systems left running" to careful.mapValues { it.value.copy(decommission = false) },
            "careful, mail archive eliminated" to careful + ("Mail archive" to c(Decision.ELIMINATE, 9)),
        )
        val lines = mutableListOf("P3 Tidy up with TIME: baseline ${model.baselineKg / 1_000} t over ${model.quarters / 4} years, ${model.capacity} projects at a time")
        val results = plans.associate { (name, plan) ->
            val r = model.result(plan)
            lines += "  " + name.padEnd(42) + "%7d t".format(r.planKg / 1_000) + "%7.1f %%".format(r.reductionPermille / 10.0) +
                "  load " + model.load(plan).joinToString("") + "  " + "★".repeat(model.stars(r, scenario.goals)) +
                (if (r.overloaded.isNotEmpty()) "  overloaded" else "") + (if (r.wronglyEliminated.isNotEmpty()) "  eliminated: ${r.wronglyEliminated}" else "")
            name to r
        }
        report(203, lines)
        fun stars(name: String) = model.stars(results.getValue(name), scenario.goals)
        assertEquals(1, stars("by quadrant, old systems switched off"))
        assertEquals(1, stars("by quadrant, old systems left running"))
        assertEquals(3, stars("careful: quick wins first, ERP tuned"))
        assertEquals(2, stars("careful, ERP migrated late"))
        assertEquals(2, stars("careful, old systems left running"))
        assertEquals(0, stars("careful, mail archive eliminated"))
    }

    @Test fun p4LeaveTheCloud() {
        val scenario = Scenario.LEAVE_THE_CLOUD
        val model = scenario.repatriation!!
        val fra = Move(Place.CLOUD_FRANKFURT); val sto = Move(Place.CLOUD_STOCKHOLM)
        val colo = Move(Place.COLOCATION); val room = Move(Place.SERVER_ROOM)
        val plans = listOf(
            "all stays in Frankfurt" to emptyMap(),
            "all to the colocation" to model.all(Place.COLOCATION),
            "steady ones to the colocation" to mapOf("ERP database" to colo, "Customer data archive" to colo, "Data warehouse" to colo),
            "all but the shop to the colocation" to model.all(Place.COLOCATION) + ("Web shop" to fra),
            "all to Stockholm" to model.all(Place.CLOUD_STOCKHOLM),
            "Stockholm, data and ERP in Frankfurt" to model.all(Place.CLOUD_STOCKHOLM) + ("Customer data archive" to fra) + ("ERP database" to fra),
            "ERP to the colocation, tests off at night" to mapOf("ERP database" to colo, "Test environments" to Move(Place.CLOUD_FRANKFURT, true)),
            "example" to model.example,
            "example, tests in the server room" to model.example + ("Test environments" to Move(Place.SERVER_ROOM, true)),
            "example, archive in Frankfurt" to model.example + ("Customer data archive" to fra),
            "example, ERP to Stockholm" to model.example + ("ERP database" to sto),
        )
        val lines = mutableListOf("P4 Leave the cloud?: baseline ${model.result(emptyMap()).baselineKg / 1_000} t, " +
            "${model.result(emptyMap()).baselineEuro / 1_000} k€ over ${model.years} years")
        val results = plans.associate { (name, plan) ->
            val r = model.result(plan)
            lines += "  " + name.padEnd(42) + "%7d t".format(r.planKg / 1_000) + "%7.1f %%".format(r.reductionPermille / 10.0) +
                "  cost %6.1f %%".format(-r.savingPermille / 10.0) + "  report %4d t".format(r.reportKg / 1_000) +
                "  booking %5d ms".format(r.bookingMs) + "  room ${model.roomUsed(plan)}  " + "★".repeat(model.stars(r, scenario.goals)) +
                "  ${r.breakdown}"
            name to r
        }
        report(204, lines)
        fun stars(name: String) = model.stars(results.getValue(name), scenario.goals)
        assertEquals(0, stars("all stays in Frankfurt"))
        assertEquals(0, stars("all to the colocation"))
        assertEquals(1, stars("steady ones to the colocation"))
        assertEquals(1, stars("all but the shop to the colocation"))
        assertEquals(0, stars("all to Stockholm"))
        assertEquals(0, stars("Stockholm, data and ERP in Frankfurt"))
        assertEquals(3, stars("example"))
        assertEquals(3, stars("example, tests in the server room"))
        assertEquals(0, stars("example, ERP to Stockholm"))
        val r = results.getValue("example")
        val b = r.breakdown
        assertTrue(kotlin.math.abs(r.planKg - r.baselineKg - (b.region + b.energy + b.pue + b.embodied)) <= 3)
    }

    @Test fun e1HowManyNines() {
        val scenario = Scenario.HOW_MANY_NINES
        val model = scenario.availability!!
        fun arch(vararg t: Pair<String, Tactic>, d: Set<String> = emptySet()) = Architecture(model.all(Tactic.ACTIVE_ACTIVE).tactics + t, d)
        val own = arch("Login" to Tactic.SINGLE, "Policy database" to Tactic.SINGLE, "Claims intake" to Tactic.WARM_STANDBY,
            "Contract overview" to Tactic.COLD_STANDBY, "Tariff calculator" to Tactic.COLD_STANDBY, "Recommendations" to Tactic.SINGLE, "Reporting" to Tactic.SINGLE)
        val plans = listOf(
            "everything active-active (reference)" to model.reference,
            "everything warm standby" to model.all(Tactic.WARM_STANDBY),
            "everything cold standby" to model.all(Tactic.COLD_STANDBY),
            "each block by its own scenario" to own,
            "the same, shared blocks warm" to Architecture(own.tactics + ("Login" to Tactic.WARM_STANDBY) + ("Policy database" to Tactic.WARM_STANDBY)),
            "the same, shared blocks active-active, circuit breaker" to Architecture(own.tactics + ("Login" to Tactic.ACTIVE_ACTIVE) + ("Policy database" to Tactic.ACTIVE_ACTIVE), setOf("fallback")),
            "example without decoupling" to Architecture(model.example.tactics + ("Policy database" to Tactic.WARM_STANDBY) + ("Recommendations" to Tactic.COLD_STANDBY), emptySet()),
            "example, circuit breaker only" to Architecture(model.example.tactics + ("Policy database" to Tactic.WARM_STANDBY) + ("Contract overview" to Tactic.COLD_STANDBY), setOf("fallback")),
            "example" to model.example,
        )
        val lines = mutableListOf("E1 How many nines?: reference ${model.kgPerYear(model.reference).toLong() / 1_000} t a year; " +
            Tactic.entries.joinToString { "$it %.4f %%".format(model.availability(it) * 100) })
        val results = plans.associate { (name, plan) ->
            val r = model.result(plan)
            lines += "  " + name.padEnd(56) + "%6d t".format(r.planKg / 1_000) + "%7.1f %%".format(-r.reductionPermille / 10.0) +
                "  servers %5.1f".format(r.serversOn) + "  " + "★".repeat(model.stars(r, scenario.goals)) +
                "  " + r.downtime.entries.joinToString { "${it.key} %.0f min".format(it.value) } + (if (r.missed.isNotEmpty()) "  missed ${r.missed}" else "")
            name to r
        }
        report(108, lines)
        fun stars(name: String) = model.stars(results.getValue(name), scenario.goals)
        assertEquals(1, stars("everything active-active (reference)"))
        assertEquals(1, stars("everything warm standby"))
        assertEquals(0, stars("everything cold standby"))
        assertEquals(0, stars("each block by its own scenario"))
        assertEquals(0, stars("the same, shared blocks warm"))
        assertEquals(2, stars("example without decoupling"))
        assertEquals(3, stars("example"))
    }

    @Test fun e8ConstantAsking() {
        val scenario = Scenario.CONSTANT_ASKING
        val model = scenario.updates!!
        val ref = model.reference
        val plans = listOf(
            "today (reference)" to ref,
            "today, push for the status" to ref.copy(closed = ref.closed + ("Delivery status" to Closed.PUSH)),
            "push status, conditional requests" to ref.copy(closed = ref.closed + ("Delivery status" to Closed.PUSH), conditional = true),
            "push status, conditional, bundled" to ref.copy(closed = ref.closed + ("Delivery status" to Closed.PUSH), conditional = true, bundled = true),
            "WebSocket everywhere" to UpdatePlan(model.kinds.associate { it.name to Open.Stream },
                mapOf("Delivery status" to Closed.SOCKET, "Offers" to Closed.SOCKET, "Unread messages" to Closed.SOCKET)),
            "push everywhere" to UpdatePlan(model.kinds.associate { it.name to Open.Push }, mapOf("Delivery status" to Closed.PUSH, "Offers" to Closed.PUSH, "Unread messages" to Closed.PUSH)),
            "offers on opening, badge refresh bundled" to UpdatePlan(
                mapOf("Delivery status" to Open.Poll(60), "Courier on the map" to Open.Poll(5), "Offers" to Open.OnOpen, "Support chat" to Open.LongPoll, "Unread messages" to Open.Poll(60)),
                mapOf("Delivery status" to Closed.PUSH, "Offers" to Closed.NONE, "Unread messages" to Closed.REFRESH), conditional = true, bundled = true),
            "example" to model.example,
        )
        val r0 = model.result(ref)
        val lines = mutableListOf("E8 Constant asking: reference ${r0.referenceKg / 1_000} t a year (phones ${r0.phonesKg / 1_000} t, servers ${r0.serversKg / 1_000} t, network ${r0.networkKg / 1_000} t, rest ${r0.otherKg / 1_000} t)")
        val results = plans.associate { (name, plan) ->
            val r = model.result(plan)
            lines += "  " + name.padEnd(44) + "%6d t".format(r.planKg / 1_000) + "%7.1f %%".format(-r.reductionPermille / 10.0) +
                "  phones %5d  servers %4d  network %5d kg".format(r.phonesKg, r.serversKg, r.networkKg) + "  " + "★".repeat(model.stars(r, scenario.goals)) +
                (if (r.missed.isNotEmpty()) "  missed ${r.missed}" else "")
            name to r
        }
        report(109, lines)
        fun stars(name: String) = model.stars(results.getValue(name), scenario.goals)
        assertEquals(0, stars("today (reference)"))
        assertEquals(1, stars("today, push for the status"))
        assertEquals(2, stars("push status, conditional, bundled"))
        assertEquals(1, stars("WebSocket everywhere"))
        assertEquals(0, stars("push everywhere"))
        assertEquals(2, stars("offers on opening, badge refresh bundled"))
        assertEquals(3, stars("example"))
    }

    @Test fun e6KeepEverything() {
        val scenario = Scenario.KEEP_EVERYTHING
        val model = scenario.lifecycle!!
        val ref = model.reference
        val ex = model.example
        fun with(kind: String, tier: String? = null, keep: String? = null) = ex.copy(
            tiers = if (tier != null) ex.tiers + (kind to tier) else ex.tiers, keep = if (keep != null) ex.keep + (kind to keep) else ex.keep)
        val plans = listOf(
            "today (reference)" to ref,
            "today, accounts deleted after 3 years" to ref.copy(keep = ref.keep + ("Inactive customer accounts" to "3")),
            "everything warm, kept as today" to ref.copy(tiers = model.kinds.associate { it.name to "warm" }, keep = ref.keep + ("Inactive customer accounts" to "3")),
            "everything cold, kept as today" to ref.copy(tiers = model.kinds.associate { it.name to "cold" }, keep = ref.keep + ("Inactive customer accounts" to "3")),
            "only the legal minimum, everything warm" to DataPlan(model.kinds.associate { it.name to "warm" },
                mapOf("Orders and invoices" to "8", "Images of old products" to "1", "Inactive customer accounts" to "3", "Logs" to "1", "Monthly backups" to "1", "Click events" to "aggregates")),
            "example, orders warm" to with("Orders and invoices", tier = "warm"),
            "example, orders cold" to with("Orders and invoices", tier = "cold"),
            "example, all logs a year" to with("Logs", keep = "1"),
            "example, logs cold" to with("Logs", tier = "cold"),
            "example, images for good" to with("Images of old products", keep = "forever"),
            "example, backups for good" to with("Monthly backups", keep = "forever"),
            "example, events for good" to with("Click events", keep = "forever"),
            "example, events 2 years" to with("Click events", keep = "2"),
            "example" to ex,
        )
        val r0 = model.result(ref)
        val lines = mutableListOf("E6 Keep everything: reference ${r0.referenceKg / 1_000} t a year, ${r0.tb.values.sum().toInt()} TB")
        val results = plans.associate { (name, plan) ->
            val r = model.result(plan)
            lines += "  " + name.padEnd(44) + "%6.1f t".format(r.planKg / 1_000.0) + "%7.1f %%".format(-r.reductionPermille / 10.0) +
                "  hot %5.0f  warm %5.0f  cold %5.0f TB".format(r.tb[StorageTier.HOT], r.tb[StorageTier.WARM], r.tb[StorageTier.COLD]) + "  " +
                "★".repeat(model.stars(r, scenario.goals)) + (if (r.missed.isNotEmpty()) "  missed ${r.missed.map { it.kind }}" else "")
            name to r
        }
        report(111, lines)
        fun stars(name: String) = model.stars(results.getValue(name), scenario.goals)
        assertEquals(0, stars("today (reference)"))
        assertEquals(1, stars("today, accounts deleted after 3 years"))
        assertEquals(1, stars("everything warm, kept as today"))
        assertEquals(0, stars("everything cold, kept as today"))
        assertEquals(2, stars("only the legal minimum, everything warm"))
        assertEquals(0, stars("example, orders cold"))
        assertEquals(0, stars("example, logs cold"))
        assertEquals(0, stars("example, events 2 years"))
        assertEquals(2, stars("example, events for good"))
        assertEquals(2, stars("example, backups for good"))
        assertEquals(3, stars("example"))
    }

    @Test fun e9OnePerCustomer() {
        val scenario = Scenario.ONE_PER_CUSTOMER
        val model = scenario.tenancy!!
        val ex = model.example
        fun all(t: Tenancy) = model.groups.associate { it.name to t }
        fun with(component: String, group: String, t: Tenancy) =
            TenancyPlan(ex.choices + (component to (ex.choices.getValue(component) + (group to t))))
        val regOwn = TenancyPlan(ex.choices.mapValues { (_, m) -> m + ("Regulated customers" to Tenancy.OWN) })
        val bigOwn = TenancyPlan(ex.choices.mapValues { (_, m) -> m + ("Large customer" to Tenancy.OWN) })
        val plans = listOf(
            "today (reference)" to model.reference,
            "everything shared" to TenancyPlan(model.components.associate { it.name to all(Tenancy.SHARED) }),
            "shared, databases bridge" to TenancyPlan(model.components.associate { it.name to all(if (it.name == "Database") Tenancy.BRIDGE else Tenancy.SHARED) }),
            "only the other customers share" to TenancyPlan(model.components.associate { it.name to all(Tenancy.OWN) + ("Other customers" to if (it.name == "Database") Tenancy.BRIDGE else Tenancy.SHARED) }),
            "example, regulated customers all own" to regOwn,
            "example, large customer all own" to bigOwn,
            "example, other customers own databases" to with("Database", "Other customers", Tenancy.OWN),
            "example, other customers one database" to with("Database", "Other customers", Tenancy.SHARED),
            "example, large customer in the payroll pool" to with("Payroll run", "Large customer", Tenancy.SHARED),
            "example" to ex,
        )
        val r0 = model.result(model.reference)
        val lines = mutableListOf("E9 One per customer?: reference ${r0.referenceKg / 1_000} t a year, ${r0.referenceServers} servers")
        val results = plans.associate { (name, plan) ->
            val r = model.result(plan)
            lines += "  " + name.padEnd(44) + "%6.1f t".format(r.planKg / 1_000.0) + "%7.1f %%".format(-r.reductionPermille / 10.0) +
                "  %4d servers".format(r.servers) + "  " + "★".repeat(model.stars(r, scenario.goals)) +
                (if (r.missed.isNotEmpty()) "  missed ${r.missed.map { it.rule.name + " " + it.component }}" else "")
            name to r
        }
        report(110, lines)
        fun stars(name: String) = model.stars(results.getValue(name), scenario.goals)
        assertEquals(1, stars("today (reference)"))
        assertEquals(0, stars("everything shared"))
        assertEquals(2, stars("only the other customers share"))
        assertEquals(2, stars("example, regulated customers all own"))
        assertEquals(2, stars("example, large customer all own"))
        assertEquals(0, stars("example, other customers one database"))
        assertEquals(0, stars("example, large customer in the payroll pool"))
        assertEquals(3, stars("example"))
    }

    @Test fun e12OnePattern() {
        val scenario = Scenario.ONE_PATTERN
        val model = scenario.patterns!!
        val ex = model.example
        fun with(sub: String, id: String) = PatternPlan(ex.choices + (sub to id))
        val plans = listOf(
            "today (reference)" to model.reference,
            "simplest everywhere" to PatternPlan(model.subsystems.associate { it.name to it.options.minBy { o -> model.kg(o) }.id }),
            "example, timetable layers" to with("Timetable information", "layers"),
            "example, timetable with events" to with("Timetable information", "es-cqrs"),
            "example, booking CQRS" to with("Booking", "cqrs"),
            "example, booking with events" to with("Booking", "es-cqrs"),
            "example, payment layers" to with("Payment", "layers"),
            "example, payment with CQRS" to with("Payment", "es-cqrs"),
            "example, notifications polling" to with("Delay notifications", "poll"),
            "example, notifications to all" to with("Delay notifications", "all"),
            "example, analytics Lambda" to with("Analytics", "lambda"),
            "example, analytics batch" to with("Analytics", "batch"),
            "example, import as services" to with("Timetable import", "services"),
            "example, import script" to with("Timetable import", "script"),
            "example" to ex,
        )
        val r0 = model.result(model.reference)
        val lines = mutableListOf("E12 One pattern for everything?: reference ${r0.referenceKg / 1_000} t a year, ${r0.servers} servers, ${r0.storageTb} TB")
        val results = plans.associate { (name, plan) ->
            val r = model.result(plan)
            lines += "  " + name.padEnd(44) + "%6.1f t".format(r.planKg / 1_000.0) + "%7.1f %%".format(-r.reductionPermille / 10.0) +
                "  %5.1f servers %5.0f TB".format(r.servers, r.storageTb) + "  " + "★".repeat(model.stars(r, scenario.goals)) +
                (if (r.missed.isNotEmpty()) "  missed ${r.missed}" else "")
            name to r
        }
        report(112, lines)
        fun stars(name: String) = model.stars(results.getValue(name), scenario.goals)
        assertEquals(1, stars("today (reference)"))
        assertEquals(0, stars("simplest everywhere"))
        assertEquals(0, stars("example, payment layers"))
        assertEquals(0, stars("example, analytics batch"))
        assertEquals(0, stars("example, import script"))
        assertEquals(1, stars("example, timetable layers"))
        assertEquals(2, stars("example, booking CQRS"))
        assertEquals(2, stars("example, payment with CQRS"))
        assertEquals(2, stars("example, analytics Lambda"))
        assertEquals(2, stars("example, import as services"))
        assertEquals(1, stars("example, notifications to all"))
        assertEquals(3, stars("example"))
    }

    /** D31: the solution the debrief shows and puts into the task earns three stars in every scenario with a clock. */
    @Test fun solutionsEarnThreeStars() {
        val lines = mutableListOf("Solutions (D31)")
        val failed = mutableListOf<String>()
        for (s in Scenario.ALL) {
            Solutions.of(s) ?: continue
            val stars = Solutions.stars(s)
            lines += "  ${s.label.padEnd(4)} ${s.title.padEnd(28)} " + "★".repeat(stars)
            if (stars < 3) failed += s.label
        }
        report(0, lines)
        assertTrue(failed.isEmpty(), "solutions below three stars: $failed")
    }

    /**
     * A player follows the solution by hand, not to the tick: the servers of O1 switched up to 30 minutes late, the
     * new plan of O12 (no pause when the forecast changes) up to two hours late. Still three stars.
     */
    @Test fun solutionsHoldWhenPlayedByHand() {
        val failed = mutableListOf<String>()
        val night = Scenario.NIGHT_SHIFT
        val plan = Solutions.serverPlan(night)
        for (lag in 0..2) {
            val changes = plan.entries.associate { (tick, n) ->
                (if (tick == night.startTick) tick else tick + lag) to night.initialPolicy.copy(capacity = CapacityMode.Manual(n))
            }
            val totals = Simulation.replay(night, changes.getValue(night.startTick), changes = changes).totals
            if (night.stars(totals, Simulation.reference(night).totals) < 3) failed += "O1 +${lag * 15} min"
        }
        for (s in Scenario.ALL) {
            val sol = Solutions.of(s) ?: continue
            if (sol.replanAt.isEmpty() || s.controls.size < 2) continue // a single-control scenario pauses for the new forecast
            for (lag in listOf(4, 8)) {
                val totals = Simulation.replay(s, sol.policy ?: s.initialPolicy, sol.replanAt.map { it + lag }.toSet(), sol.changes).totals
                if (s.stars(totals, Solutions.referenceTotals(s)) < 3) failed += "${s.label} replan +${lag * 15} min"
            }
        }
        assertTrue(failed.isEmpty(), "solutions that fail when played a little late: $failed")
    }
}
