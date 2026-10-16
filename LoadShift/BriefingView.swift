import SwiftUI
import LoadShiftCore

/// Briefing before a scenario (docs/CONCEPT.md 9): situation, the one idea, what is on the screen, and next to
/// it the facts, the goals with their stars and the start button.
struct BriefingView: View {
    let scenario: Scenario
    let onStart: () -> Void

    var body: some View {
        GeometryReader { geo in
            ScrollView {
                VStack(alignment: .leading, spacing: 28) {
                    header
                    if geo.size.width > 900 {
                        HStack(alignment: .top, spacing: 40) {
                            story.frame(maxWidth: .infinity, alignment: .leading)
                            sideCard.frame(width: min(440, geo.size.width * 0.38))
                        }
                    } else {
                        story
                        sideCard
                    }
                }
                .padding(.horizontal, max(32, geo.size.width * 0.05))
                .padding(.vertical, 40)
                .frame(maxWidth: 1_300)
                .frame(maxWidth: .infinity)
            }
        }
        .background(Theme.background)
        .foregroundStyle(Theme.ink)
    }

    private var header: some View {
        HStack(alignment: .center, spacing: 18) {
            Image(systemName: "server.rack")
                .font(.system(size: 52, weight: .light))
                .foregroundStyle(Theme.accent)
            VStack(alignment: .leading, spacing: 4) {
                Text("Load Shift")
                    .font(.title2.weight(.semibold))
                    .foregroundStyle(.secondary)
                Text("Scenario \(scenario.number): \(Text(LocalizedStringKey(scenario.title)))")
                    .font(.system(size: 48, weight: .bold))
            }
            Spacer()
            let best = Progress.stars(scenario: scenario.number)
            if best > 0 {
                VStack(alignment: .trailing, spacing: 4) {
                    Text("Best result").font(.callout).foregroundStyle(.secondary)
                    StarRow(stars: best, size: 30)
                }
            }
        }
    }

    private var story: some View {
        let text = BriefingText.of(scenario.number)
        return VStack(alignment: .leading, spacing: 26) {
            Section(title: "Situation", text: text.situation)
            Section(title: "The idea", text: text.idea)
            Section(title: "On the screen", text: text.screen)
        }
    }

    private var sideCard: some View {
        let perServer = scenario.fleet.first?.kind.capacity ?? 0
        let peak = scenario.interactive.peak
        return VStack(alignment: .leading, spacing: 22) {
            VStack(alignment: .leading, spacing: 10) {
                Fact(icon: "mappin.and.ellipse", text: scenario.setting.region.localizedName)
                Fact(icon: "leaf", text: scenario.setting.season.localizedName)
                Fact(icon: "clock", text: scenario.days == 1 ? "1 day, from 00:00" : "\(scenario.days) days, from 00:00")
                Fact(icon: "server.rack", text: "\(scenario.serverCount) servers, \(perServer) load units each")
                Fact(icon: "chart.line.uptrend.xyaxis", text: "Evening peak about \(peak) load units")
            }
            Divider()
            VStack(alignment: .leading, spacing: 12) {
                Text("Goals")
                    .font(.title2.bold())
                GoalLine(ok: nil, stars: 1, text: "SLA at least \(Format.percent(scenario.slaTarget)) and less CO₂ than the reference operator, who leaves every server on", font: .title3)
                GoalLine(ok: nil, stars: 2, text: "CO₂ \(Format.changePermille(-scenario.goals.reduction)) or better", font: .title3)
                GoalLine(ok: nil, stars: 3, text: StarText.special(scenario.goals.special), font: .title3)
                Text("One star opens the next scenario.")
                    .font(.body)
                    .foregroundStyle(.secondary)
            }
            Button(action: onStart) {
                Label("Start", systemImage: "play.fill")
                    .font(.title2.weight(.semibold))
                    .foregroundStyle(.white)
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 10)
            }
            .buttonStyle(.borderedProminent)
            .tint(Theme.accent)
            .controlSize(.large)
        }
        .padding(28)
        .background(Theme.panel, in: RoundedRectangle(cornerRadius: 20))
    }

    private struct Section: View {
        let title: LocalizedStringKey
        let text: LocalizedStringKey

        var body: some View {
            VStack(alignment: .leading, spacing: 8) {
                Text(title)
                    .font(.title2.bold())
                Text(text)
                    .font(.title3)
                    .lineSpacing(3)
                    .fixedSize(horizontal: false, vertical: true)
            }
        }
    }

    private struct Fact: View {
        let icon: String
        let text: LocalizedStringKey

        var body: some View {
            Label {
                Text(text)
            } icon: {
                Image(systemName: icon).foregroundStyle(Theme.accent).frame(width: 30)
            }
            .font(.title3)
        }
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
            screen: "The game starts paused. Top: clock and speed – pause, 1×, 4×, 16× – and below it your goals. Middle: your racks; grey servers are off, amber ones are booting, the green bar shows how busy a server is. Right: plus and minus for the servers, and the situation right now. Bottom: carbon intensity of the grid with the forecast, the load so far and the expected load (dotted); the dotted lines with \"5 servers\", \"10 servers\" … show how many servers a load needs.")
    }
}
