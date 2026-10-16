import SwiftUI
import LoadShiftCore

/// The play screen (docs/CONCEPT.md 10.1): top bar, hall and policies, timeline.
struct GameScreen: View {
    let game: GameController
    let onExit: () -> Void

    var body: some View {
        GeometryReader { geo in
            VStack(spacing: 0) {
                TopBar(game: game, onExit: onExit)
                Divider()
                HStack(spacing: 0) {
                    HallView(servers: game.simulation.servers)
                        .frame(maxWidth: .infinity, maxHeight: .infinity)
                    Divider()
                    PolicyPanel(game: game)
                        .frame(width: min(380, max(300, geo.size.width * 0.32)))
                }
                Divider()
                TimelineChart(simulation: game.simulation)
                    .frame(height: max(190, geo.size.height * 0.3))
                    .background(Theme.panel)
            }
            .background(Theme.background)
        }
        .overlay {
            if game.showEnd {
                EndCard(game: game, onExit: onExit)
            }
        }
        .foregroundStyle(Theme.ink)
    }
}

struct TopBar: View {
    let game: GameController
    let onExit: () -> Void

    var body: some View {
        let simulation = game.simulation
        let shownTick = max(simulation.scenario.startTick, simulation.tick)
        let time = GameTime(tick: shownTick)
        let intensity = simulation.grid.intensity(tick: shownTick)
        let last = simulation.history.last
        HStack(spacing: 22) {
            Button(action: onExit) {
                Image(systemName: "chevron.left")
                    .font(.title3.weight(.semibold))
            }
            .accessibilityLabel(Text("Back"))
            VStack(alignment: .leading, spacing: 0) {
                Text("Day \(time.day - GameTime.start.day + 1)")
                    .font(.caption)
                    .foregroundStyle(.secondary)
                Text(verbatim: time.clockText)
                    .font(.system(size: 30, weight: .semibold, design: .rounded).monospacedDigit())
            }
            SpeedControl(game: game)
            Spacer(minLength: 8)
            Metric(title: "Grid now", value: "\(intensity) g/kWh", color: Theme.intensityColor(intensity))
            Metric(title: "CO₂ so far", value: "\(Format.kilograms(simulation.totals.emissions)) kg")
            Metric(title: "SLA", value: Format.percent(simulation.totals.sla),
                   color: simulation.totals.sla >= simulation.scenario.slaTarget ? Theme.ink : Theme.red)
            Metric(title: "Power", value: "\(Format.kilowatts(last?.facilityPower ?? 0)) kW")
        }
        .padding(.horizontal, 20)
        .padding(.vertical, 10)
        .background(Theme.panel)
    }
}

struct SpeedControl: View {
    let game: GameController

    var body: some View {
        HStack(spacing: 4) {
            ForEach(Speed.allCases) { speed in
                Button {
                    game.setSpeed(speed)
                } label: {
                    Group {
                        if speed == .paused {
                            Image(systemName: "pause.fill")
                        } else {
                            Text(verbatim: "\(speed.rawValue)×")
                        }
                    }
                    .font(.headline.monospacedDigit())
                    .frame(minWidth: 44, minHeight: 36)
                    .background(game.speed == speed ? Theme.accent : Color.clear,
                                in: RoundedRectangle(cornerRadius: 8))
                    .foregroundStyle(game.speed == speed ? Color.white : Theme.ink)
                }
                .buttonStyle(.plain)
                .disabled(game.isFinished && speed != .paused)
            }
        }
        .padding(3)
        .background(Theme.background, in: RoundedRectangle(cornerRadius: 10))
    }
}

struct Metric: View {
    let title: LocalizedStringKey
    let value: String
    var color: Color = Theme.ink

    var body: some View {
        VStack(alignment: .leading, spacing: 1) {
            Text(title)
                .font(.caption)
                .foregroundStyle(.secondary)
            Text(verbatim: value)
                .font(.title3.weight(.semibold).monospacedDigit())
                .foregroundStyle(color)
        }
    }
}

/// End of the scenario: the player's result next to the reference operator's (CONCEPT.md 8.2, first version).
struct EndCard: View {
    let game: GameController
    let onExit: () -> Void

    var body: some View {
        let you = game.simulation.totals
        let ref = game.reference.totals
        let target = game.scenario.slaTarget
        let better = you.emissions < ref.emissions
        ZStack {
            Color.black.opacity(0.35).ignoresSafeArea()
            VStack(alignment: .leading, spacing: 18) {
                Text("Day complete")
                    .font(.largeTitle.bold())
                Text(verbatim: "\(Format.change(you.emissions, from: ref.emissions)) CO₂")
                    .font(.system(size: 44, weight: .bold, design: .rounded))
                    .foregroundStyle(better ? Theme.accent : Theme.red)
                Group {
                    if better {
                        Text("compared with the reference operator, who leaves every server on.")
                    } else {
                        Text("The reference operator, who leaves every server on, emitted less.")
                    }
                }
                .foregroundStyle(.secondary)
                SwiftUI.Grid(alignment: .trailing, horizontalSpacing: 28, verticalSpacing: 8) {
                    GridRow {
                        Text(verbatim: "").gridColumnAlignment(.leading)
                        Text("You").bold()
                        Text("Reference").bold()
                    }
                    Divider().gridCellUnsizedAxes(.horizontal)
                    row("CO₂ (kg)", Format.kilograms(you.emissions), Format.kilograms(ref.emissions))
                    row("of it embodied (kg)", Format.kilograms(you.embodied), Format.kilograms(ref.embodied))
                    row("Energy (kWh)", Format.kilowattHours(you.facilityEnergy), Format.kilowattHours(ref.facilityEnergy))
                    row("SLA", Format.percent(you.sla), Format.percent(ref.sla))
                    row("Servers on (mean)", servers(you.meanServersOn), servers(ref.meanServersOn))
                }
                .font(.body.monospacedDigit())
                Label {
                    if you.sla >= target {
                        Text("SLA target of \(Format.percent(target)) met.")
                    } else {
                        Text("SLA target of \(Format.percent(target)) missed.")
                    }
                } icon: {
                    Image(systemName: you.sla >= target ? "checkmark.circle.fill" : "xmark.circle.fill")
                        .foregroundStyle(you.sla >= target ? Theme.accent : Theme.red)
                }
                Text("Switching servers off saves energy, not hardware: the embodied emissions stay the same.")
                    .font(.callout)
                    .foregroundStyle(.secondary)
                HStack {
                    Button { game.restart() } label: { Text("Play again").foregroundStyle(.white) }
                        .buttonStyle(.borderedProminent)
                        .tint(Theme.accent)
                    Button("Back to the briefing", action: onExit)
                        .buttonStyle(.bordered)
                }
            }
            .padding(32)
            .frame(maxWidth: 560)
            .background(Theme.panel, in: RoundedRectangle(cornerRadius: 20))
            .shadow(radius: 20)
        }
    }

    private func row(_ title: LocalizedStringKey, _ you: String, _ ref: String) -> some View {
        GridRow {
            Text(title).gridColumnAlignment(.leading)
            Text(verbatim: you)
            Text(verbatim: ref)
        }
    }

    private func servers(_ permille: Int) -> String {
        (Double(permille) / 1_000).formatted(.number.precision(.fractionLength(1)))
    }
}
