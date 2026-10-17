package de.wannerit.loadshift.core

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt

/**
 * The building around the servers: cooling and power distribution (docs/CONCEPT.md, section 7.1). Most scenarios use
 * one fixed cooling type; scenario 6 has a [CoolingModel] with outside temperature and setpoint.
 */
enum class CoolingType(
    val title: String,
    /** Cooling overhead per kW of IT power, per mille (game values, CONCEPT.md 7.1). */
    val overheadPermille: Int,
) {
    ROOM_AIR("Room air conditioning", 450),
    AISLE_CONTAINMENT("Hot/cold aisle containment", 300),
    FREE_COOLING("Free cooling", 150),
    LIQUID("Direct liquid cooling", 50),
}

data class Facility(val cooling: CoolingType = CoolingType.ROOM_AIR) {
    /** PUE in per mille (1,520 = 1.52). */
    val pue: Int get() = 1_000 + cooling.overheadPermille + DISTRIBUTION_PERMILLE

    /** Facility power for an IT power, W. */
    fun facilityPower(itPower: Int): Int = itPower * pue / 1_000

    companion object {
        /** Power distribution and lighting, per mille of the IT power. */
        const val DISTRIBUTION_PERMILLE = 70
    }
}

/**
 * Scenario 6: cooling that depends on the outside temperature and the inlet setpoint (docs/CONCEPT.md 7.1). Outside
 * air cools for free when it is at least [FREE_APPROACH] K below the setpoint; above setpoint − 1 K the chiller does
 * all the work, in between both. The chiller needs more power the hotter it is outside and less the warmer the air
 * at the server inlets may be. Above 25 °C inlet the server fans speed up (IT power). Lighting, the UPS and the pumps
 * draw a fixed power, whatever the servers do – so fewer servers mean a higher PUE.
 */
data class CoolingModel(
    /** Lowest (05:00) and highest (15:00) outside temperature per day, °C. */
    val days: List<Pair<Int, Int>>,
    /** Fixed power of the facility, W. */
    val fixedPower: Int = 1_500,
    /** Power distribution losses, per mille of the IT power. */
    val distributionPermille: Int = 60,
    /** Chiller power per kW of IT power at 30 °C outside and a setpoint of 20 °C, per mille. */
    val chillerPermille: Int = 380,
    /** Fans of the free cooling, per mille of the IT power. */
    val freePermille: Int = 60,
) {
    /** Outside temperature, tenths of °C: rises from 05:00 to 15:00, falls until 05:00 the next day. */
    fun outsideTenths(tick: Int): Int {
        val day = tick / GameTime.TICKS_PER_DAY
        val hour = (tick % GameTime.TICKS_PER_DAY).toDouble() / GameTime.TICKS_PER_HOUR
        fun low(d: Int) = days[d.coerceIn(0, days.size - 1)].first.toDouble()
        fun high(d: Int) = days[d.coerceIn(0, days.size - 1)].second.toDouble()
        val t = when {
            hour < 5 -> low(day) + (high(day - 1) - low(day)) * (1 + cos(PI * (hour + 24 - 15) / 14)) / 2
            hour < 15 -> low(day) + (high(day) - low(day)) * (1 - cos(PI * (hour - 5) / 10)) / 2
            else -> low(day + 1) + (high(day) - low(day + 1)) * (1 + cos(PI * (hour - 15) / 14)) / 2
        }
        return (t * 10).roundToInt()
    }

    /** Share of the heat the chiller removes, per mille (the rest: outside air). */
    fun mechanicalPermille(tick: Int, setpoint: Int): Int {
        val outside = outsideTenths(tick)
        val free = (setpoint - FREE_APPROACH) * 10
        val full = (setpoint - 1) * 10
        return when {
            outside <= free -> 0
            outside >= full -> 1_000
            else -> (outside - free) * 1_000 / (full - free)
        }
    }

    /** Chiller power per kW of IT power, per mille. */
    fun chillerAt(tick: Int, setpoint: Int): Int {
        val outside = outsideTenths(tick)
        val value = chillerPermille * (1_000 + 3 * (outside - 300)) / 1_000 * (1_000 - 50 * (setpoint - 20)) / 1_000
        return maxOf(120, value)
    }

    /** Cooling power per kW of IT power, per mille. */
    fun coolingPermille(tick: Int, setpoint: Int): Int {
        val m = mechanicalPermille(tick, setpoint)
        return (m * chillerAt(tick, setpoint) + (1_000 - m) * freePermille) / 1_000
    }

    /** IT power with the server fans at a setpoint, per mille of the power at 25 °C or below. */
    fun fanPermille(setpoint: Int): Int = 1_000 + 30 * maxOf(0, setpoint - 25)

    /** Facility power for an IT power (fans included), W. */
    fun facilityPower(itPower: Int, tick: Int, setpoint: Int): Int =
        itPower + itPower * (coolingPermille(tick, setpoint) + distributionPermille) / 1_000 + fixedPower

    companion object {
        /** Setpoints the player can choose, °C (ASHRAE recommends 18–27 °C at the inlet [S17]). */
        val SETPOINTS = 18..27
        const val FREE_APPROACH = 6
    }
}
