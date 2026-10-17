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
