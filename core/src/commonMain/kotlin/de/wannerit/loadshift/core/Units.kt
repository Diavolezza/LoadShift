package de.wannerit.loadshift.core

/**
 * Units used in the simulation, all integers:
 *
 * - power in W,
 * - energy in mWh (1 W for one tick of 15 minutes = 250 mWh), as Long,
 * - carbon intensity in tenths of g CO₂e/kWh,
 * - emissions in µg CO₂e (1 mWh at 1 g/kWh = 1 µg; with tenths: mWh × tenths / 10), as Long,
 * - shares in per mille.
 */
object Units {
    /** 1 W for one tick (15 minutes) = 250 mWh. */
    const val MILLIWATT_HOURS_PER_WATT_TICK = 250L

    /** Days a server is expected to be in service: 6 years (Microsoft and Google [S18]). */
    const val LIFESPAN_DAYS = 6 * 365

    /** Emissions in µg of an energy in mWh at an intensity in tenths of g/kWh. */
    fun emissions(milliwattHours: Long, intensityTenths: Int): Long = milliwattHours * intensityTenths / 10
}
