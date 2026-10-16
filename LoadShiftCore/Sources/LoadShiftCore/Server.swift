/// Servers (docs/CONCEPT.md, section 6). Three kinds, two generations: one case per kind and generation.
public enum ServerKind: Int, CaseIterable, Sendable, Codable {
    case cpuOld, cpuCurrent, armCurrent, gpuOld, gpuCurrent

    public var name: String {
        switch self {
        case .cpuOld: "CPU server, old"
        case .cpuCurrent: "CPU server"
        case .armCurrent: "ARM server"
        case .gpuOld: "GPU server, old"
        case .gpuCurrent: "GPU server"
        }
    }

    /// Maximum power, W (CONCEPT.md 6: SPECpower about 303 W per socket [S9]; DGX H100 up to 10.2 kW [S12]).
    public var maxPower: Int {
        switch self {
        case .cpuOld, .cpuCurrent: 600
        case .armCurrent: 300
        case .gpuOld: 6_400
        case .gpuCurrent: 10_200
        }
    }

    /// Idle power as a share of the maximum, per mille (SPECpower 2024: 25.7 % [S9]; older servers more).
    public var idlePermille: Int {
        switch self {
        case .cpuOld: 350
        case .cpuCurrent: 260
        case .armCurrent: 200
        case .gpuOld, .gpuCurrent: 150
        }
    }

    public var idlePower: Int { maxPower * idlePermille / 1_000 }

    /// Work units per tick at full load for interactive and streaming work (game value: "current" does about
    /// twice the work per kWh of "old"; ARM about a fifth more than the current CPU server [S11]).
    /// GPU servers do no interactive work.
    public var capacity: Int {
        switch self {
        case .cpuOld: 10
        case .cpuCurrent: 20
        case .armCurrent: 12
        case .gpuOld, .gpuCurrent: 0
        }
    }

    /// Embodied emissions in kg CO₂e (Dell R760 LCA [S10], HGX H100 PCF [S13], game values in between).
    public var embodiedKilograms: Int {
        switch self {
        case .cpuOld: 500
        case .cpuCurrent: 550
        case .armCurrent: 450
        case .gpuOld: 1_450
        case .gpuCurrent: 1_800
        }
    }

    /// Embodied emissions booked per tick in service, µg (TE / (6 years × 96 ticks per day)).
    public var embodiedPerTick: Int {
        embodiedKilograms * 1_000_000_000 / (Units.lifespanDays * GameTime.ticksPerDay)
    }

    /// Serves interactive and streaming work.
    public var servesInteractive: Bool { capacity > 0 }

    /// Work per kW at full load, used to pick the most efficient servers first (per mille).
    public var efficiency: Int { capacity * 1_000_000 / maxPower }

    /// Ticks from switching on until the server takes work: 30 minutes (game value).
    public static let bootTicks = 2
}

public enum ServerState: Equatable, Sendable, Codable {
    case off
    /// Switched on, takes work from the given tick.
    case booting(readyAt: Int)
    case on

    public var isOn: Bool { self == .on }
    public var isPowered: Bool { self != .off }
}

public struct Server: Equatable, Sendable, Codable, Identifiable {
    public let id: Int
    public let kind: ServerKind
    public internal(set) var state: ServerState
    /// Share of its capacity used in the last tick, per mille.
    public internal(set) var utilization: Int = 0

    public init(id: Int, kind: ServerKind, state: ServerState = .on) {
        self.id = id
        self.kind = kind
        self.state = state
    }

    /// Power drawn at the given utilization, W: idle plus the dynamic part in proportion to the load.
    /// A booting server draws idle power, a switched-off one nothing.
    public func power(utilization: Int) -> Int {
        switch state {
        case .off: return 0
        case .booting: return kind.idlePower
        case .on: return kind.idlePower + (kind.maxPower - kind.idlePower) * utilization / 1_000
        }
    }
}
