import SwiftUI
import LoadShiftCore

/// Briefing before a scenario (docs/CONCEPT.md 9): situation, goals with stars, the one idea, what is on the screen.
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
                    Spacer()
                    let best = Progress.stars(scenario: scenario.number)
                    if best > 0 {
                        VStack(alignment: .trailing, spacing: 2) {
                            Text("Best result").font(.caption).foregroundStyle(.secondary)
                            StarRow(stars: best, size: 20)
                        }
                    }
                }
                HStack(spacing: 10) {
                    Chip(text: scenario.setting.region.localizedName, icon: "mappin.and.ellipse")
                    Chip(text: scenario.setting.season.localizedName, icon: "leaf")
                    Chip(text: scenario.days == 1 ? "1 day" : "\(scenario.days) days", icon: "clock")
                    Chip(text: "\(scenario.serverCount) servers", icon: "server.rack")
                }
                let text = BriefingText.of(scenario.number)
                Section(title: "Situation", text: text.situation)
                Section(title: "The idea", text: text.idea)
                VStack(alignment: .leading, spacing: 8) {
                    Text("Goals")
                        .font(.title3.bold())
                    GoalLine(ok: false, stars: 1,
                             text: "SLA at least \(Format.percent(scenario.slaTarget)) and less CO₂ than the reference operator, who leaves every server on")
                    GoalLine(ok: false, stars: 2, text: "CO₂ \(Format.changePermille(-scenario.goals.reduction)) or better")
                    GoalLine(ok: false, stars: 3, text: StarText.special(scenario.goals.special))
                    Text("One star opens the next scenario.")
                        .font(.callout)
                        .foregroundStyle(.secondary)
                }
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
    let idea: LocalizedStringKey
    let screen: LocalizedStringKey

    static func of(_ number: Int) -> BriefingText {
        BriefingText(
            situation: "You run the data center of a web shop in Germany. Its 20 servers are sized for the Christmas season. Today is an ordinary autumn day: in the evening many people shop, at night hardly anyone. It is midnight, and the previous shift has left every server on.",
            idea: "An idle server still draws about a quarter of its maximum power. Switch off the servers that are not needed – and switch them on again in time: a server takes 30 minutes before it can work.",
            screen: "The game starts paused. Top: clock and speed – pause, 1×, 4×, 16× – and below it your goals. Middle: your racks; grey servers are off, amber ones are booting, the green bar shows how busy a server is. Right: plus and minus for the servers, and the situation right now. Bottom: carbon intensity of the grid with the forecast, the load so far and the expected load (dotted).")
    }
}
