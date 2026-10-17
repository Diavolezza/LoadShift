package de.wannerit.loadshift.core

/*
 * The electricity grid: carbon intensity over the day, weather and forecast (docs/CONCEPT.md, section 4).
 *
 * Everything here is a pure function of the region, the season, the scenario seed and the game time, so the
 * player's run, the reference operator and the forecast all see exactly the same grid. Integer arithmetic only.
 */

/** Grid region. Values of 2025 from Electricity Maps (life-cycle, flow-traced) [S3]. */
enum class GridRegion(
    val title: String,
    /** Mean of 2025, in tenths of g CO₂e/kWh. */
    val annualMean: Int,
    /** Lowest and highest daily mean of 2025, tenths of g CO₂e/kWh; the weather keeps a day between them. */
    val lowestDay: Int,
    val highestDay: Int,
    /**
     * Half width of the forecast band 24 hours ahead, per mille (game value; Google reports errors between 0.4 %
     * and 26 % depending on grid and lead time [S5]). Grids driven by wind and sun are harder to forecast.
     */
    val forecastError: Int,
) {
    NORWAY("Norway", 301, 261, 395, 40),
    FRANCE("France", 320, 158, 722, 80),
    SPAIN("Spain", 1_321, 611, 2_357, 120),
    GREAT_BRITAIN("Great Britain", 1_759, 702, 3_480, 250),
    GERMANY("Germany", 3_350, 1_110, 5_600, 200),
    POLAND("Poland", 6_766, 4_742, 8_862, 100),
}

enum class Season(val title: String) {
    WINTER("Winter"), SPRING("Spring"), SUMMER("Summer"), AUTUMN("Autumn"),

    /** The annual profile, where the source gives no seasons. */
    YEAR("Whole year"),
}

/** Region and season of a scenario. */
data class GridSetting(val region: GridRegion, val season: Season)

/** Carbon intensity at the full hours of a day, in tenths of g CO₂e/kWh (`GridCurves.kt`). */
class GridCurve internal constructor(val hourly: List<Int>) {
    init {
        require(hourly.size == 24) { "24 hourly values" }
    }

    /** Mean of the day, in tenths of g CO₂e/kWh. */
    val mean: Int get() = hourly.sum() / 24

    companion object {
        /** Curve of a region and season; a region without seasonal profiles uses its annual one. */
        fun of(setting: GridSetting): GridCurve =
            builtIn(setting.region, setting.season)
                ?: Season.entries.firstNotNullOfOrNull { builtIn(setting.region, it) }
                ?: error("no curve for ${setting.region}")
    }
}

/** How the weather of the scenario's days is chosen. */
sealed interface WeatherMode {
    /** Drawn from the seed for every day (usual case). */
    data object Drawn : WeatherMode

    /** The same factor every day, per mille of the curve ("a windless week": a factor above 1,000). */
    data class Fixed(val factor: Int) : WeatherMode

    /**
     * One factor per day of the scenario, per mille of the curve ("windy, normal, windless"); before the first and
     * after the last day the nearest one applies.
     */
    data class Days(val factors: List<Int>) : WeatherMode {
        init {
            require(factors.isNotEmpty()) { "at least one day" }
        }
    }
}

/**
 * A forecast that gets the weather's timing wrong until it is revised (scenario 3: "the wind drops earlier than
 * forecast"). Forecasts made before [revisedAt] see the weather [shift] ticks late: a positive shift means the
 * forecast expects a change later than it comes. The next hours are forecast well all the same; the error grows
 * over the first six hours of lead time.
 */
data class ForecastMiss(val shift: Int, val revisedAt: Int) {
    companion object {
        /** Lead time up to which a forecast is still close to the real value, ticks. */
        const val NEAR = 6 * GameTime.TICKS_PER_HOUR
    }
}

class Grid(
    val setting: GridSetting,
    private val seed: Long,
    val weatherMode: WeatherMode = WeatherMode.Drawn,
    val forecastMiss: ForecastMiss? = null,
    /** O7: one season per day (a day stands for a quarter); null: the setting's season every day. */
    val seasons: List<Season>? = null,
) {
    val curve: GridCurve = GridCurve.of(setting)

    /** The curve of a day: its season's, or the setting's. */
    private val curves: List<GridCurve>? = seasons?.map { GridCurve.of(GridSetting(setting.region, it)) }

    private fun curveOf(tick: Int): GridCurve = curves?.let { it[(tick / GameTime.TICKS_PER_DAY).coerceIn(0, it.size - 1)] } ?: curve

    /** Range of the drawn weather factor, per mille of the curve. */
    internal val weatherLow: Int
    internal val weatherHigh: Int

    init {
        val mean = maxOf(1, curve.mean)
        weatherLow = minOf(1_000, setting.region.lowestDay * 1_000 / mean)
        weatherHigh = maxOf(1_000, setting.region.highestDay * 1_000 / mean)
    }

    // Weather

    /**
     * Weather of a day, per mille of the curve: it scales the whole day. Drawn from the seed, usually close to
     * 1,000, at the extremes as low or as high as the lowest and highest day of 2025.
     */
    fun weather(day: Int): Int {
        val mode = weatherMode
        if (mode is WeatherMode.Fixed) return mode.factor
        if (mode is WeatherMode.Days) return mode.factors[day.coerceIn(0, mode.factors.size - 1)]
        // Two uniform draws give a triangular distribution around the middle.
        val t = SeededRandom.value(seed, 1, day, 1_001) + SeededRandom.value(seed, 2, day, 1_001) - 1_000
        return if (t >= 0) 1_000 + t * (weatherHigh - 1_000) / 1_000
        else 1_000 + t * (1_000 - weatherLow) / 1_000
    }

    /**
     * Weather at a tick: the factor of each day applies at noon, in between it changes linearly, so the curve has
     * no jump at midnight.
     */
    internal fun weatherFactor(tick: Int): Int {
        val perDay = GameTime.TICKS_PER_DAY
        val x = tick - perDay / 2
        val day = x.floorDiv(perDay)
        val r = x - day * perDay
        return (weather(day) * (perDay - r) + weather(day + 1) * r) / perDay
    }

    // Carbon intensity

    /** Carbon intensity at a tick in tenths of g CO₂e/kWh: the curve, linear between the full hours, times the weather. */
    fun intensityTenths(tick: Int): Int = intensityTenths(tick, weatherFactor(tick))

    /** The curve at a tick with a given weather factor. */
    private fun intensityTenths(tick: Int, weather: Int): Int {
        val perHour = GameTime.TICKS_PER_HOUR
        val inDay = tick % GameTime.TICKS_PER_DAY
        val hour = inDay / perHour
        val r = inDay % perHour
        val c = curveOf(tick)
        val base = c.hourly[hour] * (perHour - r) + c.hourly[(hour + 1) % 24] * r
        val denominator = perHour * 1_000
        return (base * weather + denominator / 2) / denominator
    }

    /** Carbon intensity at a tick, g CO₂e/kWh (rounded). */
    fun intensity(tick: Int): Int = (intensityTenths(tick) + 5) / 10

    // Forecast

    /**
     * Half width of the forecast band, per mille, for a lead time in ticks: zero for now, the region's forecast
     * error 24 hours ahead and beyond.
     */
    fun band(lead: Int): Int =
        setting.region.forecastError * lead.coerceIn(0, GameTime.TICKS_PER_DAY) / GameTime.TICKS_PER_DAY

    /**
     * Forecast made at tick [madeAt] for tick [tick], in tenths of g CO₂e/kWh. The error of a target time is drawn
     * once; it shrinks with the lead time, so the forecast meets the real value when the time comes. The real value
     * lies within the band around the forecast.
     */
    fun forecastTenths(tick: Int, madeAt: Int): Int {
        val actual = intensityTenths(tick)
        val miss = forecastMiss
        val expected = if (miss != null && madeAt < miss.revisedAt) {
            val wrong = intensityTenths(tick, weatherFactor(tick - miss.shift))
            val w = (tick - madeAt).coerceIn(0, ForecastMiss.NEAR)
            (actual * (ForecastMiss.NEAR - w) + wrong * w) / ForecastMiss.NEAR
        } else {
            actual
        }
        // Noise in -700..700, drawn every three hours and linear in between, so the forecast curve stays smooth.
        val knot = 3 * GameTime.TICKS_PER_HOUR
        val index = tick.floorDiv(knot)
        val r = tick - index * knot
        val noise = (forecastNoise(index) * (knot - r) + forecastNoise(index + 1) * r) / knot
        val error = noise * band(tick - madeAt) / 1_000
        return (expected * (1_000 + error) + 500) / 1_000
    }

    /** Forecast in g CO₂e/kWh (rounded). */
    fun forecast(tick: Int, madeAt: Int): Int = (forecastTenths(tick, madeAt) + 5) / 10

    private fun forecastNoise(index: Int): Int = SeededRandom.symmetric(seed, 3, index, 700)
}
