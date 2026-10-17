package de.wannerit.loadshift.core

/*
 * Scenario O7, hardware renewal (docs/CONCEPT.md 7.3, 9.1; D5): two sites with the same old fleet in different grids.
 * One game day stands for a quarter. A new server does the work of [RenewalModel.replacedPerNew] old ones; its
 * embodied emissions are booked over its six-year life, while an old server retired before the end of its life books
 * the rest of its embodied emissions at once.
 */

/** A site of the renewal scenario. */
data class Site(val title: String, val region: GridRegion)

data class RenewalModel(
    val sites: List<Site>,
    /** Real days one game day stands for: a quarter. */
    val daysPerStep: Int = 91,
    /** Old servers a new one replaces. */
    val replacedPerNew: Int = 2,
    /** New servers the budget pays for over the whole scenario, both sites together. */
    val budget: Int = 12,
) {
    /** The seasons of the quarters: winter, spring, summer, autumn, again and again. */
    fun seasons(days: Int): List<Season> = List(days) { listOf(Season.WINTER, Season.SPRING, Season.SUMMER, Season.AUTUMN)[it % 4] }
}

/** The scenario as one site sees it: the site's grid; the rest is the same. */
fun Scenario.forSite(site: Int): Scenario {
    val model = renewal ?: return this
    return copy(setting = GridSetting(model.sites[site].region, setting.season))
}

/** Totals of two sites together (the energies, emissions and counts add up). */
operator fun Totals.plus(o: Totals): Totals = Totals(
    ticks = maxOf(ticks, o.ticks), demand = demand + o.demand, served = served + o.served, inTime = inTime + o.inTime,
    requestsServed = requestsServed + o.requestsServed, requests = requests + o.requests, stale = stale + o.stale,
    itEnergy = itEnergy + o.itEnergy, facilityEnergy = facilityEnergy + o.facilityEnergy,
    operational = operational + o.operational, embodied = embodied + o.embodied,
    serverTicks = serverTicks + o.serverTicks, capacityTicks = capacityTicks + o.capacityTicks,
    batchEnergy = batchEnergy + o.batchEnergy, batchOperational = batchOperational + o.batchOperational,
    jobsDone = jobsDone + o.jobsDone, deadlinesMissed = deadlinesMissed + o.deadlinesMissed,
    retiredEmbodied = retiredEmbodied + o.retiredEmbodied,
)
