import SwiftUI
import LoadShiftCore

/// Briefing before a scenario (docs/CONCEPT.md 9): situation, goal, the one idea, what is new on the screen.
struct BriefingView: View {
    let scenario: Scenario
    let onStart: () -> Void

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 22) {
                HStack(spacing: 14) {
                    Image(systemName: "server.rack")
                        .font(.system(size: 34, weight: .light))
                        .foregroundStyle(Theme.accent)
                    VStack(alignment: .leading, spacing: 2) {
                        Text("Load Shift")
                            .font(.headline)
                            .foregroundStyle(.secondary)
                        Text("Scenario \(scenario.number): \(Text(LocalizedStringKey(scenario.title)))")
                            .font(.largeTitle.bold())
                    }
                }
                HStack(spacing: 10) {
                    Chip(text: scenario.setting.region.localizedName, icon: "mappin.and.ellipse")
                    Chip(text: scenario.setting.season.localizedName, icon: "leaf")
                    Chip(text: scenario.days == 1 ? "1 day" : "\(scenario.days) days", icon: "clock")
                    Chip(text: "\(scenario.fleet.reduce(0) { $0 + $1.count }) servers", icon: "server.rack")
                }
                let text = BriefingText.of(scenario.number)
                Section(title: "Situation", text: text.situation)
                Section(title: "Your goal", text: text.goal)
                Section(title: "The idea", text: text.idea)
                Section(title: "On the screen", text: text.screen)
                Button(action: onStart) {
                    Label("Start", systemImage: "play.fill")
                        .font(.title3.weight(.semibold))
                        .foregroundStyle(.white)
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 6)
                }
                .buttonStyle(.borderedProminent)
                .tint(Theme.accent)
                .controlSize(.large)
            }
            .padding(40)
            .frame(maxWidth: 760)
            .frame(maxWidth: .infinity)
        }
        .background(Theme.background)
        .foregroundStyle(Theme.ink)
    }

    private struct Section: View {
        let title: LocalizedStringKey
        let text: LocalizedStringKey

        var body: some View {
            VStack(alignment: .leading, spacing: 6) {
                Text(title)
                    .font(.title3.bold())
                Text(text)
                    .font(.body)
                    .fixedSize(horizontal: false, vertical: true)
            }
        }
    }
}

struct Chip: View {
    let text: LocalizedStringKey
    let icon: String

    var body: some View {
        Label(text, systemImage: icon)
            .font(.callout)
            .padding(.horizontal, 12)
            .padding(.vertical, 6)
            .background(Theme.panel, in: Capsule())
    }
}

/// The texts of a briefing.
struct BriefingText {
    let situation: LocalizedStringKey
    let goal: LocalizedStringKey
    let idea: LocalizedStringKey
    let screen: LocalizedStringKey

    static func of(_ number: Int) -> BriefingText {
        BriefingText(
            situation: "You run the data center of a web shop in Germany. Its 20 servers are sized for the Christmas season. Today is an ordinary autumn day: in the evening many people shop, at night hardly anyone.",
            goal: "Serve the users reliably – at least 99 % of the requests in time (SLA) – and emit less CO₂ than the reference operator, who simply leaves every server on.",
            idea: "An idle server still draws about a quarter of its maximum power. Servers that are not needed should be off – but switching one on takes 30 minutes, so plan ahead.",
            screen: "The game starts paused at 06:00. Top: clock and speed – pause, 1×, 4×, 16×. Middle: your racks; grey servers are off, the green bar shows how busy a server is. Right: the capacity rules with their expected effect. Bottom: carbon intensity of the grid with the forecast, and the load over the day.")
    }
}
