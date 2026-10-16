import SwiftUI
import LoadShiftCore

@main
struct LoadShiftApp: App {
    var body: some Scene {
        WindowGroup {
            RootView()
                .preferredColorScheme(.light)
        }
    }
}

/// Scenario list, briefing or game.
struct RootView: View {
    @State private var game: GameController? = Launch.game()
    @State private var briefing: Scenario? = Launch.briefing()

    var body: some View {
        if let game {
            GameScreen(game: game) {
                briefing = game.scenario
                self.game = nil
            }
        } else if let briefing {
            BriefingView(scenario: briefing, onStart: {
                game = GameController(scenario: briefing)
            }, onBack: {
                self.briefing = nil
            })
        } else {
            ScenarioListView { briefing = $0 }
        }
    }
}

/// Launch arguments for screenshots and tests:
/// -scenario N (open its briefing), -play (start it), -ticks N (run N ticks), -autoscale P (target utilization
/// in %), -fixed N (N servers), -careful 1 (play like a careful player while running the ticks),
/// -draftAutoscale P (edit a rule without applying it).
enum Launch {
    static var scenario: Scenario? {
        let n = UserDefaults.standard.integer(forKey: "scenario")
        return Scenario.all.first { $0.number == n }
    }

    static func briefing() -> Scenario? { scenario }

    @MainActor
    static func game() -> GameController? {
        let defaults = UserDefaults.standard
        let arguments = ProcessInfo.processInfo.arguments
        guard arguments.contains("-play") || defaults.object(forKey: "ticks") != nil else { return nil }
        let game = GameController(scenario: scenario ?? .nightShift)
        if game.hasBatch {
            if defaults.object(forKey: "careful") != nil {
                // Plan like a careful player: every job into the greenest hours of the forecast.
                game.setPolicy(Policy(capacity: game.scenario.initialPolicy.capacity, batch: .greenest))
            }
            game.advance(ticks: defaults.integer(forKey: "ticks"))
            return game
        }
        if defaults.object(forKey: "autoscale") != nil {
            game.setPolicy(Policy(capacity: .autoscale(targetUtilization: defaults.integer(forKey: "autoscale") * 10)))
        } else if defaults.object(forKey: "fixed") != nil {
            game.setWantedServers(defaults.integer(forKey: "fixed"))
        }
        if defaults.object(forKey: "careful") != nil {
            // Switch like a careful player: enough servers for the next 90 minutes at 80 % utilization.
            for _ in 0..<defaults.integer(forKey: "ticks") where !game.isFinished {
                let t = game.simulation.tick
                let expected = (t...(t + 6)).map { game.scenario.interactive.expected(tick: $0) }.max() ?? 0
                game.setWantedServers((expected * 1_000 + 15_999) / 16_000)
                game.advance(ticks: 1)
            }
        } else {
            game.advance(ticks: defaults.integer(forKey: "ticks"))
        }
        if defaults.object(forKey: "draftAutoscale") != nil {
            game.draft = Policy(capacity: .autoscale(targetUtilization: defaults.integer(forKey: "draftAutoscale") * 10))
        }
        return game
    }
}
