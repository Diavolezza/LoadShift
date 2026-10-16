import SwiftUI
import LoadShiftCore

/// Briefing before a scenario (docs/CONCEPT.md 9): situation, the one idea, what is on the screen, and next to
/// it the facts, the goals with their stars and the start button.
struct BriefingView: View {
    let scenario: Scenario
    let onStart: () -> Void
    var onBack: (() -> Void)? = nil

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
            if let onBack {
                Button(action: onBack) {
                    Image(systemName: "chevron.left").font(.title.weight(.semibold))
                }
                .accessibilityLabel(Text("Back"))
            }
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
        let text = ScenarioTexts.of(scenario)
        return VStack(alignment: .leading, spacing: 26) {
            Section(title: "Situation", text: text.situation)
            Section(title: "The idea", text: text.idea)
            Section(title: "On the screen", text: text.screen)
        }
    }

    private var sideCard: some View {
        let perServer = scenario.fleet.first?.kind.capacity ?? 0
        let peak = scenario.interactive.peak
        let text = ScenarioTexts.of(scenario)
        return VStack(alignment: .leading, spacing: 22) {
            VStack(alignment: .leading, spacing: 10) {
                Fact(icon: "mappin.and.ellipse", text: scenario.setting.region.localizedName)
                Fact(icon: "leaf", text: scenario.setting.season.localizedName)
                Fact(icon: "clock", text: scenario.days == 1 ? "1 day, from 00:00" : "\(scenario.days) days, from 00:00")
                if peak > 0 {
                    Fact(icon: "server.rack", text: "\(scenario.serverCount) servers, \(perServer) load units each")
                    Fact(icon: "chart.line.uptrend.xyaxis", text: "Evening peak about \(peak) load units")
                } else {
                    Fact(icon: "server.rack", text: "\(scenario.serverCount) servers for batch jobs")
                    Fact(icon: "square.stack.3d.up", text: "\(scenario.batchJobs.count) jobs with deadlines")
                }
            }
            Divider()
            VStack(alignment: .leading, spacing: 12) {
                Text("Goals")
                    .font(.title2.bold())
                GoalLine(ok: nil, stars: 1, text: text.rules, font: .title3)
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
