import XCTest
@testable import LoadShiftCore

final class GridTests: XCTestCase {
    func testWithNeutralWeatherTheFullHoursMatchTheCurve() {
        for region in GridRegion.allCases {
            let setting = GridSetting(region, .winter)
            let grid = Grid(setting: setting, seed: 7, weather: .fixed(1_000))
            for hour in 0..<24 {
                XCTAssertEqual(grid.intensityTenths(tick: hour * GameTime.ticksPerHour), grid.curve.hourly[hour],
                               "\(region) \(hour):00")
            }
        }
    }

    func testDrawnWeatherStaysWithinTheDaysOf2025() {
        for region in GridRegion.allCases {
            let grid = Grid(setting: GridSetting(region, .spring), seed: 42)
            for day in 0..<200 {
                let w = grid.weather(day: day)
                XCTAssertGreaterThanOrEqual(w, grid.weatherLow, "\(region) day \(day)")
                XCTAssertLessThanOrEqual(w, grid.weatherHigh, "\(region) day \(day)")
            }
        }
    }

    func testSpainHasItsMiddayDip() {
        let grid = Grid(setting: GridSetting(.spain, .summer), seed: 1, weather: .fixed(1_000))
        let noon = grid.intensity(tick: GameTime(day: 0, hour: 14).tick)
        let evening = grid.intensity(tick: GameTime(day: 0, hour: 21).tick)
        XCTAssertLessThan(noon, evening * 7 / 10)
    }

    func testForecastMeetsTheRealValueWhenTheTimeComes() {
        let grid = Grid(setting: GridSetting(.greatBritain, .winter), seed: 3)
        for tick in stride(from: 0, to: 400, by: 7) {
            XCTAssertEqual(grid.forecastTenths(tick: tick, madeAt: tick), grid.intensityTenths(tick: tick))
        }
    }

    func testRealValueLiesWithinTheForecastBand() {
        for region in GridRegion.allCases {
            let grid = Grid(setting: GridSetting(region, .autumn), seed: 11)
            let now = 100
            for lead in stride(from: 0, through: 2 * GameTime.ticksPerDay, by: 5) {
                let forecast = grid.forecastTenths(tick: now + lead, madeAt: now)
                let actual = grid.intensityTenths(tick: now + lead)
                let band = grid.band(lead: lead)
                XCTAssertLessThanOrEqual(abs(forecast - actual) * 1_000, actual * band + 1_000,
                                         "\(region) lead \(lead)")
            }
        }
    }

    func testBandWidensToTheRegionsErrorOneDayAhead() {
        let grid = Grid(setting: GridSetting(.germany, .summer), seed: 1)
        XCTAssertEqual(grid.band(lead: 0), 0)
        XCTAssertEqual(grid.band(lead: GameTime.ticksPerDay), 200)
        XCTAssertEqual(grid.band(lead: 3 * GameTime.ticksPerDay), 200)
    }

    func testSameSeedSameGrid() {
        let a = Grid(setting: GridSetting(.germany, .spring), seed: 99)
        let b = Grid(setting: GridSetting(.germany, .spring), seed: 99)
        let c = Grid(setting: GridSetting(.germany, .spring), seed: 100)
        let ticks = 0..<(3 * GameTime.ticksPerDay)
        XCTAssertEqual(ticks.map { a.intensityTenths(tick: $0) }, ticks.map { b.intensityTenths(tick: $0) })
        XCTAssertNotEqual(ticks.map { a.intensityTenths(tick: $0) }, ticks.map { c.intensityTenths(tick: $0) })
    }
}
