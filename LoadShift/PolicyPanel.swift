import SwiftUI
import LoadShiftCore

/// The controls on the right (docs/CONCEPT.md 10.1): only what the scenario unlocks.
/// Switching by hand takes effect at once; rules are edited and applied. No forecast of the day's result –
/// that comes in the debrief as "what if".
struct PolicyPanel: View {
    let game: GameController

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 16) {
                if game.scenario.controls.contains(.batchByHand) {
                    BatchPanel(game: game)
                } else {
                    Text("Capacity")
                        .font(.title2.bold())
                    if game.byHand {
                        ManualCapacity(game: game)
                    } else {
                        CapacityRules(game: game)
                    }
                    NowBox(simulation: game.simulation)
                }
            }
            .padding(20)
        }
        .background(Theme.panel)
    }
}

/// Scenario 1: switch servers on and off by hand.
struct ManualCapacity: View {
    let game: GameController

    var body: some View {
        let wanted = game.wantedServers
        let total = game.scenario.serverCount
        VStack(alignment: .leading, spacing: 14) {
            Text("An idle server still draws about a quarter of its maximum power. Switch off what is not needed – but a server you switch on takes 30 minutes before it can work.")
                .font(.callout)
                .foregroundStyle(.secondary)
                .fixedSize(horizontal: false, vertical: true)
            HStack(spacing: 16) {
                RoundButton(symbol: "minus") { game.setWantedServers(wanted - 1) }
                    .disabled(wanted <= 0 || game.isFinished)
                VStack(spacing: 0) {
                    Text(verbatim: "\(wanted)")
                        .font(.system(size: 44, weight: .bold, design: .rounded).monospacedDigit())
                    Text("of \(total) servers on")
                        .font(.caption)
                        .foregroundStyle(.secondary)
                }
                .frame(minWidth: 110)
                RoundButton(symbol: "plus") { game.setWantedServers(wanted + 1) }
                    .disabled(wanted >= total || game.isFinished)
            }
            .frame(maxWidth: .infinity)
            let perServer = game.scenario.fleet.first?.kind.capacity ?? 0
            Text("Capacity: \(wanted * perServer) load units")
                .font(.headline.monospacedDigit())
                .frame(maxWidth: .infinity)
            Text("One server handles \(perServer) load units (\(perServer),000 requests per 15 minutes). Above 85 % utilization requests get slow.")
                .font(.callout)
                .fixedSize(horizontal: false, vertical: true)
            Text("Takes effect at once. Switched-off servers stop immediately; switched-on servers boot for 30 minutes.")
                .font(.caption)
                .foregroundStyle(.secondary)
                .fixedSize(horizontal: false, vertical: true)
        }
    }
}

struct RoundButton: View {
    let symbol: String
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            Image(systemName: symbol)
                .font(.title2.weight(.bold))
                .frame(width: 56, height: 56)
                .background(Theme.accent.opacity(0.14), in: Circle())
                .foregroundStyle(Theme.accent)
        }
        .buttonStyle(.plain)
    }
}

/// Rules for the capacity (from scenario 4): all on, fixed, autoscaling. Edited, then applied.
struct CapacityRules: View {
    @Bindable var game: GameController

    private enum Mode: Int { case allOn, manual, autoscale }

    private var serverCount: Int { game.scenario.serverCount }

    private func mode(of policy: Policy) -> Mode {
        switch policy.capacity {
        case .allOn: return .allOn
        case .manual: return .manual
        case .autoscale: return .autoscale
        }
    }

    private var modeBinding: Binding<Mode> {
        Binding {
            mode(of: game.draft)
        } set: { newMode in
            switch newMode {
            case .allOn: game.draft.capacity = .allOn
            case .manual: game.draft.capacity = .manual(servers: max(1, game.wantedServers))
            case .autoscale: game.draft.capacity = .autoscale(targetUtilization: 700)
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
        VStack(alignment: .leading, spacing: 14) {
            HStack(spacing: 8) {
                Image(systemName: "checkmark.seal.fill").foregroundStyle(Theme.accent)
                Text("Active: \(Text(PolicyText.describe(game.simulation.policy)))")
                    .font(.headline)
            }
            .padding(10)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(Theme.accent.opacity(0.10), in: RoundedRectangle(cornerRadius: 10))
            Text("New rule")
                .font(.subheadline.weight(.semibold))
                .foregroundStyle(.secondary)
            Picker("Rule", selection: modeBinding) {
                Text("All on").tag(Mode.allOn)
                Text("Fixed").tag(Mode.manual)
                Text("Autoscale").tag(Mode.autoscale)
            }
            .pickerStyle(.segmented)
            switch modeBinding.wrappedValue {
            case .allOn:
                Text("Every server runs all the time – like the reference operator.")
                    .font(.callout)
            case .manual:
                Stepper(value: manualCount, in: 0...max(1, serverCount)) {
                    Text("\(manualCount.wrappedValue) servers run").monospacedDigit()
                }
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
            if game.draftChanged {
                Button {
                    game.apply()
                } label: {
                    Text("Apply").frame(maxWidth: .infinity).foregroundStyle(.white)
                }
                .buttonStyle(.borderedProminent)
                .tint(Theme.accent)
                .controlSize(.large)
                .disabled(game.isFinished)
            }
        }
    }
}

enum PolicyText {
    static func describe(_ policy: Policy) -> LocalizedStringKey {
        switch policy.capacity {
        case .allOn: return "All servers on"
        case .manual(let n): return "\(n) servers"
        case .autoscale(let t): return "Autoscaling at \(Format.wholePercent(t))"
        }
    }
}

/// The situation right now: running servers, load and capacity, the expected load in an hour.
struct NowBox: View {
    let simulation: Simulation

    var body: some View {
        let running = simulation.servers.filter { $0.state.isOn }.count
        let booting = simulation.servers.filter { if case .booting = $0.state { return true } else { return false } }.count
        let last = simulation.history.last
        let demandNow = last?.demand ?? simulation.scenario.interactive.expected(tick: simulation.tick)
        let inAnHour = simulation.scenario.interactive.expected(tick: simulation.tick + GameTime.ticksPerHour)
        VStack(alignment: .leading, spacing: 8) {
            Text("Right now")
                .font(.headline)
            SwiftUI.Grid(alignment: .leading, horizontalSpacing: 12, verticalSpacing: 5) {
                GridRow {
                    Text("Servers working")
                    Text(verbatim: booting > 0 ? "\(running) (+\(booting))" : "\(running)").bold()
                }
                GridRow {
                    Text("Load / capacity")
                    Text(verbatim: "\(demandNow) / \(last?.capacity ?? simulation.readyCapacity)").bold()
                }
                GridRow {
                    Text("Utilization")
                    let capacity = last?.capacity ?? simulation.readyCapacity
                    let served = last?.served ?? min(demandNow, capacity)
                    Text(verbatim: capacity == 0 ? "–" : Format.wholePercent(served * 1_000 / capacity)).bold()
                }
                GridRow {
                    Text("Expected load in 1 h")
                    Text(verbatim: "\(inAnHour)").bold()
                }
                GridRow {
                    Text("IT power")
                    let itPower = simulation.servers.reduce(0) { $0 + $1.power(utilization: $1.utilization) }
                    Text(verbatim: "\(Format.kilowatts(itPower)) kW").bold()
                }
            }
            .font(.callout.monospacedDigit())
        }
        .padding(14)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Theme.background, in: RoundedRectangle(cornerRadius: 12))
    }
}
