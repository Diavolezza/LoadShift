import SwiftUI
import LoadShiftCore

/// The rules (docs/CONCEPT.md 10.1): one section per unlocked topic. Step 2 has capacity only.
/// Every change shows its expected effect before it is applied (principle 4).
struct PolicyPanel: View {
    @Bindable var game: GameController

    private enum Mode: Int { case allOn, manual, autoscale }

    private var serverCount: Int { game.simulation.servers.filter { $0.kind.servesInteractive }.count }

    private var mode: Binding<Mode> {
        Binding {
            switch game.draft.capacity {
            case .allOn: return Mode.allOn
            case .manual: return Mode.manual
            case .autoscale: return Mode.autoscale
            }
        } set: { newMode in
            switch newMode {
            case .allOn:
                game.draft.capacity = .allOn
            case .manual:
                let on = game.simulation.servers.filter { $0.state.isPowered && $0.kind.servesInteractive }.count
                game.draft.capacity = .manual(servers: max(1, on))
            case .autoscale:
                game.draft.capacity = .autoscale(targetUtilization: 700)
            }
        }
    }

    private var manualCount: Binding<Int> {
        Binding {
            if case .manual(let n) = game.draft.capacity { return n }
            return serverCount
        } set: { game.draft.capacity = .manual(servers: $0) }
    }

    private var target: Binding<Double> {
        Binding {
            if case .autoscale(let t) = game.draft.capacity { return Double(t) / 10 }
            return 70
        } set: { game.draft.capacity = .autoscale(targetUtilization: Int($0.rounded()) * 10) }
    }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 16) {
                Text("Capacity")
                    .font(.title2.bold())
                Text("An idle server still draws about a quarter of its maximum power. Decide how many servers run. Switching a server on takes 30 minutes.")
                    .font(.callout)
                    .foregroundStyle(.secondary)
                    .fixedSize(horizontal: false, vertical: true)
                Picker("Rule", selection: mode) {
                    Text("All on").tag(Mode.allOn)
                    Text("Fixed").tag(Mode.manual)
                    Text("Autoscale").tag(Mode.autoscale)
                }
                .pickerStyle(.segmented)
                switch mode.wrappedValue {
                case .allOn:
                    Text("Every server runs all the time – like the reference operator.")
                        .font(.callout)
                case .manual:
                    Stepper(value: manualCount, in: 1...max(1, serverCount)) {
                        Text("\(manualCount.wrappedValue) servers run")
                            .monospacedDigit()
                    }
                    Text("The same number day and night. Too few, and the evening peak gets slow.")
                        .font(.callout)
                        .foregroundStyle(.secondary)
                case .autoscale:
                    Text("Target utilization: \(Format.wholePercent(Int(target.wrappedValue * 10)))")
                        .monospacedDigit()
                    Slider(value: target, in: 40...90, step: 5)
                        .tint(Theme.accent)
                    Text("The servers needed for the expected load of the next hour run. A low target leaves headroom for peaks but more servers idle; a high target is efficient but gets slow at peaks.")
                        .font(.callout)
                        .foregroundStyle(.secondary)
                        .fixedSize(horizontal: false, vertical: true)
                }
                EffectBox(game: game)
                Button {
                    game.apply()
                } label: {
                    Text("Apply")
                        .frame(maxWidth: .infinity)
                        .foregroundStyle(.white)
                }
                .buttonStyle(.borderedProminent)
                .tint(Theme.accent)
                .controlSize(.large)
                .disabled(!game.draftChanged || game.isFinished)
            }
            .padding(20)
        }
        .background(Theme.panel)
    }
}

/// Expected result at the end of the scenario with the current rules and with the edited ones, from the
/// forecast and the expected load.
struct EffectBox: View {
    let game: GameController

    var body: some View {
        let current = game.projection(for: game.simulation.policy)
        let target = game.scenario.slaTarget
        VStack(alignment: .leading, spacing: 8) {
            Text("Expected at the end of the day")
                .font(.headline)
            if game.draftChanged {
                let draft = game.projection(for: game.draft)
                SwiftUI.Grid(alignment: .trailing, horizontalSpacing: 14, verticalSpacing: 4) {
                    GridRow {
                        Text(verbatim: "").gridColumnAlignment(.leading)
                        Text("now").foregroundStyle(.secondary)
                        Text("new").foregroundStyle(.secondary)
                    }
                    GridRow {
                        Text(verbatim: "CO₂").gridColumnAlignment(.leading)
                        Text(verbatim: "\(Format.kilograms(current.emissions)) kg")
                        Text(verbatim: "\(Format.kilograms(draft.emissions)) kg").bold()
                    }
                    GridRow {
                        Text("SLA").gridColumnAlignment(.leading)
                        Text(verbatim: Format.percent(current.sla))
                        Text(verbatim: Format.percent(draft.sla)).bold()
                            .foregroundStyle(draft.sla >= target ? Theme.ink : Theme.red)
                    }
                }
                .font(.callout.monospacedDigit())
                Text("\(Format.change(draft.emissions, from: current.emissions)) CO₂ until the end of the day")
                    .font(.callout.weight(.semibold))
                    .foregroundStyle(draft.emissions <= current.emissions ? Theme.accent : Theme.red)
            } else {
                Text("CO₂ \(Format.kilograms(current.emissions)) kg · SLA \(Format.percent(current.sla))")
                    .font(.callout.monospacedDigit())
            }
        }
        .padding(14)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Theme.background, in: RoundedRectangle(cornerRadius: 12))
    }
}
