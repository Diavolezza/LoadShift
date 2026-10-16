/// Game time. One tick is 15 game minutes, one game day 96 ticks (concept, section 3.1).
/// Tick 0 is 00:00 on day 0; the first scenario day starts at 06:00 (tick 24).
public struct GameTime: Hashable, Comparable, Codable, Sendable {
    public static let minutesPerTick = 15
    public static let ticksPerHour = 60 / minutesPerTick
    public static let ticksPerDay = 24 * ticksPerHour
    /// Tick at which a scenario starts: 06:00 on day 0.
    public static let start = GameTime(tick: 6 * ticksPerHour)

    public let tick: Int

    public init(tick: Int) {
        precondition(tick >= 0, "game time cannot be negative")
        self.tick = tick
    }

    public init(day: Int, hour: Int, minute: Int = 0) {
        precondition((0..<24).contains(hour) && minute % Self.minutesPerTick == 0 && (0..<60).contains(minute))
        self.init(tick: day * Self.ticksPerDay + hour * Self.ticksPerHour + minute / Self.minutesPerTick)
    }

    /// Day number, starting with 0.
    public var day: Int { tick / Self.ticksPerDay }
    /// Tick within the day, 0 ..< 96.
    public var tickOfDay: Int { tick % Self.ticksPerDay }
    public var hour: Int { tickOfDay / Self.ticksPerHour }
    public var minute: Int { (tickOfDay % Self.ticksPerHour) * Self.minutesPerTick }

    /// "14:15"
    public var clockText: String {
        (hour < 10 ? "0" : "") + "\(hour):" + (minute < 10 ? "0" : "") + "\(minute)"
    }

    public func advanced(by ticks: Int) -> GameTime { GameTime(tick: tick + ticks) }

    public static func < (lhs: GameTime, rhs: GameTime) -> Bool { lhs.tick < rhs.tick }
}
