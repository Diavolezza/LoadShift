import SwiftUI
import LoadShiftCore

/// The campaign: all scenarios with their stars; a scenario opens when the one before has a star.
struct ScenarioListView: View {
    let onSelect: (Scenario) -> Void

    var body: some View {
        GeometryReader { geo in
            ScrollView {
                VStack(alignment: .leading, spacing: 28) {
                    HStack(spacing: 18) {
                        Image(systemName: "server.rack")
                            .font(.system(size: 56, weight: .light))
                            .foregroundStyle(Theme.accent)
                        VStack(alignment: .leading, spacing: 4) {
                            Text("Load Shift")
                                .font(.system(size: 52, weight: .bold))
                            Text("Run a greener data center")
                                .font(.title2)
                                .foregroundStyle(.secondary)
                        }
                    }
                    let columns = [GridItem(.adaptive(minimum: 300, maximum: 420), spacing: 20)]
                    LazyVGrid(columns: columns, alignment: .leading, spacing: 20) {
                        ForEach(Scenario.all, id: \.number) { scenario in
                            ScenarioCard(scenario: scenario, unlocked: Progress.isUnlocked(scenario: scenario.number)) {
                                onSelect(scenario)
                            }
                        }
                    }
                    Text("More scenarios follow: forecasts and wind, headroom and autoscaling, cooling, hardware, AI training, eco mode, two sites.")
                        .font(.title3)
                        .foregroundStyle(.secondary)
                }
                .padding(.horizontal, max(32, geo.size.width * 0.05))
                .padding(.vertical, 48)
                .frame(maxWidth: 1_300)
                .frame(maxWidth: .infinity)
            }
        }
        .background(Theme.background)
        .foregroundStyle(Theme.ink)
    }
}

struct ScenarioCard: View {
    let scenario: Scenario
    let unlocked: Bool
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            VStack(alignment: .leading, spacing: 12) {
                HStack {
                    Text(verbatim: "\(scenario.number)")
                        .font(.system(size: 34, weight: .bold, design: .rounded))
                        .foregroundStyle(unlocked ? Theme.accent : Color.secondary)
                    Spacer()
                    if unlocked {
                        StarRow(stars: Progress.stars(scenario: scenario.number), size: 22)
                    } else {
                        Image(systemName: "lock.fill").font(.title2).foregroundStyle(.secondary)
                    }
                }
                Text(LocalizedStringKey(scenario.title))
                    .font(.title2.bold())
                HStack(spacing: 6) {
                    Text(scenario.setting.region.localizedName)
                    Text(verbatim: "·")
                    Text(scenario.setting.season.localizedName)
                    Text(verbatim: "·")
                    Text(scenario.days == 1 ? "1 day" : "\(scenario.days) days")
                }
                .font(.body)
                .foregroundStyle(.secondary)
                if !unlocked {
                    Text("Earn a star in the scenario before.")
                        .font(.callout)
                        .foregroundStyle(.secondary)
                }
            }
            .padding(22)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(Theme.panel, in: RoundedRectangle(cornerRadius: 18))
            .opacity(unlocked ? 1 : 0.6)
        }
        .buttonStyle(.plain)
        .disabled(!unlocked)
    }
}
