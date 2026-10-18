package de.wannerit.loadshift.core

/*
 * B12, "Green on paper" (docs/CONCEPT.md 9.1): the same data center under four electricity contracts – and three ways
 * to count its CO₂. Location-based: the grid's average intensity at each hour. Market-based, as the report shows it
 * (GHG Protocol, Scope 2): contracted green volumes count as zero, the rest at the residual mix, which is dirtier than
 * the average because others have claimed the green part [S36]. Hourly matched: only clean energy that is there in the
 * same hour counts – the direction the GHG Protocol's revision of Scope 2 takes [S37]; this is what the stars count.
 */

enum class Contract(val label: String, val surchargeTenthsCent: Int) {
    GREY("grid mix, no contract", 0),
    CERTIFICATES("green certificates (guarantees of origin)", 3),
    PPA("wind farm contract (PPA), annual volume", 15),
    HOURLY("24/7 carbon-free, 90 % in every hour", 40),
}

data class ContractModel(
    /** Electricity price without a contract, tenths of a cent per kWh. */
    val priceTenthsCent: Int = 250,
    /** Budget for electricity, per mille of the reference's cost. */
    val budgetPermille: Int = 1_100,
    /** Residual mix of the country, tenths of g/kWh (game value after [S36]). */
    val residualMixTenths: Int = 6_500,
    /** Energy the wind farm delivers over the scenario, mWh (sized to the reference's consumption). */
    val ppaEnergy: Long,
    /** Share of every hour the 24/7 contract covers, per mille. */
    val hourlyShare: Int = 900,
    /** Intensity at which the wind farm stands still, tenths of g/kWh: the windier, the cleaner the grid. */
    val calmTenths: Int = 4_800,
) {
    /** Wind at a grid intensity, relative (0 when calm; a wind farm's output grows steeply with the wind). */
    fun wind(intensityTenths: Int): Int = maxOf(0, calmTenths - intensityTenths).let { it * it / 100 }
}

/** B12: what the data center drew in a tick and what clean energy was there for it, W. */
data class ContractRecord(val demandWatts: Int, val cleanWatts: Int, val costSoFar: Long = 0, val marketSoFar: Long = 0)
