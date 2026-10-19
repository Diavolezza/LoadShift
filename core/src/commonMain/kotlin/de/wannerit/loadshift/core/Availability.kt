package de.wannerit.loadshift.core

import kotlin.math.roundToLong

/*
 * Engineering track, E1 "How many nines?" (docs/CONCEPT.md 9.2): an insurer's customer portal of seven building
 * blocks and five quality scenarios, each with the downtime it may have in a year. Per building block an availability
 * tactic – a single instance that is restarted, a cold or warm standby, or active-active on two sites – and two ways
 * to decouple: claims taken into a queue, and the contract overview without recommendations when they fail. A
 * scenario is available only when all building blocks it calls synchronously are. Counted for a year, location-based.
 */

enum class Tactic { SINGLE, COLD_STANDBY, WARM_STANDBY, ACTIVE_ACTIVE }

data class BuildingBlock(
    val name: String,
    val description: String,
    val servers: Int,
    /** How busy its servers are on average, percent. */
    val utilPercent: Int,
    /** Place in the diagram, per mille of its width and height. */
    val x: Int,
    val y: Int,
)

data class QualityScenario(
    val name: String,
    /** Stimulus and response, as in a quality scenario. */
    val text: String,
    /** The downtime it may have in a year, minutes. */
    val maxDowntimeMinutes: Int,
    /** The building blocks it calls synchronously. */
    val uses: List<String>,
)

/** A way to decouple: a scenario no longer waits for one building block (and what that brings along). */
data class Decoupling(
    val id: String,
    val title: String,
    val text: String,
    val scenario: String,
    val removes: String,
    /** A message broker it needs: servers, always active-active (0: none). */
    val brokerServers: Int = 0,
    val brokerUtilPercent: Int = 0,
    /** A building block that can then be sized for the average instead of the peak (null: none). */
    val resized: String? = null,
    val resizedServers: Int = 0,
    val resizedUtilPercent: Int = 0,
)

data class Architecture(val tactics: Map<String, Tactic> = emptyMap(), val decouplings: Set<String> = emptySet())

data class AvailabilityResult(
    val referenceKg: Long,
    val planKg: Long,
    val serversOn: Double,
    /** Expected downtime a year per quality scenario, minutes. */
    val downtime: Map<String, Double>,
    val missed: List<String>,
) {
    val reductionPermille: Long get() = (referenceKg - planKg) * 1_000 / referenceKg
}

data class AvailabilityModel(
    val blocks: List<BuildingBlock>,
    val scenarios: List<QualityScenario>,
    val decouplings: List<Decoupling>,
    /** Failures a year of each building block, and the downtime of one failure per tactic, minutes. */
    val failuresPerYear: Int = 6,
    val downtimePerFailure: Map<Tactic, Int> = mapOf(
        Tactic.SINGLE to 240, Tactic.COLD_STANDBY to 45, Tactic.WARM_STANDBY to 10, Tactic.ACTIVE_ACTIVE to 1,
    ),
    val idleWatts: Int = 120,
    val maxWatts: Int = 360,
    val pueHundredths: Int = 140,
    val gramsPerKwh: Int = 380,
    /** A warm standby: half the servers, running at this load to keep the data current, percent. */
    val warmUtilPercent: Int = 15,
    /** A cold standby: backups and copies of the data, percent of the building block's energy. */
    val coldExtraPercent: Int = 5,
    /** A good architecture (the debrief's "what if"; the tests check its stars). */
    val example: Architecture = Architecture(),
) {
    val minutesPerYear: Double get() = 525_600.0

    fun tactic(a: Architecture, block: String) = a.tactics[block] ?: Tactic.ACTIVE_ACTIVE

    /** Availability of one building block with a tactic, as a share. */
    fun availability(t: Tactic): Double = 1 - failuresPerYear * downtimePerFailure.getValue(t) / minutesPerYear

    private fun watts(util: Double) = idleWatts + (maxWatts - idleWatts) * util

    /** Servers and utilization of a building block in an architecture (a decoupling may resize it). */
    fun sizing(a: Architecture, b: BuildingBlock): Pair<Int, Int> {
        val d = decouplings.firstOrNull { it.id in a.decouplings && it.resized == b.name }
        return if (d != null) d.resizedServers to d.resizedUtilPercent else b.servers to b.utilPercent
    }

    /** Power of a building block with its tactic, watts (servers of both sites and standbys included). */
    fun watts(a: Architecture, b: BuildingBlock): Double {
        val (n, util) = sizing(a, b)
        val u = util / 100.0
        return when (tactic(a, b.name)) {
            Tactic.SINGLE -> n * watts(u)
            Tactic.COLD_STANDBY -> n * watts(u) * (100 + coldExtraPercent) / 100
            Tactic.WARM_STANDBY -> n * watts(u) + n * 0.5 * watts(warmUtilPercent / 100.0)
            Tactic.ACTIVE_ACTIVE -> 2 * n * watts(u / 2)
        }
    }

    fun serversOn(a: Architecture, b: BuildingBlock): Double {
        val n = sizing(a, b).first
        return when (tactic(a, b.name)) {
            Tactic.SINGLE, Tactic.COLD_STANDBY -> n.toDouble()
            Tactic.WARM_STANDBY -> n * 1.5
            Tactic.ACTIVE_ACTIVE -> n * 2.0
        }
    }

    fun brokerWatts(a: Architecture): Double = decouplings.filter { it.id in a.decouplings }
        .sumOf { it.brokerServers * watts(it.brokerUtilPercent / 100.0) }

    fun kgPerYear(a: Architecture): Double {
        val w = blocks.sumOf { watts(a, it) } + brokerWatts(a)
        return w * 8_760 / 1_000 * pueHundredths / 100 * gramsPerKwh / 1_000
    }

    /** The building blocks a scenario waits for in this architecture. */
    fun uses(a: Architecture, s: QualityScenario): List<String> {
        val removed = decouplings.filter { it.id in a.decouplings && it.scenario == s.name }.map { it.removes }.toSet()
        return s.uses.filter { it !in removed }
    }

    fun downtimeMinutes(a: Architecture, s: QualityScenario): Double {
        var avail = uses(a, s).fold(1.0) { acc, name -> acc * availability(tactic(a, name)) }
        // A message broker in the path is always active-active.
        if (decouplings.any { it.id in a.decouplings && it.scenario == s.name && it.brokerServers > 0 }) avail *= availability(Tactic.ACTIVE_ACTIVE)
        return (1 - avail) * minutesPerYear
    }

    val reference: Architecture get() = Architecture()

    fun result(a: Architecture): AvailabilityResult {
        val down = scenarios.associate { it.name to downtimeMinutes(a, it) }
        return AvailabilityResult(
            referenceKg = kgPerYear(reference).roundToLong(),
            planKg = kgPerYear(a).roundToLong(),
            serversOn = blocks.sumOf { serversOn(a, it) } + decouplings.filter { it.id in a.decouplings }.sumOf { it.brokerServers },
            downtime = down,
            missed = scenarios.filter { down.getValue(it.name) > it.maxDowntimeMinutes }.map { it.name },
        )
    }

    fun goalsMet(r: AvailabilityResult, goals: StarGoals): GoalsMet {
        val rules = r.missed.isEmpty()
        val special = (goals.special as? SpecialGoal.Reduction)?.permille ?: 1_000
        return GoalsMet(rules, rules && r.reductionPermille >= goals.reduction, rules && r.reductionPermille >= special)
    }

    fun stars(r: AvailabilityResult, goals: StarGoals): Int {
        val met = goalsMet(r, goals)
        if (!met.rules) return 0
        return 1 + (if (met.reduction) 1 else 0) + (if (met.special) 1 else 0)
    }

    fun all(t: Tactic) = Architecture(blocks.associate { it.name to t })
}
