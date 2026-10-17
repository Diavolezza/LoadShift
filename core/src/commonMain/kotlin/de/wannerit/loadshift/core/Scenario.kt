package de.wannerit.loadshift.core

/** The two strands of the campaign (docs/CONCEPT.md 9, D17). */
enum class Track { OPERATIONS, ENGINEERING }

/** A scenario: region, season, length, fleet, load, the controls it unlocks and its goals (docs/CONCEPT.md 8.3 and 9). */
data class FleetEntry(val kind: ServerKind, val count: Int)

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

    /** Scenario 6: the inlet temperature setpoint of the cooling. */
    COOLING,
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
    /** E3: stale answers allowed for the first star, per mille of the requests. */
    val staleLimit: Int? = null,
    /** Scenario 6: cooling with outside temperature and setpoint instead of a fixed PUE. */
    val cooling: CoolingModel? = null,
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
        val rules = totals.sla >= slaTarget && totals.deadlinesMissed == 0 && totals.emissions < reference.emissions &&
            (staleLimit == null || totals.stalePermille <= staleLimit)
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
         * come from the cache, and most searches are asked only once. The team has ordered four cache servers.
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
            goals = StarGoals(320, SpecialGoal.Reduction(400)),
            caching = CacheModel(
                kinds = listOf(
                    CacheKind("Product images", 300, 300, 20_000, 2_000, 1_440, note = "20,000 images of about 2 MB each."),
                    CacheKind("Catalog pages", 300, 1_400, 50_000, 500, 15, note = "50,000 pages of about 500 KB, expensive to build."),
                    CacheKind("Exchange rates", 100, 800, 20, 1, 1, note = "20 currencies, fetched from the bank."),
                    CacheKind("Search results", 150, 1_000, 300_000, 400, 60, unique = 850, note = "Pages of about 400 KB; most searches are typed only once."),
                    CacheKind("Account balance before a transfer", 50, 600, 200_000, 10, 0, note = "Read from the core banking system."),
                ),
                other = 300,
                exactCount = 40, approximateCount = 3,
                report = 12, reportWithView = 2,
            ),
            staleLimit = 1,
        )

        val ALL = listOf(NIGHT_SHIFT, WAIT_FOR_THE_SUN, WHEN_THE_WIND_DROPS, HEADROOM, CONSOLIDATION, HEAT_WAVE, MEASURE_FIRST, GROWING_DATA, HIT_OR_MISS)
    }
}
