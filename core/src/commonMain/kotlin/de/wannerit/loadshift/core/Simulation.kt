package de.wannerit.loadshift.core

import kotlin.math.roundToInt
import kotlin.math.roundToLong

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
    /** Requests served, in units of 1,000 – the functional unit; with less work per request more than [served]. */
    var requestsServed: Long = 0,
    /** Requests asked for, units of 1,000; answers older than allowed (E3), single answers. */
    var requests: Long = 0,
    var stale: Long = 0,
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
    /** O7: embodied emissions booked at once for servers retired before the end of their life, µg CO₂e (in [embodied]). */
    var retiredEmbodied: Long = 0,
    /** O7: old servers that failed after the end of their life. */
    var failedServers: Int = 0,
    /** O7: the lowest SLA of the sites added up, per mille (the SLA counts per site). */
    var worstSiteSla: Int = 1_000,
    /** O8: training work done, thousandths of a server-hour; the tick it was finished; resumes after a pause. */
    var trainingDone: Long = 0,
    var trainingFinishedAt: Int? = null,
    var trainingRestarts: Int = 0,
    /** O9: the viewers' rating, summed over the streams (units of 1,000 streams × per mille), and the streams. */
    var ratingSum: Long = 0,
    var ratedStreams: Long = 0,
    /** O10: rules broken by the choice of sites; energy of data transfers (mWh, in [facilityEnergy]). */
    var rulesBroken: Int = 0,
    var transferEnergy: Long = 0,
) {
    /** O9: the viewers' mean rating, per mille. */
    val meanRating: Int get() = if (ratedStreams == 0L) 1_000 else (ratingSum / ratedStreams).toInt()

    /** Share of the demand served in time, per mille. */
    val sla: Int get() = if (demand == 0L) 1_000 else (inTime * 1_000 / demand).toInt()

    /** Stale answers per mille of the requests (E3). */
    val stalePermille: Int get() = if (requests == 0L) 0 else (stale / requests).toInt()

    /** Stale answers per 10,000 requests (E3). */
    val stalePerTenThousand: Int get() = if (requests == 0L) 0 else (stale * 10 / requests).toInt()

    /** PUE over the scenario, per mille. */
    val pue: Int get() = if (itEnergy == 0L) 0 else (facilityEnergy * 1_000 / itEnergy).toInt()

    /** Operational plus embodied emissions, µg CO₂e. */
    val emissions: Long get() = operational + embodied

    /** SCI per interactive work unit (= 1,000 requests), µg CO₂e (GSF SCI: (O + M) per R [S2]). */
    val sciPerUnit: Long get() = if (requestsServed == 0L) 0 else emissions / requestsServed

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
    /** Scenario 5: the CPU work the VMs of each host asked for, thousandths of a work unit. */
    val hostDemandMilli: IntArray? = null,
    /** Scenario 6: the setpoint, °C. */
    val setpoint: Int = 20,
    /** E3: what the cache held. */
    val cache: CacheRecord? = null,
    /** O7: embodied emissions so far (µg) and the capacity of all servers installed, work units. */
    val embodiedSoFar: Long = 0,
    val fleetCapacity: Int = 0,
    /** O8: training work done so far, thousandths of a server-hour. */
    val trainingDone: Long = 0,
    /** O9: eco mode was on. */
    val eco: Boolean = false,
    /** O8, O11: the GPUs trained (or loaded the checkpoint) in this tick. */
    val training: Boolean = false,
)

/** E3: memory used (tenths of GB), memory of the running cache servers (GB), hits (per mille of the cacheable requests), memory per kind (tenths of GB). */
data class CacheRecord(
    val usedTenthsGb: Int, val capacityGb: Int, val hits: Int, val kindTenthsGb: List<Int> = emptyList(),
    /** Stale answers in this tick, tenths of ‰ of the requests. */
    val staleTenthsPermille: Int = 0,
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

    /** Expected work at a tick with the code as it is now (engineering track: less work per request). */
    fun expectedDemand(tick: Int): Int = scenario.interactive.expected(tick) * workPermille(tick) / 1_000 +
        extraWork(tick) + scenario.vms.sumOf { it.expectedMilli(tick) } / 1_000

    /** Work per request at a tick, per mille of the original code (engineering track). */
    fun workPermille(tick: Int): Int {
        val growth = scenario.growth
        val caching = scenario.caching
        return when {
            caching != null -> caching.work(policy.cache, scenario.interactive.expected(tick), tick)
            scenario.streaming != null -> scenario.streaming.work(policy.streaming, grid.intensity(tick))
            growth != null -> growth.perRequest((tick - scenario.startTick) / GameTime.TICKS_PER_DAY, scenario.backlog, policy.done)
            scenario.backlog.isNotEmpty() -> Engineering.work(scenario.backlog, policy.done)
            else -> 1_000
        }
    }

    /** Work that does not come from requests: the nightly jobs of E2, work units per tick. */
    fun extraWork(tick: Int): Int =
        scenario.growth?.night(tick, scenario.backlog, policy.done) ?: scenario.caching?.extraWork(policy.cache, tick) ?: 0

    /** Scenario 5: the CPU work of one host's VMs tick by tick so far, thousandths of a work unit. */
    fun hostHistory(host: Int): List<Int> = historyList.map { it.hostDemandMilli?.getOrNull(host) ?: 0 }

    constructor(scenario: Scenario, policy: Policy = scenario.initialPolicy) : this(
        scenario = scenario,
        policy = policy,
        grid = Grid(scenario.setting, scenario.seed, scenario.weather, scenario.forecastMiss, scenario.renewal?.seasons(scenario.days)),
        serverList = scenario.fleet.flatMap { e -> List(e.count) { e } }
            .mapIndexed { i, e -> Server(i, e.kind, ServerState.On, ageDays = e.ageYears * 365, installedAt = scenario.startTick) }.toMutableList(),
        jobList = scenario.batchJobs.mapIndexed { i, spec -> BatchJob(i, spec) }.toMutableList(),
        tick = scenario.startTick,
        totals = Totals(),
        historyList = ArrayList(scenario.endTick - scenario.startTick),
        plannedAt = null,
        missedCounted = mutableSetOf(),
        batchRuleSeen = null,
    ) {
        totals.rulesBroken = scenario.rulesBroken
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
        applyRenewal()
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
        copy.trainedLastTick = trainedLastTick
        copy.historyList.clear()
        copy.run()
        return copy.totals
    }

    /** The rest of the scenario with a policy, by the forecast made now: the ticks it would bring (O8: the plan). */
    fun projectedHistory(with: Policy): List<TickRecord> {
        val copy = copy()
        copy.policy = with
        copy.plannedAt = tick
        copy.trainedLastTick = trainedLastTick
        copy.historyList.clear()
        copy.run()
        return copy.historyList
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

    /** Servers batch jobs may use at a time (O11: the rest belong to the shop). */
    val batchCapacity: Int get() = scenario.batchLimit?.let { minOf(it, batchServerCount) } ?: batchServerCount

    /** Capacity of the servers that take interactive work now. */
    val readyCapacity: Int get() = serverList.sumOf { if (it.state.isOn && it.job == null) it.kind.capacity else 0 }

    // One tick

    fun step() {
        check(!isFinished) { "the scenario has ended" }
        startOfQuarter()
        finishBooting()
        updateJobs()
        applyCapacityPolicy()
        val training = trainNow()
        startBatchJobs()

        val requests = if (plannedAt == null) scenario.interactive.actual(tick, scenario.seed) else scenario.interactive.expected(tick)
        var demand = requests * workPermille(tick) / 1_000 + extraWork(tick)
        var capacity = readyCapacity
        var served = minOf(demand, capacity)
        val fast = capacity * InteractiveLoad.SLOW_ABOVE / 1_000
        var inTime = minOf(served, fast) + maxOf(0, served - fast) / 2
        val utilization = if (capacity == 0) 0 else served * 1_000 / capacity
        // VMs on hosts (scenario 5): every host serves only its own VMs.
        val hostMilli = (policy.capacity as? CapacityMode.Placed)?.let { placed ->
            val milli = IntArray(serverList.size)
            for ((vm, kind) in scenario.vms.withIndex()) {
                val host = placed.hosts.getOrNull(vm) ?: continue
                val e = if (plannedAt == null) SeededRandom.symmetric(scenario.seed, 20L + vm, tick, 50) else 0
                milli[host] += kind.expectedMilli(tick) * (1_000 + e) / 1_000
            }
            milli
        }
        val hostUtilization = hostMilli?.let { milli ->
            demand = 0; capacity = 0; served = 0; inTime = 0
            IntArray(serverList.size) { h ->
                val d = (milli[h] + 500) / 1_000
                val c = if (serverList[h].state.isOn) serverList[h].kind.capacity else 0
                val s = minOf(d, c)
                val f = c * InteractiveLoad.SLOW_ABOVE / 1_000
                demand += d; capacity += c; served += s
                inTime += minOf(s, f) + maxOf(0, s - f) / 2
                if (c == 0) 0 else minOf(1_000, milli[h] / c)
            }
        }

        var itPower = 0
        var batchPower = 0
        var batchServers = 0
        var embodied = 0L
        var poweredOn = 0
        for (i in serverList.indices) {
            val server = serverList[i]
            val job = server.job
            val newUtilization = when {
                training != null && server.kind == ServerKind.GPU_CURRENT -> if (server.state.isOn) training else 0
                job != null -> if (jobList[job].state is BatchJobState.Running && server.state.isOn) 1_000 else 0
                hostUtilization != null -> if (server.state.isOn) hostUtilization[i] else 0
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
            // Embodied emissions are booked over the server's life (six years), not beyond it.
            if (updated.ageAt(tick, scale) < Units.LIFESPAN_DAYS) embodied += updated.kind.embodiedPerTick * scale
            if (updated.state.isPowered) poweredOn += 1
        }
        // E3: the cache servers run all the time, and they were built too.
        var cacheRecord: CacheRecord? = null
        scenario.caching?.let { caching ->
            val state = caching.state(policy.cache, scenario.interactive.expected(tick))
            cacheRecord = CacheRecord((state.usedGb * 10).roundToInt(), state.capacityGb, caching.overallHits(state),
                state.memoryGb.map { (it * 10).roundToInt() },
                (caching.staleShare(policy.cache, scenario.interactive.expected(tick), tick) * 10_000).roundToInt())
            val cacheServers = caching.servers(policy.cache)
            itPower += caching.power(policy.cache)
            embodied += cacheServers * ServerKind.CPU_CURRENT.embodiedPerTick
            poweredOn += cacheServers
            totals.stale += (requests * 1_000.0 * caching.staleShare(policy.cache, scenario.interactive.expected(tick), tick)).roundToLong()
        }
        totals.requests += requests
        scenario.streaming?.let { streaming ->
            totals.ratingSum += requests.toLong() * streaming.rating(policy.streaming, grid.intensity(tick))
            totals.ratedStreams += requests
        }
        // Scenario 6: the server fans follow the setpoint; the cooling follows the outside temperature.
        val cooling = scenario.cooling
        if (cooling != null) itPower = itPower * cooling.fanPermille(policy.setpoint) / 1_000
        val facilityPower = cooling?.facilityPower(itPower, tick, policy.setpoint) ?: scenario.facility.facilityPower(itPower)
        val intensity = plannedAt?.let { grid.forecastTenths(tick, it) } ?: grid.intensityTenths(tick)
        val facilityEnergy = facilityPower * Units.MILLIWATT_HOURS_PER_WATT_TICK

        // O10: data transfers leaving this site now, in its grid.
        for (transfer in scenario.transfers) if (transfer.tick == tick) {
            totals.facilityEnergy += transfer.energy
            totals.transferEnergy += transfer.energy
            totals.operational += Units.emissions(transfer.energy, intensity)
        }
        totals.ticks += 1
        totals.demand += demand
        totals.served += served
        // Requests served (units of 1,000): the share of the demand served, applied to the requests.
        totals.requestsServed += scale * if (scenario.vms.isNotEmpty()) served.toLong()
        else if (demand == 0) 0 else requests.toLong() * served / demand
        totals.inTime += inTime
        totals.itEnergy += scale * itPower * Units.MILLIWATT_HOURS_PER_WATT_TICK
        totals.facilityEnergy += scale * facilityEnergy
        totals.operational += scale * Units.emissions(facilityEnergy, intensity)
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
                facilityPower = facilityPower, emissionsSoFar = totals.emissions, hostDemandMilli = hostMilli,
                setpoint = policy.setpoint, cache = cacheRecord,
                embodiedSoFar = totals.embodied, fleetCapacity = serverList.sumOf { it.kind.capacity }, trainingDone = totals.trainingDone,
                eco = scenario.streaming?.ecoActive(policy.streaming, (intensity + 5) / 10) ?: false,
                training = training != null && trainedLastTick,
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
            is CapacityMode.Placed -> {
                val used = mode.hosts.toSet()
                for (i in serverList.indices) {
                    when {
                        i !in used -> switchOff(i)
                        // Before the day starts the hosts are simply on: the placement is the starting point.
                        historyList.isEmpty() -> serverList[i] = serverList[i].copy(state = ServerState.On)
                        else -> switchOn(i)
                    }
                }
            }
            is CapacityMode.Autoscale -> {
                val utilization = mode.targetUtilization.coerceIn(Policy.TARGET_RANGE)
                // The expected demand of the coming hour; the last tick's demand too, if it was higher (the
                // autoscaler also reacts to what it measures – a campaign is not in the expected demand).
                val recent = historyList.lastOrNull()?.demand ?: 0
                val expected = maxOf(recent, (tick..tick + ServerKind.BOOT_TICKS + Policy.LOOKAHEAD)
                    .maxOf { scenario.interactive.expected(it) * workPermille(it) / 1_000 + extraWork(it) })
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
                val neededNow = ceilDiv(maxOf(recent, scenario.interactive.expected(tick) * workPermille(tick) / 1_000 + extraWork(tick)) * 1_000, utilization)
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

    /** Real days one tick stands for, in ticks of a real day: 1, or a quarter's 91 in O7. */
    private val scale: Long get() = (scenario.renewal?.daysPerStep ?: 1).toLong()

    /** O7: as many new servers as the policy says, of the generation of the day they arrive (switched off). */
    private fun applyRenewal() {
        val model = scenario.renewal ?: return
        val installed = serverList.count { it.kind == ServerKind.CPU_CURRENT && it.installedAt > scenario.startTick - 1 && it.ageDays == 0 }
        repeat(maxOf(0, policy.renewed - installed)) { install(model) }
    }

    private fun install(model: RenewalModel) {
        val day = (tick - scenario.startTick) / GameTime.TICKS_PER_DAY
        serverList += Server((serverList.maxOfOrNull { it.id } ?: -1) + 1, ServerKind.CPU_CURRENT, ServerState.Off,
            installedAt = tick, powerPermille = model.generationPower(day))
    }

    /**
     * O7, at the start of every quarter: old servers past their life fail at random (the same ones in every run);
     * the reference operator buys what the coming quarter needs.
     */
    private fun startOfQuarter() {
        val model = scenario.renewal ?: return
        if ((tick - scenario.startTick) % GameTime.TICKS_PER_DAY != 0) return
        val day = (tick - scenario.startTick) / GameTime.TICKS_PER_DAY
        val failed = serverList.filter {
            it.ageAt(tick, scale) >= Units.LIFESPAN_DAYS && SeededRandom.value(scenario.seed, 50L + it.id, day, 1_000) < model.failure
        }
        serverList.removeAll(failed)
        totals.failedServers += failed.size
        if (policy.buyWhenShort) {
            // Capacity for the coming quarter's peak at 75 % utilization, bought when it is missing.
            val peak = (tick until tick + GameTime.TICKS_PER_DAY).maxOf { scenario.interactive.expected(it) }
            val needed = ceilDiv(peak * 1_000, 750)
            var capacity = serverList.sumOf { it.kind.capacity }
            while (capacity < needed) { install(model); capacity += ServerKind.CPU_CURRENT.capacity }
        }
    }

    /** O8: whether the training runs this tick; the result is the GPUs' draw, or null without a training. */
    internal var trainedLastTick = false

    private fun trainNow(): Int? {
        val model = scenario.training ?: return null
        val config = policy.training
        val left = model.work - totals.trainingDone
        val perTick = model.perTick(config)
        val run = left > 0 && perTick > 0 && (!config.greenest || greenestNow(left, perTick))
        val gpus = serverList.indices.filter { serverList[it].kind == ServerKind.GPU_CURRENT }
        for ((k, i) in gpus.withIndex()) {
            serverList[i] = serverList[i].copy(state = if (run && k < config.servers) ServerState.On else ServerState.Off)
        }
        if (run) {
            // Resuming after a pause: loading the checkpoint, no progress yet.
            if (!trainedLastTick && totals.trainingDone > 0) totals.trainingRestarts += 1
            val loading = !trainedLastTick && totals.trainingDone > 0
            if (!loading) {
                totals.trainingDone += perTick
                if (totals.trainingDone >= model.work && totals.trainingFinishedAt == null) totals.trainingFinishedAt = tick
            }
        }
        trainedLastTick = run
        return model.draw(config.cap)
    }

    /**
     * Greenest hours: the training runs now if this tick is among the greenest of the ticks left before the deadline
     * that it still needs (with a margin of a tenth and the loading after pauses) – by the forecast made now.
     */
    private fun greenestNow(left: Long, perTick: Int): Boolean {
        val ticksLeft = scenario.endTick - tick
        val needed = ((left + perTick - 1) / perTick).toInt()
        val withMargin = needed + needed / 10 + 2
        if (withMargin >= ticksLeft) return true
        val forecast = (tick until scenario.endTick).map { grid.forecastTenths(it, plannedAt ?: tick) }
        val threshold = forecast.sorted()[withMargin - 1]
        return forecast[0] <= threshold
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
            if (serverList.count { it.job != null } + job.spec.servers > batchCapacity) continue
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
        val starts = BatchPlanner.plan(open, batchCapacity, busy, grid, tick)
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
