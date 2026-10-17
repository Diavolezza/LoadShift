package de.wannerit.loadshift.core

/**
 * The building around the servers: cooling and power distribution (docs/CONCEPT.md, section 7.1). So far one
 * fixed cooling type; setpoint and outside temperature come in step 5.
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
