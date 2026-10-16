import SwiftUI
import LoadShiftCore

/// The timeline (docs/CONCEPT.md 10.1): carbon intensity of the grid – what happened so far and the forecast
/// with its uncertainty band – and below it demand and capacity. The vertical line is now.
struct TimelineChart: View {
    let simulation: Simulation

    var body: some View {
        Canvas { context, size in
            draw(in: &context, size: size)
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 10)
    }

    private func draw(in context: inout GraphicsContext, size: CGSize) {
        let scenario = simulation.scenario
        let start = scenario.startTick, end = scenario.endTick
        let now = min(simulation.tick, end)
        let grid = simulation.grid
        let left = 44.0, right = size.width - 8, labelBand = 18.0
        let laneGap = 26.0
        let usable = size.height - labelBand - laneGap
        let topLane = (top: 16.0, bottom: 16.0 + usable * 0.52 - 16)
        let lowLane = (top: topLane.bottom + laneGap + 14, bottom: size.height - labelBand)
        func x(_ tick: Int) -> CGFloat { left + (right - left) * CGFloat(tick - start) / CGFloat(end - start) }

        // MARK: Carbon intensity
        var past: [(Int, Int)] = simulation.history.map { ($0.tick, $0.intensityTenths) }
        if let last = past.last, last.0 < now { past.append((now, grid.intensityTenths(tick: now))) }
        let future = now < end ? Array(now...end) : []
        let forecast = future.map { ($0, grid.forecastTenths(tick: $0, madeAt: now), grid.band(lead: $0 - now)) }
        let maxTenths = max(10, (past.map(\.1) + forecast.map { $0.1 * (1_000 + $0.2) / 1_000 }).max() ?? 10)
        let scaleTop = Double(niceCeiling(maxTenths / 10))
        func yI(_ tenths: Int) -> CGFloat {
            topLane.bottom - (topLane.bottom - topLane.top) * CGFloat(Double(tenths) / 10 / scaleTop)
        }
        // Band of the forecast.
        if forecast.count > 1 {
            var band = Path()
            band.move(to: CGPoint(x: x(forecast[0].0), y: yI(forecast[0].1 * (1_000 + forecast[0].2) / 1_000)))
            for f in forecast { band.addLine(to: CGPoint(x: x(f.0), y: yI(f.1 * (1_000 + f.2) / 1_000))) }
            for f in forecast.reversed() { band.addLine(to: CGPoint(x: x(f.0), y: yI(f.1 * (1_000 - f.2) / 1_000))) }
            band.closeSubpath()
            context.fill(band, with: .color(Color.gray.opacity(0.16)))
            var line = Path()
            line.move(to: CGPoint(x: x(forecast[0].0), y: yI(forecast[0].1)))
            for f in forecast.dropFirst() { line.addLine(to: CGPoint(x: x(f.0), y: yI(f.1))) }
            context.stroke(line, with: .color(Color.gray), style: StrokeStyle(lineWidth: 1.5, dash: [5, 4]))
        }
        // What happened: area colored by intensity.
        if past.count > 1 {
            var area = Path()
            area.move(to: CGPoint(x: x(past[0].0), y: topLane.bottom))
            for (t, v) in past { area.addLine(to: CGPoint(x: x(t), y: yI(v))) }
            area.addLine(to: CGPoint(x: x(past.last!.0), y: topLane.bottom))
            area.closeSubpath()
            let stops = [0, 150, 350, 500, 700].map { g -> Gradient.Stop in
                let location = min(1, max(0, Double(g) / scaleTop))
                return Gradient.Stop(color: Theme.intensityColor(g).opacity(0.55), location: location)
            }
            context.fill(area, with: .linearGradient(Gradient(stops: stops),
                                                     startPoint: CGPoint(x: 0, y: topLane.bottom),
                                                     endPoint: CGPoint(x: 0, y: yI(Int(scaleTop) * 10))))
            var line = Path()
            line.move(to: CGPoint(x: x(past[0].0), y: yI(past[0].1)))
            for (t, v) in past.dropFirst() { line.addLine(to: CGPoint(x: x(t), y: yI(v))) }
            context.stroke(line, with: .color(Theme.ink.opacity(0.8)), lineWidth: 1.5)
        }
        axis(&context, top: topLane.top, bottom: topLane.bottom, left: left, right: right,
             maxLabel: "\(Int(scaleTop))")
        context.draw(Text("Carbon intensity of the grid, g CO₂/kWh – dashed: forecast").font(.caption.weight(.semibold)),
                     at: CGPoint(x: left, y: topLane.top - 12), anchor: .leading)

        // MARK: Demand and capacity
        let history = simulation.history
        let expected = future.map { ($0, scenario.interactive.expected(tick: $0)) }
        let fleetCapacity = simulation.servers.reduce(0) { $0 + $1.kind.capacity }
        let maxUnits = max(10, max(fleetCapacity, (history.map(\.demand) + expected.map(\.1)).max() ?? 10))
        let unitsTop = Double(niceCeiling(maxUnits))
        func yU(_ units: Int) -> CGFloat {
            lowLane.bottom - (lowLane.bottom - lowLane.top) * CGFloat(Double(units) / unitsTop)
        }
        if !history.isEmpty {
            var capacity = Path()
            capacity.move(to: CGPoint(x: x(history[0].tick), y: lowLane.bottom))
            for r in history {
                capacity.addLine(to: CGPoint(x: x(r.tick), y: yU(r.capacity)))
                capacity.addLine(to: CGPoint(x: x(r.tick + 1), y: yU(r.capacity)))
            }
            capacity.addLine(to: CGPoint(x: x(history.last!.tick + 1), y: lowLane.bottom))
            capacity.closeSubpath()
            context.fill(capacity, with: .color(Theme.capacity.opacity(0.6)))
            var demand = Path()
            demand.move(to: CGPoint(x: x(history[0].tick), y: yU(history[0].demand)))
            for r in history.dropFirst() { demand.addLine(to: CGPoint(x: x(r.tick), y: yU(r.demand))) }
            context.stroke(demand, with: .color(Theme.demand), lineWidth: 1.8)
        }
        if expected.count > 1 {
            var line = Path()
            line.move(to: CGPoint(x: x(expected[0].0), y: yU(expected[0].1)))
            for (t, v) in expected.dropFirst() { line.addLine(to: CGPoint(x: x(t), y: yU(v))) }
            context.stroke(line, with: .color(Theme.demand.opacity(0.6)), style: StrokeStyle(lineWidth: 1.5, dash: [2, 3]))
        }
        axis(&context, top: lowLane.top, bottom: lowLane.bottom, left: left, right: right, maxLabel: "\(Int(unitsTop))")
        context.draw(Text("Demand (line) and capacity of the running servers (area), 1,000 requests per 15 min")
                        .font(.caption.weight(.semibold)),
                     at: CGPoint(x: left, y: lowLane.top - 12), anchor: .leading)

        // MARK: Hours and now
        var tick = start
        while tick <= end {
            let time = GameTime(tick: tick)
            if time.minute == 0 && time.hour % 3 == 0 {
                let anchor: UnitPoint = tick == start ? .leading : (tick == end ? .trailing : .center)
                context.draw(Text(verbatim: time.clockText).font(.caption2.monospacedDigit()).foregroundStyle(.secondary),
                             at: CGPoint(x: x(tick), y: size.height - labelBand / 2), anchor: anchor)
                var mark = Path()
                mark.move(to: CGPoint(x: x(tick), y: topLane.top)); mark.addLine(to: CGPoint(x: x(tick), y: lowLane.bottom))
                context.stroke(mark, with: .color(Color.gray.opacity(0.15)), lineWidth: 1)
            }
            tick += 1
        }
        var nowLine = Path()
        nowLine.move(to: CGPoint(x: x(now), y: topLane.top - 4))
        nowLine.addLine(to: CGPoint(x: x(now), y: lowLane.bottom))
        context.stroke(nowLine, with: .color(Theme.ink), lineWidth: 1.5)
    }

    private func axis(_ context: inout GraphicsContext, top: CGFloat, bottom: CGFloat, left: CGFloat, right: CGFloat,
                      maxLabel: String) {
        var base = Path()
        base.move(to: CGPoint(x: left, y: bottom)); base.addLine(to: CGPoint(x: right, y: bottom))
        context.stroke(base, with: .color(Color.gray.opacity(0.5)), lineWidth: 1)
        context.draw(Text(verbatim: maxLabel).font(.caption2.monospacedDigit()).foregroundStyle(.secondary),
                     at: CGPoint(x: left - 6, y: top), anchor: .trailing)
        context.draw(Text(verbatim: "0").font(.caption2.monospacedDigit()).foregroundStyle(.secondary),
                     at: CGPoint(x: left - 6, y: bottom), anchor: .trailing)
        var topLine = Path()
        topLine.move(to: CGPoint(x: left, y: top)); topLine.addLine(to: CGPoint(x: right, y: top))
        context.stroke(topLine, with: .color(Color.gray.opacity(0.15)), style: StrokeStyle(lineWidth: 1, dash: [3, 3]))
    }

    /// The next round number at or above a value (1, 2 or 5 × 10ⁿ).
    private func niceCeiling(_ value: Int) -> Int {
        var magnitude = 1
        while magnitude * 10 <= value { magnitude *= 10 }
        for step in [1, 2, 5, 10] where step * magnitude >= value { return step * magnitude }
        return 10 * magnitude
    }
}
