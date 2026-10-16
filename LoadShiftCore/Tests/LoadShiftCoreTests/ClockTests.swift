import XCTest
@testable import LoadShiftCore

final class ClockTests: XCTestCase {
    func testDayHasNinetySixTicks() {
        XCTAssertEqual(GameTime.ticksPerDay, 96)
        XCTAssertEqual(GameTime.ticksPerHour, 4)
    }

    func testScenarioStartsAtMidnight() {
        XCTAssertEqual(GameTime.start.day, 0)
        XCTAssertEqual(GameTime.start.clockText, "00:00")
    }

    func testDayAndClock() {
        let t = GameTime(day: 2, hour: 14, minute: 15)
        XCTAssertEqual(t.tick, 2 * 96 + 14 * 4 + 1)
        XCTAssertEqual(t.day, 2)
        XCTAssertEqual(t.hour, 14)
        XCTAssertEqual(t.minute, 15)
        XCTAssertEqual(t.clockText, "14:15")
    }

    func testMidnightRollsOverToTheNextDay() {
        let lastTick = GameTime(day: 0, hour: 23, minute: 45)
        let next = lastTick.advanced(by: 1)
        XCTAssertEqual(next.day, 1)
        XCTAssertEqual(next.clockText, "00:00")
    }

    func testAWeekIsSixHundredSeventyTwoTicks() {
        XCTAssertEqual(GameTime(day: 7, hour: 0).tick, 672)
    }
}
