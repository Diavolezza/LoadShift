package de.wannerit.loadshift.core

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class GridTest {
    @Test fun withNeutralWeatherTheFullHoursMatchTheCurve() {
        for (region in GridRegion.entries) {
            val grid = Grid(GridSetting(region, Season.WINTER), 7, WeatherMode.Fixed(1_000))
            for (hour in 0 until 24) {
                assertEquals(grid.curve.hourly[hour], grid.intensityTenths(hour * GameTime.TICKS_PER_HOUR), "$region $hour:00")
            }
        }
    }

    @Test fun drawnWeatherStaysWithinTheDaysOf2025() {
        for (region in GridRegion.entries) {
            val grid = Grid(GridSetting(region, Season.SPRING), 42)
            for (day in 0 until 200) {
                val w = grid.weather(day)
                assertTrue(w >= grid.weatherLow && w <= grid.weatherHigh, "$region day $day: $w")
            }
        }
    }

    @Test fun spainHasItsMiddayDip() {
        val grid = Grid(GridSetting(GridRegion.SPAIN, Season.SUMMER), 1, WeatherMode.Fixed(1_000))
        val noon = grid.intensity(GameTime.at(0, 14).tick)
        val evening = grid.intensity(GameTime.at(0, 21).tick)
        assertTrue(noon < evening * 7 / 10)
    }

    @Test fun forecastMeetsTheRealValueWhenTheTimeComes() {
        val grid = Grid(GridSetting(GridRegion.GREAT_BRITAIN, Season.WINTER), 3)
        for (tick in 0 until 400 step 7) assertEquals(grid.intensityTenths(tick), grid.forecastTenths(tick, tick))
    }

    @Test fun theRealValueLiesWithinTheForecastBand() {
        for (region in GridRegion.entries) {
            val grid = Grid(GridSetting(region, Season.AUTUMN), 11)
            val now = 100
            for (lead in 0..2 * GameTime.TICKS_PER_DAY step 5) {
                val forecast = grid.forecastTenths(now + lead, now)
                val actual = grid.intensityTenths(now + lead)
                assertTrue(abs(forecast - actual) * 1_000 <= actual * grid.band(lead) + 1_000, "$region lead $lead")
            }
        }
    }

    @Test fun theBandWidensToTheRegionsErrorOneDayAhead() {
        val grid = Grid(GridSetting(GridRegion.GERMANY, Season.SUMMER), 1)
        assertEquals(0, grid.band(0))
        assertEquals(200, grid.band(GameTime.TICKS_PER_DAY))
        assertEquals(200, grid.band(3 * GameTime.TICKS_PER_DAY))
    }

    @Test fun sameSeedSameGrid() {
        val a = Grid(GridSetting(GridRegion.GERMANY, Season.SPRING), 99)
        val b = Grid(GridSetting(GridRegion.GERMANY, Season.SPRING), 99)
        val c = Grid(GridSetting(GridRegion.GERMANY, Season.SPRING), 100)
        val ticks = 0 until 3 * GameTime.TICKS_PER_DAY
        assertEquals(ticks.map { a.intensityTenths(it) }, ticks.map { b.intensityTenths(it) })
        assertNotEquals(ticks.map { a.intensityTenths(it) }, ticks.map { c.intensityTenths(it) })
    }
}
