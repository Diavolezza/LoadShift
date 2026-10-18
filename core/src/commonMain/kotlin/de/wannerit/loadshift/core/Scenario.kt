package de.wannerit.loadshift.core

/** The two strands of the campaign (docs/CONCEPT.md 9, D17). */
enum class Track { OPERATIONS, ENGINEERING, PORTFOLIO }

/** A scenario: region, season, length, fleet, load, the controls it unlocks and its goals (docs/CONCEPT.md 8.3 and 9). */
data class FleetEntry(val kind: ServerKind, val count: Int, val ageYears: Int = 0)

/** Controls a scenario offers (one new idea at a time, CONCEPT.md principle 3). */
enum class Control {
    /** Switching servers on and off by hand; takes effect at once. */
    MANUAL_CAPACITY,

    /** Rules for the capacity: all on, fixed number, autoscaling with a target utilization. */
    CAPACITY_RULES,

    /** Planning batch jobs by hand: a start time per job. */
    BATCH_BY_HAND,

    /** Rules for batch jobs: as soon as possible, fixed hour, greenest hours, threshold; deadline safety. */
    BATCH_RULES,

    /** Placing VMs on hosts by hand (before the day starts). */
    PLACEMENT,

    /** Engineering track: a sprint plan from the backlog at the start of every day. */
    SPRINTS,

    /** E3: what to cache, for how long, in how much memory. */
    CACHING,

    /** E6: who answers each kind of question (no AI, a small or large model), and batching. */
    INFERENCE,

    /** E4: images, video, lists and tracking of the shop's pages. */
    PAGES,

    /** E7: build agents, caches, tests, triggers and test environments. */
    PIPELINE,

    /** E5: cut, protocol, master data, caches for external calls, serverless. */
    SERVICES,

    /** B12: the electricity contract. */
    CONTRACT,

    /** Scenario 6: the inlet temperature setpoint of the cooling. */
    COOLING,

    /** O7: replacing old servers by new ones, per site and quarter. */
    RENEWAL,

    /** O8: servers, power cap and schedule of an AI training run. */
    TRAINING,

    /** O9: the highest quality per device and the eco mode of a streaming service. */
    STREAMING,

    /** O10: which batch jobs run at which site, and the batch rule. */
    LOCATION,
}

/** The special goal of a scenario: the third star. */
sealed interface SpecialGoal {
    /** Mean utilization of the running servers at least this, per mille. */
    data class MeanUtilization(val permille: Int) : SpecialGoal

    /** CO₂ at least this much below the reference, per mille. */
    data class Reduction(val permille: Int) : SpecialGoal

    /** Mean carbon intensity of the energy used for batch work at most this, g/kWh. */
    data class BatchIntensity(val gramsPerKWh: Int) : SpecialGoal

    /** On average at most this many servers on. */
    data class MaxServersOn(val servers: Int) : SpecialGoal
}

/** Goals of a scenario (CONCEPT.md 8.3). */
data class StarGoals(
    /** Second star: CO₂ at least this much below the reference operator, per mille. */
    val reduction: Int,
    /** Third star. */
    val special: SpecialGoal,
)

data class GoalsMet(val rules: Boolean, val reduction: Boolean, val special: Boolean)

data class Scenario(
    val number: Int,
    val title: String,
    val setting: GridSetting,
    val weather: WeatherMode = WeatherMode.Drawn,
    val days: Int,
    val seed: Long,
    val fleet: List<FleetEntry>,
    val interactive: InteractiveLoad,
    val facility: Facility = Facility(),
    /** SLA target, per mille of the demand served in time. */
    val slaTarget: Int = 990,
    val controls: Set<Control> = setOf(Control.CAPACITY_RULES),
    /** Policy at the start: how the data center was left by the previous shift. */
    val initialPolicy: Policy = Policy.REFERENCE,
    /** What the reference operator does in this scenario. */
    val referencePolicy: Policy = Policy.REFERENCE,
    val goals: StarGoals = StarGoals(150, SpecialGoal.Reduction(250)),
    val batchJobs: List<BatchJobSpec> = emptyList(),
    /** A forecast that gets the weather's timing wrong until it is revised (the game pauses then). */
    val forecastMiss: ForecastMiss? = null,
    /** Virtual machines to place on the hosts (scenario 5). */
    val vms: List<VmKind> = emptyList(),
    val track: Track = Track.OPERATIONS,
    /** The name in the list: "5", "E1". */
    val label: String = number.toString(),
    /** The scenario whose star opens this one (null: always open). */
    val unlockAfter: Int? = if (number <= 1) null else number - 1,
    /** Engineering track: where the CPU time per request goes, the backlog and the team's story points per sprint. */
    val hotSpots: List<HotSpot> = emptyList(),
    val backlog: List<BacklogItem> = emptyList(),
    val sprintPoints: Int = 0,
    /** E2: the data grows, and parts of the work with it. */
    val growth: GrowthModel? = null,
    /** E3: kinds of data that can be cached. */
    val caching: CacheModel? = null,
    /** E3: stale answers allowed for the first star, per 10,000 requests. */
    val staleLimit: Int? = null,
    /** Scenario 6: cooling with outside temperature and setpoint instead of a fixed PUE. */
    val cooling: CoolingModel? = null,
    /** O7: two sites with old fleets; one game day is a quarter. */
    val renewal: RenewalModel? = null,
    /** O8: an AI training run. */
    val training: TrainingModel? = null,
    /** O9: a video streaming service. */
    val streaming: StreamingModel? = null,
    /** O10: two sites for the batch work; the jobs that move are the player's choice. */
    val location: LocationModel? = null,
    /** O10: data transfers this site books (energy at its grid). */
    val transfers: List<Transfer> = emptyList(),
    /** O10: rules broken by the choice of sites (a job with customer data moved abroad). */
    val rulesBroken: Int = 0,
    /** O11: at most this many servers work for batch jobs at a time – the rest belong to the shop (null: all). */
    val batchLimit: Int? = null,
    /** E6: a shop assistant with language models. */
    val inference: InferenceModel? = null,
    /** E4: the network and the visitors' devices count too. */
    val web: WebModel? = null,
    /** E7: a team's CI/CD pipeline and test environments. */
    val pipeline: PipelineModel? = null,
    /** E5: the logistics system's services. */
    val services: ServiceModel? = null,
    /** B12: electricity contracts and three ways to count CO₂ (operational counts hourly matched). */
    val contract: ContractModel? = null,
    /** Portfolio track: the company's applications, a year at a time. */
    val portfolio: PortfolioModel? = null,
    /** P2: a list of measures as a marginal abatement cost curve. */
    val abatement: AbatementModel? = null,
    /** P3: the portfolio by TIME, three years in quarters. */
    val tidy: TidyModel? = null,
    /** P4: six applications, each to a place – cloud, colocation or the company's own server room. */
    val repatriation: RepatriationModel? = null,
    /** E8: building blocks, availability tactics and quality scenarios. */
    val availability: AvailabilityModel? = null,
    /** E9: how an app learns about changes – polling, streams, push. */
    val updates: UpdatesModel? = null,
) {
    /** Expected interactive work at a tick: the web load or the VMs' profiles (what an operator can plan with). */
    fun expectedDemand(tick: Int): Int = interactive.expected(tick) + vms.sumOf { it.expectedMilli(tick) } / 1_000

    /** First tick: 00:00 on day 0. */
    val startTick: Int get() = GameTime.START.tick

    /** The tick after the last one. */
    val endTick: Int get() = startTick + days * GameTime.TICKS_PER_DAY
    val serverCount: Int get() = fleet.sumOf { it.count }

    // Stars

    /** The goals a result reaches. */
    fun goalsMet(totals: Totals, reference: Totals): GoalsMet {
        val rules = totals.sla >= slaTarget && totals.worstSiteSla >= slaTarget &&
            (training == null || totals.trainingFinishedAt != null) &&
            (streaming == null || totals.meanRating >= streaming.ratingTarget) &&
            totals.rulesBroken == 0 && totals.deadlinesMissed == 0 && totals.emissions < reference.emissions &&
            (staleLimit == null || totals.stalePerTenThousand <= staleLimit) &&
            (web == null || totals.meanLcp <= web.lcpLimit) &&
            (services == null || totals.meanLatency <= services.latencyLimit) &&
            (contract == null || totals.energyCost * 1_000 <= reference.energyCost * contract.budgetPermille) &&
            (pipeline == null || totals.meanFeedback <= pipeline.feedbackLimit * 10) &&
            (inference == null || (totals.meanAnswerQuality >= inference.qualityTarget && totals.answersOnTime >= inference.onTimeTarget))
        val reduction = rules && totals.emissions * 1_000 <= reference.emissions * (1_000 - goals.reduction)
        val special = rules && when (val goal = goals.special) {
            is SpecialGoal.MeanUtilization -> totals.meanUtilization >= goal.permille
            is SpecialGoal.Reduction -> totals.emissions * 1_000 <= reference.emissions * (1_000 - goal.permille)
            is SpecialGoal.BatchIntensity ->
                totals.batchEnergy > 0 && totals.meanBatchIntensityTenths <= goal.gramsPerKWh * 10
            is SpecialGoal.MaxServersOn -> totals.meanServersOn <= goal.servers * 1_000
        }
        return GoalsMet(rules, reduction, special)
    }

    /**
     * Stars of a result, 0..3: ★ the rules (SLA target, deadlines, less CO₂ than the reference), ★★ the reduction,
     * ★★★ the special goal. The second and third star need the first, not each other.
     */
    fun stars(totals: Totals, reference: Totals): Int {
        val met = goalsMet(totals, reference)
        if (!met.rules) return 0
        return 1 + (if (met.reduction) 1 else 0) + (if (met.special) 1 else 0)
    }

    companion object {
        /**
         * Scenario 1, "Night shift": a web shop in Germany in autumn. The fleet is sized for the Christmas season;
         * at night the load falls to a fifth of the evening peak, and idle servers still draw a quarter of their
         * maximum power. The day starts at midnight with every server on; the player switches servers by hand.
         */
        val NIGHT_SHIFT = Scenario(
            number = 1, title = "Night shift", setting = GridSetting(GridRegion.GERMANY, Season.AUTUMN), days = 1,
            seed = 1, fleet = listOf(FleetEntry(ServerKind.CPU_CURRENT, 20)),
            interactive = InteractiveLoad(peak = 240),
            controls = setOf(Control.MANUAL_CAPACITY),
            initialPolicy = Policy(CapacityMode.Manual(20)),
            goals = StarGoals(150, SpecialGoal.MeanUtilization(600)),
        )

        /**
         * Scenario 2, "Wait for the sun": the batch cluster of the web shop in Spain. Reports, backups and imports
         * must be done by their deadlines; when they run is up to the player. In Spain's solar grid the midday
         * hours are the cleanest – not the night.
         */
        val WAIT_FOR_THE_SUN = Scenario(
            number = 2, title = "Wait for the sun", setting = GridSetting(GridRegion.SPAIN, Season.YEAR), days = 2,
            seed = 2, fleet = listOf(FleetEntry(ServerKind.CPU_CURRENT, 10)),
            interactive = InteractiveLoad(peak = 0),
            controls = setOf(Control.BATCH_BY_HAND),
            initialPolicy = Policy(CapacityMode.Manual(0), BatchMode.ByHand),
            referencePolicy = Policy(CapacityMode.Manual(0), BatchMode.Asap),
            goals = StarGoals(100, SpecialGoal.BatchIntensity(83)),
            batchJobs = listOf(
                BatchJobSpec.of("Daily report", 4, 3, GameTime.at(0, 0), GameTime.at(0, 18)),
                BatchJobSpec.of("Backup", 3, 4, GameTime.at(0, 1), GameTime.at(1, 6)),
                BatchJobSpec.of("Data import", 6, 2, GameTime.at(0, 7), GameTime.at(0, 20)),
                BatchJobSpec.of("Image conversion", 5, 4, GameTime.at(0, 18), GameTime.at(1, 18)),
                BatchJobSpec.of("Daily report", 4, 3, GameTime.at(1, 0), GameTime.at(1, 18)),
                BatchJobSpec.of("Backup", 3, 4, GameTime.at(1, 1), GameTime.at(2, 0)),
                BatchJobSpec.of("Search index", 4, 3, GameTime.at(1, 6), GameTime.at(2, 0)),
            ),
        )

        /**
         * Scenario 3, "When the wind drops": the batch cluster in Great Britain in winter, three days. A windy day
         * makes the grid clean, then the wind drops and stays away. The player sets a rule instead of planning
         * each job; the forecast is uncertain (±25 % a day ahead), and without the deadline safety a rule that
         * waits for clean power misses deadlines once the wind has gone.
         */
        val WHEN_THE_WIND_DROPS = Scenario(
            number = 3, title = "When the wind drops", setting = GridSetting(GridRegion.GREAT_BRITAIN, Season.WINTER),
            weather = WeatherMode.Days(listOf(1_000, 550, 1_700)), days = 3,
            // Until day 2, 12:00 the forecast expects the wind to drop 12 hours later than it does.
            forecastMiss = ForecastMiss(shift = 12 * GameTime.TICKS_PER_HOUR, revisedAt = GameTime.at(1, 12).tick),
            seed = 3, fleet = listOf(FleetEntry(ServerKind.CPU_CURRENT, 12)),
            interactive = InteractiveLoad(peak = 0),
            controls = setOf(Control.BATCH_RULES),
            initialPolicy = Policy(CapacityMode.Manual(0), BatchMode.Asap),
            referencePolicy = Policy(CapacityMode.Manual(0), BatchMode.Asap),
            goals = StarGoals(100, SpecialGoal.BatchIntensity(145)),
            batchJobs = listOf(
                BatchJobSpec.of("Daily report", 4, 3, GameTime.at(0, 0), GameTime.at(0, 18)),
                BatchJobSpec.of("Backup", 3, 4, GameTime.at(0, 1), GameTime.at(1, 8)),
                BatchJobSpec.of("Data import", 6, 2, GameTime.at(0, 7), GameTime.at(1, 14)),
                BatchJobSpec.of("Recommendations", 6, 5, GameTime.at(0, 12), GameTime.at(1, 22)),
                BatchJobSpec.of("Daily report", 4, 3, GameTime.at(1, 0), GameTime.at(1, 18)),
                BatchJobSpec.of("Backup", 3, 4, GameTime.at(1, 1), GameTime.at(2, 8)),
                BatchJobSpec.of("Image conversion", 5, 4, GameTime.at(1, 10), GameTime.at(2, 16)),
                BatchJobSpec.of("Daily report", 4, 3, GameTime.at(2, 0), GameTime.at(2, 18)),
                BatchJobSpec.of("Search index", 4, 3, GameTime.at(2, 6), GameTime.at(3, 0)),
            ),
        )

        /**
         * Scenario 4, "Headroom": the web shop in Germany in winter, two days, with autoscaling. On day 2 a
         * newsletter goes out at 19:00 and doubles the load for three hours. The autoscaler knows only the usual
         * day and reacts to what it measures – too late, because a server takes 30 minutes to boot. Headroom costs
         * energy all the time; a scheduled minimum for the campaign costs it only when needed.
         */
        val HEADROOM = Scenario(
            number = 4, title = "Headroom", setting = GridSetting(GridRegion.GERMANY, Season.WINTER), days = 2,
            seed = 4, fleet = listOf(FleetEntry(ServerKind.CPU_CURRENT, 30)),
            interactive = InteractiveLoad(
                peak = 240,
                campaigns = listOf(Campaign(GameTime.at(1, 19).tick, GameTime.at(1, 22).tick, 2_000)),
            ),
            controls = setOf(Control.CAPACITY_RULES),
            initialPolicy = Policy(CapacityMode.Autoscale(800)),
            referencePolicy = Policy(CapacityMode.AllOn),
            goals = StarGoals(300, SpecialGoal.MeanUtilization(700)),
        )

        /**
         * Scenario 6, "Heat wave": the web shop in a German summer, 30 to 37 °C outside. The cooling works hard; a
         * warmer setpoint lets the outside air cool longer and the chiller work less, but above 24 °C the server fans
         * draw more. The PUE trap: fewer servers mean less energy but a higher PUE (the facility draws a fixed power),
         * and faster fans mean more energy but a lower PUE. The stars count CO₂, not the PUE.
         */
        val HEAT_WAVE = Scenario(
            number = 6, title = "Heat wave", setting = GridSetting(GridRegion.GERMANY, Season.SUMMER), days = 3,
            seed = 6, fleet = listOf(FleetEntry(ServerKind.CPU_CURRENT, 40)),
            interactive = InteractiveLoad(peak = 300),
            controls = setOf(Control.COOLING, Control.CAPACITY_RULES),
            initialPolicy = Policy(CapacityMode.Autoscale(500)),
            referencePolicy = Policy(CapacityMode.Autoscale(500)),
            goals = StarGoals(130, SpecialGoal.Reduction(165)),
            cooling = CoolingModel(days = listOf(17 to 28, 21 to 35, 23 to 37)),
        )

        /**
         * Scenario 7, "Old iron": the company's two data centers, in Warsaw and in Paris, each with 24 old servers, four
         * years old. Three years, one game day per quarter. Every year a budget for four new servers and a new
         * generation that does 20 % more work per kWh; the shop grows by 15 % a year; from year 3 the old servers are
         * past their life and fail. New servers in the coal-heavy Polish grid save many times their embodied emissions
         * at once; in the nuclear French grid they hardly save anything – there it pays to wait for a better generation
         * until the capacity is needed.
         */
        val OLD_IRON = Scenario(
            number = 7, title = "Old iron", setting = GridSetting(GridRegion.POLAND, Season.WINTER), days = 12,
            seed = 7, fleet = listOf(FleetEntry(ServerKind.CPU_OLD, 24, ageYears = 4)),
            interactive = InteractiveLoad(peak = 150, growthPerYear = 150, daysPerYear = 4),
            controls = setOf(Control.RENEWAL),
            initialPolicy = Policy(CapacityMode.Autoscale(700)),
            referencePolicy = Policy(CapacityMode.Autoscale(700), buyWhenShort = true),
            goals = StarGoals(200, SpecialGoal.Reduction(280)),
            renewal = RenewalModel(listOf(Site("Warsaw", GridRegion.POLAND), Site("Paris", GridRegion.FRANCE))),
        )

        /**
         * Scenario 8, "Training run": a language model for the shop's search must be trained within three days of a
         * German spring – sunny middays, windy and calm days. Eight GPU servers; the reference operator trains with all
         * of them at once, without a cap, from the first minute.
         */
        val TRAINING_RUN = Scenario(
            number = 8, title = "Training run", setting = GridSetting(GridRegion.GERMANY, Season.SPRING), days = 3,
            // A calm day, a windy day, a calm day.
            weather = WeatherMode.Days(listOf(1_150, 650, 1_100)),
            seed = 8, fleet = listOf(FleetEntry(ServerKind.GPU_CURRENT, 8)),
            interactive = InteractiveLoad(peak = 0),
            controls = setOf(Control.TRAINING),
            initialPolicy = Policy(CapacityMode.AllOn),
            referencePolicy = Policy(CapacityMode.AllOn),
            goals = StarGoals(200, SpecialGoal.Reduction(270)),
            training = TrainingModel(work = 240_000),
        )

        /**
         * Scenario 9, "Eco mode": a video streaming service in Poland, in summer. The viewers watch most in the evening –
         * just when the coal-heavy grid is dirtiest. The reference operator streams in 4K to every device.
         */
        val ECO_MODE = Scenario(
            number = 9, title = "Eco mode", setting = GridSetting(GridRegion.POLAND, Season.SUMMER), days = 2,
            seed = 9, fleet = listOf(FleetEntry(ServerKind.CPU_CURRENT, 30)),
            interactive = InteractiveLoad(peak = 360),
            controls = setOf(Control.STREAMING),
            initialPolicy = Policy(CapacityMode.Autoscale(700)),
            referencePolicy = Policy(CapacityMode.Autoscale(700)),
            goals = StarGoals(450, SpecialGoal.Reduction(560)),
            streaming = StreamingModel(
                devices = listOf(
                    Device("Phones", 400, listOf(1_000, 1_000, 800)),
                    Device("Laptops", 250, listOf(1_000, 950, 600)),
                    Device("TVs", 350, listOf(1_000, 800, 400)),
                ),
                ratingTarget = 930,
            ),
        )

        /**
         * Scenario 10, "Two sites": the batch work of the company runs in Frankfurt; a hydro-powered data center in Oslo
         * has eight servers free. Which jobs should move? Those with a lot of computing and little data; not the ones
         * with terabytes of data, not the ones that talk to the database in Frankfurt all the time, and not the one
         * with customer data that must stay in Germany.
         */
        val TWO_SITES = Scenario(
            number = 10, title = "Two sites", setting = GridSetting(GridRegion.GERMANY, Season.AUTUMN), days = 3,
            seed = 10, fleet = listOf(FleetEntry(ServerKind.CPU_CURRENT, 14)),
            interactive = InteractiveLoad(peak = 0),
            controls = setOf(Control.LOCATION),
            batchJobs = emptyList(),
            initialPolicy = Policy(CapacityMode.Manual(0), BatchMode.Asap),
            referencePolicy = Policy(CapacityMode.Manual(0), BatchMode.Asap),
            goals = StarGoals(350, SpecialGoal.Reduction(450)),
            location = LocationModel(
                sites = listOf(Site("Frankfurt", GridRegion.GERMANY), Site("Oslo", GridRegion.NORWAY)),
                servers = listOf(14, 8),
                jobs = listOf(
                    LocationJob(BatchJobSpec.of("Daily report", 4, 3, GameTime.at(0, 0), GameTime.at(0, 18)), 2),
                    LocationJob(BatchJobSpec.of("Recommendation model training", 8, 10, GameTime.at(0, 6), GameTime.at(2, 12)), 10),
                    LocationJob(BatchJobSpec.of("Product video encoding", 6, 5, GameTime.at(0, 8), GameTime.at(1, 20)), 300),
                    LocationJob(BatchJobSpec.of("Customer analytics", 5, 4, GameTime.at(0, 12), GameTime.at(1, 12)), 20, stay = StayReason.RESIDENCY),
                    LocationJob(BatchJobSpec.of("Price calculation", 4, 3, GameTime.at(1, 0), GameTime.at(1, 7)), 1, remoteDuration = 3_000),
                    LocationJob(BatchJobSpec.of("Daily report", 4, 3, GameTime.at(1, 0), GameTime.at(1, 18)), 2),
                    LocationJob(BatchJobSpec.of("Search index", 4, 4, GameTime.at(1, 6), GameTime.at(2, 18)), 30),
                    LocationJob(BatchJobSpec.of("Image recognition training", 6, 8, GameTime.at(1, 12), GameTime.at(2, 22)), 5),
                    LocationJob(BatchJobSpec.of("Daily report", 4, 3, GameTime.at(2, 0), GameTime.at(2, 18)), 2),
                    LocationJob(BatchJobSpec.of("Price calculation", 4, 3, GameTime.at(2, 0), GameTime.at(2, 7)), 1, remoteDuration = 3_000),
                ),
            ),
        )

        /**
         * Scenario 12, "Green on paper" (shown as 11, before the capstone): a week of the web shop in Germany, with batch
         * jobs that can wait. The board wants "green IT"; the electricity contract decides what the report shows, but
         * only clean energy in the same hour lowers the CO₂ the stars count. The budget for electricity may rise by 10 %.
         */
        val GREEN_ON_PAPER = Scenario(
            number = 12, label = "11", unlockAfter = 10,
            title = "Green on paper", setting = GridSetting(GridRegion.GERMANY, Season.AUTUMN), days = 7,
            weather = WeatherMode.Days(listOf(1_150, 650, 1_300, 800, 1_250, 600, 1_000, 1_000)),
            seed = 120, fleet = listOf(FleetEntry(ServerKind.CPU_CURRENT, 40)),
            interactive = InteractiveLoad(peak = 240),
            controls = setOf(Control.CONTRACT, Control.CAPACITY_RULES, Control.BATCH_RULES),
            initialPolicy = Policy(CapacityMode.Autoscale(500), BatchMode.Asap),
            referencePolicy = Policy(CapacityMode.Autoscale(500), BatchMode.Asap),
            goals = StarGoals(600, SpecialGoal.Reduction(750)),
            batchLimit = 12,
            batchJobs = listOf(
                BatchJobSpec.of("Full backup", 8, 14, GameTime.at(0, 1), GameTime.at(2, 23)),
                BatchJobSpec.of("Data warehouse reload", 10, 12, GameTime.at(1, 2), GameTime.at(3, 20)),
                BatchJobSpec.of("Recommendations", 8, 16, GameTime.at(2, 6), GameTime.at(4, 22)),
                BatchJobSpec.of("Billing run", 12, 10, GameTime.at(3, 0), GameTime.at(5, 12)),
                BatchJobSpec.of("Search index", 8, 10, GameTime.at(4, 6), GameTime.at(6, 18)),
                BatchJobSpec.of("Full backup", 8, 14, GameTime.at(5, 1), GameTime.at(6, 23)),
            ),
            contract = ContractModel(ppaEnergy = 2_038_948_500L),
        )

        /**
         * Scenario 11, "Dark doldrums": a week in a German winter with hardly any wind or sun – until the wind returns
         * on day 6. Everything together: the web shop the batch jobs (three of them can
         * wait for the wind, three cannot) and an AI training due at the end of the week. Until day 4, 12:00 the
         * forecast expects the wind a day early; a plan made with it puts the waiting jobs into a still dirty day.
         */
        val DARK_DOLDRUMS = Scenario(
            number = 11, label = "12", unlockAfter = 12, title = "Dark doldrums", setting = GridSetting(GridRegion.GERMANY, Season.WINTER), days = 7,
            // Day 8 (after the week) is calm again: the forecast that sees the weather a day early shows it on day 7.
            weather = WeatherMode.Days(listOf(1_300, 1_400, 1_450, 1_400, 1_350, 600, 450, 1_300)),
            forecastMiss = ForecastMiss(shift = -GameTime.TICKS_PER_DAY, revisedAt = GameTime.at(3, 12).tick),
            seed = 11, fleet = listOf(FleetEntry(ServerKind.CPU_CURRENT, 40), FleetEntry(ServerKind.GPU_CURRENT, 4)),
            interactive = InteractiveLoad(peak = 240),
            controls = setOf(Control.CAPACITY_RULES, Control.BATCH_RULES, Control.TRAINING),
            initialPolicy = Policy(CapacityMode.AllOn, BatchMode.Asap, training = TrainingConfig(servers = 4)),
            referencePolicy = Policy(CapacityMode.AllOn, BatchMode.Asap, training = TrainingConfig(servers = 4)),
            goals = StarGoals(400, SpecialGoal.Reduction(505)),
            batchLimit = 16,
            training = TrainingModel(work = 90_000),
            batchJobs = listOf(
                BatchJobSpec.of("Full backup", 8, 10, GameTime.at(0, 2), GameTime.at(6, 8)),
                BatchJobSpec.of("Search index", 6, 6, GameTime.at(0, 8), GameTime.at(2, 8)),
                BatchJobSpec.of("Billing run", 12, 8, GameTime.at(1, 0), GameTime.at(6, 20)),
                BatchJobSpec.of("Data warehouse reload", 10, 8, GameTime.at(2, 6), GameTime.at(6, 23)),
                BatchJobSpec.of("Payroll", 6, 4, GameTime.at(3, 0), GameTime.at(4, 6)),
                BatchJobSpec.of("Recommendations", 6, 5, GameTime.at(4, 12), GameTime.at(5, 18)),
            ),
        )

        /** The 24 VMs of scenario 5. */
        private val SHOP_VMS = List(8) { VmKind.FRONT_END } + List(6) { VmKind.BATCH_WORKER } + List(4) { VmKind.REPORTING } +
            List(2) { VmKind.DATABASE } + List(4) { VmKind.CACHE }

        /**
         * Scenario 5, "Consolidation": the web shop's VMs, spread over ten hosts by the previous team. Packed
         * tighter, fewer hosts run – but the RAM is full before the CPU, and VMs whose peaks come at the same time
         * do not fit together (bin packing in two dimensions, with daily profiles).
         */
        val CONSOLIDATION = Scenario(
            number = 5, title = "Consolidation", setting = GridSetting(GridRegion.GERMANY, Season.AUTUMN), days = 1,
            seed = 5, fleet = listOf(FleetEntry(ServerKind.CPU_CURRENT, 10)),
            interactive = InteractiveLoad(peak = 0),
            controls = setOf(Control.PLACEMENT),
            initialPolicy = Policy(CapacityMode.Placed(Packing.spread(SHOP_VMS, 10))),
            referencePolicy = Policy(CapacityMode.Placed(Packing.spread(SHOP_VMS, 10))),
            goals = StarGoals(100, SpecialGoal.MaxServersOn(6)),
            vms = SHOP_VMS,
        )

        /**
         * E1, "Measure first": the web shop's code. A team with 8 story points per sprint and a backlog of
         * improvements; each game day stands for the week after a sprint. The team's estimates of where the CPU
         * time goes are wrong – profiling shows it. Autoscaling turns less work per request into fewer servers.
         */
        val MEASURE_FIRST = Scenario(
            number = 101, label = "E1", track = Track.ENGINEERING, unlockAfter = 1,
            title = "Measure first", setting = GridSetting(GridRegion.GERMANY, Season.AUTUMN), days = 4,
            seed = 11, fleet = listOf(FleetEntry(ServerKind.CPU_CURRENT, 20)),
            interactive = InteractiveLoad(peak = 240),
            controls = setOf(Control.SPRINTS),
            initialPolicy = Policy(CapacityMode.Autoscale(700)),
            referencePolicy = Policy(CapacityMode.Autoscale(700)),
            goals = StarGoals(300, SpecialGoal.Reduction(400)),
            hotSpots = listOf(
                HotSpot("Database: N+1 queries in the product list", 300),
                HotSpot("JSON for the product catalog, built on every request", 220),
                HotSpot("Logging at DEBUG level", 120),
                HotSpot("Sessions and encryption", 100),
                HotSpot("Image resizing on every request", 80),
                HotSpot("Checkout", 50),
                HotSpot("Framework and the rest", 130),
            ),
            backlog = listOf(
                BacklogItem("Profile the shop", 3, 0, 0, profiling = true),
                BacklogItem("Cache the resized images", 3, 300, 70, hotSpot = 4),
                BacklogItem("Rewrite the checkout in a compiled language", 8, 400, 40, hotSpot = 5),
                BacklogItem("Upgrade the web framework", 5, 100, 30, hotSpot = 6),
                BacklogItem("Cache the catalog responses", 8, 100, 180, hotSpot = 1),
                BacklogItem("Fix the N+1 queries in the product list", 5, 50, 240, hotSpot = 0),
                BacklogItem("Log at INFO instead of DEBUG in production", 1, 10, 100, hotSpot = 2),
                BacklogItem("Lazy-load the product images", 3, 200, 10, hotSpot = 4),
                BacklogItem("Cache the session lookups", 5, 150, 40, hotSpot = 3),
                BacklogItem("Reuse TLS sessions", 2, 30, 50, hotSpot = 3),
                BacklogItem("Update to the current JVM version", 2, 50, 40, hotSpot = 6),
                BacklogItem("Precompute the shipping costs", 5, 120, 10, hotSpot = 5),
            ),
            sprintPoints = 8,
        )

        /**
         * E2, "Growing data": the shop's data grows by 15 % a month; each game day stands for a month, one sprint
         * per month with 3 story points for performance (the rest goes into features). Parts of the work grow
         * with the data – a lookup in a list, a query without an index, a nightly job that compares all pairs.
         * Measured today they hardly matter; in a year they dominate.
         */
        val GROWING_DATA = Scenario(
            number = 102, label = "E2", track = Track.ENGINEERING, unlockAfter = 101,
            title = "Growing data", setting = GridSetting(GridRegion.GERMANY, Season.AUTUMN), days = 12,
            seed = 12, fleet = listOf(FleetEntry(ServerKind.CPU_CURRENT, 80)),
            interactive = InteractiveLoad(peak = 240),
            controls = setOf(Control.SPRINTS),
            initialPolicy = Policy(CapacityMode.Autoscale(700)),
            referencePolicy = Policy(CapacityMode.Autoscale(700)),
            goals = StarGoals(150, SpecialGoal.Reduction(240)),
            growth = GrowthModel(
                ratePermille = 150, constant = 920,
                parts = listOf(
                    GrowthPart("Product lookup in a list", 40, Complexity.LINEAR, Complexity.CONSTANT, 35),
                    GrowthPart("Order history without an index", 40, Complexity.LINEAR, Complexity.LOGARITHMIC, 35),
                    GrowthPart("Search index, rebuilt every night", 40, Complexity.QUADRATIC, Complexity.LINEAR, 8, night = true),
                ),
            ),
            backlog = listOf(
                BacklogItem("Add an index to the order table", 2, fixes = 1),
                BacklogItem("Use a hash map for the product lookup", 3, fixes = 0),
                BacklogItem("Update the search index incrementally instead of comparing all pairs", 8, fixes = 2),
                BacklogItem("Faster string handling in the templates", 3, effect = 40),
                BacklogItem("Upgrade the JSON library", 2, effect = 30),
                BacklogItem("Rewrite the image service in a compiled language", 8, effect = 60),
            ),
            sprintPoints = 2,
        )

        /**
         * E3, "Hit or miss": caching in the web shop (Green IT LE06). Five kinds of data – product images, catalog
         * pages, exchange rates, search results, account balances – each with its own cost, popularity, size and
         * allowed age. All cached kinds share the memory of the cache servers. Caching saves work while it hits; a
         * cache server draws power all the time, an answer older than allowed is wrong, an account balance must never
         * come from the cache, and most searches are asked only once. The team has ordered four cache servers. On day
         * 2 at 18:00 the prices change: catalog pages in the cache are wrong until they expire – or are invalidated.
         * Two more measures: counting unique visitors approximately (HyperLogLog) and a materialized view for the
         * sales dashboard.
         */
        val HIT_OR_MISS = Scenario(
            number = 103, label = "E3", track = Track.ENGINEERING, unlockAfter = 102,
            title = "Hit or miss", setting = GridSetting(GridRegion.GERMANY, Season.AUTUMN), days = 2,
            seed = 13, fleet = listOf(FleetEntry(ServerKind.CPU_CURRENT, 30)),
            interactive = InteractiveLoad(peak = 240),
            controls = setOf(Control.CACHING),
            // The team has ordered four cache servers, to be on the safe side; nothing is cached yet.
            initialPolicy = Policy(CapacityMode.Autoscale(700), cache = CacheConfig(servers = CacheModel.MAX_SERVERS)),
            referencePolicy = Policy(CapacityMode.Autoscale(700)),
            goals = StarGoals(320, SpecialGoal.Reduction(380)),
            caching = CacheModel(
                kinds = listOf(
                    CacheKind("Product images", 300, 300, 20_000, 2_000, 1_440, note = "20,000 images of about 2 MB each."),
                    CacheKind("Catalog pages", 300, 1_400, 50_000, 500, 15, note = "50,000 pages of about 500 KB, expensive to build; new prices on day 2 at 18:00."),
                    CacheKind("Exchange rates", 100, 800, 20, 1, 1, note = "20 currencies, fetched from the bank."),
                    CacheKind("Search results", 150, 1_000, 300_000, 400, 60, unique = 850, note = "Pages of about 400 KB; most searches are typed only once."),
                    CacheKind("Account balance before a transfer", 50, 600, 200_000, 10, 0, note = "Read from the core banking system."),
                ),
                other = 300,
                exactCount = 40, approximateCount = 3,
                report = 12, reportWithView = 2,
                // Day 2, 18:00: new prices in the whole catalog.
                priceChange = GameTime.at(1, 18).tick, priceKind = 1,
                // About 460,000 page variants (language, currency, sort order) at 1.4 thousandths of a unit each.
                warmUpWork = 40,
            ),
            staleLimit = 5,
        )

        /**
         * E6, "Right-sized AI": the shop assistant. The team's prototype sends every question to the large model, one at
         * a time. Many questions need no AI at all (the order status is a lookup), some are answered as well by a small
         * model; the large model is worth it where it adds quality. Batching shares the GPU's work among questions but
         * makes them wait; reasoning helps complaints a little and makes them wait long.
         */
        val RIGHT_SIZED_AI = Scenario(
            number = 104, label = "E6", track = Track.ENGINEERING, unlockAfter = 107,
            title = "Right-sized AI", setting = GridSetting(GridRegion.GERMANY, Season.AUTUMN), days = 2,
            seed = 14, fleet = listOf(FleetEntry(ServerKind.CPU_CURRENT, 10)),
            interactive = InteractiveLoad(peak = 120),
            controls = setOf(Control.INFERENCE),
            initialPolicy = Policy(CapacityMode.Autoscale(700), inference = InferenceConfig(List(5) { Answerer.LARGE }, 0)),
            referencePolicy = Policy(CapacityMode.Autoscale(700), inference = InferenceConfig(List(5) { Answerer.LARGE }, 0)),
            goals = StarGoals(700, SpecialGoal.Reduction(760)),
            inference = InferenceModel(
                kinds = listOf(
                    QuestionKind("Where is my order?", 300, listOf(980, 950, 970, 970), "The status of one order, from the order database."),
                    QuestionKind("Greetings and thanks", 100, listOf(950, 970, 980, 980), "\"Hello\", \"Thank you\", \"Bye\"."),
                    QuestionKind("Questions about a product", 300, listOf(700, 880, 960, 970), "Size, material, care – the answer is in the product data."),
                    QuestionKind("Returns and complaints", 150, listOf(750, 780, 930, 960), "Needs the order, the return rules and a friendly tone."),
                    QuestionKind("Outfit advice", 150, listOf(600, 800, 950, 960), "What goes with these shoes? Needs taste and the whole catalog."),
                ),
                peakRate = 12, qualityTarget = 920, onTimeTarget = 950, gpuServers = 6,
            ),
        )

        /**
         * E4, "Data diet": the shop's pages weigh 12 MB a visit. Counted with the network and the visitors' devices, the
         * data center is the small part. Lighter images, no video that plays by itself, shorter lists and less tracking
         * cut the bytes – and make the page faster; two tempting changes make it slower.
         */
        val DATA_DIET = Scenario(
            number = 105, label = "E4", track = Track.ENGINEERING, unlockAfter = 103,
            title = "Data diet", setting = GridSetting(GridRegion.GERMANY, Season.AUTUMN), days = 2,
            seed = 15, fleet = listOf(FleetEntry(ServerKind.CPU_CURRENT, 30)),
            interactive = InteractiveLoad(peak = 240),
            controls = setOf(Control.PAGES),
            initialPolicy = Policy(CapacityMode.Autoscale(700)),
            referencePolicy = Policy(CapacityMode.Autoscale(700)),
            goals = StarGoals(550, SpecialGoal.Reduction(760)),
            web = WebModel(),
        )

        /**
         * E7, "Green pipeline": a week of a team of 40 developers. Every commit builds and runs all tests on 16 agents
         * that run day and night; six test environments with three replicas each never sleep. The team must get its
         * feedback within 20 minutes, and the UI tests must have run before Friday's release.
         */
        val GREEN_PIPELINE = Scenario(
            number = 106, label = "E7", track = Track.ENGINEERING, unlockAfter = 104,
            title = "Green pipeline", setting = GridSetting(GridRegion.GERMANY, Season.AUTUMN), days = 7,
            seed = 16, fleet = listOf(FleetEntry(ServerKind.CPU_CURRENT, 34)),
            interactive = InteractiveLoad(peak = 0),
            controls = setOf(Control.PIPELINE),
            initialPolicy = Policy(CapacityMode.Manual(0)),
            referencePolicy = Policy(CapacityMode.Manual(0)),
            goals = StarGoals(600, SpecialGoal.Reduction(780)),
            pipeline = PipelineModel(),
        )

        /**
         * E5, "Chatty services": the logistics system. Four microservices talk REST with JSON, the routes service asks the
         * truck and customer services and two external providers on every tracking request. The tracking is too slow;
         * the provider's positions may be one minute old, the traffic information five.
         */
        val CHATTY_SERVICES = Scenario(
            number = 107, label = "E5", track = Track.ENGINEERING, unlockAfter = 105,
            title = "Chatty services", setting = GridSetting(GridRegion.GERMANY, Season.AUTUMN), days = 2,
            seed = 17, fleet = listOf(FleetEntry(ServerKind.CPU_CURRENT, 30)),
            interactive = InteractiveLoad(peak = 0),
            controls = setOf(Control.SERVICES),
            initialPolicy = Policy(CapacityMode.Manual(0)),
            referencePolicy = Policy(CapacityMode.Manual(0)),
            goals = StarGoals(380, SpecialGoal.Reduction(455)),
            services = ServiceModel(),
        )

        /**
         * P1, "Inventory": eighteen applications in the own data center, in the cloud and as SaaS. Each hosting path
         * has its own source of data; better data costs from the same budget as the measures.
         */
        val INVENTORY = Scenario(
            number = 201, label = "P1", track = Track.PORTFOLIO, unlockAfter = 101,
            title = "Inventory", setting = GridSetting(GridRegion.GERMANY, Season.YEAR), days = 1,
            seed = 21, fleet = emptyList(), interactive = InteractiveLoad(peak = 0), controls = emptySet(),
            goals = StarGoals(200, SpecialGoal.Reduction(250)),
            portfolio = PortfolioModel(
                budget = 150_000, meterKWh = 1_000_000, gridGrams = 400,
                apps = listOf(
                    PortfolioApp("ERP", Hosting.ON_PREM, "8 large database servers, busy around the clock", 900_000, 104_000,
                        Measure("Move to new, more efficient servers", 120_000, 35), servers = 8, cpuPermille = 300),
                    PortfolioApp("Data warehouse", Hosting.ON_PREM, "10 servers: loads at night, reports by day", 400_000, 120_000,
                        Measure("Archive old data, shorten the loads", 60_000, 30), servers = 10, cpuPermille = 450),
                    PortfolioApp("Intranet and wiki", Hosting.ON_PREM, "24 small servers", 80_000, 24_000,
                        Measure("Consolidate onto 4 servers", 40_000, 50), servers = 24, cpuPermille = 50),
                    PortfolioApp("Test environments", Hosting.ON_PREM, "8 older servers, on around the clock, used on weekdays 8–18", 60_000, 88_000,
                        Measure("Switch off at night and at weekends", 15_000, 60), servers = 8, cpuPermille = 30),
                    PortfolioApp("Mail", Hosting.ON_PREM, "8 servers", 120_000, 24_000,
                        Measure("Archive old mailboxes", 20_000, 20), servers = 8, cpuPermille = 70),
                    PortfolioApp("File server", Hosting.ON_PREM, "12 servers with 400 TB of disks", 70_000, 40_000,
                        Measure("Delete duplicates and old files", 25_000, 40), servers = 12, cpuPermille = 100),
                    PortfolioApp("Web shop", Hosting.CLOUD, "Autoscaling, region Frankfurt", 350_000, 70_000,
                        Measure("Rightsize the instances", 20_000, 30)),
                    PortfolioApp("Analytics platform", Hosting.CLOUD, "Cluster of 120 VMs around the clock, region Frankfurt", 300_000, 120_000,
                        Measure("Move to a region with clean power (Stockholm)", 30_000, 80)),
                    PortfolioApp("Recommendation model", Hosting.CLOUD, "Trained every night on GPUs, region Frankfurt", 250_000, 90_000,
                        Measure("Smaller model, trained once a week", 40_000, 50)),
                    PortfolioApp("Customer portal", Hosting.CLOUD, "Region Frankfurt", 90_000, 25_000,
                        Measure("Add caching", 20_000, 30)),
                    PortfolioApp("Backup storage", Hosting.CLOUD, "2 PB, region Frankfurt", 60_000, 15_000,
                        Measure("Shorter retention, cold storage class", 10_000, 50)),
                    PortfolioApp("Build pipeline", Hosting.CLOUD, "Build agents always on, region Frankfurt", 110_000, 30_000,
                        Measure("Build agents only on demand", 15_000, 50)),
                    PortfolioApp("CRM", Hosting.SAAS, "1,500 users", 600_000, 15_000,
                        Measure("Clean up attachments and old records", 30_000, 30)),
                    PortfolioApp("Office and collaboration", Hosting.SAAS, "3,000 users", 400_000, 40_000,
                        Measure("Limit storage quotas", 20_000, 20)),
                    PortfolioApp("Video conferencing", Hosting.SAAS, "3,000 users, about 9,000 hours of meetings a week", 120_000, 45_000,
                        Measure("Camera off by default in large meetings, 720p", 10_000, 40)),
                    PortfolioApp("HR system", Hosting.SAAS, "3,000 employees", 150_000, 4_000,
                        Measure("Change to another supplier", 60_000, 30), supplierAnswers = false),
                    PortfolioApp("Cloud file sharing", Hosting.SAAS, "Marketing has stored its videos there for years", 60_000, 35_000,
                        Measure("Retention rule: delete old videos", 15_000, 60)),
                    PortfolioApp("Design tool", Hosting.SAAS, "120 licences", 90_000, 6_000,
                        Measure("Cancel unused licences", 10_000, 20)),
                ),
            ),
        )

        /**
         * P2, "Where to start": sixteen measures on a consultant's abatement cost curve, a budget for three years.
         */
        val WHERE_TO_START = Scenario(
            number = 202, label = "P2", track = Track.PORTFOLIO, unlockAfter = 201,
            title = "Where to start", setting = GridSetting(GridRegion.GERMANY, Season.YEAR), days = 1,
            seed = 22, fleet = emptyList(), interactive = InteractiveLoad(peak = 0), controls = emptySet(),
            goals = StarGoals(240, SpecialGoal.Reduction(260)),
            abatement = AbatementModel(
                budget = 200_000, footprintKgPerYear = 955_000,
                targets = mapOf("test" to 88_000L, "analytics" to 120_000L, "model" to 90_000L, "video" to 45_000L, "sharing" to 35_000L,
                    "pcs" to 60_000L, "erp" to 104_000L, "warehouse" to 120_000L, "build" to 30_000L, "backup" to 15_000L, "crm" to 15_000L, "mail" to 24_000L),
                measures = listOf(
                    AbatementMeasure("Switch the test environments off at night and at weekends", "Test environments", "8 servers, used on weekdays 8–18",
                        15_000, 53_000, 1, "test", 60),
                    AbatementMeasure("Move the test environments to the cloud, on demand", "Test environments", "The same 8 servers; the migration takes four quarters",
                        40_000, 70_000, 4, "test", 80),
                    AbatementMeasure("Move the analytics platform to Stockholm", "Analytics platform", "A region with clean power; the move takes two quarters",
                        30_000, 96_000, 2, "analytics", 80),
                    AbatementMeasure("Rightsize the analytics cluster from 120 to 60 VMs", "Analytics platform", "The same cluster",
                        20_000, 60_000, 1, "analytics", 50),
                    AbatementMeasure("Smaller recommendation model, trained once a week", "Recommendation model", "Two quarters of work for the data science team",
                        40_000, 45_000, 2, "model", 50),
                    AbatementMeasure("Camera off by default in large meetings", "Video conferencing", "A setting of the supplier",
                        10_000, 18_000, 1, "video", 40),
                    AbatementMeasure("Retention rule for the old videos", "Cloud file sharing", "Marketing has stored its videos there for years",
                        15_000, 21_000, 1, "sharing", 60),
                    AbatementMeasure("Replace the 600 oldest desktop PCs by laptops", "Workplace", "A laptop uses about 65 % less electricity; making one emits about 300 kg CO₂e",
                        45_000, 40_000, 2, "pcs", 65, embodiedKg = 180_000),
                    AbatementMeasure("Guarantees of origin for the own data center", "Own data center", "1,000 MWh a year from hydro plants that have run for decades; the market-based report counts them as zero",
                        40_000, 400_000, 0),
                    AbatementMeasure("Solar panels on the data center's roof", "Own data center", "100 kWp, about 95 MWh a year used on site; building takes four quarters",
                        120_000, 38_000, 4, onSiteKgPerYear = 38_000),
                    AbatementMeasure("ERP on new, more efficient servers", "ERP", "Making the 8 new servers emits about 10 t CO₂e; three quarters",
                        120_000, 36_000, 3, "erp", 35, embodiedKg = 10_000),
                    AbatementMeasure("Archive old data of the data warehouse", "Data warehouse", "Two quarters",
                        60_000, 36_000, 2, "warehouse", 30),
                    AbatementMeasure("Build agents only on demand", "Build pipeline", "A setting of the pipeline",
                        15_000, 15_000, 1, "build", 50),
                    AbatementMeasure("Shorter retention, cold storage class", "Backup storage", "A setting of the cloud provider",
                        10_000, 7_500, 1, "backup", 50),
                    AbatementMeasure("Clean up attachments and old records", "CRM", "One quarter",
                        30_000, 4_500, 1, "crm", 30),
                    AbatementMeasure("Archive old mailboxes", "Mail", "One quarter",
                        20_000, 4_800, 1, "mail", 20),
                ),
            ),
        )

        /** P3, "Tidy up with TIME": thirteen applications, four decisions, three projects at a time, three years. */
        val TIDY_UP = Scenario(
            number = 203, label = "P3", track = Track.PORTFOLIO, unlockAfter = 202,
            title = "Tidy up with TIME", setting = GridSetting(GridRegion.GERMANY, Season.YEAR), days = 1,
            seed = 23, fleet = emptyList(), interactive = InteractiveLoad(peak = 0), controls = emptySet(),
            goals = StarGoals(250, SpecialGoal.Reduction(300)),
            tidy = TidyModel(listOf(
                TidyApp("ERP", "The core of the company, on an old platform", true, false, 150, 900, 120_000,
                    Option("Tune the database, consolidate the servers", 4, 80), Option("A new ERP in the cloud", 6, 35), 2),
                TidyApp("Warehouse management", "Built in-house; one person still maintains the code", true, false, 300, 760, 70_000,
                    Option("Clean up the slowest batch jobs", 3, 85), Option("A standard SaaS for warehouses", 4, 30), 2),
                TidyApp("Old customer portal", "Its functions are being rebuilt on the web shop's platform", true, false, 380, 640, 50_000,
                    Option("Caching and smaller pages", 2, 80), Option("Move onto the web shop's platform", 2, 30), 2),
                TidyApp("Web shop", "Modern, well maintained", true, true, 820, 880, 90_000,
                    Option("Rightsizing and caching", 2, 65), Option("Rebuild as a new shop", 5, 60), 2),
                TidyApp("Data warehouse", "Modern, nightly loads", true, true, 700, 740, 110_000,
                    Option("Archive old data, shorten the loads", 3, 70), Option("A new analytics platform", 6, 50), 2),
                TidyApp("CRM", "Standard software, kept up to date", true, true, 880, 620, 30_000,
                    Option("Clean up old records", 1, 80), Option("Change to another CRM", 3, 50), 2),
                TidyApp("Old reporting system", "Most of its reports also exist in the data warehouse", false, false, 220, 330, 45_000,
                    Option("Archive old reports", 2, 80), Option("Rebuild the remaining reports", 3, 40), 2),
                TidyApp("Old intranet", "Hardly used since the new wiki", false, false, 120, 180, 30_000,
                    Option("Smaller servers", 2, 80), Option("Move the pages to the wiki", 2, 40), 1),
                TidyApp("Fax server", "Three faxes a week", false, false, 330, 90, 8_000,
                    Option("A smaller server", 1, 80), Option("Fax as a cloud service", 1, 40), 1),
                TidyApp("Old test environments", "Built for a project that ended two years ago", false, false, 80, 400, 60_000,
                    Option("Switch off at night", 1, 80), Option("Move to the cloud, on demand", 2, 40), 1),
                TidyApp("Wiki", "Its pages would fit into the office suite's own wiki", false, true, 700, 330, 10_000,
                    Option("Smaller servers", 1, 80), Option("Move to the office suite", 1, 50), 1),
                TidyApp("Time recording", "Runs smoothly", false, true, 860, 200, 15_000,
                    Option("Switch off at night", 1, 70), Option("The HR system's module", 2, 50), 1),
                TidyApp("Mail archive", "Mails must be kept for ten years by law", false, true, 620, 110, 25_000,
                    Option("Cold storage for old years", 1, 70), Option("An archive service in the cloud", 2, 40), 1,
                    mustKeep = "Kept for ten years by law"),
            ), example = mapOf(
                "Old test environments" to Choice(Decision.ELIMINATE, 0), "Old reporting system" to Choice(Decision.ELIMINATE, 0),
                "Old intranet" to Choice(Decision.ELIMINATE, 0), "Web shop" to Choice(Decision.INVEST, 1), "Fax server" to Choice(Decision.ELIMINATE, 2),
                "Wiki" to Choice(Decision.ELIMINATE, 1), "Data warehouse" to Choice(Decision.INVEST, 2), "Time recording" to Choice(Decision.ELIMINATE, 3),
                "Warehouse management" to Choice(Decision.MIGRATE, 3, true), "CRM" to Choice(Decision.INVEST, 4),
                "Old customer portal" to Choice(Decision.MIGRATE, 5, true), "ERP" to Choice(Decision.INVEST, 5),
            )),
        )

        /** P4, "Leave the cloud?": six applications, four places, three years; costs down, and CO₂? */
        val LEAVE_THE_CLOUD = Scenario(
            number = 204, label = "P4", track = Track.PORTFOLIO, unlockAfter = 203,
            title = "Leave the cloud?", setting = GridSetting(GridRegion.GERMANY, Season.YEAR), days = 1,
            seed = 24, fleet = emptyList(), interactive = InteractiveLoad(peak = 0), controls = emptySet(),
            goals = StarGoals(100, SpecialGoal.Reduction(200)),
            repatriation = RepatriationModel(
                listOf(
                    MoveApp("Web shop", "Campaigns bring up to five times the load", 7, 60, 22, 19,
                        week(listOf(20, 25, 30, 22), campaign = 2)),
                    MoveApp("ERP database", "Runs around the clock; the plant books on it", 16, 65, 13, 80,
                        week(listOf(80, 85, 85, 80)), queriesPerBooking = 200),
                    MoveApp("Customer data archive", "Storage, grows steadily", 8, 70, 9, 62,
                        week(listOf(70, 70, 72, 70)), germanyOnly = true),
                    MoveApp("Nightly runs", "Time and place do not matter", 2, 60, 6, 17,
                        week(listOf(100, 0, 0, 0)), usePercent = 20, useUtilPercent = 85, onDemandInCloud = true),
                    MoveApp("Test environments", "Run around the clock, used on workdays 9–17", 6, 12, 6, 12,
                        week(listOf(0, 0, 60, 10), weekend = listOf(0, 0, 0, 0)), usePercent = 30, useUtilPercent = 40),
                    MoveApp("Data warehouse", "Queries during the day", 8, 35, 9, 31,
                        week(listOf(10, 60, 70, 15), weekend = listOf(10, 15, 15, 10))),
                ),
                listOf(
                    PlaceSpec(Place.CLOUD_FRANKFURT, "Cloud Frankfurt", 115, 380, 0, 20, inGermany = true),
                    PlaceSpec(Place.CLOUD_STOCKHOLM, "Cloud Stockholm", 115, 30, 0, 140, inGermany = false),
                    PlaceSpec(Place.COLOCATION, "Colocation Stuttgart", 135, 380, 380, 5, inGermany = true),
                    PlaceSpec(Place.SERVER_ROOM, "Own server room", 170, 380, 380, 5, inGermany = true),
                ),
                example = mapOf(
                    "Web shop" to Move(Place.CLOUD_STOCKHOLM), "ERP database" to Move(Place.COLOCATION),
                    "Customer data archive" to Move(Place.COLOCATION), "Nightly runs" to Move(Place.CLOUD_STOCKHOLM),
                    "Test environments" to Move(Place.CLOUD_STOCKHOLM, offOutsideUse = true), "Data warehouse" to Move(Place.CLOUD_STOCKHOLM),
                ),
            ),
        )

        /** E8, "How many nines?": an insurer's portal, seven building blocks, five quality scenarios, a year. */
        val HOW_MANY_NINES = Scenario(
            number = 108, label = "E8", track = Track.ENGINEERING, unlockAfter = 106,
            title = "How many nines?", setting = GridSetting(GridRegion.GERMANY, Season.YEAR), days = 1,
            seed = 18, fleet = emptyList(), interactive = InteractiveLoad(peak = 0), controls = emptySet(),
            goals = StarGoals(200, SpecialGoal.Reduction(280)),
            availability = AvailabilityModel(
                blocks = listOf(
                    BuildingBlock("Claims intake", "Takes the reports of damage", 6, 25, 160, 160),
                    BuildingBlock("Contract overview", "Contracts and documents of the logged-in customer", 6, 30, 500, 160),
                    BuildingBlock("Tariff calculator", "Prices for new customers, without logging in", 8, 30, 840, 160),
                    BuildingBlock("Login", "Every logged-in function needs it", 4, 30, 200, 500),
                    BuildingBlock("Recommendations", "Offers that fit the customer's contracts", 10, 40, 760, 500),
                    BuildingBlock("Policy database", "All contracts and customers", 12, 50, 500, 840),
                    BuildingBlock("Reporting", "Nightly figures for the actuaries", 12, 35, 840, 840),
                ),
                scenarios = listOf(
                    QualityScenario("Report a claim", "After a storm thousands of customers report damage at the same time; the portal takes every report.",
                        263, listOf("Claims intake", "Login", "Policy database")),
                    QualityScenario("See contracts", "Customers look up their contracts and download documents.",
                        526, listOf("Contract overview", "Login", "Policy database", "Recommendations")),
                    QualityScenario("Calculate a tariff", "Prospective customers calculate a price, without logging in.",
                        526, listOf("Tariff calculator")),
                    QualityScenario("Get recommendations", "Logged-in customers see offers that fit their contracts.",
                        5_256, listOf("Recommendations")),
                    QualityScenario("Reports for actuaries", "Each morning the actuaries need yesterday's figures.",
                        5_256, listOf("Reporting")),
                ),
                decouplings = listOf(
                    Decoupling("queue", "Claims into a queue",
                        "A claim is stored in a queue at once and checked against the policy database later; the intake no longer waits for the database. The queue evens out the peaks: 3 servers instead of 6. The message broker runs on 2 servers, active-active.",
                        "Report a claim", "Policy database", brokerServers = 2, brokerUtilPercent = 20,
                        resized = "Claims intake", resizedServers = 3, resizedUtilPercent = 50),
                    Decoupling("fallback", "Contracts without recommendations when these fail",
                        "A circuit breaker: when the recommendations do not answer, the contract overview shows the contracts without them.",
                        "See contracts", "Recommendations"),
                ),
                example = Architecture(mapOf(
                    "Login" to Tactic.WARM_STANDBY, "Policy database" to Tactic.COLD_STANDBY, "Claims intake" to Tactic.WARM_STANDBY,
                    "Contract overview" to Tactic.WARM_STANDBY, "Tariff calculator" to Tactic.COLD_STANDBY,
                    "Recommendations" to Tactic.SINGLE, "Reporting" to Tactic.SINGLE,
                ), setOf("queue", "fallback")),
            ),
        )

        /** E9, "Constant asking": a delivery app on 400,000 phones, five kinds of updates, a year. */
        val CONSTANT_ASKING = Scenario(
            number = 109, label = "E9", track = Track.ENGINEERING, unlockAfter = 108,
            title = "Constant asking", setting = GridSetting(GridRegion.GERMANY, Season.YEAR), days = 1,
            seed = 19, fleet = emptyList(), interactive = InteractiveLoad(peak = 0), controls = emptySet(),
            goals = StarGoals(300, SpecialGoal.Reduction(450)),
            updates = UpdatesModel(
                kinds = listOf(
                    UpdateKind("Delivery status", "Six changes an order; \"The courier is at the door\" must reach the closed app too",
                        30_000L * 365, 4, 36, 60, 4, 20, openFreshness = 60, closedFreshness = 60,
                        openOptions = listOf(Open.Poll(10), Open.Poll(30), Open.Poll(60), Open.Stream, Open.Push),
                        closedOptions = listOf(Closed.NONE, Closed.REFRESH, Closed.SOCKET, Closed.PUSH)),
                    UpdateKind("Courier on the map", "A new position every 5 seconds while the map is open",
                        15_000L * 365, 3, 0, 360, 1, 10, openFreshness = 5, closedFreshness = null,
                        openOptions = listOf(Open.Poll(2), Open.Poll(5), Open.Poll(10), Open.Stream, Open.Push),
                        closedOptions = listOf(Closed.NONE)),
                    UpdateKind("Offers", "New offers twice a day; they must be there when the app is opened",
                        400_000L * 365, 6, 1_434, 20, 60, 15, openFreshness = null, closedFreshness = null,
                        openOptions = listOf(Open.Poll(60), Open.OnOpen, Open.Stream, Open.Push),
                        closedOptions = listOf(Closed.NONE, Closed.REFRESH, Closed.SOCKET, Closed.PUSH)),
                    UpdateKind("Support chat", "Answers must show within 5 seconds while the chat is open",
                        2_000L * 365, 8, 0, 100, 1, 10, openFreshness = 5, closedFreshness = null,
                        openOptions = listOf(Open.Poll(2), Open.Poll(5), Open.LongPoll, Open.Stream, Open.Push),
                        closedOptions = listOf(Closed.NONE)),
                    UpdateKind("Unread messages", "The number on the app icon: up to date within 15 minutes, within a minute while the app is open",
                        400_000L * 365, 6, 1_434, 5, 1, 15, openFreshness = 60, closedFreshness = 900,
                        openOptions = listOf(Open.Poll(30), Open.Poll(60), Open.OnOpen, Open.Stream, Open.Push),
                        closedOptions = listOf(Closed.NONE, Closed.REFRESH, Closed.SOCKET, Closed.PUSH)),
                ),
                phones = 400_000,
                reference = UpdatePlan(
                    open = mapOf("Delivery status" to Open.Poll(10), "Courier on the map" to Open.Poll(2), "Offers" to Open.Poll(60),
                        "Support chat" to Open.Poll(2), "Unread messages" to Open.Poll(30)),
                    closed = mapOf("Delivery status" to Closed.REFRESH, "Offers" to Closed.REFRESH, "Unread messages" to Closed.REFRESH),
                ),
                otherKg = 40_000,
                example = UpdatePlan(
                    open = mapOf("Delivery status" to Open.Push, "Courier on the map" to Open.Stream, "Offers" to Open.OnOpen,
                        "Support chat" to Open.Stream, "Unread messages" to Open.Push),
                    closed = mapOf("Delivery status" to Closed.PUSH, "Offers" to Closed.NONE, "Unread messages" to Closed.PUSH),
                    conditional = true, bundled = true,
                ),
            ),
        )

        /** A week of load, four values a day (the weekend may differ; one campaign day may reach the peak). */
        private fun week(day: List<Int>, weekend: List<Int> = day, campaign: Int? = null): List<Int> =
            (0 until 7).flatMap { d -> if (d == campaign) listOf(30, 100, 95, 35) else if (d >= 5) weekend else day }

        val ALL = listOf(NIGHT_SHIFT, WAIT_FOR_THE_SUN, WHEN_THE_WIND_DROPS, HEADROOM, CONSOLIDATION, HEAT_WAVE, OLD_IRON, TRAINING_RUN, ECO_MODE, TWO_SITES, GREEN_ON_PAPER, DARK_DOLDRUMS, MEASURE_FIRST, GROWING_DATA, HIT_OR_MISS, DATA_DIET, CHATTY_SERVICES, RIGHT_SIZED_AI, GREEN_PIPELINE, HOW_MANY_NINES, CONSTANT_ASKING, INVENTORY, WHERE_TO_START, TIDY_UP, LEAVE_THE_CLOUD)
    }
}
