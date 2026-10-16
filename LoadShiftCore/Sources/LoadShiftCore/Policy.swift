/// The player's rules (docs/CONCEPT.md, section 2: policies, not clicks): capacity and batch work.
public enum CapacityMode: Equatable, Sendable, Codable {
    /// Every server runs all the time (the reference operator).
    case allOn
    /// A fixed number of servers for interactive work runs; the most efficient ones are used first.
    case manual(servers: Int)
    /// The simulation keeps enough servers on to meet the expected demand of the next hour at the target
    /// utilization (per mille, 400 … 900).
    case autoscale(targetUtilization: Int)
}

/// When batch jobs start (CONCEPT.md 5.2).
public enum BatchMode: Equatable, Sendable, Codable {
    /// The player plans every job by hand (start time per job); unplanned jobs wait.
    case byHand
    /// Every job starts as soon as it has arrived and servers are free (the reference operator).
    case asap
    /// Every job starts at the next given full hour after its arrival ("nightly batch at 01:00"), or at its
    /// latest start if that comes first.
    case atHour(Int)
    /// Every job is planned into the hours with the lowest forecast carbon intensity before its deadline,
    /// taking the free servers into account; plans are made again whenever a job arrives.
    case greenest
}

public struct Policy: Equatable, Sendable, Codable {
    public var capacity: CapacityMode
    public var batch: BatchMode

    public init(capacity: CapacityMode, batch: BatchMode = .asap) {
        self.capacity = capacity
        self.batch = batch
    }

    /// What the reference operator does unless a scenario says otherwise: all servers on, batch jobs as soon as
    /// possible (CONCEPT.md 8.2).
    public static let reference = Policy(capacity: .allOn, batch: .asap)

    /// Allowed range of the target utilization, per mille.
    public static let targetRange = 400...900
    /// Ticks the autoscaler looks ahead beyond the boot time: one hour.
    public static let lookahead = GameTime.ticksPerHour
}
