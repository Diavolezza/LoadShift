package de.wannerit.loadshift.core

import kotlin.math.roundToLong

/*
 * Engineering track, E9 "Constant asking" (docs/CONCEPT.md 9.2): a delivery app on 400,000 phones fetches five kinds
 * of updates. Per kind one way while the app is open – polling every so many seconds, once on opening, long polling,
 * a stream (WebSocket), push – and one while it is closed: nothing, a background refresh every 15 minutes, a
 * connection kept open, push. Two switches for all: conditional requests (an unchanged answer is a short
 * "304 Not Modified") and one background refresh for all kinds. Counted for a year: the phones, the servers and the
 * network, location-based; the rest of the app's traffic stays as it is.
 */

sealed class Open {
    data class Poll(val seconds: Int) : Open()
    data object OnOpen : Open()
    data object LongPoll : Open()
    data object Stream : Open()
    data object Push : Open()
}

enum class Closed { NONE, REFRESH, SOCKET, PUSH }

data class UpdateKind(
    val name: String,
    val description: String,
    /** How often the situation occurs in a year (an order, a map opened, a phone's day …). */
    val periodsPerYear: Long,
    /** Minutes of a period with the app open and closed. */
    val openMinutes: Int,
    val closedMinutes: Int,
    /** Changes in a period, tenths (spread over open and closed time). */
    val changesTenths: Int,
    val payloadKb: Int,
    /** Times the app is opened in a period, tenths. */
    val opensTenths: Int,
    /** Seconds within which a change must show while the app is open (null: when it is opened is enough). */
    val openFreshness: Int?,
    /** Seconds within which a change must reach the closed app (null: not needed). */
    val closedFreshness: Int?,
    val openOptions: List<Open>,
    val closedOptions: List<Closed>,
)

data class UpdatePlan(
    val open: Map<String, Open> = emptyMap(),
    val closed: Map<String, Closed> = emptyMap(),
    val conditional: Boolean = false,
    val bundled: Boolean = false,
)

data class UpdatesResult(
    val referenceKg: Long,
    val planKg: Long,
    val phonesKg: Long,
    val serversKg: Long,
    val networkKg: Long,
    val otherKg: Long,
    /** Seconds within which a change shows while open and when closed, per kind (null: not at all until opened). */
    val openSeconds: Map<String, Int?>,
    val closedSeconds: Map<String, Int?>,
    val missed: List<String>,
) {
    val reductionPermille: Long get() = (referenceKg - planKg) * 1_000 / referenceKg
}

data class UpdatesModel(
    val kinds: List<UpdateKind>,
    val phones: Long,
    val reference: UpdatePlan,
    /** Everything else the app causes in a year (browsing, ordering), kg CO₂ – unchanged. */
    val otherKg: Long,
    /** Phone: a request while the app is open, a wake-up of the radio while it is closed, per KB, joules. */
    val openRequestJ: Double = 0.5,
    val wakeJ: Double = 6.0,
    val kbJ: Double = 0.01,
    /** Server: a full answer, a short one ("not modified" or a timeout), a message on a connection, joules; an open connection an hour. */
    val fullJ: Double = 0.4,
    val shortJ: Double = 0.05,
    val messageJ: Double = 0.05,
    val connectionHourJ: Double = 3.0,
    /** Headers of a request and an answer, KB. */
    val headerKb: Double = 1.0,
    val refreshMinutes: Int = 15,
    val keepaliveMinutes: Int = 4,
    /** Push: how fast it usually arrives, seconds (it is not guaranteed and is throttled for frequent messages). */
    val pushSeconds: Int = 5,
    /** At most this many push messages a minute make sense for one kind. */
    val pushPerMinuteLimitTenths: Int = 5,
    val pueHundredths: Int = 140,
    val kwhPerGb: Double = 0.06,
    val gramsPerKwh: Int = 380,
    /** A good plan (the debrief's "what if"; the tests check its stars). */
    val example: UpdatePlan = UpdatePlan(),
) {
    fun open(p: UpdatePlan, k: UpdateKind): Open = p.open[k.name] ?: reference.open.getValue(k.name)
    fun closed(p: UpdatePlan, k: UpdateKind): Closed = p.closed[k.name] ?: reference.closed[k.name] ?: Closed.NONE

    private fun changesOpen(k: UpdateKind) = k.changesTenths / 10.0 * k.openMinutes / (k.openMinutes + k.closedMinutes)
    private fun changesClosed(k: UpdateKind) = k.changesTenths / 10.0 * k.closedMinutes / (k.openMinutes + k.closedMinutes)

    /** Joules on the phone, the server and kilobytes over the network for one period of a kind, while open. */
    private fun openCost(p: UpdatePlan, k: UpdateKind): Triple<Double, Double, Double> {
        val opens = k.opensTenths / 10.0
        return when (val o = open(p, k)) {
            is Open.Poll -> {
                val n = k.openMinutes * 60.0 / o.seconds
                val full = if (p.conditional) minOf(n, changesOpen(k) + opens) else n
                val short = n - full
                Triple(n * openRequestJ, full * fullJ + short * shortJ, full * (k.payloadKb + headerKb) + short * headerKb)
            }
            Open.OnOpen -> Triple(opens * openRequestJ, opens * fullJ, opens * (k.payloadKb + headerKb))
            Open.LongPoll -> {
                // A request waits until something changes or a minute is over.
                val n = changesOpen(k) + k.openMinutes + opens
                Triple(n * openRequestJ, (changesOpen(k) + opens) * fullJ + k.openMinutes * shortJ + k.openMinutes / 60.0 * connectionHourJ,
                    n * headerKb + (changesOpen(k) + opens) * k.payloadKb)
            }
            Open.Stream -> {
                val msgs = changesOpen(k) + opens
                Triple(msgs * 0.1 + opens * openRequestJ, msgs * messageJ + opens * fullJ + k.openMinutes / 60.0 * connectionHourJ,
                    msgs * k.payloadKb + opens * headerKb * 2)
            }
            Open.Push -> {
                val msgs = changesOpen(k) + opens
                Triple(msgs * 0.3, msgs * (2 * messageJ + fullJ), msgs * (k.payloadKb + headerKb))
            }
        }
    }

    /** The same for the time the app is closed (a bundled refresh is counted once for all kinds, see [result]). */
    private fun closedCost(p: UpdatePlan, k: UpdateKind): Triple<Double, Double, Double> = when (closed(p, k)) {
        Closed.NONE -> Triple(0.0, 0.0, 0.0)
        Closed.REFRESH -> if (p.bundled) Triple(0.0, 0.0, 0.0) else {
            val n = k.closedMinutes.toDouble() / refreshMinutes
            val full = if (p.conditional) minOf(n, changesClosed(k)) else n
            Triple(n * wakeJ, full * fullJ + (n - full) * shortJ, full * (k.payloadKb + headerKb) + (n - full) * headerKb)
        }
        Closed.SOCKET -> {
            val n = k.closedMinutes.toDouble() / keepaliveMinutes
            Triple((n + changesClosed(k)) * wakeJ, k.closedMinutes / 60.0 * connectionHourJ + changesClosed(k) * messageJ,
                n * 0.1 + changesClosed(k) * k.payloadKb)
        }
        Closed.PUSH -> {
            val n = changesClosed(k)
            Triple(n * wakeJ, n * (2 * messageJ + fullJ), n * (k.payloadKb + headerKb))
        }
    }

    /** One background refresh for all kinds that refresh, per phone and day. */
    private fun bundledCost(p: UpdatePlan): Triple<Double, Double, Double> {
        val kinds = kinds.filter { closed(p, it) == Closed.REFRESH }
        if (!p.bundled || kinds.isEmpty()) return Triple(0.0, 0.0, 0.0)
        val n = 1_440.0 / refreshMinutes * phones * 365
        val payload = kinds.sumOf { it.payloadKb }
        val changes = kinds.sumOf { changesClosed(it) * it.periodsPerYear }
        val full = if (p.conditional) minOf(n, changes) else n
        return Triple(n * wakeJ, full * fullJ + (n - full) * shortJ, full * (payload + headerKb) + (n - full) * headerKb)
    }

    fun openSeconds(p: UpdatePlan, k: UpdateKind): Int? = when (val o = open(p, k)) {
        is Open.Poll -> o.seconds
        Open.OnOpen -> null
        Open.LongPoll, Open.Stream -> 1
        Open.Push -> if (pushFits(k)) pushSeconds else null
    }

    fun closedSeconds(p: UpdatePlan, k: UpdateKind): Int? = when (closed(p, k)) {
        Closed.NONE -> null
        Closed.REFRESH -> refreshMinutes * 60
        Closed.SOCKET -> 1
        Closed.PUSH -> if (pushFits(k)) pushSeconds else null
    }

    /** Push suits changes that come now and then, not a stream of them (the platforms throttle frequent messages). */
    fun pushFits(k: UpdateKind): Boolean =
        k.changesTenths / 10.0 / (k.openMinutes + k.closedMinutes) <= pushPerMinuteLimitTenths / 10.0

    fun misses(p: UpdatePlan, k: UpdateKind): Boolean {
        val o = openSeconds(p, k)
        if (k.openFreshness != null && (o == null || o > k.openFreshness)) return true
        val c = closedSeconds(p, k)
        if (k.closedFreshness != null && (c == null || c > k.closedFreshness)) return true
        return false
    }

    private fun kg(p: UpdatePlan): Triple<Double, Double, Double> {
        var phoneJ = 0.0; var serverJ = 0.0; var kb = 0.0
        for (k in kinds) {
            val (a, b, c) = openCost(p, k)
            val (d, e, f) = closedCost(p, k)
            phoneJ += (a + d) * k.periodsPerYear; serverJ += (b + e) * k.periodsPerYear; kb += (c + f) * k.periodsPerYear
        }
        val (g, h, i) = bundledCost(p)
        phoneJ += g; serverJ += h; kb += i
        val perKwh = gramsPerKwh / 1_000.0
        return Triple(phoneJ / 3.6e6 * perKwh, serverJ / 3.6e6 * pueHundredths / 100 * perKwh, kb / 1e6 * kwhPerGb * perKwh)
    }

    fun result(p: UpdatePlan): UpdatesResult {
        val (ph, sv, nw) = kg(p)
        val (rph, rsv, rnw) = kg(reference)
        return UpdatesResult(
            referenceKg = (rph + rsv + rnw).roundToLong() + otherKg,
            planKg = (ph + sv + nw).roundToLong() + otherKg,
            phonesKg = ph.roundToLong(), serversKg = sv.roundToLong(), networkKg = nw.roundToLong(), otherKg = otherKg,
            openSeconds = kinds.associate { it.name to openSeconds(p, it) },
            closedSeconds = kinds.associate { it.name to closedSeconds(p, it) },
            missed = kinds.filter { misses(p, it) }.map { it.name },
        )
    }

    fun goalsMet(r: UpdatesResult, goals: StarGoals): GoalsMet {
        val rules = r.missed.isEmpty()
        val special = (goals.special as? SpecialGoal.Reduction)?.permille ?: 1_000
        return GoalsMet(rules, rules && r.reductionPermille >= goals.reduction, rules && r.reductionPermille >= special)
    }

    fun stars(r: UpdatesResult, goals: StarGoals): Int {
        val met = goalsMet(r, goals)
        if (!met.rules) return 0
        return 1 + (if (met.reduction) 1 else 0) + (if (met.special) 1 else 0)
    }
}
