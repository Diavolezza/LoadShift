/// The simulation of one data center through a scenario (docs/CONCEPT.md, sections 3, 5–7, 11).
///
/// Deterministic: the same scenario and the same policy changes at the same ticks always give the same result.
/// Order within a tick: servers that finished booting go on; the policy switches servers; the demand is served;
/// power, energy and emissions are booked.

/// Running totals of a simulation.
public struct Totals: Equatable, Sendable {
    public internal(set) var ticks = 0
    /// Interactive work units (1 unit = 1,000 requests) demanded, served, and served in time (slow requests
    /// count half).
    public internal(set) var demand = 0
    public internal(set) var served = 0
    public internal(set) var inTime = 0
    /// Energy of the servers and of the facility, mWh.
    public internal(set) var itEnergy = 0
    public internal(set) var facilityEnergy = 0
    /// Operational and embodied emissions, µg CO₂e.
    public internal(set) var operational = 0
    public internal(set) var embodied = 0
    /// Sum over all ticks of the servers switched on (booting included).
    public internal(set) var serverTicks = 0

    /// Share of the demand served in time, per mille.
    public var sla: Int { demand == 0 ? 1_000 : inTime * 1_000 / demand }
    /// PUE over the scenario, per mille.
    public var pue: Int { itEnergy == 0 ? 0 : facilityEnergy * 1_000 / itEnergy }
    /// Operational plus embodied emissions, µg CO₂e.
    public var emissions: Int { operational + embodied }
    /// SCI per interactive work unit (= 1,000 requests), µg CO₂e (GSF SCI: (O + M) per R [S2]).
    public var sciPerUnit: Int { served == 0 ? 0 : emissions / served }
    /// Mean number of servers switched on, per mille of a server.
    public var meanServersOn: Int { ticks == 0 ? 0 : serverTicks * 1_000 / ticks }
    /// Mean carbon intensity of the energy used, tenths of g/kWh.
    public var meanIntensityTenths: Int { facilityEnergy == 0 ? 0 : operational * 10 / facilityEnergy }
}

/// What happened in one tick, for the timeline and the debrief.
public struct TickRecord: Equatable, Sendable {
    public let tick: Int
    public let intensityTenths: Int
    public let demand: Int
    public let capacity: Int
    public let served: Int
    public let serversOn: Int
    public let itPower: Int
    public let facilityPower: Int
}

public struct Simulation: Sendable {
    public let scenario: Scenario
    public let grid: Grid
    public var policy: Policy
    public private(set) var servers: [Server]
    /// The next tick to simulate.
    public private(set) var tick: Int
    public private(set) var totals = Totals()
    public private(set) var history: [TickRecord] = []
    /// Set in a projection: from here on the simulation knows only what the operator knows at that tick –
    /// the grid forecast made then and the expected demand without noise (`projected(with:)`).
    public private(set) var plannedAt: Int?

    public init(scenario: Scenario, policy: Policy) {
        self.scenario = scenario
        self.policy = policy
        self.grid = Grid(setting: scenario.setting, seed: scenario.seed, weather: scenario.weather)
        var servers: [Server] = []
        for entry in scenario.fleet {
            for _ in 0..<entry.count {
                servers.append(Server(id: servers.count, kind: entry.kind, state: .on))
            }
        }
        self.servers = servers
        self.tick = scenario.startTick
        history.reserveCapacity(scenario.endTick - scenario.startTick)
    }

    public var time: GameTime { GameTime(tick: tick) }
    public var isFinished: Bool { tick >= scenario.endTick }

    /// Runs the scenario to its end.
    public mutating func run() {
        while !isFinished { step() }
    }

    /// Runs the scenario with the reference operator's policy (CONCEPT.md 8.2).
    public static func reference(for scenario: Scenario) -> Simulation {
        var simulation = Simulation(scenario: scenario, policy: .reference)
        simulation.run()
        return simulation
    }

    /// What the scenario will probably bring with a policy from now on: the rest of the scenario simulated
    /// with the forecast made now and the expected demand (CONCEPT.md, principle 4). The totals include what
    /// has already happened, so two projections can be compared directly.
    public func projected(with policy: Policy) -> Totals {
        var copy = self
        copy.policy = policy
        copy.plannedAt = tick
        copy.history = []
        copy.run()
        return copy.totals
    }

    // MARK: - One tick

    public mutating func step() {
        precondition(!isFinished, "the scenario has ended")
        finishBooting()
        applyCapacityPolicy()

        let demand = plannedAt == nil ? scenario.interactive.actual(tick: tick, seed: scenario.seed)
                                      : scenario.interactive.expected(tick: tick)
        let capacity = readyCapacity
        let served = min(demand, capacity)
        let fast = capacity * InteractiveLoad.slowAbove / 1_000
        let inTime = min(served, fast) + max(0, served - fast) / 2
        let utilization = capacity == 0 ? 0 : served * 1_000 / capacity

        var itPower = 0
        var embodied = 0
        var poweredOn = 0
        for i in servers.indices {
            if servers[i].state.isOn, servers[i].kind.servesInteractive {
                servers[i].utilization = utilization
            } else {
                servers[i].utilization = 0
            }
            itPower += servers[i].power(utilization: servers[i].utilization)
            embodied += servers[i].kind.embodiedPerTick
            if servers[i].state.isPowered { poweredOn += 1 }
        }
        let facilityPower = scenario.facility.facilityPower(itPower: itPower)
        let intensity = plannedAt.map { grid.forecastTenths(tick: tick, madeAt: $0) }
            ?? grid.intensityTenths(tick: tick)
        let facilityEnergy = facilityPower * Units.milliwattHoursPerWattTick

        totals.ticks += 1
        totals.demand += demand
        totals.served += served
        totals.inTime += inTime
        totals.itEnergy += itPower * Units.milliwattHoursPerWattTick
        totals.facilityEnergy += facilityEnergy
        totals.operational += Units.emissions(milliwattHours: facilityEnergy, intensityTenths: intensity)
        totals.embodied += embodied
        totals.serverTicks += poweredOn

        history.append(TickRecord(tick: tick, intensityTenths: intensity, demand: demand, capacity: capacity,
                                  served: served, serversOn: poweredOn, itPower: itPower,
                                  facilityPower: facilityPower))
        tick += 1
    }

    /// Capacity of the servers that take interactive work now.
    public var readyCapacity: Int {
        servers.reduce(0) { $0 + ($1.state.isOn ? $1.kind.capacity : 0) }
    }

    private mutating func finishBooting() {
        for i in servers.indices {
            if case .booting(let readyAt) = servers[i].state, readyAt <= tick {
                servers[i].state = .on
            }
        }
    }

    /// Indices of the servers for interactive work, most efficient first.
    private var interactiveOrder: [Int] {
        servers.indices
            .filter { servers[$0].kind.servesInteractive }
            .sorted { a, b in
                let ea = servers[a].kind.efficiency, eb = servers[b].kind.efficiency
                return ea != eb ? ea > eb : a < b
            }
    }

    private mutating func applyCapacityPolicy() {
        let order = interactiveOrder
        switch policy.capacity {
        case .allOn:
            for i in order { switchOn(i) }
        case .manual(let count):
            let wanted = Set(order.prefix(max(0, count)))
            for i in order {
                if wanted.contains(i) { switchOn(i) } else { servers[i].state = .off }
            }
        case .autoscale(let target):
            let utilization = min(max(target, Policy.targetRange.lowerBound), Policy.targetRange.upperBound)
            let window = tick...(tick + ServerKind.bootTicks + Policy.lookahead)
            let expected = window.map { scenario.interactive.expected(tick: $0) }.max() ?? 0
            let needed = max(1, ceilDiv(expected * 1_000, utilization))
            // The most efficient servers that together reach the needed capacity (at least one).
            var wanted = Set<Int>()
            var sum = 0
            for i in order where sum < needed {
                wanted.insert(i)
                sum += servers[i].kind.capacity
            }
            for i in order where wanted.contains(i) { switchOn(i) }
            // Surplus servers go off, but only as long as the wanted ones that are ready cover the need:
            // a server keeps running until its replacement has finished booting.
            var ready = order.filter { wanted.contains($0) && servers[$0].state.isOn }
                .reduce(0) { $0 + servers[$1].kind.capacity }
            let neededNow = ceilDiv(scenario.interactive.expected(tick: tick) * 1_000, utilization)
            for i in order.reversed() where !wanted.contains(i) && servers[i].state.isPowered {
                if ready >= neededNow {
                    servers[i].state = .off
                } else if servers[i].state.isOn {
                    ready += servers[i].kind.capacity
                }
            }
        }
    }

    private mutating func switchOn(_ i: Int) {
        if servers[i].state == .off {
            servers[i].state = .booting(readyAt: tick + ServerKind.bootTicks)
        }
    }
}
