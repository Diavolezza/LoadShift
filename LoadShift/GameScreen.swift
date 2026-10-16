import SwiftUI
import LoadShiftCore

/// The play screen (docs/CONCEPT.md 10.1): top bar with the goals, hall and controls, timeline.
struct GameScreen: View {
    let game: GameController
    let onExit: () -> Void

    var body: some View {
        GeometryReader { geo in
            VStack(spacing: 0) {
                TopBar(game: game, onExit: onExit)
                GoalBar(game: game)
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
        let intensity = simulation.grid.intensity(tick: min(shownTick, simulation.scenario.endTick - 1))
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

/// The goals of the scenario and where the run stands: SLA target, CO₂ compared with the reference so far,
/// and what the second and third star need.
struct GoalBar: View {
    let game: GameController

    var body: some View {
        let scenario = game.scenario
        let totals = game.simulation.totals
        let slaOK = totals.sla >= scenario.slaTarget
        let change = game.changeSoFar
        HStack(spacing: 20) {
            Text("Goals")
                .font(.caption.weight(.semibold))
                .foregroundStyle(.secondary)
            GoalItem(ok: slaOK, started: change != nil, text: "SLA ≥ \(Format.percent(scenario.slaTarget))")
            GoalItem(ok: (change ?? 0) < 0, started: change != nil, text: co2Text(change))
            Spacer(minLength: 0)
            HStack(spacing: 4) {
                Text(verbatim: "★★").foregroundStyle(Theme.amber)
                Text("CO₂ from \(Format.changePermille(-scenario.goals.reduction))")
            }
            HStack(spacing: 4) {
                Text(verbatim: "★★★").foregroundStyle(Theme.amber)
                Text(StarText.short(scenario.goals.special))
            }
        }
        .lineLimit(1)
        .minimumScaleFactor(0.8)
        .font(.callout.monospacedDigit())
        .padding(.horizontal, 20)
        .padding(.vertical, 6)
        .background(Theme.panel)
    }
}

private func co2Text(_ change: Int?) -> LocalizedStringKey {
    guard let change else { return "CO₂ vs. reference" }
    return "CO₂ vs. reference: \(Format.changePermille(change))"
}

struct GoalItem: View {
    let ok: Bool
    let started: Bool
    let text: LocalizedStringKey

    var body: some View {
        HStack(spacing: 5) {
            Image(systemName: !started ? "circle" : (ok ? "checkmark.circle.fill" : "exclamationmark.triangle.fill"))
                .foregroundStyle(!started ? Color.secondary : (ok ? Theme.accent : Theme.red))
            Text(text)
        }
    }
}

enum StarText {
    static func special(_ goal: SpecialGoal) -> LocalizedStringKey {
        switch goal {
        case .meanUtilization(let p): return "mean utilization at least \(Format.wholePercent(p))"
        case .reduction(let p): return "CO₂ \(Format.changePermille(-p)) or better"
        }
    }

    static func short(_ goal: SpecialGoal) -> LocalizedStringKey {
        switch goal {
        case .meanUtilization(let p): return "utilization ≥ \(Format.wholePercent(p))"
        case .reduction(let p): return "CO₂ from \(Format.changePermille(-p))"
        }
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

struct StarRow: View {
    let stars: Int
    var size: CGFloat = 30

    var body: some View {
        HStack(spacing: 4) {
            ForEach(0..<3, id: \.self) { i in
                Image(systemName: i < stars ? "star.fill" : "star")
                    .font(.system(size: size))
                    .foregroundStyle(i < stars ? Theme.amber : Color.secondary.opacity(0.5))
            }
        }
        .accessibilityLabel(Text("\(stars) of 3 stars"))
    }
}

/// End of the scenario (CONCEPT.md 8.2 and 8.3): stars, the goals, the result next to the reference operator's,
/// and "what if" – the same day with other rules.
struct EndCard: View {
    let game: GameController
    let onExit: () -> Void

    var body: some View {
        ZStack {
            Color.black.opacity(0.35).ignoresSafeArea()
            ViewThatFits(in: .vertical) {
                details
                ScrollView { details }
            }
            .frame(maxWidth: 600)
            .safeAreaInset(edge: .bottom) {
                HStack {
                    Button { game.restart() } label: { Text("Play again").foregroundStyle(.white) }
                        .buttonStyle(.borderedProminent)
                        .tint(Theme.accent)
                    Button("Back to the briefing", action: onExit)
                        .buttonStyle(.bordered)
                    Spacer()
                }
                .padding(.horizontal, 30)
                .padding(.vertical, 16)
                .background(Theme.panel)
            }
            .frame(maxWidth: 600)
            .background(Theme.panel, in: RoundedRectangle(cornerRadius: 20))
            .clipShape(RoundedRectangle(cornerRadius: 20))
            .shadow(radius: 20)
            .padding(.horizontal, 24)
            .padding(.vertical, 60)
        }
    }

    private var details: some View {
        let scenario = game.scenario
        let you = game.simulation.totals
        let ref = game.reference.totals
        let met = scenario.goalsMet(you, reference: ref)
        return VStack(alignment: .leading, spacing: 16) {
            HStack(alignment: .firstTextBaseline) {
                Text("Day complete")
                    .font(.largeTitle.bold())
                Spacer()
                StarRow(stars: game.stars)
            }
            Text(verbatim: "\(Format.change(you.emissions, from: ref.emissions)) CO₂")
                .font(.system(size: 40, weight: .bold, design: .rounded))
                .foregroundStyle(you.emissions < ref.emissions ? Theme.accent : Theme.red)
            Text("compared with the reference operator, who leaves every server on.")
                .foregroundStyle(.secondary)
            VStack(alignment: .leading, spacing: 6) {
                GoalLine(ok: met.rules, stars: 1,
                         text: "SLA at least \(Format.percent(scenario.slaTarget)) and less CO₂ than the reference")
                GoalLine(ok: met.reduction, stars: 2,
                         text: "CO₂ \(Format.changePermille(-scenario.goals.reduction)) or better")
                GoalLine(ok: met.special, stars: 3, text: StarText.special(scenario.goals.special))
            }
            SwiftUI.Grid(alignment: .trailing, horizontalSpacing: 24, verticalSpacing: 6) {
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
                row("Mean utilization", Format.wholePercent(you.meanUtilization), Format.wholePercent(ref.meanUtilization))
            }
            .font(.body.monospacedDigit())
            Text("Switching servers off saves energy, not hardware: the embodied emissions stay the same.")
                .font(.callout)
                .foregroundStyle(.secondary)
            WhatIf(game: game)
        }
        .padding(30)
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

struct GoalLine: View {
    /// Nil: no check mark (briefing).
    let ok: Bool?
    let stars: Int
    let text: LocalizedStringKey
    var font: Font = .callout

    var body: some View {
        HStack(alignment: .firstTextBaseline, spacing: 8) {
            if let ok {
                Image(systemName: ok ? "checkmark.circle.fill" : "circle")
                    .foregroundStyle(ok ? Theme.accent : Color.secondary)
            }
            Text(verbatim: String(repeating: "★", count: stars))
                .foregroundStyle(Theme.amber)
                .fixedSize()
                .frame(width: 64, alignment: .leading)
            Text(text)
                .fixedSize(horizontal: false, vertical: true)
        }
        .font(font)
    }
}

/// The same day replayed with other rules: the same load and the same grid mix, so only the rules differ.
struct WhatIf: View {
    let game: GameController

    var body: some View {
        let you = game.simulation.totals
        let ref = game.reference.totals
        VStack(alignment: .leading, spacing: 8) {
            Text("What if …")
                .font(.headline)
            Text("The same day with exactly the same load and grid mix – only the rules differ:")
                .font(.callout)
                .foregroundStyle(.secondary)
            SwiftUI.Grid(alignment: .trailing, horizontalSpacing: 16, verticalSpacing: 5) {
                GridRow {
                    Text(verbatim: "").gridColumnAlignment(.leading)
                    Text("CO₂ vs. you").foregroundStyle(.secondary)
                    Text("SLA").foregroundStyle(.secondary)
                    Text(verbatim: "").gridColumnAlignment(.leading)
                }
                ForEach(Array(game.whatIf().enumerated()), id: \.offset) { _, alternative in
                    GridRow {
                        Text(alternative.label).gridColumnAlignment(.leading)
                        Text(verbatim: Format.change(alternative.totals.emissions, from: you.emissions))
                        Text(verbatim: Format.percent(alternative.totals.sla))
                            .foregroundStyle(alternative.totals.sla >= game.scenario.slaTarget ? Theme.ink : Theme.red)
                        StarRow(stars: game.scenario.stars(alternative.totals, reference: ref), size: 11)
                    }
                }
            }
            .font(.callout.monospacedDigit())
        }
        .padding(14)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Theme.background, in: RoundedRectangle(cornerRadius: 12))
    }
}
