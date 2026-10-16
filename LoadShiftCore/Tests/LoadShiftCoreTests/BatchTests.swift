import XCTest
@testable import LoadShiftCore

final class BatchTests: XCTestCase {
    private func scenario(_ jobs: [BatchJobSpec], servers: Int = 6) -> Scenario {
        Scenario(number: 0, title: "Test", setting: GridSetting(.spain, .year), weather: .fixed(1_000), days: 2,
                 seed: 4, fleet: [FleetEntry(.cpuCurrent, servers)], interactive: InteractiveLoad(peak: 0),
                 controls: [.batchByHand], initialPolicy: Policy(capacity: .manual(servers: 0), batch: .byHand),
                 referencePolicy: Policy(capacity: .manual(servers: 0), batch: .asap), batchJobs: jobs)
    }

    private let report = BatchJobSpec("Report", servers: 4, hours: 3,
                                      arrival: GameTime(day: 0, hour: 0), deadline: GameTime(day: 0, hour: 18))

    func testAJobBootsThenWorksThenReleasesItsServers() {
        var simulation = Simulation(scenario: scenario([report]), policy: Policy(capacity: .manual(servers: 0), batch: .asap))
        simulation.step()
        XCTAssertEqual(simulation.jobs[0].state, .started(at: 0, workFrom: 2))
        XCTAssertEqual(simulation.history[0].batchServers, 4)
        XCTAssertEqual(simulation.history[0].itPower, 4 * 156, "booting servers draw idle power")
        for _ in 0..<2 { simulation.step() }
        XCTAssertEqual(simulation.history[2].itPower, 4 * 600, "working servers draw full power")
        for _ in 0..<12 { simulation.step() }
        XCTAssertEqual(simulation.jobs[0].state, .done(startedAt: 0, at: 14))
        XCTAssertEqual(simulation.history.last?.itPower, 0, "released servers are off")
        XCTAssertEqual(simulation.totals.jobsDone, 1)
    }

    func testAPlannedJobStartsAtItsTime() {
        var simulation = Simulation(scenario: scenario([report]))
        simulation.plan(job: 0, start: GameTime(day: 0, hour: 11).tick)
        while simulation.tick < GameTime(day: 0, hour: 11).tick { simulation.step() }
        XCTAssertEqual(simulation.totals.batchEnergy, 0)
        simulation.step()
        XCTAssertEqual(simulation.jobs[0].startedAt, GameTime(day: 0, hour: 11).tick)
    }

    func testAnUnplannedJobMissesItsDeadline() {
        let simulation = Simulation.replay(scenario([report]), policy: Policy(capacity: .manual(servers: 0), batch: .byHand))
        XCTAssertEqual(simulation.totals.deadlinesMissed, 1)
        XCTAssertEqual(simulation.totals.jobsDone, 0)
    }

    func testAJobWaitsForFreeServers() {
        let second = BatchJobSpec("Import", servers: 4, hours: 2, arrival: GameTime(day: 0, hour: 0),
                                  deadline: GameTime(day: 1, hour: 0))
        var simulation = Simulation(scenario: scenario([report, second]), policy: Policy(capacity: .manual(servers: 0), batch: .asap))
        simulation.step()
        XCTAssertTrue(simulation.jobs[0].state.isStarted)
        XCTAssertEqual(simulation.jobs[1].state, .planned(start: 0), "only six servers: the second job waits")
        simulation.run()
        XCTAssertEqual(simulation.jobs[1].startedAt, 14)
        XCTAssertEqual(simulation.totals.deadlinesMissed, 0)
    }

    func testFinishingLateCountsOnce() {
        var simulation = Simulation(scenario: scenario([report]))
        simulation.plan(job: 0, start: GameTime(day: 0, hour: 16).tick)
        simulation.run()
        XCTAssertEqual(simulation.totals.deadlinesMissed, 1)
        XCTAssertEqual(simulation.totals.jobsDone, 1)
    }

    func testThePlannerPicksTheMiddayDipInSpain() {
        let grid = Grid(setting: GridSetting(.spain, .year), seed: 1, weather: .fixed(1_000))
        let plan = BatchPlanner.plan([(id: 0, spec: report)], servers: 10, busy: [:], grid: grid, madeAt: 0)
        let start = GameTime(tick: plan[0]!)
        XCTAssertTrue((10...13).contains(start.hour), "start at \(start.clockText)")
    }

    func testThePlannerKeepsWithinTheServers() {
        let a = BatchJobSpec("A", servers: 6, hours: 3, arrival: GameTime(day: 0, hour: 0), deadline: GameTime(day: 1, hour: 0))
        let b = BatchJobSpec("B", servers: 6, hours: 3, arrival: GameTime(day: 0, hour: 0), deadline: GameTime(day: 1, hour: 0))
        let grid = Grid(setting: GridSetting(.spain, .year), seed: 1, weather: .fixed(1_000))
        let plan = BatchPlanner.plan([(0, a), (1, b)], servers: 10, busy: [:], grid: grid, madeAt: 0)
        let s0 = plan[0]!, s1 = plan[1]!
        XCTAssertTrue(s0 + a.span <= s1 || s1 + b.span <= s0, "the two jobs must not overlap: \(s0), \(s1)")
    }
}
