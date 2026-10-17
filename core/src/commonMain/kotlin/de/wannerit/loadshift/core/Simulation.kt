package de.wannerit.loadshift.core

/*
 * The simulation of one data center through a scenario (docs/CONCEPT.md, sections 3, 5–7, 11).
 *
 * Deterministic: the same scenario and the same policy changes at the same ticks always give the same result.
 * Order within a tick: servers that finished booting go on; batch jobs move on (booted → working → done); the
 * capacity policy switches the servers not working for batch jobs; batch jobs due now start; the demand is
 * served; power, energy and emissions are booked.
 */

/** Running totals of a simulation. */
data class Totals(
    var ticks: Int = 0,
    /** Interactive work units (1 unit = 1,000 requests) demanded, served, and served in time (slow ones count half). */
    var demand: Long = 0,
    var served: Long = 0,
    var inTime: Long = 0,
    /** Energy of the servers and of the facility, mWh. */
    var itEnergy: Long = 0,
    var facilityEnergy: Long = 0,
    /** Operational and embodied emissions, µg CO₂e. */
    var operational: Long = 0,
    var embodied: Long = 0,
    /** Sum over all ticks of the servers switched on (booting included). */
    var serverTicks: Long = 0,
    /** Sum over all ticks of the capacity of the running servers, work units. */
    var capacityTicks: Long = 0,
    /** Facility energy (mWh) and operational emissions (µg) of the servers working for batch jobs. */
    var batchEnergy: Long = 0,
    var batchOperational: Long = 0,
    /** Batch jobs finished, and deadlines missed. */
    var jobsDone: Int = 0,
    var deadlinesMissed: Int = 0,
) {
    /** Share of the demand served in time, per mille. */
    val sla: Int get() = if (demand == 0L) 1_000 else (inTime * 1_000 / demand).toInt()

    /** PUE over the scenario, per mille. */
    val pue: Int get() = if (itEnergy == 0L) 0 else (facilityEnergy * 1_000 / itEnergy).toInt()

    /** Operational plus embodied emissions, µg CO₂e. */
    val emissions: Long get() = operational + embodied

    /** SCI per interactive work unit (= 1,000 requests), µg CO₂e (GSF SCI: (O + M) per R [S2]). */
    val sciPerUnit: Long get() = if (served == 0L) 0 else emissions / served

    /** Mean utilization of the running servers' capacity, per mille. */
    val meanUtilization: Int get() = if (capacityTicks == 0L) 0 else (served * 1_000 / capacityTicks).toInt()

    /** Mean number of servers switched on, per mille of a server. */
    val meanServersOn: Int get() = if (ticks == 0) 0 else (serverTicks * 1_000 / ticks).toInt()

    /** Mean carbon intensity of the energy used, tenths of g/kWh. */
    val meanIntensityTenths: Int get() = if (facilityEnergy == 0L) 0 else (operational * 10 / facilityEnergy).toInt()

    /** Mean carbon intensity of the energy used for batch work, tenths of g/kWh. */
    val meanBatchIntensityTenths: Int
        get() = if (batchEnergy == 0L) 0 else (batchOperational * 10 / batchEnergy).toInt()
}

/** What happened in one tick, for the timeline and the debrief. */
data class TickRecord(
    val tick: Int,
    val intensityTenths: Int,
    val demand: Int,
    val capacity: Int,
    val served: Int,
    /** Work served in time (slow requests count half); below [demand] means slow or dropped requests. */
    val inTime: Int,
    val serversOn: Int,
    /** Servers switched on for batch jobs. */
    val batchServers: Int,
    val itPower: Int,
    val facilityPower: Int,
    /** Operational plus embodied emissions from the start up to and including this tick, µg CO₂e. */
    val emissionsSoFar: Long,
)

class Simulation private constructor(
    val scenario: Scenario,
    var policy: Policy,
    val grid: Grid,
    private val serverList: MutableList<Server>,
    private val jobList: MutableList<BatchJob>,
    tick: Int,
    totals: Totals,
    private val historyList: MutableList<TickRecord>,
    plannedAt: Int?,
    private val missedCounted: MutableSet<Int>,
    /** Batch rule and deadline safety of the last tick: a changed rule plans the jobs not started yet anew. */
    private var batchRuleSeen: Pair<BatchMode, Boolean>?,
) {
    /** The next tick to simulate. */
    var tick: Int = tick
        private set
    var totals: Totals = totals
        private set

    /**
     * Set in a projection: from here on the simulation knows only what the operator knows at that tick – the grid
     * forecast made then and the expected demand without noise ([projected]).
     */
    var plannedAt: Int? = plannedAt
        private set

    val servers: List<Server> get() = serverList
    val jobs: List<BatchJob> get() = jobList
    val history: List<TickRecord> get() = historyList

    constructor(scenario: Scenario, policy: Policy = scenario.initialPolicy) : this(
        scenario = scenario,
        policy = policy,
        grid = Grid(scenario.setting, scenario.seed, scenario.weather, scenario.forecastMiss),
        serverList = scenario.fleet.flatMap { e -> List(e.count) { e.kind } }
            .mapIndexed { i, kind -> Server(i, kind, ServerState.On) }.toMutableList(),
        jobList = scenario.batchJobs.mapIndexed { i, spec -> BatchJob(i, spec) }.toMutableList(),
        tick = scenario.startTick,
        totals = Totals(),
        historyList = ArrayList(scenario.endTick - scenario.startTick),
        plannedAt = null,
        missedCounted = mutableSetOf(),
        batchRuleSeen = null,
    ) {
        // The start shows the servers and the batch plans as the policy wants them (the first tick would do the same).
        applyCapacityPolicy()
        planBatchJobs()
    }

    /** An independent copy (the simulation is mutable; projections and "what if" run on copies). */
    fun copy(): Simulation = Simulation(
        scenario, policy, grid, serverList.toMutableList(), jobList.toMutableList(), tick, totals.copy(),
        historyList.toMutableList(), plannedAt, missedCounted.toMutableSet(), batchRuleSeen,
    )

    val time: GameTime get() = GameTime(tick)
    val isFinished: Boolean get() = tick >= scenario.endTick

    /**
     * Applies a changed policy at once, before the next tick: capacity and batch plans. The result is the same as
     * applying it at the start of the next tick; hall and job list show the change while the game is paused.
     */
    fun applyPolicy() {
        if (isFinished) return
        applyCapacityPolicy()
        planBatchJobs()
    }

    /** Runs the scenario to its end. */
    fun run() {
        while (!isFinished) step()
    }

    /** Emissions of this run up to (and including) the given tick, µg CO₂e. */
    fun emissions(through: Int): Long {
        val index = through - scenario.startTick
        if (historyList.isEmpty() || index < 0) return 0
        return historyList[minOf(index, historyList.size - 1)].emissionsSoFar
    }

    /**
     * What the scenario will probably bring with a policy from now on: the rest of the scenario simulated with the
     * forecast made now and the expected demand. The totals include what has already happened, so two projections
     * can be compared directly.
     */
    fun projected(with: Policy): Totals {
        val copy = copy()
        copy.policy = with
        copy.plannedAt = tick
        copy.historyList.clear()
        copy.run()
        return copy.totals
    }

    // Planning batch jobs by hand

    /** Plans a job to start at a tick (not before its arrival or now). Started jobs cannot be moved. */
    fun plan(job: Int, start: Int) {
        if (job !in jobList.indices || jobList[job].state.isStarted) return
        jobList[job] = jobList[job].copy(state = BatchJobState.Planned(maxOf(start, jobList[job].spec.arrival, tick)))
    }

    fun unplan(job: Int) {
        if (job !in jobList.indices || jobList[job].state.isStarted) return
        jobList[job] = jobList[job].copy(state = BatchJobState.Waiting)
    }

    /** Servers that can run batch jobs. */
    val batchServerCount: Int get() = serverList.count { it.kind.servesBatch }

    /** Capacity of the servers that take interactive work now. */
    val readyCapacity: Int get() = serverList.sumOf { if (it.state.isOn && it.job == null) it.kind.capacity else 0 }

    // One tick

    fun step() {
        check(!isFinished) { "the scenario has ended" }
        finishBooting()
        updateJobs()
        applyCapacityPolicy()
        startBatchJobs()

        val demand = if (plannedAt == null) scenario.interactive.actual(tick, scenario.seed)
        else scenario.interactive.expected(tick)
        val capacity = readyCapacity
        val served = minOf(demand, capacity)
        val fast = capacity * InteractiveLoad.SLOW_ABOVE / 1_000
        val inTime = minOf(served, fast) + maxOf(0, served - fast) / 2
        val utilization = if (capacity == 0) 0 else served * 1_000 / capacity

        var itPower = 0
        var batchPower = 0
        var batchServers = 0
        var embodied = 0L
        var poweredOn = 0
        for (i in serverList.indices) {
            val server = serverList[i]
            val job = server.job
            val newUtilization = when {
                job != null -> if (jobList[job].state is BatchJobState.Running && server.state.isOn) 1_000 else 0
                server.state.isOn && server.kind.servesInteractive -> utilization
                else -> 0
            }
            val updated = server.copy(utilization = newUtilization)
            serverList[i] = updated
            val power = updated.power()
            if (job != null) {
                batchPower += power
                if (updated.state.isPowered) batchServers += 1
            }
            itPower += power
            embodied += updated.kind.embodiedPerTick
            if (updated.state.isPowered) poweredOn += 1
        }
        val facilityPower = scenario.facility.facilityPower(itPower)
        val intensity = plannedAt?.let { grid.forecastTenths(tick, it) } ?: grid.intensityTenths(tick)
        val facilityEnergy = facilityPower * Units.MILLIWATT_HOURS_PER_WATT_TICK

        totals.ticks += 1
        totals.demand += demand
        totals.served += served
        totals.inTime += inTime
        totals.itEnergy += itPower * Units.MILLIWATT_HOURS_PER_WATT_TICK
        totals.facilityEnergy += facilityEnergy
        totals.operational += Units.emissions(facilityEnergy, intensity)
        totals.embodied += embodied
        totals.serverTicks += poweredOn
        totals.capacityTicks += capacity
        val batchEnergy = scenario.facility.facilityPower(batchPower) * Units.MILLIWATT_HOURS_PER_WATT_TICK
        totals.batchEnergy += batchEnergy
        totals.batchOperational += Units.emissions(batchEnergy, intensity)

        historyList.add(
            TickRecord(
                tick = tick, intensityTenths = intensity, demand = demand, capacity = capacity, served = served,
                inTime = inTime, serversOn = poweredOn, batchServers = batchServers, itPower = itPower,
                facilityPower = facilityPower, emissionsSoFar = totals.emissions,
            ),
        )
        tick += 1
        if (isFinished) updateJobs()
    }

    private fun finishBooting() {
        for (i in serverList.indices) {
            val state = serverList[i].state
            if (state is ServerState.Booting && state.readyAt <= tick) {
                serverList[i] = serverList[i].copy(state = ServerState.On)
            }
        }
    }

    // Capacity

    /** Indices of the servers for interactive work (not working for a batch job), most efficient first. */
    private val interactiveOrder: List<Int>
        get() = serverList.indices
            .filter { serverList[it].kind.servesInteractive && serverList[it].job == null }
            .sortedWith(compareByDescending<Int> { serverList[it].kind.efficiency }.thenBy { it })

    private fun applyCapacityPolicy() {
        val order = interactiveOrder
        when (val mode = policy.capacity) {
            CapacityMode.AllOn -> order.forEach { switchOn(it) }
            is CapacityMode.Manual -> {
                val wanted = order.take(maxOf(0, mode.servers)).toSet()
                for (i in order) {
                    if (i in wanted) switchOn(i) else switchOff(i)
                }
            }
            is CapacityMode.Autoscale -> {
                val utilization = mode.targetUtilization.coerceIn(Policy.TARGET_RANGE)
                // The expected demand of the coming hour; the last tick's demand too, if it was higher (the
                // autoscaler also reacts to what it measures – a campaign is not in the expected demand).
                val recent = historyList.lastOrNull()?.demand ?: 0
                val expected = maxOf(recent, (tick..tick + ServerKind.BOOT_TICKS + Policy.LOOKAHEAD)
                    .maxOf { scenario.interactive.expected(it) })
                val needed = maxOf(1, ceilDiv(expected * 1_000, utilization))
                val minimum = mode.minimum?.takeIf { it.activeAt(tick) }?.servers ?: 0
                // The most efficient servers that together reach the needed capacity (at least one).
                val wanted = mutableSetOf<Int>()
                var sum = 0
                for (i in order) {
                    if (sum >= needed && wanted.size >= minimum) break
                    wanted += i
                    sum += serverList[i].kind.capacity
                }
                for (i in order) if (i in wanted) switchOn(i)
                // Surplus servers go off, but only as long as the wanted ones that are ready cover the need:
                // a server keeps running until its replacement has finished booting.
                var ready = order.filter { it in wanted && serverList[it].state.isOn }.sumOf { serverList[it].kind.capacity }
                val neededNow = ceilDiv(maxOf(recent, scenario.interactive.expected(tick)) * 1_000, utilization)
                for (i in order.reversed()) {
                    if (i in wanted || !serverList[i].state.isPowered) continue
                    if (ready >= neededNow) {
                        switchOff(i)
                    } else if (serverList[i].state.isOn) {
                        ready += serverList[i].kind.capacity
                    }
                }
            }
        }
    }

    private fun switchOn(i: Int) {
        if (serverList[i].state == ServerState.Off) {
            serverList[i] = serverList[i].copy(state = ServerState.Booting(tick + ServerKind.BOOT_TICKS))
        }
    }

    private fun switchOff(i: Int) {
        serverList[i] = serverList[i].copy(state = ServerState.Off)
    }

    // Batch jobs

    /**
     * Jobs whose servers have booted begin to work; finished jobs release their servers; deadlines that pass
     * unfinished are counted (once per job).
     */
    private fun updateJobs() {
        for (i in jobList.indices) {
            val job = jobList[i]
            when (val state = job.state) {
                is BatchJobState.Started -> if (state.workFrom <= tick) {
                    jobList[i] = job.copy(state = BatchJobState.Running(state.at, tick + job.spec.duration))
                }
                is BatchJobState.Running -> if (state.endsAt <= tick) {
                    jobList[i] = job.copy(state = BatchJobState.Done(state.startedAt, state.endsAt))
                    totals.jobsDone += 1
                    if (state.endsAt > job.spec.deadline && missedCounted.add(i)) totals.deadlinesMissed += 1
                    for (s in serverList.indices) {
                        if (serverList[s].job == i) serverList[s] = serverList[s].copy(job = null, state = ServerState.Off)
                    }
                }
                else -> {}
            }
            // Unfinished when the deadline passes: counted once.
            if (!jobList[i].state.isDone && tick >= job.spec.deadline && missedCounted.add(i)) {
                totals.deadlinesMissed += 1
            }
        }
    }

    /** Starts the jobs the batch policy wants to start now, if enough servers are free. */
    private fun startBatchJobs() {
        if (jobList.isEmpty()) return
        planBatchJobs()
        startPlannedJobs()
    }

    /** Plans the jobs by the batch rule (at this tick); a changed rule plans the jobs not started yet anew. */
    private fun planBatchJobs() {
        if (jobList.isEmpty()) return
        // A changed rule: jobs not started yet are planned again by the new rule (plans by hand stay).
        val rule = policy.batch to policy.deadlineSafety
        val first = batchRuleSeen == null
        var ruleChanged = false
        if (rule != batchRuleSeen) {
            if (batchRuleSeen != null && policy.batch.isRule) {
                ruleChanged = true
                for (i in jobList.indices) {
                    if (jobList[i].state is BatchJobState.Planned) jobList[i] = jobList[i].copy(state = BatchJobState.Waiting)
                }
            }
            batchRuleSeen = rule
        }
        when (val mode = policy.batch) {
            BatchMode.ByHand -> {}
            BatchMode.Asap -> for (i in jobList.indices) {
                if (jobList[i].state == BatchJobState.Waiting && jobList[i].spec.arrival <= tick) {
                    jobList[i] = jobList[i].copy(state = BatchJobState.Planned(tick))
                }
            }
            is BatchMode.AtHour -> for (i in jobList.indices) {
                val job = jobList[i]
                if (job.state == BatchJobState.Waiting && job.spec.arrival <= tick) {
                    var start = job.spec.arrival
                    while (GameTime(start).hour != mode.hour || GameTime(start).minute != 0) start += 1
                    val safe = if (policy.deadlineSafety) minOf(start, maxOf(job.spec.latestStart, tick)) else start
                    jobList[i] = job.copy(state = BatchJobState.Planned(safe))
                }
            }
            // Planned once, when the rule is applied; again only on request ([replan]): a plan made with an old
            // forecast stays until somebody looks at the new one.
            BatchMode.Greenest -> if (first || ruleChanged) replanGreenest()
            is BatchMode.Threshold -> {
                val now = plannedAt?.let { grid.forecastTenths(tick, it) } ?: grid.intensityTenths(tick)
                for (i in jobList.indices) {
                    val job = jobList[i]
                    if (job.state != BatchJobState.Waiting || job.spec.arrival > tick) continue
                    val clean = now <= mode.gramsPerKWh * 10
                    val due = policy.deadlineSafety && tick >= job.spec.latestStart
                    if (clean || due) jobList[i] = job.copy(state = BatchJobState.Planned(tick))
                }
            }
        }
    }

    /** Starts the planned jobs whose time has come, if enough servers are free. */
    private fun startPlannedJobs() {
        for (i in jobList.indices) {
            val job = jobList[i]
            val planned = job.state as? BatchJobState.Planned ?: continue
            if (planned.start > tick || job.spec.arrival > tick) continue
            val free = serverList.indices.filter { serverList[it].kind.servesBatch && serverList[it].job == null }
            if (free.size < job.spec.servers) continue
            // Prefer servers that are off, so interactive work keeps its running servers.
            val chosen = free.sortedWith(compareBy<Int> { if (serverList[it].state.isPowered) 1 else 0 }.thenBy { it })
                .take(job.spec.servers)
            var ready = tick
            for (s in chosen) {
                when (val state = serverList[s].state) {
                    ServerState.Off -> {
                        serverList[s] = serverList[s].copy(job = i, state = ServerState.Booting(tick + ServerKind.BOOT_TICKS))
                        ready = maxOf(ready, tick + ServerKind.BOOT_TICKS)
                    }
                    is ServerState.Booting -> {
                        serverList[s] = serverList[s].copy(job = i)
                        ready = maxOf(ready, state.readyAt)
                    }
                    ServerState.On -> serverList[s] = serverList[s].copy(job = i)
                }
            }
            jobList[i] = job.copy(
                state = if (ready <= tick) BatchJobState.Running(tick, tick + job.spec.duration)
                else BatchJobState.Started(tick, ready),
            )
        }
    }

    /** Tick of the last plan by the greenest-hours rule (its forecast was made then). */
    var plannedWithForecastOf: Int? = null
        private set

    /** Plans the jobs not started yet again with the latest forecast (greenest-hours rule only). */
    fun replan() {
        if (policy.batch == BatchMode.Greenest && !isFinished) replanGreenest()
    }

    /** Plans all jobs not started yet into the greenest hours (forecast made now), around the running ones. */
    private fun replanGreenest() {
        plannedWithForecastOf = tick
        val busy = mutableMapOf<Int, Int>()
        for (job in jobList) {
            val until = when (val state = job.state) {
                is BatchJobState.Started -> state.workFrom + job.spec.duration
                is BatchJobState.Running -> state.endsAt
                else -> continue
            }
            for (t in tick until until) busy[t] = (busy[t] ?: 0) + job.spec.servers
        }
        val open = jobList.filter { !it.state.isStarted }.map { it.id to it.spec }
        val starts = BatchPlanner.plan(open, batchServerCount, busy, grid, tick)
        for ((id, start) in starts) jobList[id] = jobList[id].copy(state = BatchJobState.Planned(start))
    }

    companion object {
        /** Runs the scenario with the reference operator's policy (CONCEPT.md 8.2). */
        fun reference(scenario: Scenario): Simulation = replay(scenario, scenario.referencePolicy)

        /**
         * Runs the whole scenario with one policy from start to end – for "what if" in the debrief; [replanAt]: ticks
         * at which the plan is made again with the latest forecast; [changes]: policies applied at given ticks.
         */
        fun replay(scenario: Scenario, policy: Policy, replanAt: Set<Int> = emptySet(), changes: Map<Int, Policy> = emptyMap()): Simulation =
            Simulation(scenario, policy).also {
                while (!it.isFinished) {
                    changes[it.tick]?.let { changed -> it.policy = changed; it.applyPolicy() }
                    if (it.tick in replanAt) it.replan()
                    it.step()
                }
            }
    }
}
