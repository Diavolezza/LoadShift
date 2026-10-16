/// The player's rules (docs/CONCEPT.md, section 2: policies, not clicks). Step 1: capacity only.
public enum CapacityMode: Equatable, Sendable, Codable {
    /// Every server runs all the time (the reference operator).
    case allOn
    /// A fixed number of servers for interactive work runs; the most efficient ones are used first.
    case manual(servers: Int)
    /// The simulation keeps enough servers on to meet the expected demand of the next hour at the target
    /// utilization (per mille, 400 … 900).
    case autoscale(targetUtilization: Int)
}

public struct Policy: Equatable, Sendable, Codable {
    public var capacity: CapacityMode

    public init(capacity: CapacityMode) {
        self.capacity = capacity
    }

    /// What the reference operator does: all servers on (CONCEPT.md 8.2).
    public static let reference = Policy(capacity: .allOn)

    /// Allowed range of the target utilization, per mille.
    public static let targetRange = 400...900
    /// Ticks the autoscaler looks ahead beyond the boot time: one hour.
    public static let lookahead = GameTime.ticksPerHour
}
