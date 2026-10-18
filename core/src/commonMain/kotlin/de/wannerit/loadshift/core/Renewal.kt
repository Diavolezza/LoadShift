package de.wannerit.loadshift.core

/*
 * Scenario O7, hardware renewal (docs/CONCEPT.md 7.3, 9.1; D5, D22): two sites with the same old fleet in different
 * grids. One game day stands for a quarter, three years in all. Every year brings a budget for new servers and a new
 * server generation that does more work per kWh; the shop grows; old servers past their six-year life begin to fail.
 * A new server's embodied emissions are booked over its six-year life.
 */

/** A site of the renewal scenario. */
data class Site(val title: String, val region: GridRegion)

data class RenewalModel(
    val sites: List<Site>,
    /** Real days one game day stands for: a quarter; game days per year. */
    val daysPerStep: Int = 91,
    val daysPerYear: Int = 4,
    /** New servers the budget pays for each year, both sites together (what is not spent in a year is gone). */
    val budgetPerYear: Int = 4,
    /** Work per kWh of each year's generation, more than the year before, per mille. */
    val generationGain: Int = 200,
    /** Chance that an old server past its life fails in a quarter, per mille. */
    val failure: Int = 80,
) {
    /** The seasons of the quarters: winter, spring, summer, autumn, again and again. */
    fun seasons(days: Int): List<Season> = List(days) { listOf(Season.WINTER, Season.SPRING, Season.SUMMER, Season.AUTUMN)[it % 4] }

    /** Year of a game day, from 0. */
    fun year(day: Int): Int = day / daysPerYear

    /** Power of the generation bought on a day, per mille of the first year's (the same work for less power). */
    fun generationPower(day: Int): Int {
        var p = 1_000
        repeat(year(day)) { p = p * 1_000 / (1_000 + generationGain) }
        return p
    }
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
    visits = visits + o.visits, lcpSum = lcpSum + o.lcpSum, kilobytes = kilobytes + o.kilobytes,
    networkEnergy = networkEnergy + o.networkEnergy, deviceEnergy = deviceEnergy + o.deviceEnergy,
    networkOperational = networkOperational + o.networkOperational, deviceOperational = deviceOperational + o.deviceOperational,
    answers = answers + o.answers, answerQualitySum = answerQualitySum + o.answerQualitySum, answersInTime = answersInTime + o.answersInTime,
    itEnergy = itEnergy + o.itEnergy, facilityEnergy = facilityEnergy + o.facilityEnergy,
    operational = operational + o.operational, embodied = embodied + o.embodied,
    serverTicks = serverTicks + o.serverTicks, capacityTicks = capacityTicks + o.capacityTicks,
    batchEnergy = batchEnergy + o.batchEnergy, batchOperational = batchOperational + o.batchOperational,
    jobsDone = jobsDone + o.jobsDone, deadlinesMissed = deadlinesMissed + o.deadlinesMissed,
    retiredEmbodied = retiredEmbodied + o.retiredEmbodied, failedServers = failedServers + o.failedServers,
    worstSiteSla = minOf(worstSiteSla, sla, o.worstSiteSla, o.sla),
    rulesBroken = rulesBroken + o.rulesBroken, transferEnergy = transferEnergy + o.transferEnergy,
)
