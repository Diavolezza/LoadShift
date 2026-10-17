package de.wannerit.loadshift.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BatchTest {
    private fun scenario(jobs: List<BatchJobSpec>, servers: Int = 6) = Scenario(
        number = 0, title = "Test", setting = GridSetting(GridRegion.SPAIN, Season.YEAR),
        weather = WeatherMode.Fixed(1_000), days = 2, seed = 4,
        fleet = listOf(FleetEntry(ServerKind.CPU_CURRENT, servers)), interactive = InteractiveLoad(0),
        controls = setOf(Control.BATCH_BY_HAND), initialPolicy = Policy(CapacityMode.Manual(0), BatchMode.ByHand),
        referencePolicy = Policy(CapacityMode.Manual(0), BatchMode.Asap), batchJobs = jobs,
    )

    private val report = BatchJobSpec.of("Report", 4, 3, GameTime.at(0, 0), GameTime.at(0, 18))
    private val asap = Policy(CapacityMode.Manual(0), BatchMode.Asap)

    @Test fun aJobBootsThenWorksThenReleasesItsServers() {
        val simulation = Simulation(scenario(listOf(report)), asap)
        simulation.step()
        assertEquals(BatchJobState.Started(0, 2), simulation.jobs[0].state)
        assertEquals(4, simulation.history[0].batchServers)
        assertEquals(4 * 156, simulation.history[0].itPower, "booting servers draw idle power")
        repeat(2) { simulation.step() }
        assertEquals(4 * 600, simulation.history[2].itPower, "working servers draw full power")
        repeat(12) { simulation.step() }
        assertEquals(BatchJobState.Done(0, 14), simulation.jobs[0].state)
        assertEquals(0, simulation.history.last().itPower, "released servers are off")
        assertEquals(1, simulation.totals.jobsDone)
    }

    @Test fun aPlannedJobStartsAtItsTime() {
        val simulation = Simulation(scenario(listOf(report)))
        val eleven = GameTime.at(0, 11).tick
        simulation.plan(0, eleven)
        while (simulation.tick < eleven) simulation.step()
        assertEquals(0L, simulation.totals.batchEnergy)
        simulation.step()
        assertEquals(eleven, simulation.jobs[0].startedAt)
    }

    @Test fun anUnplannedJobMissesItsDeadline() {
        val simulation = Simulation.replay(scenario(listOf(report)), Policy(CapacityMode.Manual(0), BatchMode.ByHand))
        assertEquals(1, simulation.totals.deadlinesMissed)
        assertEquals(0, simulation.totals.jobsDone)
    }

    @Test fun aJobWaitsForFreeServers() {
        val second = BatchJobSpec.of("Import", 4, 2, GameTime.at(0, 0), GameTime.at(1, 0))
        val simulation = Simulation(scenario(listOf(report, second)), asap)
        simulation.step()
        assertTrue(simulation.jobs[0].state.isStarted)
        assertEquals(BatchJobState.Planned(0), simulation.jobs[1].state, "only six servers: the second job waits")
        simulation.run()
        assertEquals(14, simulation.jobs[1].startedAt)
        assertEquals(0, simulation.totals.deadlinesMissed)
    }

    @Test fun finishingLateCountsOnce() {
        val simulation = Simulation(scenario(listOf(report)))
        simulation.plan(0, GameTime.at(0, 16).tick)
        simulation.run()
        assertEquals(1, simulation.totals.deadlinesMissed)
        assertEquals(1, simulation.totals.jobsDone)
    }

    @Test fun thePlannerPicksTheMiddayDipInSpain() {
        val grid = Grid(GridSetting(GridRegion.SPAIN, Season.YEAR), 1, WeatherMode.Fixed(1_000))
        val start = GameTime(BatchPlanner.plan(listOf(0 to report), 10, emptyMap(), grid, 0).getValue(0))
        assertTrue(start.hour in 10..13, "start at ${start.clockText}")
    }

    @Test fun thePlannerKeepsWithinTheServers() {
        val a = BatchJobSpec.of("A", 6, 3, GameTime.at(0, 0), GameTime.at(1, 0))
        val b = BatchJobSpec.of("B", 6, 3, GameTime.at(0, 0), GameTime.at(1, 0))
        val grid = Grid(GridSetting(GridRegion.SPAIN, Season.YEAR), 1, WeatherMode.Fixed(1_000))
        val plan = BatchPlanner.plan(listOf(0 to a, 1 to b), 10, emptyMap(), grid, 0)
        val s0 = plan.getValue(0)
        val s1 = plan.getValue(1)
        assertTrue(s0 + a.span <= s1 || s1 + b.span <= s0, "the two jobs must not overlap: $s0, $s1")
    }
}
