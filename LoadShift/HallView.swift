import SwiftUI
import LoadShiftCore

/// The data center hall in isometric view (docs/CONCEPT.md 10.1, D2): racks of ten servers each. A server
/// is a slab on the rack's front: light grey when off, light blue while booting, dark when on – its bar shows
/// how busy it is, blue for interactive work, amber for a batch job.
struct HallView: View {
    let servers: [Server]
    var interactive = true
    var batch = false

    static let slotsPerRack = 10

    var body: some View {
        let on = servers.filter { $0.state.isPowered }.count
        let itPower = servers.reduce(0) { $0 + $1.power(utilization: $1.utilization) }
        VStack(alignment: .leading, spacing: 10) {
            Canvas { context, size in
                draw(in: &context, size: size)
            }
            .accessibilityLabel(Text("\(on) of \(servers.count) servers on"))
            HStack(spacing: 18) {
                LegendSwatch(color: Theme.off, text: "Off")
                LegendSwatch(color: Theme.booting, text: "Booting")
                if interactive {
                    LegendSwatch(color: Theme.slab, fill: Theme.interactiveLoad, text: "Web requests")
                }
                if batch {
                    LegendSwatch(color: Theme.slab, fill: Theme.batchLoad, text: "Batch job")
                }
                Spacer()
                Text("\(on) of \(servers.count) servers on · \(Format.kilowatts(itPower)) kW IT power")
                    .font(.callout.monospacedDigit())
                    .lineLimit(1)
                    .fixedSize()
            }
            .font(.caption)
        }
        .padding(16)
    }

    // MARK: - Drawing

    /// Rack size in floor units: width (x), depth (y), height (z).
    private let rackWidth = 1.0, rackDepth = 0.7, rackHeight = 2.4, rackGap = 0.45

    private func draw(in context: inout GraphicsContext, size: CGSize) {
        let racks = max(1, (servers.count + Self.slotsPerRack - 1) / Self.slotsPerRack)
        let length = Double(racks) * rackWidth + Double(racks - 1) * rackGap
        // Floor from -0.6 to length + 0.6 in x, -0.6 to depth + 0.9 in y.
        let floorX = (-0.6, length + 0.6), floorY = (-0.6, rackDepth + 0.9)
        // Fit the projected floor and racks into the canvas.
        let corners = [iso(floorX.0, floorY.0, 0), iso(floorX.1, floorY.0, 0), iso(floorX.0, floorY.1, 0),
                       iso(floorX.1, floorY.1, 0), iso(0, 0, rackHeight), iso(length, 0, rackHeight)]
        let minX = corners.map(\.x).min()!, maxX = corners.map(\.x).max()!
        let minY = corners.map(\.y).min()!, maxY = corners.map(\.y).max()!
        let scale = min(size.width / (maxX - minX), size.height / (maxY - minY)) * 0.94
        let offset = CGPoint(x: (size.width - (maxX - minX) * scale) / 2 - minX * scale,
                             y: (size.height - (maxY - minY) * scale) / 2 - minY * scale)
        func p(_ x: Double, _ y: Double, _ z: Double) -> CGPoint {
            let q = iso(x, y, z)
            return CGPoint(x: q.x * scale + offset.x, y: q.y * scale + offset.y)
        }
        func polygon(_ points: [CGPoint]) -> Path {
            var path = Path()
            path.addLines(points)
            path.closeSubpath()
            return path
        }

        // Floor with a light grid.
        context.fill(polygon([p(floorX.0, floorY.0, 0), p(floorX.1, floorY.0, 0), p(floorX.1, floorY.1, 0),
                              p(floorX.0, floorY.1, 0)]), with: .color(Color(red: 0.89, green: 0.92, blue: 0.90)))
        var grid = Path()
        var x = floorX.0
        while x <= floorX.1 + 0.001 {
            grid.move(to: p(x, floorY.0, 0)); grid.addLine(to: p(x, floorY.1, 0)); x += 0.5
        }
        var y = floorY.0
        while y <= floorY.1 + 0.001 {
            grid.move(to: p(floorX.0, y, 0)); grid.addLine(to: p(floorX.1, y, 0)); y += 0.5
        }
        context.stroke(grid, with: .color(Color.white.opacity(0.7)), lineWidth: 1)

        for rack in 0..<racks {
            let x0 = Double(rack) * (rackWidth + rackGap), x1 = x0 + rackWidth
            let y0 = 0.0, y1 = rackDepth, top = rackHeight
            // Shadow, body: top, front (y = y1), side (x = x1).
            context.fill(polygon([p(x0, y1, 0), p(x1, y1, 0), p(x1 + 0.25, y1 + 0.25, 0), p(x0 + 0.25, y1 + 0.25, 0)]),
                         with: .color(Color.black.opacity(0.06)))
            context.fill(polygon([p(x0, y0, top), p(x1, y0, top), p(x1, y1, top), p(x0, y1, top)]),
                         with: .color(Color(white: 0.95)))
            context.fill(polygon([p(x0, y1, 0), p(x1, y1, 0), p(x1, y1, top), p(x0, y1, top)]),
                         with: .color(Color(white: 0.78)))
            context.fill(polygon([p(x1, y0, 0), p(x1, y1, 0), p(x1, y1, top), p(x1, y0, top)]),
                         with: .color(Color(white: 0.68)))

            // Servers on the front, from the bottom up.
            let slotHeight = (top - 0.2) / Double(Self.slotsPerRack)
            for slot in 0..<Self.slotsPerRack {
                let index = rack * Self.slotsPerRack + slot
                guard index < servers.count else { break }
                let server = servers[index]
                let z0 = 0.1 + Double(slot) * slotHeight + 0.025, z1 = z0 + slotHeight - 0.05
                let sx0 = x0 + 0.07, sx1 = x1 - 0.07
                let slab = polygon([p(sx0, y1, z0), p(sx1, y1, z0), p(sx1, y1, z1), p(sx0, y1, z1)])
                switch server.state {
                case .off:
                    context.fill(slab, with: .color(Theme.off))
                case .booting:
                    context.fill(slab, with: .color(Theme.booting))
                case .on:
                    context.fill(slab, with: .color(Theme.slab))
                    let width = (sx1 - sx0 - 0.08) * Double(max(server.utilization, 20)) / 1_000
                    let barZ0 = z0 + (z1 - z0) * 0.3, barZ1 = z1 - (z1 - z0) * 0.3
                    context.fill(polygon([p(sx0 + 0.04, y1, barZ0), p(sx0 + 0.04 + width, y1, barZ0),
                                          p(sx0 + 0.04 + width, y1, barZ1), p(sx0 + 0.04, y1, barZ1)]),
                                 with: .color(server.job == nil ? Theme.interactiveLoad : Theme.batchLoad))
                }
            }
        }
    }

    /// Isometric projection of floor coordinates (x to the right back, y to the left front, z up).
    private func iso(_ x: Double, _ y: Double, _ z: Double) -> CGPoint {
        CGPoint(x: (x - y) * 0.866, y: (x + y) * 0.5 - z)
    }
}

struct LegendSwatch: View {
    let color: Color
    var fill: Color? = nil
    let text: LocalizedStringKey

    var body: some View {
        HStack(spacing: 6) {
            ZStack(alignment: .leading) {
                RoundedRectangle(cornerRadius: 2).fill(color).frame(width: 22, height: 10)
                if let fill {
                    RoundedRectangle(cornerRadius: 1).fill(fill).frame(width: 12, height: 4).padding(.leading, 3)
                }
            }
            Text(text)
        }
    }
}
