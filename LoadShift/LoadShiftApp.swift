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

/// Briefing or game.
struct RootView: View {
    @State private var game: GameController? = Launch.game()

    var body: some View {
        if let game {
            GameScreen(game: game) { self.game = nil }
        } else {
            BriefingView(scenario: .nightShift) {
                game = GameController(scenario: .nightShift)
            }
        }
    }
}

/// Launch arguments for screenshots and tests:
/// -play (skip the briefing), -ticks N (run N ticks), -autoscale P (target utilization in %),
/// -fixed N (N servers), -careful 1 (switch like a careful player while running the ticks),
/// -draftAutoscale P (edit a rule without applying it).
enum Launch {
    @MainActor
    static func game() -> GameController? {
        let defaults = UserDefaults.standard
        let arguments = ProcessInfo.processInfo.arguments
        guard arguments.contains("-play") || defaults.object(forKey: "ticks") != nil else { return nil }
        let game = GameController(scenario: .nightShift)
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
