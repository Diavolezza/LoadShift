import XCTest
@testable import LoadShiftCore

final class SimulationTests: XCTestCase {
    private func scenario(peak: Int, servers: Int = 10, days: Int = 1) -> Scenario {
        Scenario(number: 0, title: "Test", setting: GridSetting(.germany, .autumn), weather: .fixed(1_000),
                 days: days, seed: 5, fleet: [FleetEntry(.cpuCurrent, servers)],
                 interactive: InteractiveLoad(peak: peak))
    }

    func testServerPowerIsIdlePlusTheDynamicPart() {
        var server = Server(id: 0, kind: .cpuCurrent)
        XCTAssertEqual(server.power(utilization: 0), 156)
        XCTAssertEqual(server.power(utilization: 500), 378)
        XCTAssertEqual(server.power(utilization: 1_000), 600)
        server.state = .booting(readyAt: 3)
        XCTAssertEqual(server.power(utilization: 0), 156)
        server.state = .off
        XCTAssertEqual(server.power(utilization: 1_000), 0)
    }

    func testIdleDataCenterBooksIdlePowerTimesPue() {
        var simulation = Simulation(scenario: scenario(peak: 0), policy: .reference)
        simulation.step()
        let record = simulation.history[0]
        XCTAssertEqual(record.itPower, 10 * 156)
        XCTAssertEqual(record.facilityPower, 10 * 156 * 1_520 / 1_000)
        XCTAssertEqual(simulation.totals.facilityEnergy, record.facilityPower * 250)
        XCTAssertEqual(simulation.totals.operational, record.facilityPower * 250 * record.intensityTenths / 10)
        XCTAssertEqual(simulation.totals.embodied, 10 * ServerKind.cpuCurrent.embodiedPerTick)
    }

    func testEmbodiedEmissionsOfADayMatchTheLifespan() {
        // 550 kg over 6 × 365 days: about 251 g per day.
        let perDay = ServerKind.cpuCurrent.embodiedPerTick * GameTime.ticksPerDay
        XCTAssertEqual(perDay / 1_000_000, 251)
    }

    func testSameScenarioSameResult() {
        var a = Simulation(scenario: .nightShift, policy: Policy(capacity: .autoscale(targetUtilization: 700)))
        var b = Simulation(scenario: .nightShift, policy: Policy(capacity: .autoscale(targetUtilization: 700)))
        a.run()
        b.run()
        XCTAssertEqual(a.totals, b.totals)
        XCTAssertEqual(a.history, b.history)
    }

    func testAllOnKeepsEveryServerRunning() {
        let simulation = Simulation.reference(for: scenario(peak: 150))
        XCTAssertTrue(simulation.history.allSatisfy { $0.serversOn == 10 })
        XCTAssertEqual(simulation.totals.sla, 1_000)
    }

    func testSwitchedOnServersTakeWorkAfterBooting() {
        var simulation = Simulation(scenario: scenario(peak: 100), policy: Policy(capacity: .manual(servers: 0)))
        simulation.step()
        XCTAssertEqual(simulation.history.last?.capacity, 0)
        simulation.policy = Policy(capacity: .manual(servers: 4))
        simulation.step()
        simulation.step()
        XCTAssertEqual(simulation.history.last?.capacity, 0, "still booting")
        simulation.step()
        XCTAssertEqual(simulation.history.last?.capacity, 80)
    }

    func testOverloadedServersServeSlowly() {
        // Two servers (capacity 40) at a demand far above it: served in time = 34 + 6 / 2.
        var simulation = Simulation(scenario: scenario(peak: 1_000), policy: Policy(capacity: .manual(servers: 2)))
        simulation.step()
        let record = simulation.history[0]
        XCTAssertEqual(record.served, 40)
        XCTAssertEqual(record.inTime, 37)
        XCTAssertEqual(simulation.totals.inTime, 37)
    }

    func testAutoscalingUsesFewerServersAndKeepsTheSla() {
        let reference = Simulation.reference(for: .nightShift)
        var scaled = Simulation(scenario: .nightShift, policy: Policy(capacity: .autoscale(targetUtilization: 700)))
        scaled.run()
        XCTAssertLessThan(scaled.totals.serverTicks, reference.totals.serverTicks * 3 / 4)
        XCTAssertGreaterThanOrEqual(scaled.totals.sla, Scenario.nightShift.slaTarget)
        XCTAssertEqual(scaled.totals.embodied, reference.totals.embodied, "switching off saves no embodied emissions")
    }

    func testProjectionUsesTheForecastAndLeavesTheSimulationUntouched() {
        var simulation = Simulation(scenario: .nightShift, policy: .reference)
        for _ in 0..<20 { simulation.step() }
        let before = simulation.totals
        let projected = simulation.projected(with: Policy(capacity: .autoscale(targetUtilization: 700)))
        XCTAssertEqual(simulation.totals, before)
        XCTAssertEqual(simulation.tick, Scenario.nightShift.startTick + 20)
        XCTAssertEqual(projected.ticks, GameTime.ticksPerDay)
        XCTAssertLessThan(projected.emissions, simulation.projected(with: .reference).emissions)
        // Projected and real end differ only by forecast error and noise: within a fifth.
        var real = simulation
        real.policy = Policy(capacity: .autoscale(targetUtilization: 700))
        real.run()
        XCTAssertLessThan(abs(real.totals.emissions - projected.emissions) * 5, real.totals.emissions)
    }

    func testStarsNeedTheRulesFirst() {
        let scenario = Scenario.nightShift
        let reference = Simulation.reference(for: scenario).totals
        var cheap = Totals()
        cheap.demand = 1_000; cheap.served = 1_000; cheap.inTime = 900; cheap.capacityTicks = 1_000
        cheap.operational = reference.emissions / 2
        XCTAssertEqual(scenario.stars(cheap, reference: reference), 0, "SLA missed: no star at all")
        cheap.inTime = 1_000
        XCTAssertEqual(scenario.stars(cheap, reference: reference), 3)
    }

    func testEmissionsSoFarFollowTheTotals() {
        let simulation = Simulation.reference(for: .nightShift)
        XCTAssertEqual(simulation.history.last?.emissionsSoFar, simulation.totals.emissions)
        XCTAssertEqual(simulation.emissions(through: Scenario.nightShift.endTick + 5), simulation.totals.emissions)
        XCTAssertLessThan(simulation.emissions(through: 40), simulation.totals.emissions / 2)
    }
}
