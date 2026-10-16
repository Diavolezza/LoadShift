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
/// -fixed N (N servers), -draftAutoscale P (edit the rule without applying it).
enum Launch {
    @MainActor
    static func game() -> GameController? {
        let defaults = UserDefaults.standard
        let arguments = ProcessInfo.processInfo.arguments
        guard arguments.contains("-play") || defaults.object(forKey: "ticks") != nil else { return nil }
        var policy = Policy.reference
        if defaults.object(forKey: "autoscale") != nil {
            policy = Policy(capacity: .autoscale(targetUtilization: defaults.integer(forKey: "autoscale") * 10))
        } else if defaults.object(forKey: "fixed") != nil {
            policy = Policy(capacity: .manual(servers: defaults.integer(forKey: "fixed")))
        }
        let game = GameController(scenario: .nightShift, policy: policy)
        game.advance(ticks: defaults.integer(forKey: "ticks"))
        if defaults.object(forKey: "draftAutoscale") != nil {
            game.draft = Policy(capacity: .autoscale(targetUtilization: defaults.integer(forKey: "draftAutoscale") * 10))
        }
        return game
    }
}
