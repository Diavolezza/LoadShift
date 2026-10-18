package de.wannerit.loadshift.core

import kotlin.math.roundToLong

/*
 * Portfolio track, P4 "Leave the cloud?" (docs/CONCEPT.md 9.3): six applications run in a public cloud in Frankfurt.
 * Per application the player picks a place – the cloud in Frankfurt or Stockholm, a colocation in Stuttgart, the
 * company's own server room – and, where the load allows, to switch it off outside its hours of use. Counted for
 * three years: CO₂ location-based with the making of the servers (for the stars), CO₂ in the report (market-based)
 * and the costs.
 *
 * In the cloud an application is billed and counted for the servers it is given; the provider's hosts are packed
 * well. On the company's own servers it needs enough of them for its peak, and they run whether busy or not.
 */

enum class Place { CLOUD_FRANKFURT, CLOUD_STOCKHOLM, COLOCATION, SERVER_ROOM }

data class PlaceSpec(
    val place: Place,
    val title: String,
    /** Power usage effectiveness, hundredths. */
    val pueHundredths: Int,
    /** Grid, location-based, g CO₂ per kWh. */
    val gramsPerKwh: Int,
    /** Grid as the report counts it (market-based), g CO₂ per kWh. */
    val reportGramsPerKwh: Int,
    /** Time a query from the plant takes to reach it and back, tenths of a millisecond. */
    val roundTripTenthMs: Int,
    val inGermany: Boolean,
) {
    val cloud: Boolean get() = place == Place.CLOUD_FRANKFURT || place == Place.CLOUD_STOCKHOLM
}

data class MoveApp(
    val name: String,
    val description: String,
    /** Server equivalents it is given in the cloud today, and how busy they are on average, percent. */
    val cloudServers: Int,
    val cloudUtilPercent: Int,
    /** Servers it needs on the company's own hardware (enough for its peak), and how busy they are on average. */
    val ownServers: Int,
    val ownUtilPercent: Int,
    /** The load over a week, four values a day, percent of the peak (for the sparkline). */
    val week: List<Int>,
    /** Share of the hours it is used, percent (null: it is used all the time), and how busy it is in those hours. */
    val usePercent: Int? = null,
    val useUtilPercent: Int = 0,
    /** In the cloud it runs only when needed anyway (switching off changes nothing there). */
    val onDemandInCloud: Boolean = false,
    /** A contract keeps its data in Germany. */
    val germanyOnly: Boolean = false,
    /** Queries one booking in the plant sends one after the other (0: nobody waits on it in the plant). */
    val queriesPerBooking: Int = 0,
)

data class Move(val place: Place = Place.CLOUD_FRANKFURT, val offOutsideUse: Boolean = false)

/** Where a change of CO₂ comes from (kg over the whole time): the grid, the energy of the servers, the cooling, the making of the servers. */
data class Breakdown(val region: Long, val energy: Long, val pue: Long, val embodied: Long)

data class MoveResult(
    val baselineKg: Long,
    val planKg: Long,
    val baselineReportKg: Long,
    val reportKg: Long,
    val baselineEuro: Long,
    val planEuro: Long,
    val breakdown: Breakdown,
    /** Applications whose data must stay in Germany, placed outside it. */
    val outsideGermany: List<String>,
    /** The slowest booking in the plant, milliseconds. */
    val bookingMs: Long,
    val bookingLimitMs: Long,
) {
    val reductionPermille: Long get() = (baselineKg - planKg) * 1_000 / baselineKg
    val savingPermille: Long get() = (baselineEuro - planEuro) * 1_000 / baselineEuro
    val bookingTooSlow: Boolean get() = bookingMs > bookingLimitMs
}

data class RepatriationModel(
    val apps: List<MoveApp>,
    val places: List<PlaceSpec>,
    val years: Int = 3,
    /** One server: power idle and at full load, watts. */
    val idleWatts: Int = 120,
    val maxWatts: Int = 360,
    /** Making one server, kg CO₂, written off over its life. */
    val embodiedKg: Int = 1_500,
    val lifeYears: Int = 5,
    /** Electricity on the company's own sites, cents per kWh. */
    val centsPerKwh: Int = 25,
    /** A server equivalent in the cloud, € a year. */
    val cloudEuroPerServer: Int = 11_000,
    /** A rack unit in the colocation (space, network, care), € a year per server. */
    val colocationEuroPerServer: Int = 2_600,
    /** The server room's upkeep, € a year per server. */
    val roomEuroPerServer: Int = 1_200,
    /** A new server, €. */
    val serverEuro: Int = 8_000,
    /** A team for the company's own hardware, € a year, as soon as anything runs there. */
    val teamEuroPerYear: Int = 55_000,
    /** Free servers in the server room, already made, three years old. */
    val roomCapacity: Int = 6,
    /** Rules: the costs at least this much lower, per mille; a booking in the plant at most this long. */
    val savingGoalPermille: Int = 200,
    val bookingLimitMs: Int = 1_000,
    /** A good plan (the debrief's "what if"; the tests check its stars). */
    val example: Map<String, Move> = emptyMap(),
) {
    fun spec(place: Place) = places.first { it.place == place }

    fun servers(app: MoveApp, place: Place) = if (spec(place).cloud) app.cloudServers else app.ownServers

    /** Electricity of the servers over the whole time, kWh. */
    fun itKwh(app: MoveApp, m: Move): Double {
        val cloud = spec(m.place).cloud
        val off = m.offOutsideUse && app.usePercent != null && !(cloud && app.onDemandInCloud)
        val hours = 8_760.0 * years * (if (off) app.usePercent!! / 100.0 else 1.0)
        val util = (if (off) app.useUtilPercent else if (cloud) app.cloudUtilPercent else app.ownUtilPercent) / 100.0
        return servers(app, m.place) * hours * (idleWatts + (maxWatts - idleWatts) * util) / 1_000
    }

    /** The making of the servers it takes, the share of these years (the server room's are already made). */
    fun embodied(app: MoveApp, place: Place): Double =
        if (place == Place.SERVER_ROOM) 0.0 else servers(app, place).toDouble() * embodiedKg * years / lifeYears

    fun kg(app: MoveApp, m: Move): Double {
        val s = spec(m.place)
        return itKwh(app, m) * s.pueHundredths / 100 * s.gramsPerKwh / 1_000 + embodied(app, m.place)
    }

    fun reportKg(app: MoveApp, m: Move): Double {
        val s = spec(m.place)
        return itKwh(app, m) * s.pueHundredths / 100 * s.reportGramsPerKwh / 1_000
    }

    /** Costs over the whole time, € (without the team). */
    fun euro(app: MoveApp, m: Move): Double {
        val s = spec(m.place)
        val n = servers(app, m.place)
        val power = itKwh(app, m) * s.pueHundredths / 100 * centsPerKwh / 100
        return when (m.place) {
            Place.CLOUD_FRANKFURT, Place.CLOUD_STOCKHOLM -> {
                val off = m.offOutsideUse && app.usePercent != null && !app.onDemandInCloud
                n.toDouble() * cloudEuroPerServer * years * (if (off) app.usePercent!! / 100.0 else 1.0)
            }
            Place.COLOCATION -> n.toDouble() * (colocationEuroPerServer * years + serverEuro) + power
            Place.SERVER_ROOM -> n.toDouble() * roomEuroPerServer * years + power
        }
    }

    fun moveOf(plan: Map<String, Move>, app: MoveApp) = plan[app.name] ?: Move()

    fun roomUsed(plan: Map<String, Move>): Int = apps.filter { moveOf(plan, it).place == Place.SERVER_ROOM }.sumOf { it.ownServers }

    /** Whether the server room still has space for the application (counting what else is there). */
    fun fitsRoom(plan: Map<String, Move>, app: MoveApp): Boolean =
        roomUsed(plan - app.name) + app.ownServers <= roomCapacity

    fun bookingMs(app: MoveApp, place: Place): Long = app.queriesPerBooking.toLong() * spec(place).roundTripTenthMs / 10

    fun euro(plan: Map<String, Move>): Double {
        val own = apps.any { !spec(moveOf(plan, it).place).cloud }
        return apps.sumOf { euro(it, moveOf(plan, it)) } + if (own) teamEuroPerYear.toDouble() * years else 0.0
    }

    fun result(plan: Map<String, Move>): MoveResult {
        var region = 0.0; var energy = 0.0; var pue = 0.0; var made = 0.0
        for (app in apps) {
            val m = moveOf(plan, app)
            val s0 = spec(Place.CLOUD_FRANKFURT); val s1 = spec(m.place)
            val e0 = itKwh(app, Move()); val e1 = itKwh(app, m)
            region += e0 * s0.pueHundredths / 100 * (s1.gramsPerKwh - s0.gramsPerKwh) / 1_000
            energy += (e1 - e0) * s0.pueHundredths / 100 * s1.gramsPerKwh / 1_000
            pue += e1 * (s1.pueHundredths - s0.pueHundredths) / 100 * s1.gramsPerKwh / 1_000
            made += embodied(app, m.place) - embodied(app, Place.CLOUD_FRANKFURT)
        }
        return MoveResult(
            baselineKg = apps.sumOf { kg(it, Move()) }.roundToLong(),
            planKg = apps.sumOf { kg(it, moveOf(plan, it)) }.roundToLong(),
            baselineReportKg = apps.sumOf { reportKg(it, Move()) }.roundToLong(),
            reportKg = apps.sumOf { reportKg(it, moveOf(plan, it)) }.roundToLong(),
            baselineEuro = euro(emptyMap()).roundToLong(),
            planEuro = euro(plan).roundToLong(),
            breakdown = Breakdown(region.roundToLong(), energy.roundToLong(), pue.roundToLong(), made.roundToLong()),
            outsideGermany = apps.filter { it.germanyOnly && !spec(moveOf(plan, it).place).inGermany }.map { it.name },
            bookingMs = apps.maxOf { bookingMs(it, moveOf(plan, it).place) },
            bookingLimitMs = bookingLimitMs.toLong(),
        )
    }

    fun goalsMet(r: MoveResult, goals: StarGoals): GoalsMet {
        val rules = r.savingPermille >= savingGoalPermille && r.outsideGermany.isEmpty() && !r.bookingTooSlow
        val special = (goals.special as? SpecialGoal.Reduction)?.permille ?: 1_000
        return GoalsMet(rules, rules && r.reductionPermille >= goals.reduction, rules && r.reductionPermille >= special)
    }

    fun stars(r: MoveResult, goals: StarGoals): Int {
        val met = goalsMet(r, goals)
        if (!met.rules) return 0
        return 1 + (if (met.reduction) 1 else 0) + (if (met.special) 1 else 0)
    }

    /** Everything to one place (for the debrief's "what if"; the server room only takes what fits). */
    fun all(place: Place): Map<String, Move> = apps.associate { it.name to Move(place) }
}
