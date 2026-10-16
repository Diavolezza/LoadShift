import XCTest
@testable import LoadShiftCore

/// Every scenario must teach its idea (docs/CONCEPT.md, section 11): the reference operator meets the rules,
/// a good operator is clearly better in CO₂, and a deliberately bad policy shows the expected failure.
/// The numbers go to build/lesson-report.txt for balancing.
final class LessonTests: XCTestCase {
    private static var report: [String] = []

    override class func tearDown() {
        let root = URL(fileURLWithPath: #filePath)
            .deletingLastPathComponent().deletingLastPathComponent().deletingLastPathComponent()
            .deletingLastPathComponent()
        let dir = root.appendingPathComponent("build")
        try? FileManager.default.createDirectory(at: dir, withIntermediateDirectories: true)
        try? report.joined(separator: "\n").appending("\n")
            .write(to: dir.appendingPathComponent("lesson-report.txt"), atomically: true, encoding: .utf8)
        super.tearDown()
    }

    private func run(_ scenario: Scenario, _ policy: Policy) -> Simulation {
        var simulation = Simulation(scenario: scenario, policy: policy)
        simulation.run()
        return simulation
    }

    /// A careful operator switching by hand: every 15 minutes enough servers for the highest expected load of
    /// the next 90 minutes at 80 % utilization (what a player can do with the dotted expected-load curve).
    private func runCarefulByHand(_ scenario: Scenario, utilization: Int = 800) -> Simulation {
        var simulation = Simulation(scenario: scenario)
        let perServer = ServerKind.cpuCurrent.capacity
        while !simulation.isFinished {
            let t = simulation.tick
            let expected = (t...(t + ServerKind.bootTicks + GameTime.ticksPerHour))
                .map { scenario.interactive.expected(tick: $0) }.max() ?? 0
            let servers = (expected * 1_000 + perServer * utilization - 1) / (perServer * utilization)
            simulation.policy = Policy(capacity: .manual(servers: max(1, servers)))
            simulation.step()
        }
        return simulation
    }

    /// A player who switches twice: few servers for the night, many for the day.
    private func runTwoSettings(_ scenario: Scenario, night: Int, day: Int, dayFrom: Int) -> Simulation {
        var simulation = Simulation(scenario: scenario, policy: Policy(capacity: .manual(servers: night)))
        while !simulation.isFinished {
            if simulation.time.hour == dayFrom && simulation.time.minute == 0 {
                simulation.policy = Policy(capacity: .manual(servers: day))
            }
            simulation.step()
        }
        return simulation
    }

    func testScenario1NightShift() {
        let scenario = Scenario.nightShift
        let reference = run(scenario, .reference)
        let careful = runCarefulByHand(scenario)
        let twoSettings = runTwoSettings(scenario, night: 8, day: 16, dayFrom: 6)
        let fixedHalf = run(scenario, Policy(capacity: .manual(servers: 10)))
        let autoscale = run(scenario, Policy(capacity: .autoscale(targetUtilization: 700)))

        Self.report.append("Scenario 1 – \(scenario.title) (\(scenario.setting.region.name), \(scenario.setting.season.name), \(scenario.days) day)")
        Self.report.append(Self.header)
        for (name, simulation) in [("reference: all on", reference), ("by hand, careful", careful),
                                   ("by hand, 8 / 16 from 6:00", twoSettings), ("fixed 10 servers", fixedHalf),
                                   ("autoscale 70 % (sc. 4)", autoscale)] {
            Self.report.append(Self.line(name, simulation.totals, reference: reference.totals)
                + "  " + String(repeating: "★", count: scenario.stars(simulation.totals, reference: reference.totals)))
        }
        Self.report.append("")
        Self.report.append("Hour  g/kWh  demand  servers on (reference / careful)")
        for hour in 0..<24 {
            let tick = scenario.startTick + hour * GameTime.ticksPerHour
            guard let r = reference.history.first(where: { $0.tick == tick }),
                  let g = careful.history.first(where: { $0.tick == tick }) else { continue }
            Self.report.append(String(format: "%@  %5d  %6d  %2d / %2d", GameTime(tick: tick).clockText,
                                      (r.intensityTenths + 5) / 10, r.demand, r.serversOn, g.serversOn))
        }
        Self.report.append("")

        // The scenario is fair: the reference operator meets the SLA – and earns no star.
        XCTAssertGreaterThanOrEqual(reference.totals.sla, scenario.slaTarget)
        XCTAssertEqual(scenario.stars(reference.totals, reference: reference.totals), 0)
        // The lesson works: a careful player earns all three stars.
        XCTAssertEqual(scenario.stars(careful.totals, reference: reference.totals), 3)
        // Two switches a day are a good start, but not the full result.
        let rough = scenario.stars(twoSettings.totals, reference: reference.totals)
        XCTAssertTrue((1...2).contains(rough), "two settings: \(rough) stars")
        // A fixed half of the fleet is not enough at the evening peak.
        XCTAssertLessThan(fixedHalf.totals.sla, scenario.slaTarget)
        XCTAssertEqual(scenario.stars(fixedHalf.totals, reference: reference.totals), 0)
    }

    // MARK: - Report

    private static let header =
        "policy                  CO₂ kg  oper.  embod.    kWh  g/kWh   PUE   SLA %  servers  util. %  SCI g/1000 req  vs. ref"

    private static func line(_ name: String, _ t: Totals, reference: Totals) -> String {
        let diff = reference.emissions == 0 ? 0 : (t.emissions - reference.emissions) * 1_000 / reference.emissions
        return name.padding(toLength: 22, withPad: " ", startingAt: 0) + String(
            format: " %7.1f %6.1f %6.1f %7.1f  %5d  %4.2f  %6.1f  %7.1f  %7.1f  %14.2f  %+6.1f %%",
            Double(t.emissions) / 1e9, Double(t.operational) / 1e9, Double(t.embodied) / 1e9,
            Double(t.facilityEnergy) / 1e6, (t.meanIntensityTenths + 5) / 10, Double(t.pue) / 1_000,
            Double(t.sla) / 10, Double(t.meanServersOn) / 1_000, Double(t.meanUtilization) / 10,
            Double(t.sciPerUnit) / 1e6, Double(diff) / 10)
    }
}
