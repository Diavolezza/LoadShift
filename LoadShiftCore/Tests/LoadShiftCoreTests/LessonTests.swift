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

    func testScenario1NightShift() {
        let scenario = Scenario.nightShift
        let reference = run(scenario, .reference)
        let good = run(scenario, Policy(capacity: .autoscale(targetUtilization: 700)))
        let greedy = run(scenario, Policy(capacity: .autoscale(targetUtilization: 900)))
        let fixedHalf = run(scenario, Policy(capacity: .manual(servers: 10)))

        Self.report.append("Scenario 1 – \(scenario.title) (\(scenario.setting.region.name), \(scenario.setting.season.name), \(scenario.days) day)")
        Self.report.append(Self.header)
        for (name, simulation) in [("reference: all on", reference), ("autoscale 70 %", good),
                                   ("autoscale 90 %", greedy), ("manual: 10 servers", fixedHalf)] {
            Self.report.append(Self.line(name, simulation.totals, reference: reference.totals))
        }
        Self.report.append("")
        Self.report.append("Hour  g/kWh  demand  servers on (reference / autoscale 70 %)")
        for hour in 0..<24 {
            let tick = scenario.startTick + hour * GameTime.ticksPerHour
            guard let r = reference.history.first(where: { $0.tick == tick }),
                  let g = good.history.first(where: { $0.tick == tick }) else { continue }
            Self.report.append(String(format: "%@  %5d  %6d  %2d / %2d", GameTime(tick: tick).clockText,
                                      (r.intensityTenths + 5) / 10, r.demand, r.serversOn, g.serversOn))
        }
        Self.report.append("")

        // The scenario is fair: the reference operator meets the SLA.
        XCTAssertGreaterThanOrEqual(reference.totals.sla, scenario.slaTarget)
        // The lesson works: switching off idle servers saves clearly, with the SLA kept.
        XCTAssertGreaterThanOrEqual(good.totals.sla, scenario.slaTarget)
        XCTAssertLessThanOrEqual(good.totals.emissions, reference.totals.emissions * 90 / 100)
        // Less headroom saves more but costs SLA (the trade-off of scenario 4 shows already).
        XCTAssertLessThan(greedy.totals.emissions, good.totals.emissions)
        XCTAssertLessThan(greedy.totals.sla, good.totals.sla)
        // A fixed half of the fleet is not enough at the evening peak.
        XCTAssertLessThan(fixedHalf.totals.sla, scenario.slaTarget)
    }

    // MARK: - Report

    private static let header =
        "policy                  CO₂ kg  oper.  embod.    kWh  g/kWh   PUE   SLA %  servers  SCI g/1000 req  vs. ref"

    private static func line(_ name: String, _ t: Totals, reference: Totals) -> String {
        let diff = reference.emissions == 0 ? 0 : (t.emissions - reference.emissions) * 1_000 / reference.emissions
        return name.padding(toLength: 22, withPad: " ", startingAt: 0) + String(
            format: " %7.1f %6.1f %6.1f %7.1f  %5d  %4.2f  %6.1f  %7.1f  %14.2f  %+6.1f %%",
            Double(t.emissions) / 1e9, Double(t.operational) / 1e9, Double(t.embodied) / 1e9,
            Double(t.facilityEnergy) / 1e6, (t.meanIntensityTenths + 5) / 10, Double(t.pue) / 1_000,
            Double(t.sla) / 10, Double(t.meanServersOn) / 1_000, Double(t.sciPerUnit) / 1e6, Double(diff) / 10)
    }
}
