import SwiftUI
import LoadShiftCore

/// Colors and number formats of the app.
enum Theme {
    static let background = Color(red: 0.95, green: 0.96, blue: 0.95)
    static let panel = Color.white
    static let ink = Color(red: 0.13, green: 0.18, blue: 0.17)
    static let accent = Color(red: 0.12, green: 0.56, blue: 0.40)
    static let amber = Color(red: 0.91, green: 0.64, blue: 0.23)
    static let red = Color(red: 0.80, green: 0.30, blue: 0.25)
    static let demand = Color(red: 0.16, green: 0.36, blue: 0.68)
    static let capacity = Color(red: 0.62, green: 0.75, blue: 0.92)
    static let off = Color(white: 0.84)
    /// A server switched on that is still booting.
    static let booting = Color(red: 0.72, green: 0.82, blue: 0.92)
    /// Load bar of interactive work and of batch work in the hall.
    static let interactiveLoad = Color(red: 0.36, green: 0.62, blue: 0.95)
    static let batchLoad = amber
    static let slab = Color(red: 0.25, green: 0.30, blue: 0.29)

    /// Color of a carbon intensity: green when clean, amber, red when dirty (g/kWh).
    static func intensityColor(_ gramsPerKWh: Int) -> Color {
        switch gramsPerKWh {
        case ..<150: accent
        case ..<350: Color(red: 0.55, green: 0.66, blue: 0.27)
        case ..<500: amber
        default: red
        }
    }
}

enum Format {
    /// µg CO₂e as kg with one decimal ("91.8").
    static func kilograms(_ micrograms: Int) -> String {
        (Double(micrograms) / 1e9).formatted(.number.precision(.fractionLength(1)))
    }

    /// Per mille as percent with one decimal ("99.3 %").
    static func percent(_ permille: Int) -> String {
        (Double(permille) / 10).formatted(.number.precision(.fractionLength(1))) + "\u{202F}%"
    }

    /// Per mille as percent without decimals ("70 %").
    static func wholePercent(_ permille: Int) -> String {
        "\((permille + 5) / 10)\u{202F}%"
    }

    /// Signed change in percent with one decimal ("−19.1 %").
    static func change(_ value: Int, from base: Int) -> String {
        guard base != 0 else { return "–" }
        let permille = (value - base) * 1_000 / base
        let text = (Double(abs(permille)) / 10).formatted(.number.precision(.fractionLength(1)))
        return (permille < 0 ? "−" : "+") + text + "\u{202F}%"
    }

    /// A change given in per mille, signed, with one decimal ("−12.3 %").
    static func changePermille(_ permille: Int) -> String {
        let text = (Double(abs(permille)) / 10).formatted(.number.precision(.fractionLength(1)))
        return (permille < 0 ? "−" : "+") + text + "\u{202F}%"
    }

    /// W as kW with one decimal.
    static func kilowatts(_ watts: Int) -> String {
        (Double(watts) / 1_000).formatted(.number.precision(.fractionLength(1)))
    }

    /// mWh as kWh without decimals.
    static func kilowattHours(_ milliwattHours: Int) -> String {
        (milliwattHours / 1_000_000).formatted()
    }
}

extension GridRegion {
    var localizedName: LocalizedStringKey { LocalizedStringKey(name) }
}

extension Season {
    var localizedName: LocalizedStringKey { LocalizedStringKey(name) }
}
