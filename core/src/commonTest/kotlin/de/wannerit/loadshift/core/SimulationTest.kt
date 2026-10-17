package de.wannerit.loadshift.core

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SimulationTest {
    private fun scenario(peak: Int, servers: Int = 10, days: Int = 1) = Scenario(
        number = 0, title = "Test", setting = GridSetting(GridRegion.GERMANY, Season.AUTUMN),
        weather = WeatherMode.Fixed(1_000), days = days, seed = 5,
        fleet = listOf(FleetEntry(ServerKind.CPU_CURRENT, servers)), interactive = InteractiveLoad(peak),
    )

    @Test fun serverPowerIsIdlePlusTheDynamicPart() {
        val server = Server(0, ServerKind.CPU_CURRENT)
        assertEquals(156, server.power(0))
        assertEquals(378, server.power(500))
        assertEquals(600, server.power(1_000))
        assertEquals(156, server.copy(state = ServerState.Booting(3)).power(0))
        assertEquals(0, server.copy(state = ServerState.Off).power(1_000))
    }

    @Test fun anIdleDataCenterBooksIdlePowerTimesPue() {
        val simulation = Simulation(scenario(0), Policy.REFERENCE)
        simulation.step()
        val record = simulation.history[0]
        assertEquals(10 * 156, record.itPower)
        assertEquals(10 * 156 * 1_520 / 1_000, record.facilityPower)
        assertEquals(record.facilityPower * 250L, simulation.totals.facilityEnergy)
        assertEquals(record.facilityPower * 250L * record.intensityTenths / 10, simulation.totals.operational)
        assertEquals(10 * ServerKind.CPU_CURRENT.embodiedPerTick, simulation.totals.embodied)
    }

    @Test fun embodiedEmissionsOfADayMatchTheLifespan() {
        // 550 kg over 6 × 365 days: about 251 g per day.
        assertEquals(251L, ServerKind.CPU_CURRENT.embodiedPerTick * GameTime.TICKS_PER_DAY / 1_000_000)
    }

    @Test fun sameScenarioSameResult() {
        val a = Simulation.replay(Scenario.NIGHT_SHIFT, Policy(CapacityMode.Autoscale(700)))
        val b = Simulation.replay(Scenario.NIGHT_SHIFT, Policy(CapacityMode.Autoscale(700)))
        assertEquals(a.totals, b.totals)
        assertEquals(a.history, b.history)
    }

    @Test fun allOnKeepsEveryServerRunning() {
        val simulation = Simulation.replay(scenario(150), Policy.REFERENCE)
        assertTrue(simulation.history.all { it.serversOn == 10 })
        assertEquals(1_000, simulation.totals.sla)
    }

    @Test fun switchedOnServersTakeWorkAfterBooting() {
        val simulation = Simulation(scenario(100), Policy(CapacityMode.Manual(0)))
        simulation.step()
        assertEquals(0, simulation.history.last().capacity)
        simulation.policy = Policy(CapacityMode.Manual(4))
        simulation.step()
        simulation.step()
        assertEquals(0, simulation.history.last().capacity, "still booting")
        simulation.step()
        assertEquals(80, simulation.history.last().capacity)
    }

    @Test fun overloadedServersServeSlowly() {
        // Two servers (capacity 40) at a demand far above it: served in time = 34 + 6 / 2.
        val simulation = Simulation(scenario(1_000), Policy(CapacityMode.Manual(2)))
        simulation.step()
        assertEquals(40, simulation.history[0].served)
        assertEquals(37, simulation.history[0].inTime)
        assertEquals(37L, simulation.totals.inTime)
    }

    @Test fun autoscalingUsesFewerServersAndKeepsTheSla() {
        val reference = Simulation.replay(Scenario.NIGHT_SHIFT, Policy.REFERENCE)
        val scaled = Simulation.replay(Scenario.NIGHT_SHIFT, Policy(CapacityMode.Autoscale(700)))
        assertTrue(scaled.totals.serverTicks < reference.totals.serverTicks * 3 / 4)
        assertTrue(scaled.totals.sla >= Scenario.NIGHT_SHIFT.slaTarget)
        assertEquals(reference.totals.embodied, scaled.totals.embodied, "switching off saves no embodied emissions")
    }

    @Test fun theProjectionUsesTheForecastAndLeavesTheSimulationUntouched() {
        val simulation = Simulation(Scenario.NIGHT_SHIFT, Policy.REFERENCE)
        repeat(20) { simulation.step() }
        val before = simulation.totals.copy()
        val projected = simulation.projected(Policy(CapacityMode.Autoscale(700)))
        assertEquals(before, simulation.totals)
        assertEquals(Scenario.NIGHT_SHIFT.startTick + 20, simulation.tick)
        assertEquals(GameTime.TICKS_PER_DAY, projected.ticks)
        assertTrue(projected.emissions < simulation.projected(Policy.REFERENCE).emissions)
        val real = simulation.copy()
        real.policy = Policy(CapacityMode.Autoscale(700))
        real.run()
        assertTrue(abs(real.totals.emissions - projected.emissions) * 5 < real.totals.emissions)
    }

    @Test fun starsNeedTheRulesFirst() {
        val scenario = Scenario.NIGHT_SHIFT
        val reference = Simulation.reference(scenario).totals
        val cheap = Totals(demand = 1_000, served = 1_000, inTime = 900, capacityTicks = 1_000, operational = reference.emissions / 2)
        assertEquals(0, scenario.stars(cheap, reference), "SLA missed: no star at all")
        cheap.inTime = 1_000
        assertEquals(3, scenario.stars(cheap, reference))
    }

    @Test fun emissionsSoFarFollowTheTotals() {
        val simulation = Simulation.reference(Scenario.NIGHT_SHIFT)
        assertEquals(simulation.totals.emissions, simulation.history.last().emissionsSoFar)
        assertEquals(simulation.totals.emissions, simulation.emissions(Scenario.NIGHT_SHIFT.endTick + 5))
        assertTrue(simulation.emissions(40) < simulation.totals.emissions / 2)
    }
}
