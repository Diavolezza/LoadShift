/// The electricity grid: carbon intensity over the day, weather and forecast (docs/CONCEPT.md, section 4).
///
/// Everything here is a pure function of the region, the season, the scenario seed and the game time, so the
/// player's run, the reference operator and the forecast preview all see exactly the same grid.
/// Integer arithmetic only.

/// Grid region. Values of 2025 from Electricity Maps (life-cycle, flow-traced) [S3].
public enum GridRegion: Int, CaseIterable, Sendable, Codable {
    case norway, france, spain, greatBritain, germany, poland

    public var name: String {
        switch self {
        case .norway: "Norway"
        case .france: "France"
        case .spain: "Spain"
        case .greatBritain: "Great Britain"
        case .germany: "Germany"
        case .poland: "Poland"
        }
    }

    /// Mean of 2025, in tenths of g CO₂e/kWh.
    public var annualMean: Int {
        switch self {
        case .norway: 301
        case .france: 320
        case .spain: 1_321
        case .greatBritain: 1_759
        case .germany: 3_350
        case .poland: 6_766
        }
    }

    /// Lowest and highest daily mean of 2025, in tenths of g CO₂e/kWh. The weather keeps the mean of a game
    /// day between them.
    public var dayRange: (lowest: Int, highest: Int) {
        switch self {
        case .norway: (261, 395)
        case .france: (158, 722)
        case .spain: (611, 2_357)
        case .greatBritain: (702, 3_480)
        case .germany: (1_110, 5_600)
        case .poland: (4_742, 8_862)
        }
    }

    /// Half width of the forecast band 24 hours ahead, per mille (game value; Google reports errors between
    /// 0.4 % and 26 % depending on grid and lead time [S5]). Grids driven by wind and sun are harder to forecast.
    public var forecastError: Int {
        switch self {
        case .norway: 40
        case .france: 80
        case .spain: 120
        case .greatBritain: 250
        case .germany: 200
        case .poland: 100
        }
    }
}

public enum Season: Int, CaseIterable, Sendable, Codable {
    case winter, spring, summer, autumn
    /// The annual profile, where the source gives no seasons.
    case year

    public var name: String {
        switch self {
        case .winter: "Winter"
        case .spring: "Spring"
        case .summer: "Summer"
        case .autumn: "Autumn"
        case .year: "Whole year"
        }
    }
}

/// Region and season of a scenario.
public struct GridSetting: Equatable, Hashable, Sendable, Codable {
    public let region: GridRegion
    public let season: Season

    public init(_ region: GridRegion, _ season: Season) {
        self.region = region
        self.season = season
    }
}

/// Carbon intensity at the full hours of a day, in tenths of g CO₂e/kWh (`GridCurves.swift`).
public struct GridCurve: Equatable, Sendable {
    public let hourly: [Int]

    init(hourly: [Int]) {
        precondition(hourly.count == 24, "24 hourly values")
        self.hourly = hourly
    }

    /// Mean of the day, in tenths of g CO₂e/kWh.
    public var mean: Int { hourly.reduce(0, +) / 24 }

    /// Curve of a region and season; a region without seasonal profiles uses its annual one.
    public static func of(_ setting: GridSetting) -> GridCurve {
        if let curve = builtIn(setting.region, setting.season) { return curve }
        for season in Season.allCases {
            if let curve = builtIn(setting.region, season) { return curve }
        }
        fatalError("no curve for \(setting.region)")
    }
}

/// How the weather of the scenario's days is chosen.
public enum WeatherMode: Equatable, Sendable, Codable {
    /// Drawn from the seed for every day (usual case).
    case drawn
    /// The same factor every day, per mille of the curve ("a windless week": a factor above 1,000).
    case fixed(Int)
}

public struct Grid: Sendable {
    public let setting: GridSetting
    public let curve: GridCurve
    public let weatherMode: WeatherMode
    let seed: UInt64
    /// Range of the drawn weather factor, per mille of the curve.
    let weatherLow: Int
    let weatherHigh: Int

    public init(setting: GridSetting, seed: UInt64, weather: WeatherMode = .drawn) {
        self.setting = setting
        self.curve = GridCurve.of(setting)
        self.seed = seed
        self.weatherMode = weather
        let mean = max(1, curve.mean)
        let range = setting.region.dayRange
        weatherLow = min(1_000, range.lowest * 1_000 / mean)
        weatherHigh = max(1_000, range.highest * 1_000 / mean)
    }

    // MARK: - Weather

    /// Weather of a day, per mille of the curve: it scales the whole day. Drawn from the seed, usually close
    /// to 1,000, at the extremes as low or as high as the lowest and highest day of 2025.
    public func weather(day: Int) -> Int {
        if case .fixed(let factor) = weatherMode { return factor }
        // Two uniform draws give a triangular distribution around the middle.
        let t = SeededRandom.value(seed: seed, salt: 1, index: day, range: 1_001)
            + SeededRandom.value(seed: seed, salt: 2, index: day, range: 1_001) - 1_000
        return t >= 0 ? 1_000 + t * (weatherHigh - 1_000) / 1_000
                      : 1_000 + t * (1_000 - weatherLow) / 1_000
    }

    /// Weather at a tick: the factor of each day applies at noon, in between it changes linearly, so the
    /// curve has no jump at midnight.
    func weatherFactor(tick: Int) -> Int {
        let perDay = GameTime.ticksPerDay
        let x = tick - perDay / 2
        let day = floorDiv(x, perDay)
        let r = x - day * perDay
        return (weather(day: day) * (perDay - r) + weather(day: day + 1) * r) / perDay
    }

    // MARK: - Carbon intensity

    /// Carbon intensity at a tick in tenths of g CO₂e/kWh: the curve, linear between the full hours, times
    /// the weather.
    public func intensityTenths(tick: Int) -> Int {
        let perHour = GameTime.ticksPerHour
        let inDay = tick % GameTime.ticksPerDay
        let hour = inDay / perHour
        let r = inDay % perHour
        let base = curve.hourly[hour] * (perHour - r) + curve.hourly[(hour + 1) % 24] * r
        let denominator = perHour * 1_000
        return (base * weatherFactor(tick: tick) + denominator / 2) / denominator
    }

    /// Carbon intensity at a tick, g CO₂e/kWh (rounded).
    public func intensity(tick: Int) -> Int { (intensityTenths(tick: tick) + 5) / 10 }

    // MARK: - Forecast

    /// Half width of the forecast band, per mille, for a lead time in ticks: zero for now, the region's
    /// forecast error 24 hours ahead and beyond.
    public func band(lead: Int) -> Int {
        setting.region.forecastError * min(max(0, lead), GameTime.ticksPerDay) / GameTime.ticksPerDay
    }

    /// Forecast made at tick `now` for tick `target`, in tenths of g CO₂e/kWh. The error of a target time is
    /// drawn once; it shrinks with the lead time, so the forecast meets the real value when the time comes.
    /// The real value lies within the band around the forecast.
    public func forecastTenths(tick target: Int, madeAt now: Int) -> Int {
        let actual = intensityTenths(tick: target)
        // Noise in -700 … 700, drawn every three hours and linear in between, so the forecast curve stays smooth.
        let knot = 3 * GameTime.ticksPerHour
        let index = floorDiv(target, knot)
        let r = target - index * knot
        let noise = (forecastNoise(index) * (knot - r) + forecastNoise(index + 1) * r) / knot
        let error = noise * band(lead: target - now) / 1_000
        return (actual * (1_000 + error) + 500) / 1_000
    }

    /// Forecast in g CO₂e/kWh (rounded).
    public func forecast(tick target: Int, madeAt now: Int) -> Int {
        (forecastTenths(tick: target, madeAt: now) + 5) / 10
    }

    private func forecastNoise(_ index: Int) -> Int {
        SeededRandom.symmetric(seed: seed, salt: 3, index: index, spread: 700)
    }
}
