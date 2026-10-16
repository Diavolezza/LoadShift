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
    /// The reference operator's run of the same scenario (all servers on), for the comparison at the end.
    let reference: Simulation
    private(set) var speed: Speed = .paused
    /// The rules being edited in the panel; they take effect with "Apply".
    var draft: Policy
    var showEnd = false
    private var loop: Task<Void, Never>?

    init(scenario: Scenario, policy: Policy = .reference) {
        self.scenario = scenario
        self.simulation = Simulation(scenario: scenario, policy: policy)
        self.reference = Simulation.reference(for: scenario)
        self.draft = policy
    }

    var isFinished: Bool { simulation.isFinished }
    var draftChanged: Bool { draft != simulation.policy }

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
        if simulation.isFinished {
            setSpeed(.paused)
            showEnd = true
        }
    }

    /// Runs a number of ticks at once (launch arguments, tests).
    func advance(ticks: Int) {
        for _ in 0..<ticks where !simulation.isFinished { simulation.step() }
        if simulation.isFinished { showEnd = true }
    }

    func apply() {
        simulation.policy = draft
    }

    /// What the scenario will probably bring with a policy from now on (forecast, expected demand).
    func projection(for policy: Policy) -> Totals {
        simulation.projected(with: policy)
    }

    func restart() {
        setSpeed(.paused)
        simulation = Simulation(scenario: scenario, policy: .reference)
        draft = .reference
        showEnd = false
    }
}
