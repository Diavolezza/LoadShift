/// A scenario: region, season, length, fleet and load (docs/CONCEPT.md, section 9).
public struct FleetEntry: Equatable, Sendable, Codable {
    public let kind: ServerKind
    public let count: Int

    public init(_ kind: ServerKind, _ count: Int) {
        self.kind = kind
        self.count = count
    }
}

public struct Scenario: Equatable, Sendable {
    public let number: Int
    public let title: String
    public let setting: GridSetting
    public let weather: WeatherMode
    public let days: Int
    public let seed: UInt64
    public let fleet: [FleetEntry]
    public let interactive: InteractiveLoad
    public let facility: Facility
    /// SLA target, per mille of the demand served in time.
    public let slaTarget: Int

    public init(number: Int, title: String, setting: GridSetting, weather: WeatherMode = .drawn, days: Int,
                seed: UInt64, fleet: [FleetEntry], interactive: InteractiveLoad,
                facility: Facility = Facility(), slaTarget: Int = 990) {
        self.number = number
        self.title = title
        self.setting = setting
        self.weather = weather
        self.days = days
        self.seed = seed
        self.fleet = fleet
        self.interactive = interactive
        self.facility = facility
        self.slaTarget = slaTarget
    }

    /// First tick: 06:00 on day 0.
    public var startTick: Int { GameTime.start.tick }
    /// The tick after the last one.
    public var endTick: Int { startTick + days * GameTime.ticksPerDay }

    /// Scenario 1, "Night shift": a web shop in Germany in autumn. The fleet is sized for the Christmas season;
    /// at night the load falls to a fifth of the evening peak, and idle servers still draw a quarter of their
    /// maximum power (G1, G2).
    public static let nightShift = Scenario(
        number: 1, title: "Night shift", setting: GridSetting(.germany, .autumn), days: 1, seed: 1,
        fleet: [FleetEntry(.cpuCurrent, 20)],
        interactive: InteractiveLoad(peak: 240))

    public static let all: [Scenario] = [nightShift]
}
