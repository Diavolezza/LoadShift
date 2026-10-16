import SwiftUI
import LoadShiftCore

/// Scenario 2: plan the batch jobs by hand (docs/CONCEPT.md 5.2). Each job gets a start time; a planned job
/// starts by itself, if enough servers are free.
struct BatchPanel: View {
    let game: GameController

    var body: some View {
        let simulation = game.simulation
        let busy = simulation.servers.filter { $0.job != nil }.count
        VStack(alignment: .leading, spacing: 14) {
            Text("Batch jobs")
                .font(.title2.bold())
            Text("Each job needs servers for some hours and must be finished by its deadline. When it starts, its servers switch on and boot for 30 minutes first; servers without a job are off.")
                .font(.callout)
                .foregroundStyle(.secondary)
                .fixedSize(horizontal: false, vertical: true)
            Text("Servers in use: \(busy) of \(simulation.batchServerCount)")
                .font(.headline.monospacedDigit())
            if let urgent = game.urgentJob {
                UrgentBanner(name: simulation.jobs[urgent].spec.name,
                             latest: simulation.jobs[urgent].spec.latestStart) { game.dismissUrgent() }
            }
            ForEach(simulation.jobs) { job in
                JobRow(game: game, job: job)
            }
        }
    }
}

struct UrgentBanner: View {
    let name: String
    let latest: Int
    let dismiss: () -> Void

    var body: some View {
        HStack(alignment: .top, spacing: 10) {
            Image(systemName: "exclamationmark.triangle.fill").foregroundStyle(Theme.red)
            VStack(alignment: .leading, spacing: 4) {
                Text("\(Text(LocalizedStringKey(name))) has no start time and must start by \(TimeText.of(latest)), or it misses its deadline.")
                    .fixedSize(horizontal: false, vertical: true)
                Button("OK", action: dismiss).font(.callout.weight(.semibold))
            }
        }
        .font(.callout)
        .padding(12)
        .background(Theme.red.opacity(0.10), in: RoundedRectangle(cornerRadius: 10))
    }
}

enum TimeText {
    /// "Day 2, 13:30"
    static func of(_ tick: Int) -> String {
        let time = GameTime(tick: tick)
        let day = time.day - GameTime.start.day + 1
        return String(localized: "Day \(day), \(time.clockText)")
    }
}

struct JobRow: View {
    let game: GameController
    let job: BatchJob

    var body: some View {
        let spec = job.spec
        let now = game.simulation.tick
        VStack(alignment: .leading, spacing: 8) {
            HStack(alignment: .firstTextBaseline) {
                Text(LocalizedStringKey(spec.name))
                    .font(.headline)
                Spacer()
                status
            }
            Text("\(spec.servers) servers · \(spec.duration / GameTime.ticksPerHour) h · due \(TimeText.of(spec.deadline))")
                .font(.callout.monospacedDigit())
                .foregroundStyle(.secondary)
            if !job.state.isStarted {
                if let start = job.plannedStart {
                    HStack(spacing: 10) {
                        StepButton(symbol: "minus") { game.plan(job: job.id, start: start - GameTime.ticksPerHour) }
                            .disabled(start - GameTime.ticksPerHour < max(now, spec.arrival))
                        VStack(spacing: 0) {
                            Text("Start").font(.caption).foregroundStyle(.secondary)
                            Text(verbatim: TimeText.of(start)).font(.callout.weight(.semibold).monospacedDigit())
                        }
                        .frame(minWidth: 110)
                        StepButton(symbol: "plus") { game.plan(job: job.id, start: start + GameTime.ticksPerHour) }
                            .disabled(start + GameTime.ticksPerHour > game.scenario.endTick - spec.span)
                        Spacer()
                        Button { game.unplan(job: job.id) } label: { Image(systemName: "xmark.circle") }
                            .accessibilityLabel(Text("Remove the plan"))
                    }
                    let mean = BatchPlanner.meanForecast(spec, start: start, grid: game.simulation.grid, madeAt: now)
                    if start > spec.latestStart {
                        Text("Too late for the deadline – start by \(TimeText.of(spec.latestStart)).")
                            .font(.caption.weight(.semibold))
                            .foregroundStyle(Theme.red)
                    } else {
                        Text("Forecast while it runs: \((mean + 5) / 10) g/kWh · start by \(TimeText.of(spec.latestStart))")
                            .font(.caption.monospacedDigit())
                            .foregroundStyle(.secondary)
                    }
                } else {
                    HStack {
                        Button("Plan") { game.plan(job: job.id, start: game.suggestedStart(job: job.id)) }
                            .buttonStyle(.bordered)
                            .lineLimit(1)
                        if spec.arrival <= now {
                            Button("Start now") { game.plan(job: job.id, start: now) }
                                .buttonStyle(.bordered)
                                .lineLimit(1)
                        }
                    }
                    .disabled(game.isFinished)
                    Text("start by \(TimeText.of(spec.latestStart))")
                        .font(.caption.monospacedDigit())
                        .foregroundStyle(.secondary)
                }
            }
        }
        .padding(12)
        .background(Theme.background, in: RoundedRectangle(cornerRadius: 12))
    }

    @ViewBuilder private var status: some View {
        let now = game.simulation.tick
        switch job.state {
        case .waiting:
            if job.spec.arrival > now {
                Label("arrives \(TimeText.of(job.spec.arrival))", systemImage: "clock").foregroundStyle(Color.secondary)
            } else {
                Label("not planned", systemImage: "circle.dashed")
                    .foregroundStyle(job.missedDeadline(at: now) ? Theme.red : Color.secondary)
            }
        case .planned:
            Label("planned", systemImage: "calendar").foregroundStyle(Theme.accent)
        case .started:
            Label("booting", systemImage: "power").foregroundStyle(Theme.demand)
        case .running(_, let endsAt):
            Label("runs until \(GameTime(tick: endsAt).clockText)", systemImage: "gearshape.2.fill").foregroundStyle(Theme.amber)
        case .done(_, let at):
            if at > job.spec.deadline {
                Label("late, \(GameTime(tick: at).clockText)", systemImage: "xmark.circle.fill").foregroundStyle(Theme.red)
            } else {
                Label("done \(GameTime(tick: at).clockText)", systemImage: "checkmark.circle.fill").foregroundStyle(Theme.accent)
            }
        }
    }
}

struct StepButton: View {
    let symbol: String
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            Image(systemName: symbol)
                .font(.headline.weight(.bold))
                .frame(width: 36, height: 36)
                .background(Theme.accent.opacity(0.14), in: Circle())
                .foregroundStyle(Theme.accent)
        }
        .buttonStyle(.plain)
    }
}
