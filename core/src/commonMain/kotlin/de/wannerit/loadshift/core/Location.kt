package de.wannerit.loadshift.core

/*
 * Scenario O10, location shifting (docs/CONCEPT.md 9.1): the company's batch work runs in Frankfurt; a data center in
 * Oslo, powered by hydro, has a few servers free. Before the three days start the player decides which jobs run in
 * Oslo. A job that moves needs its data in Oslo first: the transfer takes time and the network needs energy (in the
 * German grid, where the data leaves). A job that talks to the live database in Frankfurt runs much slower far away.
 * Some jobs may not move at all: customer data that must stay in Germany by contract.
 */

/** Why a job should stay where it is. */
enum class StayReason { RESIDENCY }

/** A batch job of the location scenario: its data, and what happens when it runs far from Frankfurt. */
data class LocationJob(
    val spec: BatchJobSpec,
    /** Data the job needs, tenths of a terabyte. */
    val dataTenthsTb: Int,
    /** Duration in Oslo, per mille of the duration in Frankfurt (a job that talks to the database there: slower). */
    val remoteDuration: Int = 1_000,
    val stay: StayReason? = null,
)

data class LocationModel(
    val sites: List<Site>,
    /** Servers for batch work at each site. */
    val servers: List<Int>,
    val jobs: List<LocationJob>,
    /** Network between the sites: terabytes per hour, tenths (40 = 4 TB/h, about 9 Gbit/s). */
    val tenthsTbPerHour: Int = 40,
    /** Energy of the network per terabyte, kWh (game value in the range of the literature [S24]). */
    val kWhPerTb: Int = 5,
) {
    /** Ticks to copy a job's data to the other site (at least one). */
    fun transferTicks(job: LocationJob): Int =
        maxOf(1, (job.dataTenthsTb * GameTime.TICKS_PER_HOUR + tenthsTbPerHour - 1) / tenthsTbPerHour)

    /** Energy of the transfer, mWh. */
    fun transferEnergy(job: LocationJob): Long = job.dataTenthsTb.toLong() * kWhPerTb * 1_000_000 / 10

    /** The job as it runs at the other site: it arrives when its data is there and may take longer. */
    fun moved(job: LocationJob): BatchJobSpec {
        val duration = job.spec.duration * job.remoteDuration / 1_000
        return job.spec.copy(arrival = job.spec.arrival + transferTicks(job), duration = duration)
    }
}

/** A data transfer booked at a site: energy (mWh) at a tick, in that site's grid. */
data class Transfer(val tick: Int, val energy: Long)

/**
 * The scenario as one site sees it, with the jobs [moved] to the second site: its grid, its servers, its jobs; the
 * first site books the transfers of the moved jobs. Moving a job that must stay breaks a rule.
 */
fun Scenario.forLocation(site: Int, moved: Set<Int>): Scenario {
    val model = location ?: return this
    val here = model.jobs.indices.filter { (it in moved) == (site == 1) }
    return copy(
        setting = GridSetting(model.sites[site].region, setting.season),
        fleet = listOf(FleetEntry(ServerKind.CPU_CURRENT, model.servers[site])),
        batchJobs = here.map { if (site == 1) model.moved(model.jobs[it]) else model.jobs[it].spec },
        transfers = if (site == 0) moved.sorted().map { Transfer(model.jobs[it].spec.arrival, model.transferEnergy(model.jobs[it])) } else emptyList(),
        rulesBroken = if (site == 1) moved.count { model.jobs[it].stay != null } else 0,
    )
}

/** Runs both sites with the jobs [moved] and a policy; the first is Frankfurt. */
fun Scenario.runLocation(moved: Set<Int>, policy: Policy): List<Simulation> =
    (0 until (location?.sites?.size ?: 1)).map { Simulation.replay(forLocation(it, moved), policy) }
