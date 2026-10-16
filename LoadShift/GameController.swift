import SwiftUI
import Observation
import LoadShiftCore

/// Speed of the game clock: ticks (15 game minutes) per real second.
enum Speed: Int, CaseIterable, Identifiable {
    case paused = 0, x1 = 1, x4 = 4, x16 = 16
    var id: Int { rawValue }
}

/// Runs a scenario for the screen: the simulation, the clock and the rules the player is editing.
@MainActor @Observable
final class GameController {
    let scenario: Scenario
    private(set) var simulation: Simulation
    /// The reference operator's run of the same scenario (all servers on), for the comparison.
    let reference: Simulation
    private(set) var speed: Speed = .paused
    /// Rules being edited in the panel (scenarios with capacity rules); they take effect with "Apply".
    var draft: Policy
    var showEnd = false
    /// Stars of the finished run (set once at the end).
    private(set) var stars = 0
    private var loop: Task<Void, Never>?

    init(scenario: Scenario) {
        self.scenario = scenario
        self.simulation = Simulation(scenario: scenario)
        self.reference = Simulation.reference(for: scenario)
        self.draft = scenario.initialPolicy
    }

    var isFinished: Bool { simulation.isFinished }
    var draftChanged: Bool { draft != simulation.policy }
    /// The player switches servers by hand (changes take effect at once).
    var byHand: Bool { scenario.controls.contains(.manualCapacity) && !scenario.controls.contains(.capacityRules) }

    func setSpeed(_ newSpeed: Speed) {
        guard !isFinished || newSpeed == .paused else { return }
        speed = newSpeed
        loop?.cancel()
        loop = nil
        guard newSpeed != .paused else { return }
        let nanoseconds = UInt64(1_000_000_000 / newSpeed.rawValue)
        loop = Task { [weak self] in
            while !Task.isCancelled {
                try? await Task.sleep(nanoseconds: nanoseconds)
                guard let self, !Task.isCancelled else { return }
                self.step()
            }
        }
    }

    func step() {
        guard !simulation.isFinished else { return }
        simulation.step()
        if simulation.isFinished { finish() }
    }

    /// Runs a number of ticks at once (launch arguments).
    func advance(ticks: Int) {
        for _ in 0..<ticks where !simulation.isFinished { simulation.step() }
        if simulation.isFinished { finish() }
    }

    private func finish() {
        setSpeed(.paused)
        stars = scenario.stars(simulation.totals, reference: reference.totals)
        Progress.record(stars: stars, scenario: scenario.number)
        showEnd = true
    }

    /// Changes the policy at once (switching by hand, launch arguments).
    func setPolicy(_ policy: Policy) {
        simulation.policy = policy
        draft = policy
    }

    /// Servers wanted when switching by hand.
    var wantedServers: Int {
        if case .manual(let n) = simulation.policy.capacity { return n }
        return simulation.servers.filter { $0.state.isPowered }.count
    }

    func setWantedServers(_ n: Int) {
        setPolicy(Policy(capacity: .manual(servers: min(max(0, n), scenario.serverCount))))
    }

    func apply() {
        simulation.policy = draft
    }

    /// The player's emissions so far compared with the reference operator's up to the same moment, per mille
    /// (negative = less). Nil before the first tick.
    var changeSoFar: Int? {
        guard let last = simulation.history.last else { return nil }
        let ref = reference.emissions(through: last.tick)
        guard ref > 0 else { return nil }
        return (last.emissionsSoFar - ref) * 1_000 / ref
    }

    /// "What if" for the debrief: the whole scenario replayed with other rules, with the real weather and load.
    func whatIf() -> [(label: LocalizedStringKey, totals: Totals)] {
        let half = scenario.serverCount / 2, most = scenario.serverCount * 7 / 10
        return [
            ("All servers on (reference)", reference.totals),
            ("Fixed: \(half) servers all day", Simulation.replay(scenario, policy: Policy(capacity: .manual(servers: half))).totals),
            ("Fixed: \(most) servers all day", Simulation.replay(scenario, policy: Policy(capacity: .manual(servers: most))).totals),
            ("Autoscaling at 70 % (from scenario 4)",
             Simulation.replay(scenario, policy: Policy(capacity: .autoscale(targetUtilization: 700))).totals),
        ]
    }

    func restart() {
        setSpeed(.paused)
        simulation = Simulation(scenario: scenario)
        draft = scenario.initialPolicy
        stars = 0
        showEnd = false
    }
}

/// Best stars per scenario, kept on the device.
enum Progress {
    static func stars(scenario: Int) -> Int {
        UserDefaults.standard.integer(forKey: "stars.\(scenario)")
    }

    static func record(stars: Int, scenario: Int) {
        if stars > self.stars(scenario: scenario) {
            UserDefaults.standard.set(stars, forKey: "stars.\(scenario)")
        }
    }

    /// A scenario is open when it is the first or the one before has at least one star.
    static func isUnlocked(scenario: Int) -> Bool {
        scenario <= 1 || stars(scenario: scenario - 1) >= 1
    }
}
