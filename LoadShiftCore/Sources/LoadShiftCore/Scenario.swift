/// A scenario: region, season, length, fleet, load, the controls it unlocks and its goals
/// (docs/CONCEPT.md, sections 8.3 and 9).
public struct FleetEntry: Equatable, Sendable, Codable {
    public let kind: ServerKind
    public let count: Int

    public init(_ kind: ServerKind, _ count: Int) {
        self.kind = kind
        self.count = count
    }
}

/// Controls a scenario offers (one new idea at a time, CONCEPT.md principle 3).
public enum Control: Int, CaseIterable, Sendable, Codable {
    /// Switching servers on and off by hand; takes effect at once.
    case manualCapacity
    /// Rules for the capacity: all on, fixed number, autoscaling with a target utilization.
    case capacityRules
    /// Planning batch jobs by hand: a start time per job.
    case batchByHand
}

/// The special goal of a scenario: the third star.
public enum SpecialGoal: Equatable, Sendable {
    /// Mean utilization of the running servers at least this, per mille.
    case meanUtilization(Int)
    /// Mean carbon intensity of the energy used for batch work at most this, g/kWh.
    case batchIntensity(Int)
    /// CO₂ at least this much below the reference, per mille.
    case reduction(Int)
}

/// Goals of a scenario (CONCEPT.md 8.3).
public struct StarGoals: Equatable, Sendable {
    /// Second star: CO₂ at least this much below the reference operator, per mille.
    public let reduction: Int
    /// Third star.
    public let special: SpecialGoal

    public init(reduction: Int, special: SpecialGoal) {
        self.reduction = reduction
        self.special = special
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
    public let controls: Set<Control>
    /// Policy at the start: how the data center was left by the previous shift.
    public let initialPolicy: Policy
    /// What the reference operator does in this scenario.
    public let referencePolicy: Policy
    public let goals: StarGoals
    public let batchJobs: [BatchJobSpec]

    public init(number: Int, title: String, setting: GridSetting, weather: WeatherMode = .drawn, days: Int,
                seed: UInt64, fleet: [FleetEntry], interactive: InteractiveLoad,
                facility: Facility = Facility(), slaTarget: Int = 990,
                controls: Set<Control> = [.capacityRules], initialPolicy: Policy = .reference,
                referencePolicy: Policy = .reference,
                goals: StarGoals = StarGoals(reduction: 150, special: .reduction(250)),
                batchJobs: [BatchJobSpec] = []) {
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
        self.controls = controls
        self.initialPolicy = initialPolicy
        self.referencePolicy = referencePolicy
        self.goals = goals
        self.batchJobs = batchJobs
    }

    /// First tick: 00:00 on day 0.
    public var startTick: Int { GameTime.start.tick }
    /// The tick after the last one.
    public var endTick: Int { startTick + days * GameTime.ticksPerDay }
    public var serverCount: Int { fleet.reduce(0) { $0 + $1.count } }

    // MARK: - Stars

    /// The goals a result reaches.
    public func goalsMet(_ totals: Totals, reference: Totals) -> (rules: Bool, reduction: Bool, special: Bool) {
        let rules = totals.sla >= slaTarget && totals.deadlinesMissed == 0 && totals.emissions < reference.emissions
        let reduction = rules && totals.emissions * 1_000 <= reference.emissions * (1_000 - goals.reduction)
        let special: Bool
        switch goals.special {
        case .meanUtilization(let minimum):
            special = rules && totals.meanUtilization >= minimum
        case .reduction(let r):
            special = rules && totals.emissions * 1_000 <= reference.emissions * (1_000 - r)
        case .batchIntensity(let maximum):
            special = rules && totals.batchEnergy > 0 && totals.meanBatchIntensityTenths <= maximum * 10
        }
        return (rules, reduction, special)
    }

    /// Stars of a result, 0 … 3: ★ the rules (SLA target, less CO₂ than the reference), ★★ the reduction,
    /// ★★★ the special goal. The second and third star need the first, not each other.
    public func stars(_ totals: Totals, reference: Totals) -> Int {
        let met = goalsMet(totals, reference: reference)
        guard met.rules else { return 0 }
        return 1 + (met.reduction ? 1 : 0) + (met.special ? 1 : 0)
    }

    // MARK: - Scenarios

    /// Scenario 1, "Night shift": a web shop in Germany in autumn. The fleet is sized for the Christmas season;
    /// at night the load falls to a fifth of the evening peak, and idle servers still draw a quarter of their
    /// maximum power. The day starts at midnight with every server on; the player switches servers by hand
    /// (G1, G2).
    public static let nightShift = Scenario(
        number: 1, title: "Night shift", setting: GridSetting(.germany, .autumn), days: 1, seed: 1,
        fleet: [FleetEntry(.cpuCurrent, 20)],
        interactive: InteractiveLoad(peak: 240),
        controls: [.manualCapacity],
        initialPolicy: Policy(capacity: .manual(servers: 20)),
        goals: StarGoals(reduction: 150, special: .meanUtilization(600)))

    /// Scenario 2, "Wait for the sun": the batch cluster of the web shop in Spain in summer. Reports, backups and
    /// imports must be done by their deadlines; when they run is up to the player. In Spain's solar grid the
    /// midday hours are the cleanest – not the night (G3).
    public static let waitForTheSun = Scenario(
        number: 2, title: "Wait for the sun", setting: GridSetting(.spain, .year), days: 2, seed: 2,
        fleet: [FleetEntry(.cpuCurrent, 10)],
        interactive: InteractiveLoad(peak: 0),
        controls: [.batchByHand],
        initialPolicy: Policy(capacity: .manual(servers: 0), batch: .byHand),
        referencePolicy: Policy(capacity: .manual(servers: 0), batch: .asap),
        goals: StarGoals(reduction: 100, special: .batchIntensity(83)),
        batchJobs: [
            BatchJobSpec("Daily report", servers: 4, hours: 3,
                         arrival: GameTime(day: 0, hour: 0), deadline: GameTime(day: 0, hour: 18)),
            BatchJobSpec("Backup", servers: 3, hours: 4,
                         arrival: GameTime(day: 0, hour: 1), deadline: GameTime(day: 1, hour: 6)),
            BatchJobSpec("Data import", servers: 6, hours: 2,
                         arrival: GameTime(day: 0, hour: 7), deadline: GameTime(day: 0, hour: 20)),
            BatchJobSpec("Image conversion", servers: 5, hours: 4,
                         arrival: GameTime(day: 0, hour: 18), deadline: GameTime(day: 1, hour: 18)),
            BatchJobSpec("Daily report", servers: 4, hours: 3,
                         arrival: GameTime(day: 1, hour: 0), deadline: GameTime(day: 1, hour: 18)),
            BatchJobSpec("Backup", servers: 3, hours: 4,
                         arrival: GameTime(day: 1, hour: 1), deadline: GameTime(day: 2, hour: 0)),
            BatchJobSpec("Search index", servers: 4, hours: 3,
                         arrival: GameTime(day: 1, hour: 6), deadline: GameTime(day: 2, hour: 0)),
        ])

    public static let all: [Scenario] = [nightShift, waitForTheSun]
}
