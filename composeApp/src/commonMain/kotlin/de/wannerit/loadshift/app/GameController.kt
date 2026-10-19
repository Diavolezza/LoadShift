package de.wannerit.loadshift.app

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import de.wannerit.loadshift.core.BatchMode
import de.wannerit.loadshift.core.Contract
import de.wannerit.loadshift.core.MasterData
import de.wannerit.loadshift.core.Protocol
import de.wannerit.loadshift.core.ServiceCut
import de.wannerit.loadshift.core.ServiceConfig
import de.wannerit.loadshift.core.Trigger
import de.wannerit.loadshift.core.Runners
import de.wannerit.loadshift.core.Environments
import de.wannerit.loadshift.core.HeavyTests
import de.wannerit.loadshift.core.PipelineConfig
import de.wannerit.loadshift.core.LazyLoading
import de.wannerit.loadshift.core.ImageFormat
import de.wannerit.loadshift.core.Tracking
import de.wannerit.loadshift.core.PageConfig
import de.wannerit.loadshift.core.InferenceConfig
import de.wannerit.loadshift.core.Answerer
import de.wannerit.loadshift.core.CacheConfig
import de.wannerit.loadshift.core.CapacityMode
import de.wannerit.loadshift.core.Control
import de.wannerit.loadshift.core.Engineering
import de.wannerit.loadshift.core.Team
import de.wannerit.loadshift.core.GameTime
import de.wannerit.loadshift.core.Packing
import de.wannerit.loadshift.core.Policy
import de.wannerit.loadshift.core.ScheduledMinimum
import de.wannerit.loadshift.core.Scenario
import de.wannerit.loadshift.core.ServerKind
import de.wannerit.loadshift.core.Simulation
import de.wannerit.loadshift.core.Solution
import de.wannerit.loadshift.core.Solutions
import de.wannerit.loadshift.core.Totals
import de.wannerit.loadshift.core.Quality
import de.wannerit.loadshift.core.StreamingConfig
import de.wannerit.loadshift.core.TrainingConfig
import de.wannerit.loadshift.core.VmKind
import de.wannerit.loadshift.core.forSite
import de.wannerit.loadshift.core.forLocation
import de.wannerit.loadshift.core.runLocation
import de.wannerit.loadshift.core.plus

/** Speed of the game clock: ticks (15 game minutes) per real second. */
enum class Speed(val ticksPerSecond: Int) {
    PAUSED(0), X1(1), X4(4), X16(16),

    /** Long scenarios (five days and more). */
    X64(64),

    /** O7: a quarter (a game day) in two seconds. */
    QUARTERS(48),
}

/**
 * Runs a scenario for the screen: the simulation, the clock and the player's controls. The simulation is mutable;
 * [version] changes with every change. Reading [simulation] reads [version], so every composable that looks at the
 * simulation redraws after a change – Compose skips a composable whose arguments are the same objects as before.
 */
/** [fresh]: without the panel settings kept from the last run (the briefing's preview must not show them). */
class GameController(val scenario: Scenario, fresh: Boolean = false) {
    /** O7: one simulation per site, all in step; the screen shows the [site] chosen. Other scenarios: one. */
    private val siteCount = scenario.renewal?.sites?.size ?: scenario.location?.sites?.size ?: 1
    /** O10: the jobs that run in Oslo (chosen before the start). */
    var moved by mutableStateOf(setOf<Int>())
        private set
    private fun siteScenario(site: Int, moved: Set<Int>) =
        if (scenario.location != null) scenario.forLocation(site, moved) else scenario.forSite(site)
    private fun newSims(policy: Policy? = null) = List(siteCount) { i ->
        val s = siteScenario(i, moved)
        if (policy != null) Simulation(s, policy) else Simulation(s)
    }
    private var sims: List<Simulation> = newSims()
    var site by mutableIntStateOf(0)
    private val current: Simulation get() = sims[site]
    val simulation: Simulation get() = version.let { current }

    /** The reference operator's runs of the same scenario (one per site), for the comparison. */
    private val references: List<Simulation> = List(siteCount) { Simulation.reference(siteScenario(it, emptySet())) }

    /** Two or more sites (O7, O10). */
    val multiSite: Boolean get() = siteCount > 1

    /** O10: moves a job to the other site and back – only before the start. */
    val canMove: Boolean get() = scenario.location != null && current.history.isEmpty()
    fun toggleMove(job: Int) {
        if (!canMove) return
        moved = if (job in moved) moved - job else moved + job
        val policy = current.policy
        sims = newSims(policy)
        changed()
    }

    /** O10: the batch rule at both sites (as soon as possible or the greenest hours). */
    fun setLocationRule(greenest: Boolean) {
        for (sim in sims) {
            sim.policy = sim.policy.copy(batch = if (greenest) BatchMode.Greenest else BatchMode.Asap)
            sim.applyPolicy()
        }
        changed()
    }

    /** Batch jobs of all sites: done, and all. */
    val jobsDone: Int get() = version.let { sims.sumOf { it.totals.jobsDone } }
    val jobsTotal: Int get() = sims.sumOf { it.jobs.size }
    val reference: Simulation get() = references[site]

    /** Totals of all sites (O7), else of the one simulation. */
    val totals: Totals get() = version.let { sims.map { it.totals }.reduce { a, b -> a + b } }
    val referenceTotals: Totals get() = references.map { it.totals }.reduce { a, b -> a + b }

    /** The simulation of a site (O7), and the reference operator's. */
    fun simulationOf(site: Int): Simulation = version.let { sims[site] }
    fun referenceOf(site: Int): Simulation = references[site]

    var version by mutableIntStateOf(0)
        private set
    var speed by mutableStateOf(Speed.PAUSED)
        private set
    var showEnd by mutableStateOf(false)
        private set

    /** Stars of the finished run (set once at the end). */
    var stars by mutableIntStateOf(0)
        private set

    /** The forecast has been revised (scenario 3): the game paused for it. */
    var forecastNews by mutableStateOf(false)
        private set

    /** A batch job that must start soon or will miss its deadline: the game paused for it. */
    var urgentJob by mutableStateOf<Int?>(null)
        private set
    private val warnedJobs = mutableSetOf<Int>()

    val isFinished: Boolean get() = simulation.isFinished
    val hasBatch: Boolean get() = scenario.batchJobs.isNotEmpty() || scenario.location != null
    val hasInteractive: Boolean get() = scenario.interactive.peak > 0 || scenario.vms.isNotEmpty()

    private fun changed() {
        version++
    }

    fun changeSpeed(newSpeed: Speed) {
        if (isFinished && newSpeed != Speed.PAUSED) return
        speed = newSpeed
    }

    /** The rules in force when this run's first tick ran (the next run starts with them). */
    private var startPolicy: Policy? = null

    /** One tick; called by the clock. */
    fun step() {
        if (simulation.isFinished) return
        if (current.history.isEmpty()) startPolicy = current.policy
        beforeTick()
        sims.forEach { if (!it.isFinished) it.step() }
        if (simulation.isFinished) finish() else {
            checkUrgentJobs()
            checkForecast()
            checkSprint()
            checkQuarter()
        }
        changed()
    }

    // Hardware renewal (O7): new servers per site, bought whenever the game is paused; they arrive at the start of
    // the next quarter. Each year brings a budget; what is not spent by the end of the year is gone.

    val hasRenewal: Boolean get() = scenario.renewal != null
    /** New servers per site, bought so far (installed or arriving at the start of the next quarter). */
    var renewDraft by mutableStateOf(List(siteCount) { 0 })
        private set
    /** New servers bought in each year (by the year in which the game was paused to buy them). */
    private val boughtInYear = mutableMapOf<Int, Int>()
    fun installed(site: Int): Int = version.let { sims[site].policy.renewed }
    val year: Int get() = scenario.renewal?.year(currentDay.coerceAtMost(scenario.days - 1)) ?: 0
    /** New servers the budget still pays for this year. */
    val budgetLeft: Int get() = version.let { (scenario.renewal?.budgetPerYear ?: 0) - (boughtInYear[year] ?: 0) }
    val canRenew: Boolean get() = hasRenewal && !isFinished

    fun changeRenewal(site: Int, delta: Int) {
        if (!canRenew) return
        // More: as far as this year's budget goes; fewer: only servers bought this year that have not arrived yet.
        val boughtNow = boughtInYear[year] ?: 0
        val n = (renewDraft[site] + delta).coerceIn(maxOf(installed(site), renewDraft[site] - boughtNow), renewDraft[site] + maxOf(0, budgetLeft))
        boughtInYear[year] = boughtNow + (n - renewDraft[site])
        renewDraft = renewDraft.toMutableList().also { it[site] = n }
        changed()
    }

    /** Lets the quarters run. */
    fun startQuarter() {
        if (!canRenew) return
        speed = Speed.QUARTERS
    }

    /** O7, before a tick: at the start of a quarter the bought servers arrive; a new year brings a new budget. */
    private fun beforeTick() {
        presetTick()
        if (!hasRenewal || (current.tick - scenario.startTick) % GameTime.TICKS_PER_DAY != 0) return
        for (i in sims.indices) {
            if (renewDraft[i] > sims[i].policy.renewed) {
                sims[i].policy = sims[i].policy.copy(renewed = renewDraft[i])
                sims[i].applyPolicy()
            }
        }
    }

    /** Pauses at the start of every year: a new budget. */
    private fun checkQuarter() {
        val model = scenario.renewal ?: return
        val sinceStart = current.tick - scenario.startTick
        if (sinceStart % (GameTime.TICKS_PER_DAY * model.daysPerYear) == 0) speed = Speed.PAUSED
    }

    /** O7: capacity of all servers of a site and the peak of the coming quarter (the current one while it runs). */
    fun capacityOf(site: Int): Int = version.let { sims[site].servers.sumOf { it.kind.capacity } } +
        (renewDraft[site] - installed(site)) * de.wannerit.loadshift.core.ServerKind.CPU_CURRENT.capacity
    fun peakOf(site: Int): Int = version.let {
        val sim = sims[site]
        val day = (sim.tick - scenario.startTick) / GameTime.TICKS_PER_DAY
        val from = scenario.startTick + minOf(day, scenario.days - 1) * GameTime.TICKS_PER_DAY
        (from until from + GameTime.TICKS_PER_DAY).maxOf { sim.scenario.interactive.expected(it) }
    }

    // Sprints (engineering track): one sprint plan at the start of every day

    /** The team and its progress through the backlog (carry-over of started items). */
    private var team = Team(scenario.backlog, scenario.sprintPoints)

    /** Items done so far, with the sprint (day index) they were done in. */
    val sprintDone: Map<Int, Int> get() = version.let { team.done }
    /** The items the player has picked for the coming sprint, in the order the team works on them. */
    var sprintDraft by mutableStateOf(listOf<Int>())
        private set
    /** Day of the last sprint started (-1: none yet). */
    private var sprintStartedDay = -1
    /** Plans of this run by day, kept for the next run. */
    private val sprintPlans = mutableMapOf<Int, List<Int>>()

    val hasSprints: Boolean get() = Control.SPRINTS in scenario.controls
    val currentDay: Int get() = (current.tick - scenario.startTick) / GameTime.TICKS_PER_DAY
    val canPlanSprint: Boolean
        get() = hasSprints && !isFinished && (current.tick - scenario.startTick) % GameTime.TICKS_PER_DAY == 0 && sprintStartedDay < currentDay
    val profiled: Boolean get() = scenario.growth != null || sprintDone.keys.any { scenario.backlog[it].profiling }

    /** Items started in an earlier sprint and not finished: the team continues them first. */
    val sprintCarried: List<Int> get() = version.let { team.started }
    fun remaining(item: Int): Int = version.let { team.remaining(item) }

    /** Story points of the carried items and the draft. */
    val sprintPointsPicked: Int get() = sprintCarried.sumOf { team.remaining(it) } + sprintDraft.sumOf { scenario.backlog[it].effort }

    /** Launch options: a whole plan at once. */
    fun planSprint(items: List<Int>) {
        if (canPlanSprint) sprintDraft = items.filter { it !in sprintDone && it !in sprintCarried }
    }

    fun toggleSprintItem(item: Int) {
        if (!canPlanSprint || item in sprintDone || item in sprintCarried) return
        sprintDraft = if (item in sprintDraft) sprintDraft - item else sprintDraft + item
    }

    /** Starts the sprint and lets the week run (fast: a sprint shows its effect over a whole day). */
    fun startSprint() {
        if (!canPlanSprint) return
        val day = currentDay
        sprintPlans[day] = sprintDraft
        team.sprint(day, sprintDraft)
        sprintStartedDay = day
        sprintDraft = emptyList()
        setPolicy(current.policy.copy(done = team.done.keys.toSet()))
        speed = Speed.X16
    }

    /** E3: what an item saves today (per mille of today's work) – "measured" with today's data. */
    fun savingToday(item: Int): Int {
        val done = sprintDone.keys
        val before = Engineering.dailyWork(scenario, currentDay.coerceAtMost(scenario.days - 1), done)
        val after = Engineering.dailyWork(scenario, currentDay.coerceAtMost(scenario.days - 1), done + item)
        return if (before == 0L) 0 else ((before - after) * 1_000 / before).toInt()
    }

    /** Pauses at the start of every sprint (the plan is the player's: nothing from the last run appears by itself). */
    private fun checkSprint() {
        if (!hasSprints || (current.tick - scenario.startTick) % GameTime.TICKS_PER_DAY != 0) return
        speed = Speed.PAUSED
        presetSprints.getOrNull(currentDay)?.let { planSprint(it) }
    }

    /** Pauses when the forecast is revised (CONCEPT.md 3.3). */
    private fun checkForecast() {
        // O11: no pause and no news – noticing that the forecast changed is part of the task.
        if (scenario.forecastMiss?.revisedAt == current.tick && !isCapstone) {
            forecastNews = true
            speed = Speed.PAUSED
        }
    }

    fun dismissForecastNews() {
        forecastNews = false
    }

    /** Plans the open jobs again with the latest forecast (greenest-hours rule). */
    /** The player planned again after the forecast changed (compared with the solution in the debrief). */
    private var replanned = false

    fun replan() {
        replanned = true
        current.replan()
        forecastNews = false
        changed()
    }

    /** Runs a number of ticks at once (launch options). */
    fun advance(ticks: Int) {
        if (ticks > 0 && current.history.isEmpty()) startPolicy = current.policy
        repeat(ticks) { beforeTick(); sims.forEach { if (!it.isFinished) it.step() } }
        if (simulation.isFinished) finish()
        changed()
    }

    private fun finish() {
        saveDraft()
        speed = Speed.PAUSED
        stars = scenario.stars(totals, referenceTotals)
        Progress.record(stars, scenario.number)
        showEnd = true
    }

    /** Pauses once per job when an unplanned job has to start within the next hour (CONCEPT.md 3.3). */
    private fun checkUrgentJobs() {
        if (Control.BATCH_BY_HAND !in scenario.controls) return
        for (job in simulation.jobs) {
            if (!job.state.isStarted && job.plannedStart == null && job.id !in warnedJobs &&
                simulation.tick >= job.spec.latestStart - GameTime.TICKS_PER_HOUR
            ) {
                warnedJobs += job.id
                urgentJob = job.id
                speed = Speed.PAUSED
                return
            }
        }
    }

    fun dismissUrgent() {
        urgentJob = null
    }

    /** Changes the policy at once (switching by hand, launch options). */
    fun setPolicy(policy: Policy) {
        current.policy = policy
        current.applyPolicy()
        resetDraft()
        changed()
    }

    // Servers by hand

    val wantedServers: Int
        get() = (simulation.policy.capacity as? CapacityMode.Manual)?.servers
            ?: simulation.servers.count { it.state.isPowered }

    fun setWantedServers(n: Int) {
        setPolicy(simulation.policy.copy(capacity = CapacityMode.Manual(n.coerceIn(0, scenario.serverCount))))
    }

    // Batch rules (scenario 3): the player edits a draft and applies it

    /** The kinds of batch rule the panel offers. */
    enum class RuleKind { ASAP, HOUR, GREENEST, THRESHOLD }

    var draftKind by mutableStateOf(RuleKind.ASAP)
    var draftHour by mutableIntStateOf(3)
    var draftThreshold by mutableIntStateOf(150)
    var draftSafety by mutableStateOf(true)

    /** The batch rule of the draft. */
    val draftBatch: BatchMode
        get() = when (draftKind) {
            RuleKind.ASAP -> BatchMode.Asap
            RuleKind.HOUR -> BatchMode.AtHour(draftHour)
            RuleKind.GREENEST -> BatchMode.Greenest
            RuleKind.THRESHOLD -> BatchMode.Threshold(draftThreshold)
        }

    // Capacity rules (scenario 4)

    enum class CapacityKind { ALL_ON, FIXED, AUTOSCALE }

    var capKind by mutableStateOf(CapacityKind.AUTOSCALE)
    var capFixed by mutableIntStateOf(15)
    var capTarget by mutableIntStateOf(800)
    var capMinimumOn by mutableStateOf(false)
    var capMinimum by mutableIntStateOf(24)
    var capFrom by mutableIntStateOf(18)
    var capTo by mutableIntStateOf(22)

    // Streaming (O9)

    var streamMax by mutableStateOf(listOf<Quality>())
    var streamEco by mutableStateOf(false)
    var streamEcoAbove by mutableIntStateOf(600)
    var streamEcoQuality by mutableStateOf(Quality.HD)
    val draftStreaming: StreamingConfig get() = StreamingConfig(streamMax, streamEco, streamEcoAbove, streamEcoQuality)

    fun setMaxQuality(device: Int, quality: Quality) {
        val n = scenario.streaming?.devices?.size ?: return
        streamMax = List(n) { if (it == device) quality else streamMax.getOrElse(it) { Quality.UHD } }
    }

    // Services (E6)

    val hasServices: Boolean get() = scenario.services != null
    var servicesDraft by mutableStateOf(ServiceConfig())

    // Pipeline (E9)

    val hasPipeline: Boolean get() = scenario.pipeline != null
    var pipelineDraft by mutableStateOf(PipelineConfig())

    // Pages (E5)

    val hasWeb: Boolean get() = scenario.web != null
    var pageDraft by mutableStateOf(PageConfig())

    // Shop assistant (E8)

    val hasInference: Boolean get() = scenario.inference != null
    var aiAnswerers by mutableStateOf(listOf<Answerer>())
    var aiWindow by mutableIntStateOf(0)
    val draftInference: InferenceConfig get() = InferenceConfig(aiAnswerers, aiWindow)

    fun setAnswerer(kind: Int, answerer: Answerer) {
        val n = scenario.inference?.kinds?.size ?: return
        aiAnswerers = List(n) { if (it == kind) answerer else aiAnswerers.getOrElse(it) { Answerer.LARGE } }
    }

    // Training (O8)

    val hasTraining: Boolean get() = scenario.training != null

    /** O11: everything together – the panel has tabs (shop, batch, training), the timeline shows all three. */
    val isCapstone: Boolean get() = scenario.controls.size > 1 && scenario.training != null
    var panelTab by mutableIntStateOf(0)

    /** The panel's tabs when a scenario has more than one kind of control (O11, B12). */
    enum class PanelTab { CONTRACT, SHOP, BATCH, TRAINING }
    val tabs: List<PanelTab> get() = listOfNotNull(
        PanelTab.CONTRACT.takeIf { Control.CONTRACT in scenario.controls },
        PanelTab.SHOP.takeIf { Control.CAPACITY_RULES in scenario.controls },
        PanelTab.BATCH.takeIf { Control.BATCH_RULES in scenario.controls },
        PanelTab.TRAINING.takeIf { Control.TRAINING in scenario.controls },
    )
    val isTabbed: Boolean get() = tabs.size > 1

    // Electricity contract (B12)

    var contractDraft by mutableStateOf(Contract.GREY)

    /** GPU servers in the fleet. */
    val gpuCount: Int get() = scenario.fleet.filter { it.kind == ServerKind.GPU_CURRENT }.sumOf { it.count }
    var trainServers by mutableIntStateOf(8)
    var trainCap by mutableIntStateOf(1_000)
    var trainGreenest by mutableStateOf(false)
    val draftTraining: TrainingConfig get() = TrainingConfig(trainServers, trainCap, trainGreenest)

    // Cooling (scenario 6)

    var draftSetpoint by mutableIntStateOf(20)

    // Caching (E4)

    var cacheServers by mutableIntStateOf(0)
    var cacheTtl by mutableStateOf(listOf<Int>())
    var cacheCount by mutableStateOf(false)
    var cacheView by mutableStateOf(false)
    var cacheInvalidate by mutableStateOf(false)
    var cacheWarmUp by mutableStateOf(false)

    val draftCache: CacheConfig
        get() = CacheConfig(cacheServers, cacheTtl, cacheCount, cacheView, cacheInvalidate, cacheWarmUp)

    fun setTtl(kind: Int, minutes: Int) {
        cacheTtl = cacheTtl.toMutableList().also { while (it.size <= kind) it += 0; it[kind] = minutes }
    }

    /** The capacity rule of the draft. */
    val draftCapacity: CapacityMode
        get() = when (capKind) {
            CapacityKind.ALL_ON -> CapacityMode.AllOn
            CapacityKind.FIXED -> CapacityMode.Manual(capFixed)
            CapacityKind.AUTOSCALE -> CapacityMode.Autoscale(capTarget, if (capMinimumOn) ScheduledMinimum(capMinimum, capFrom, capTo) else null)
        }

    /** The whole draft: the rules the scenario lets the player edit, the rest as it runs. */
    val draftPolicy: Policy
        get() {
            var policy = simulation.policy
            if (Control.BATCH_RULES in scenario.controls) policy = policy.copy(batch = draftBatch, deadlineSafety = draftSafety)
            if (Control.CAPACITY_RULES in scenario.controls) policy = policy.copy(capacity = draftCapacity)
            if (Control.CACHING in scenario.controls) policy = policy.copy(cache = draftCache)
            if (Control.COOLING in scenario.controls) policy = policy.copy(setpoint = draftSetpoint)
            if (Control.TRAINING in scenario.controls) policy = policy.copy(training = draftTraining)
            if (Control.STREAMING in scenario.controls) policy = policy.copy(streaming = draftStreaming)
            if (Control.INFERENCE in scenario.controls) policy = policy.copy(inference = draftInference)
            if (Control.PAGES in scenario.controls) policy = policy.copy(pages = pageDraft)
            if (Control.PIPELINE in scenario.controls) policy = policy.copy(pipeline = pipelineDraft)
            if (Control.SERVICES in scenario.controls) policy = policy.copy(services = servicesDraft)
            if (Control.CONTRACT in scenario.controls) policy = policy.copy(contract = contractDraft)
            return policy
        }

    /** The draft is what runs now. */
    val draftIsActive: Boolean get() = simulation.policy == draftPolicy

    fun applyDraft() {
        setPolicy(draftPolicy)
    }

    /** Sets the draft to the rules that run now. */
    private fun resetDraft() = setDraftFrom(current.policy)

    /** Sets the draft to the given rules. */
    private fun setDraftFrom(policy: Policy) {
        when (val capacity = policy.capacity) {
            CapacityMode.AllOn -> capKind = CapacityKind.ALL_ON
            is CapacityMode.Manual -> { capKind = CapacityKind.FIXED; capFixed = capacity.servers }
            is CapacityMode.Autoscale -> {
                capKind = CapacityKind.AUTOSCALE
                capTarget = capacity.targetUtilization
                capMinimumOn = capacity.minimum != null
                capacity.minimum?.let { capMinimum = it.servers; capFrom = it.fromHour; capTo = it.toHour }
            }
            is CapacityMode.Placed -> {}
        }
        draftSafety = policy.deadlineSafety
        draftSetpoint = policy.setpoint
        trainServers = policy.training.servers; trainCap = policy.training.cap; trainGreenest = policy.training.greenest
        scenario.streaming?.let { m ->
            val c = policy.streaming
            streamMax = m.devices.indices.map { c.maxQuality.getOrElse(it) { Quality.UHD } }
            streamEco = c.eco; streamEcoAbove = c.ecoAbove; streamEcoQuality = c.ecoQuality
        }
        pageDraft = policy.pages
        pipelineDraft = policy.pipeline
        servicesDraft = policy.services
        contractDraft = policy.contract
        scenario.inference?.let { m ->
            aiAnswerers = m.kinds.indices.map { policy.inference.answerers.getOrElse(it) { Answerer.LARGE } }
            aiWindow = policy.inference.windowMs
        }
        scenario.caching?.let { model ->
            val c = policy.cache
            cacheServers = c.servers
            cacheTtl = model.kinds.indices.map { c.ttlMinutes.getOrElse(it) { 0 } }
            cacheCount = c.approximateCount
            cacheView = c.materializedView
            cacheInvalidate = c.invalidate
            cacheWarmUp = c.warmUp
        }
        draftKind = when (val mode = policy.batch) {
            is BatchMode.AtHour -> RuleKind.HOUR.also { draftHour = mode.hour }
            BatchMode.Greenest -> RuleKind.GREENEST
            is BatchMode.Threshold -> RuleKind.THRESHOLD.also { draftThreshold = mode.gramsPerKWh }
            else -> RuleKind.ASAP
        }
    }

    init {
        resetDraft()
        if (!fresh) restoreDraft()
    }

    /** The draft of the last run of this scenario (kept while the app runs). */
    private class Draft(
        val kind: RuleKind, val hour: Int, val threshold: Int, val safety: Boolean,
        val capKind: CapacityKind, val capFixed: Int, val capTarget: Int, val capMinimumOn: Boolean, val capMinimum: Int,
        val capFrom: Int, val capTo: Int, val startPolicy: Policy?, val cache: CacheConfig, val setpoint: Int,
        val training: TrainingConfig,
        val streaming: StreamingConfig,
        val inference: InferenceConfig,
        val pages: PageConfig,
        val pipeline: PipelineConfig,
        val services: ServiceConfig,
        val contract: Contract,
    )

    private companion object {
        val drafts = mutableMapOf<Int, Draft>()
    }

    /** Keeps the panel's settings for the next run (a new run starts with the scenario's rules, the panel keeps yours). */
    fun saveDraft() {
        drafts[scenario.number] = Draft(draftKind, draftHour, draftThreshold, draftSafety, capKind, capFixed, capTarget,
            capMinimumOn, capMinimum, capFrom, capTo, startPolicy ?: current.policy.takeIf { current.history.isEmpty() }, draftCache, draftSetpoint, draftTraining, draftStreaming, draftInference, pageDraft, pipelineDraft, servicesDraft, contractDraft)
    }

    private fun restoreDraft() {
        val d = drafts[scenario.number] ?: return
        draftKind = d.kind; draftHour = d.hour; draftThreshold = d.threshold; draftSafety = d.safety
        capKind = d.capKind; capFixed = d.capFixed; capTarget = d.capTarget; capMinimumOn = d.capMinimumOn
        capMinimum = d.capMinimum; capFrom = d.capFrom; capTo = d.capTo
        cacheServers = d.cache.servers
        if (d.cache.ttlMinutes.isNotEmpty()) cacheTtl = d.cache.ttlMinutes
        cacheCount = d.cache.approximateCount; cacheView = d.cache.materializedView; cacheInvalidate = d.cache.invalidate; cacheWarmUp = d.cache.warmUp
        draftSetpoint = d.setpoint
        trainServers = d.training.servers; trainCap = d.training.cap; trainGreenest = d.training.greenest
        if (d.streaming.maxQuality.isNotEmpty()) streamMax = d.streaming.maxQuality
        streamEco = d.streaming.eco; streamEcoAbove = d.streaming.ecoAbove; streamEcoQuality = d.streaming.ecoQuality
        if (d.inference.answerers.isNotEmpty()) aiAnswerers = d.inference.answerers
        aiWindow = d.inference.windowMs
        pageDraft = d.pages
        pipelineDraft = d.pipeline
        servicesDraft = d.services
        contractDraft = d.contract
        // A new run starts with the rules the last run started with (the panel shows the last run's final ones).
        if (d.startPolicy != null && current.history.isEmpty() && !hasSprints) {
            current.policy = d.startPolicy
            current.applyPolicy()
        }
    }

    // Placing VMs (scenario 5)

    /** The VM the player has picked up to move, if any. */
    var selectedVm by mutableStateOf<Int?>(null)
        private set
    var placementMessage by mutableStateOf<String?>(null)
        private set

    /** The host of every VM, in the placement scenario. */
    val placement: List<Int>? get() = (simulation.policy.capacity as? CapacityMode.Placed)?.hosts

    /** VMs can be moved until the day starts. */
    val canPlace: Boolean get() = simulation.history.isEmpty() && placement != null

    /** Picks a VM up for dragging (always selects it). */
    fun pickVm(vm: Int) {
        if (!canPlace) return
        selectedVm = vm
        placementMessage = null
    }

    fun dropVm() {
        selectedVm = null
    }

    fun selectVm(vm: Int) {
        if (!canPlace) return
        selectedVm = if (selectedVm == vm) null else vm
        placementMessage = null
    }

    /** Moves the picked-up VM to a host, if its RAM fits there. */
    fun moveSelectedTo(host: Int) {
        val vm = selectedVm ?: return
        val p = placement ?: return
        if (!canPlace) return
        if (p[vm] == host) { selectedVm = null; return }
        val kind = scenario.vms[vm]
        val free = simulation.servers[host].kind.ramGb - Packing.ramUsed(scenario.vms, p, host)
        if (kind.ramGb > free) {
            placementMessage = t("%@ needs %lld GB – host %lld has only %lld GB free.", t(kind.title), kind.ramGb, host + 1, free)
            return
        }
        setPolicy(Policy(CapacityMode.Placed(p.toMutableList().also { it[vm] = host })))
        selectedVm = null
        placementMessage = null
    }

    fun spreadAgain() {
        if (!canPlace) return
        setPolicy(scenario.initialPolicy)
        selectedVm = null
        placementMessage = null
    }

    // Batch jobs by hand

    fun plan(job: Int, start: Int) {
        simulation.plan(job, start)
        if (urgentJob == job) urgentJob = null
        changed()
    }

    fun unplan(job: Int) {
        simulation.unplan(job)
        changed()
    }

    /** Default start when the player plans a job: the next full hour, not before its arrival. */
    fun suggestedStart(job: Int): Int {
        val earliest = maxOf(simulation.tick, simulation.jobs[job].spec.arrival)
        val perHour = GameTime.TICKS_PER_HOUR
        return (earliest + perHour - 1) / perHour * perHour
    }

    /** The player's emissions so far against the reference's up to the same moment, per mille (null before the first tick). */
    val changeSoFar: Long?
        get() {
            val last = simulation.history.lastOrNull() ?: return null
            // O11: the training waits for the wind – CO₂ so far would flatter; compare the expected end instead.
            if (isCapstone && !isFinished) return expectedChange()
            val ref = references.sumOf { it.emissions(last.tick) }
            if (ref <= 0) return null
            return (sims.sumOf { it.emissions(last.tick) } - ref) * 1_000 / ref
        }

    private var expectedAt = -1 to 0L

    /** O11: the expected change at the end, with the rules now (computed once per tick). */
    private fun expectedChange(): Long {
        val key = version
        if (expectedAt.first != key) {
            val ref = referenceTotals.emissions
            expectedAt = key to (current.projected(current.policy).emissions - ref) * 1_000 / maxOf(1, ref)
        }
        return expectedAt.second
    }

    // The solution (D31, core Solutions – LessonTest checks three stars for each): taken over into a new run. Rules
    // apply from the start and change when the solution changes them; sprint plans fill in at the start of each
    // sprint; servers by hand and servers bought switch by themselves; planning again happens when the forecast changes.

    val solution: Solution? = Solutions.of(scenario)
    private var presetSprints: List<List<Int>> = emptyList()
    private var presetServers: Map<Int, Int> = emptyMap()
    private var presetChanges: Map<Int, Policy> = emptyMap()
    private var presetReplan: Set<Int> = emptySet()
    private var presetRenewal: List<Map<Int, Int>> = emptyList()

    /** Applies what the solution does at this tick (called before each tick). */
    private fun presetTick() {
        val tick = current.tick
        presetServers[tick]?.let { setWantedServers(it) }
        presetChanges[tick]?.let { setDraftFrom(it); setPolicy(it) }
        if (tick in presetReplan) replan()
        if (presetRenewal.isNotEmpty() && (tick - scenario.startTick) % GameTime.TICKS_PER_DAY == 0) {
            val day = (tick - scenario.startTick) / GameTime.TICKS_PER_DAY
            for ((i, plan) in presetRenewal.withIndex()) plan[day]?.let { n ->
                if (n > renewDraft[i]) {
                    boughtInYear[year] = (boughtInYear[year] ?: 0) + (n - renewDraft[i])
                    renewDraft = renewDraft.toMutableList().also { it[i] = n }
                }
            }
        }
    }

    /** A new run with the solution in the panel. */
    fun loadSolution() {
        val sol = solution ?: return
        restart(keepDraft = false)
        for (job in sol.moved) toggleMove(job)
        sol.policy?.let { p ->
            if (scenario.location != null) setLocationRule(p.batch == BatchMode.Greenest) else { setDraftFrom(p); setPolicy(p) }
        }
        presetChanges = sol.changes
        presetReplan = sol.replanAt
        presetServers = sol.servers
        presetRenewal = sol.renewal
        presetSprints = sol.sprints
        presetSprints.firstOrNull()?.let { planSprint(it) }
        presetServers[current.tick]?.let { setWantedServers(it) }
    }

    private fun at(tick: Int): String {
        val time = GameTime(tick)
        return if (scenario.days > 1) t("day %lld, %@", (time.day - GameTime.START.day + 1).toLong(), time.clockText) else time.clockText
    }

    /** The parts of the rules the panel shows, each with its name and a description. */
    private fun aspects(p: Policy): List<Pair<String, String>> {
        val c = scenario.controls
        return buildList {
            if (Control.CONTRACT in c) add(t("Electricity contract") to t(p.contract.label))
            if (Control.CAPACITY_RULES in c || Control.PLACEMENT in c) add(t("Capacity") to capacityText(p.capacity))
            if (Control.COOLING in c) add(t("Setpoint") to t("%lld °C", p.setpoint.toLong()))
            if (Control.BATCH_RULES in c || Control.BATCH_BY_HAND in c || Control.LOCATION in c) add(t("Batch jobs") to ruleText(p))
            if (Control.TRAINING in c) add(t("Training") to trainingText(p.training))
            scenario.streaming?.let { m -> add(t("Video quality") to streamingText(p.streaming, m)) }
            if (Control.CACHING in c) add(t("Cache") to cacheText(p.cache))
            scenario.inference?.let { m -> add(t("Shop assistant") to inferenceText(p.inference, m)) }
            if (Control.PAGES in c) add(t("Pages") to pagesText(p.pages))
            if (Control.PIPELINE in c) add(t("Pipeline") to pipelineText(p.pipeline))
            if (Control.SERVICES in c) add(t("Services") to servicesText(p.services))
        }
    }

    private fun items(list: Collection<Int>) = if (list.isEmpty()) t("nothing") else list.joinToString(", ") { t(scenario.backlog[it].title) }
    private fun jobs(set: Set<Int>) = if (set.isEmpty()) t("nothing") else scenario.batchJobs.withIndex().filter { it.index in set }.joinToString(", ") { t(it.value.name) }

    /** The solution in words, step by step. */
    fun solutionSteps(): List<String> {
        val sol = solution ?: return emptyList()
        val lines = mutableListOf<String>()
        when {
            sol.sprints.isNotEmpty() -> sol.sprints.forEachIndexed { i, plan ->
                if (plan.isNotEmpty()) lines += t("Sprint %lld: %@", (i + 1).toLong(), items(plan))
            }
            sol.servers.isNotEmpty() -> {
                val plan = sol.servers
                val hourly = linkedMapOf<Int, Int>()
                var last = -1
                for (tick in plan.keys) {
                    val hour = tick / GameTime.TICKS_PER_HOUR * GameTime.TICKS_PER_HOUR
                    val v = plan.entries.last { it.key <= hour + GameTime.TICKS_PER_HOUR - 1 }.value
                    if (v != last && hour !in hourly) { hourly[hour] = v; last = v }
                }
                lines += t("Switch to %lld servers at the start.", plan.values.first().toLong())
                lines += t("Then keep just enough servers on for the expected load of the next 90 minutes at about 80 % – a server needs 30 minutes to start, so switch on before the load rises:")
                lines += hourly.entries.drop(1).joinToString(" · ") { t("%@ → %lld", at(it.key), it.value.toLong()) }
            }
            sol.renewal.isNotEmpty() -> {
                val sites = scenario.renewal?.sites ?: emptyList()
                for ((i, plan) in sol.renewal.withIndex()) {
                    val site = sites.getOrNull(i)?.let { t(it.title) } ?: "${i + 1}"
                    lines += t("%@: %@", site, plan.entries.joinToString(" · ") { (day, n) ->
                        t("from year %lld, quarter %lld: %lld new servers", (day / 4 + 1).toLong(), (day % 4 + 1).toLong(), n.toLong())
                    })
                }
            }
            else -> {
                if (sol.moved.isNotEmpty()) lines += t("Move to %@: %@.", t(scenario.location?.sites?.getOrNull(1)?.title ?: ""), jobs(sol.moved))
                sol.policy?.let { p -> for ((name, text) in aspects(p)) lines += t("%@: %@", name, text) }
                for ((tick, p) in sol.changes) {
                    val before = sol.policy?.let { aspects(it) } ?: emptyList()
                    for ((name, text) in aspects(p)) if ((name to text) !in before) lines += t("From %@ – %@: %@", at(tick), name, text)
                }
            }
        }
        if (sol.replanAt.isNotEmpty()) lines += t("When the forecast changes: plan again with the latest forecast.")
        return lines
    }

    /**
     * The player's run against the solution: what differs at the start and at the end of the run, and the steps the
     * solution takes that the run did not – so that the debrief says what to change, not only what is best.
     */
    fun differences(): List<String> {
        val sol = solution ?: return emptyList()
        val lines = mutableListOf<String>()
        fun diff(aspect: String, mine: String, theirs: String) {
            if (mine != theirs) lines += t("%@ – you: %@; solution: %@", aspect, mine, theirs)
        }
        when {
            sol.sprints.isNotEmpty() -> sol.sprints.forEachIndexed { i, plan ->
                diff(t("Sprint %lld", (i + 1).toLong()), items(sprintPlans[i] ?: emptyList()), items(plan))
            }
            sol.renewal.isNotEmpty() -> {
                val sites = scenario.renewal?.sites ?: emptyList()
                for ((i, plan) in sol.renewal.withIndex()) diff(t("New servers in %@", sites.getOrNull(i)?.let { t(it.title) } ?: ""),
                    "${renewDraft.getOrElse(i) { 0 }}", "${plan.values.maxOrNull() ?: 0}")
            }
            sol.servers.isNotEmpty() -> {}
            else -> {
                if (scenario.location != null) diff(t("Moved"), jobs(moved), jobs(sol.moved))
                val solStart = sol.policy ?: scenario.initialPolicy
                val myStart = startPolicy ?: current.policy
                val myEnd = current.policy
                val laterChange = sol.changes.keys.minOrNull()
                val startSuffix = if (laterChange != null) " " + t("(at the start)") else ""
                for ((a, b) in aspects(myStart).zip(aspects(solStart))) diff(a.first + startSuffix, a.second, b.second)
                if (laterChange != null) {
                    val solEnd = sol.changes.values.last()
                    for ((a, b) in aspects(myEnd).zip(aspects(solEnd))) diff(a.first + " " + t("(from %@)", at(laterChange)), a.second, b.second)
                } else if (myStart != myEnd) lines += t("You changed the rules during the run; the solution has them from the start.")
            }
        }
        if (sol.replanAt.isNotEmpty() && !replanned)
            lines += t("After the forecast changed – you: did not plan again; solution: planned again with the latest forecast")
        return lines
    }

    /** "What if" for the debrief: the whole scenario replayed with other rules, with the same load and grid mix. */
    fun whatIf(): List<Pair<String, Totals>> {
        if (scenario.location != null) {
            fun run(moved: Set<Int>, greenest: Boolean) = scenario.runLocation(moved,
                scenario.initialPolicy.copy(batch = if (greenest) BatchMode.Greenest else BatchMode.Asap)).map { it.totals }.reduce { a, b -> a + b }
            val smart = setOf(0, 1, 5, 6, 7, 8)
            return listOf(
                t("All in Frankfurt, as soon as possible (reference)") to referenceTotals,
                t("All in Frankfurt, greenest hours") to run(emptySet(), true),
                t("Both trainings to Oslo") to run(setOf(1, 7), false),
                t("Trainings, reports and search index to Oslo") to run(smart, false),
                t("The same, greenest hours") to run(smart, true),
                t("The same plus the product videos") to run(smart + 2, true),
            )
        }
        if (scenario.streaming != null) {
            fun run(c: StreamingConfig) = Simulation.replay(scenario, scenario.initialPolicy.copy(streaming = c)).totals
            val byDevice = StreamingConfig(listOf(Quality.HD, Quality.HD, Quality.UHD))
            return listOf(
                t("4K on every device (reference)") to referenceTotals,
                t("HD everywhere") to run(StreamingConfig(listOf(Quality.HD, Quality.HD, Quality.HD))),
                t("HD on phones and laptops, 4K on TVs") to run(byDevice),
                t("The same, eco mode HD above 650 g/kWh") to run(byDevice.copy(eco = true, ecoAbove = 650)),
                t("The same, eco mode HD above 550 g/kWh") to run(byDevice.copy(eco = true, ecoAbove = 550)),
                t("The same, eco mode SD above 650 g/kWh") to run(byDevice.copy(eco = true, ecoAbove = 650, ecoQuality = Quality.SD)),
            )
        }
        scenario.contract?.let {
            fun run(c: Contract, auto: Boolean, greenest: Boolean) = Simulation.replay(scenario, scenario.referencePolicy.copy(contract = c,
                capacity = if (auto) CapacityMode.Autoscale(800) else scenario.referencePolicy.capacity,
                batch = if (greenest) BatchMode.Greenest else BatchMode.Asap)).totals
            return listOf(
                t("Grid mix, autoscaling at 50 %, jobs at once (reference)") to referenceTotals,
                t("Green certificates") to run(Contract.CERTIFICATES, false, false),
                t("Autoscaling at 80 %") to run(Contract.GREY, true, false),
                t("Wind farm contract") to run(Contract.PPA, false, false),
                t("Wind farm contract, jobs in the greenest hours") to run(Contract.PPA, false, true),
                t("The same, autoscaling at 80 %") to run(Contract.PPA, true, true),
                t("24/7 carbon-free") to run(Contract.HOURLY, false, false),
                t("24/7 carbon-free, autoscaling at 80 %") to run(Contract.HOURLY, true, false),
            )
        }
        scenario.services?.let {
            val base = ServiceConfig()
            fun run(c: ServiceConfig) = Simulation.replay(scenario, scenario.initialPolicy.copy(services = c)).totals
            val cached = base.copy(gpsTtl = 60, mapTtl = 300)
            return listOf(
                t("Microservices, REST, synchronous calls, no cache (reference)") to referenceTotals,
                t("Cache: positions 1 min, traffic 5 min") to run(cached),
                t("The same, positions 5 min, traffic 1 h") to run(base.copy(gpsTtl = 300, mapTtl = 3_600)),
                t("Cache, gRPC with Protobuf") to run(cached.copy(protocol = Protocol.GRPC)),
                t("Cache, events for the master data") to run(cached.copy(masterData = MasterData.EVENTS)),
                t("Cache, gRPC, events, serverless functions") to run(cached.copy(protocol = Protocol.GRPC, masterData = MasterData.EVENTS, serverless = true)),
                t("Cache, serverless functions, but synchronous calls") to run(cached.copy(serverless = true)),
                t("Modular monolith with cache") to run(cached.copy(cut = ServiceCut.MODULITH)),
                t("The same, route optimization as a service of its own") to run(cached.copy(cut = ServiceCut.MODULITH, extractOptimizer = true)),
                t("Modular monolith without cache") to run(base.copy(cut = ServiceCut.MODULITH)),
            )
        }
        scenario.pipeline?.let {
            val base = PipelineConfig()
            fun run(c: PipelineConfig) = Simulation.replay(scenario, scenario.initialPolicy.copy(pipeline = c)).totals
            val lean = base.copy(heavy = HeavyTests.NIGHTLY, environments = Environments.OFFICE_HOURS, replicas = 1, testImpact = true)
            return listOf(
                t("Agents and environments always on, all tests with every commit (reference)") to referenceTotals,
                t("Environments only on weekdays 7–19") to run(base.copy(environments = Environments.OFFICE_HOURS)),
                t("UI tests every night instead of with every commit") to run(base.copy(heavy = HeavyTests.NIGHTLY)),
                t("Both, test impact analysis, one replica") to run(lean),
                t("The same, agents on demand without a remote cache") to run(lean.copy(runners = Runners.ON_DEMAND)),
                t("The same, agents on demand with a remote cache") to run(lean.copy(runners = Runners.ON_DEMAND, remoteCache = true)),
                t("The same, builds every 30 minutes") to run(lean.copy(runners = Runners.ON_DEMAND, remoteCache = true, trigger = Trigger.EVERY_30_MIN)),
                t("The same with every commit, UI tests never") to run(lean.copy(runners = Runners.ON_DEMAND, remoteCache = true, heavy = HeavyTests.NEVER)),
            )
        }
        scenario.web?.let {
            val base = PageConfig()
            fun run(c: PageConfig) = Simulation.replay(scenario, scenario.initialPolicy.copy(pages = c)).totals
            val light = base.copy(autoplay = false, tracking = Tracking.ONE_TAG, longLists = false)
            val images = light.copy(format = ImageFormat.AVIF, responsive = true, lazy = LazyLoading.BELOW_FOLD)
            return listOf(
                t("Large images, video plays by itself, 100 products, six tags (reference)") to referenceTotals,
                t("Video only on click") to run(base.copy(autoplay = false)),
                t("Video on click, one analytics tag") to run(base.copy(autoplay = false, tracking = Tracking.ONE_TAG)),
                t("The same, 24 products per list") to run(light),
                t("The same, AVIF in the right size, lazy loading below the fold") to run(images),
                t("The same, counting on the server") to run(images.copy(tracking = Tracking.SERVER)),
                t("The same, but AVIF converted on every request") to run(images.copy(format = ImageFormat.AVIF_LIVE)),
                t("The same, but lazy loading for all images") to run(images.copy(lazy = LazyLoading.ALL)),
            )
        }
        scenario.inference?.let {
            fun code(c: String, window: Int) = InferenceConfig(c.map { ch ->
                when (ch) { 'N' -> Answerer.NONE; 'S' -> Answerer.SMALL; 'L' -> Answerer.LARGE; else -> Answerer.REASONING }
            }, window)
            fun run(c: String, window: Int) = Simulation.replay(scenario, scenario.initialPolicy.copy(inference = code(c, window))).totals
            return listOf(
                t("Large model for everything, one question at a time (reference)") to referenceTotals,
                t("Order status without AI") to run("NLLLL", 0),
                t("Order status and greetings without AI") to run("NNLLL", 0),
                t("Small model for everything") to run("SSSSS", 0),
                t("Large model for everything, batching 1 s") to run("LLLLL", 1_000),
                t("Status and greetings without AI, batching 1 s") to run("NNLLL", 1_000),
                t("The same, small model for product questions") to run("NNSLL", 1_000),
                t("The same, batching 2 s") to run("NNSLL", 2_000),
                t("The same with 1 s, reasoning for complaints") to run("NNSRL", 1_000),
            )
        }
        if (isCapstone) {
            val revised = setOfNotNull(scenario.forecastMiss?.revisedAt)
            val auto = CapacityMode.Autoscale(800)
            val green = TrainingConfig(4, 600, true)
            fun run(capacity: CapacityMode, batch: BatchMode, training: TrainingConfig, replan: Boolean = true) =
                Simulation.replay(scenario, Policy(capacity, batch, training = training), if (replan) revised else emptySet()).totals
            return listOf(
                t("All on, jobs at once, training at once (reference)") to referenceTotals,
                t("Only autoscaling at 80 %") to run(auto, BatchMode.Asap, scenario.referencePolicy.training),
                t("Only the training in the wind (cap 60 %)") to run(CapacityMode.AllOn, BatchMode.Asap, green),
                t("Only the batch jobs in the greenest hours") to run(CapacityMode.AllOn, BatchMode.Greenest, scenario.referencePolicy.training),
                t("Autoscaling and training") to run(auto, BatchMode.Asap, green),
                t("All three, without planning again after the forecast update") to run(auto, BatchMode.Greenest, green, replan = false),
                t("All three, planned again") to run(auto, BatchMode.Greenest, green),
                t("All three, training without a cap") to run(auto, BatchMode.Greenest, TrainingConfig(4, 1_000, true)),
            )
        }
        if (hasTraining) {
            fun run(n: Int, cap: Int, greenest: Boolean) = Simulation.replay(scenario, scenario.initialPolicy.copy(training = TrainingConfig(n, cap, greenest))).totals
            return listOf(
                t("8 servers, no cap, at once (reference)") to referenceTotals,
                t("8 servers, no cap, greenest hours") to run(8, 1_000, true),
                t("8 servers, cap 70 %, at once") to run(8, 700, false),
                t("8 servers, cap 70 %, greenest hours") to run(8, 700, true),
                t("6 servers, cap 60 %, greenest hours") to run(6, 600, true),
                t("4 servers, cap 70 %, at once") to run(4, 700, false),
            )
        }
        if (hasRenewal) {
            /** Both sites with plans: day → new servers installed by then. */
            fun run(vararg plans: Map<Int, Int>): Totals = plans.mapIndexed { site, plan ->
                val s = scenario.forSite(site)
                Simulation.replay(s, s.initialPolicy, changes = plan.entries.associate { (day, n) -> GameTime.at(day, 0).tick to s.initialPolicy.copy(renewed = n) }).totals
            }.reduce { a, b -> a + b }
            return listOf(
                t("Buy when capacity runs short (reference)") to referenceTotals,
                t("Paris first") to run(mapOf(8 to 4), mapOf(0 to 4, 4 to 8)),
                t("Two and two every year") to run(mapOf(0 to 2, 4 to 4, 8 to 6), mapOf(0 to 2, 4 to 4, 8 to 6)),
                t("Warsaw first, Paris in year 3") to run(mapOf(0 to 4, 4 to 8), mapOf(8 to 4)),
                t("Warsaw first, Paris only what it needs, as late as possible") to run(mapOf(0 to 4, 4 to 8, 8 to 10), mapOf(10 to 2)),
            )
        }
        if (scenario.cooling != null) {
            fun run(capacity: CapacityMode, setpoint: Int) = Simulation.replay(scenario, Policy(capacity, setpoint = setpoint)).totals
            return listOf(
                t("Autoscaling at 50 %, 20 °C (reference)") to reference.totals,
                t("Autoscaling at 50 %, 27 °C") to run(CapacityMode.Autoscale(500), 27),
                t("Autoscaling at 80 %, 20 °C") to run(CapacityMode.Autoscale(800), 20),
                t("Autoscaling at 80 %, 25 °C") to run(CapacityMode.Autoscale(800), 25),
                t("All servers on, 27 °C (the best PUE)") to run(CapacityMode.AllOn, 27),
            )
        }
        if (hasBatch && Control.BATCH_RULES in scenario.controls) {
            val capacity = scenario.referencePolicy.capacity
            fun replay(mode: BatchMode, safety: Boolean = true) = Simulation.replay(scenario, Policy(capacity, mode, safety)).totals
            return listOf(
                t("Every job as soon as it arrives (reference)") to reference.totals,
                t("Every job at night, at 03:00") to replay(BatchMode.AtHour(3)),
                t("Below 150 g/kWh, with deadline safety") to replay(BatchMode.Threshold(150)),
                t("Below 150 g/kWh, without deadline safety") to replay(BatchMode.Threshold(150), false),
                t("Greenest hours, planned once") to replay(BatchMode.Greenest),
                t("Greenest hours, planned again after the forecast update") to
                    scenario.forecastMiss.let { miss ->
                        Simulation.replay(scenario, Policy(capacity, BatchMode.Greenest), setOfNotNull(miss?.revisedAt)).totals
                    },
            )
        }
        if (hasBatch) {
            val capacity = scenario.referencePolicy.capacity
            fun replay(mode: BatchMode) = Simulation.replay(scenario, Policy(capacity, mode)).totals
            return listOf(
                t("Every job as soon as it arrives (reference)") to reference.totals,
                t("Every job at night, at 01:00") to replay(BatchMode.AtHour(1)),
                t("Every job at 11:00") to replay(BatchMode.AtHour(11)),
                t("Planned by the forecast (from scenario O3)") to replay(BatchMode.Greenest),
            )
        }
        if (scenario.caching != null) {
            fun run(c: CacheConfig) = Simulation.replay(scenario, scenario.initialPolicy.copy(cache = c)).totals
            val allowed = CacheConfig(1, listOf(1_440, 15, 1, 0, 0), invalidate = true)
            return listOf(
                t("No cache (reference)") to reference.totals,
                t("Everything allowed, search too, 4 cache servers (the team's plan)") to run(allowed.copy(servers = 4, ttlMinutes = listOf(1_440, 15, 1, 60, 0))),
                t("As allowed, without search, 1 cache server") to run(allowed),
                t("The same without invalidation") to run(allowed.copy(invalidate = false)),
                t("The same, catalog pages for 1 min instead of invalidation") to run(allowed.copy(invalidate = false, ttlMinutes = listOf(1_440, 1, 1, 0, 0))),
                t("As allowed, 1 cache server, invalidation, HyperLogLog and materialized view") to run(allowed.copy(approximateCount = true, materializedView = true)),
                t("The same with the nightly warm-up") to run(allowed.copy(approximateCount = true, materializedView = true, warmUp = true)),
            )
        }
        if (hasSprints) {
            fun run(plans: List<List<Int>>) = Simulation.replay(scenario, scenario.initialPolicy, changes = Engineering.changes(scenario, plans)).totals
            if (scenario.growth != null) {
                val backlog = scenario.backlog
                return listOf(
                    t("The team changes nothing (reference)") to reference.totals,
                    t("The biggest saving today first") to run(Engineering.greedy(scenario) { i, _, _, _ -> backlog[i].effect }),
                    t("By today's saving per story point") to run(Engineering.greedy(scenario, value = Engineering.byToday(scenario))),
                    t("By the saving in a year per story point") to run(Engineering.greedy(scenario, value = Engineering.byLastDay(scenario))),
                )
            }
            return listOf(
                t("The team changes nothing (reference)") to reference.totals,
                t("By the team's estimates") to run(Engineering.greedy(scenario, value = Engineering.byEstimates(scenario))),
                t("Profiling first, then by the measurements") to run(Engineering.greedy(scenario, profileFirst = true, value = Engineering.byEstimates(scenario))),
            )
        }
        if (Control.PLACEMENT in scenario.controls) {
            val vms = scenario.vms
            val host = scenario.fleet.first().kind
            val fleet = scenario.serverCount
            val both = { k: VmKind -> k.ramGb * 1_000 / host.ramGb + k.peak * 1_000 / host.capacity }
            fun replay(p: List<Int>) = Simulation.replay(scenario, Policy(CapacityMode.Placed(p))).totals
            return listOf(
                t("Spread over all hosts (reference)") to reference.totals,
                t("First Fit Decreasing, RAM only") to replay(Packing.firstFitDecreasing(vms, host, fleet, Packing.Check.RAM_ONLY) { it.ramGb }),
                t("First Fit Decreasing, sum of the peaks") to replay(Packing.firstFitDecreasing(vms, host, fleet, Packing.Check.PEAK_SUM) { it.ramGb }),
                t("First Fit Decreasing, daily profiles up to 85 %") to replay(Packing.firstFitDecreasing(vms, host, fleet, Packing.Check.PROFILES, key = both)),
                t("First Fit Decreasing, daily profiles up to 100 %") to replay(Packing.firstFitDecreasing(vms, host, fleet, Packing.Check.PROFILES, 1_000, both)),
            )
        }
        if (Control.CAPACITY_RULES in scenario.controls) {
            fun replay(policy: Policy, changes: Map<Int, Policy> = emptyMap()) = Simulation.replay(scenario, policy, changes = changes).totals
            val start = scenario.initialPolicy
            val target = (start.capacity as? CapacityMode.Autoscale)?.targetUtilization ?: 800
            val day2 = GameTime.at(1, 12).tick
            val prepared = Policy(CapacityMode.Autoscale(target, ScheduledMinimum(26, 18, 22)))
            return listOf(
                t("All servers on (reference)") to reference.totals,
                t("Autoscaling at %@ (as at the start)", Format.wholePercent(target)) to replay(start),
                t("Autoscaling at %@", Format.wholePercent(600)) to replay(Policy(CapacityMode.Autoscale(600))),
                t("Autoscaling at %@, every day at least %lld servers 18:00–22:00", Format.wholePercent(target), 26) to replay(prepared),
                t("Autoscaling at %@, on day 2 at least %lld servers 18:00–22:00", Format.wholePercent(target), 26) to replay(start, mapOf(day2 to prepared)),
            )
        }
        val half = scenario.serverCount / 2
        val most = scenario.serverCount * 7 / 10
        fun replay(mode: CapacityMode) = Simulation.replay(scenario, Policy(mode)).totals
        return listOf(
            t("All servers on (reference)") to reference.totals,
            t("Fixed: %lld servers all day", half) to replay(CapacityMode.Manual(half)),
            t("Fixed: %lld servers all day", most) to replay(CapacityMode.Manual(most)),
            t("Autoscaling at 70 % (from scenario O4)") to replay(CapacityMode.Autoscale(700)),
        )
    }

    /** Back to the scenario's starting state: its rules, the panel's settings and (O10) every job in Frankfurt. */
    fun startOver() {
        drafts.remove(scenario.number)
        moved = emptySet()
        panelTab = 0
        restart(keepDraft = false)
    }

    fun restart(keepDraft: Boolean = true) {
        if (keepDraft) saveDraft()
        speed = Speed.PAUSED
        sims = newSims()
        renewDraft = List(siteCount) { 0 }
        boughtInYear.clear()
        if (keepDraft) restoreDraft() else resetDraft()
        startPolicy = null
        team = Team(scenario.backlog, scenario.sprintPoints)
        sprintDraft = emptyList()
        sprintStartedDay = -1
        sprintPlans.clear()
        presetSprints = emptyList()
        presetServers = emptyMap()
        presetChanges = emptyMap()
        presetReplan = emptySet()
        presetRenewal = emptyList()
        replanned = false
        stars = 0
        showEnd = false
        urgentJob = null
        forecastNews = false
        warnedJobs.clear()
        changed()
    }
}
