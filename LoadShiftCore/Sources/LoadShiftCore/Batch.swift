/// Batch work (docs/CONCEPT.md 5.2): jobs with a size, an arrival time and a deadline. A job needs a number of
/// servers for a number of ticks; once started it runs to the end. Its servers are switched on when it starts
/// and boot first, so a job takes boot time plus its duration.

public struct BatchJobSpec: Equatable, Sendable {
    public let name: String
    /// Servers the job needs at the same time.
    public let servers: Int
    /// Work time in ticks (after booting).
    public let duration: Int
    /// Tick from which the job can start.
    public let arrival: Int
    /// The job must be finished at this tick at the latest.
    public let deadline: Int

    public init(_ name: String, servers: Int, hours: Int, arrival: GameTime, deadline: GameTime) {
        self.name = name
        self.servers = servers
        self.duration = hours * GameTime.ticksPerHour
        self.arrival = arrival.tick
        self.deadline = deadline.tick
    }

    /// The last tick at which the job can be started and still meet its deadline (boot time included).
    public var latestStart: Int { deadline - duration - ServerKind.bootTicks }
    /// Ticks from start to finish.
    public var span: Int { ServerKind.bootTicks + duration }
}

public enum BatchJobState: Equatable, Sendable {
    /// Not planned yet.
    case waiting
    /// Planned to start at the tick (waits there if no servers are free).
    case planned(start: Int)
    /// Started at the tick; its servers boot, the work begins at `workFrom`.
    case started(at: Int, workFrom: Int)
    /// Working; finished at `endsAt`.
    case running(startedAt: Int, endsAt: Int)
    /// Finished at the tick.
    case done(startedAt: Int, at: Int)

    public var isStarted: Bool {
        switch self {
        case .started, .running, .done: return true
        default: return false
        }
    }

    public var isDone: Bool { if case .done = self { return true } else { return false } }
}

public struct BatchJob: Equatable, Sendable, Identifiable {
    public let id: Int
    public let spec: BatchJobSpec
    public internal(set) var state: BatchJobState = .waiting

    /// Tick at which the job started (switched on its servers), if it has.
    public var startedAt: Int? {
        switch state {
        case .started(let at, _): return at
        case .running(let at, _): return at
        case .done(let at, _): return at
        default: return nil
        }
    }

    /// Tick at which the job finished, if it has.
    public var finishedAt: Int? { if case .done(_, let at) = state { return at } else { return nil } }

    /// Planned start, if planned and not started yet.
    public var plannedStart: Int? { if case .planned(let s) = state { return s } else { return nil } }

    /// The job finished after its deadline, or has not finished and the deadline has passed.
    public func missedDeadline(at tick: Int) -> Bool {
        if let finished = finishedAt { return finished > spec.deadline }
        return tick >= spec.deadline
    }
}

/// Planning help: start ticks with the lowest forecast carbon intensity (CONCEPT.md 5.2, "greenest hours").
public enum BatchPlanner {
    /// Mean forecast intensity (tenths of g/kWh) over the work time of a job started at `start`.
    public static func meanForecast(_ spec: BatchJobSpec, start: Int, grid: Grid, madeAt now: Int) -> Int {
        let from = start + ServerKind.bootTicks
        let sum = (from..<(from + spec.duration)).reduce(0) { $0 + grid.forecastTenths(tick: $1, madeAt: now) }
        return sum / max(1, spec.duration)
    }

    /// Plans jobs one after another into the start with the lowest mean forecast intensity before their latest
    /// start, so that at no tick more servers are used than `servers`. `busy` gives servers already taken per
    /// tick (running jobs). Jobs that fit nowhere get their latest start.
    public static func plan(_ specs: [(id: Int, spec: BatchJobSpec)], servers: Int, busy: [Int: Int], grid: Grid,
                            madeAt now: Int) -> [Int: Int] {
        var used = busy
        var result: [Int: Int] = [:]
        // Tightest deadlines first.
        for (id, spec) in specs.sorted(by: { $0.spec.latestStart < $1.spec.latestStart }) {
            let earliest = max(now, spec.arrival)
            let latest = max(earliest, spec.latestStart)
            var best: (start: Int, mean: Int)?
            for start in earliest...latest {
                let fits = (start..<(start + spec.span)).allSatisfy { (used[$0] ?? 0) + spec.servers <= servers }
                guard fits else { continue }
                let mean = meanForecast(spec, start: start, grid: grid, madeAt: now)
                if best == nil || mean < best!.mean { best = (start, mean) }
            }
            let start = best?.start ?? latest
            result[id] = start
            for t in start..<(start + spec.span) { used[t, default: 0] += spec.servers }
        }
        return result
    }
}
