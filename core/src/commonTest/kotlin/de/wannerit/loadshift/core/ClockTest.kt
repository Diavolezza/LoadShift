package de.wannerit.loadshift.core

import kotlin.test.Test
import kotlin.test.assertEquals

class ClockTest {
    @Test fun aDayHasNinetySixTicks() {
        assertEquals(96, GameTime.TICKS_PER_DAY)
        assertEquals(4, GameTime.TICKS_PER_HOUR)
    }

    @Test fun aScenarioStartsAtMidnight() {
        assertEquals(0, GameTime.START.day)
        assertEquals("00:00", GameTime.START.clockText)
    }

    @Test fun dayAndClock() {
        val t = GameTime.at(2, 14, 15)
        assertEquals(2 * 96 + 14 * 4 + 1, t.tick)
        assertEquals(2, t.day)
        assertEquals(14, t.hour)
        assertEquals(15, t.minute)
        assertEquals("14:15", t.clockText)
    }

    @Test fun midnightRollsOverToTheNextDay() {
        val next = GameTime.at(0, 23, 45).advanced(1)
        assertEquals(1, next.day)
        assertEquals("00:00", next.clockText)
    }

    @Test fun aWeekIsSixHundredSeventyTwoTicks() {
        assertEquals(672, GameTime.at(7, 0).tick)
    }
}
