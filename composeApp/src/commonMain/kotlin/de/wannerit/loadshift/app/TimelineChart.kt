package de.wannerit.loadshift.app

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import de.wannerit.loadshift.core.BatchJobState
import de.wannerit.loadshift.core.GameTime
import de.wannerit.loadshift.core.ServerKind
import de.wannerit.loadshift.core.Simulation

/**
 * The timeline (docs/CONCEPT.md 10.1): carbon intensity of the grid – what happened so far and the forecast with its
 * uncertainty band – and below it the load and capacity, or one row per batch job. The vertical line is now.
 */
@Composable
fun TimelineChart(simulation: Simulation, version: Int, modifier: Modifier = Modifier) {
    val measurer = rememberTextMeasurer()
    // Where the mouse is: the chart shows the values at that time.
    var hover by remember { mutableStateOf<Offset?>(null) }
    Canvas(
        modifier.padding(horizontal = 16.dp, vertical = 10.dp).pointerInput(Unit) {
            awaitPointerEventScope {
                while (true) {
                    val event = awaitPointerEvent()
                    hover = if (event.type == PointerEventType.Exit) null else event.changes.firstOrNull()?.position
                }
            }
        },
    ) {
        // Reading the version redraws the chart after every tick.
        if (version >= 0) TimelineDrawing(simulation, measurer, this, hover).draw()
    }
}

private class TimelineDrawing(val simulation: Simulation, val measurer: TextMeasurer, val scope: DrawScope, val hover: Offset?) {
    val scenario = simulation.scenario
    val start = scenario.startTick
    val end = scenario.endTick
    val now = minOf(simulation.tick, end)
    val batchLane = scenario.interactive.peak == 0 && simulation.jobs.isNotEmpty()
    val density = scope.density
    val left = 44f * density
    val right = scope.size.width - (if (batchLane) 130f else if (scenario.caching != null) 100f else 74f) * density
    val labelBand = 18f * density

    fun x(tick: Int): Float = left + (right - left) * (tick - start) / (end - start).toFloat()

    fun text(s: String, at: Offset, style: TextStyle = Type.small, anchorX: Float = 0f, anchorY: Float = 0.5f) {
        val layout = measurer.measure(s, style)
        scope.drawText(layout, topLeft = Offset(at.x - layout.size.width * anchorX, at.y - layout.size.height * anchorY))
    }

    fun draw() = with(scope) {
        val laneGap = 26f * density
        val usable = size.height - labelBand - laneGap
        val topTop = 16f * density
        val topBottom = topTop + usable * 0.52f - 16f * density
        val lowTop = topBottom + laneGap + 14f * density
        val lowBottom = size.height - labelBand
        val grid = simulation.grid

        val future = if (now < end) (now..end).toList() else emptyList()
        // Upper lane: what matters for the scenario – the cache memory (E3), the outside temperature (6) or the grid.
        when {
            scenario.caching != null -> drawCache(topTop, topBottom, future)
            scenario.cooling != null -> drawTemperature(topTop, topBottom)
            else -> {
                // Carbon intensity
                val past = simulation.history.map { it.tick to it.intensityTenths }.toMutableList()
                if (past.isNotEmpty() && past.last().first < now) past += now to grid.intensityTenths(now)
                val forecast = future.map { Triple(it, grid.forecastTenths(it, now), grid.band(it - now)) }
                val maxTenths = maxOf(10, (past.map { it.second } + forecast.map { it.second * (1_000 + it.third) / 1_000 }).maxOrNull() ?: 10)
                val scaleTop = niceCeiling(maxTenths / 10).toFloat()
                fun yI(tenths: Int) = topBottom - (topBottom - topTop) * (tenths / 10f / scaleTop)
                if (forecast.size > 1) {
                    val band = Path().apply {
                        moveTo(x(forecast[0].first), yI(forecast[0].second * (1_000 + forecast[0].third) / 1_000))
                        forecast.forEach { lineTo(x(it.first), yI(it.second * (1_000 + it.third) / 1_000)) }
                        forecast.reversed().forEach { lineTo(x(it.first), yI(it.second * (1_000 - it.third) / 1_000)) }
                        close()
                    }
                    drawPath(band, Color.Gray.copy(alpha = 0.16f))
                    val line = Path().apply {
                        moveTo(x(forecast[0].first), yI(forecast[0].second))
                        forecast.drop(1).forEach { lineTo(x(it.first), yI(it.second)) }
                    }
                    drawPath(line, Color.Gray, style = Stroke(1.5f * density, pathEffect = PathEffect.dashPathEffect(floatArrayOf(5f * density, 4f * density))))
                }
                if (past.size > 1) {
                    val area = Path().apply {
                        moveTo(x(past[0].first), topBottom)
                        past.forEach { lineTo(x(it.first), yI(it.second)) }
                        lineTo(x(past.last().first), topBottom)
                        close()
                    }
                    val stops = listOf(0, 150, 350, 500, 700)
                        .map { g -> (1f - (g / scaleTop).coerceIn(0f, 1f)) to Palette.intensity(g).copy(alpha = 0.55f) }
                        .sortedBy { it.first }.toTypedArray()
                    drawPath(area, Brush.verticalGradient(*stops, startY = topTop, endY = topBottom))
                    val line = Path().apply {
                        moveTo(x(past[0].first), yI(past[0].second))
                        past.drop(1).forEach { lineTo(x(it.first), yI(it.second)) }
                    }
                    drawPath(line, Palette.ink.copy(alpha = 0.8f), style = Stroke(1.5f * density))
                }
                axis(topTop, topBottom, scaleTop.toInt().toString())
                text(t("Carbon intensity of the grid, g CO₂/kWh – dashed: forecast"), Offset(left, topTop - 12f * density), Type.style(12, FontWeight.SemiBold))
            }
        }

        // Lower lane
        if (batchLane) {
            drawJobs(lowTop, lowBottom)
            text(t("Batch jobs – light: arrival to deadline, outlined: planned, filled: running or done"), Offset(left, lowTop - 12f * density), Type.style(12, FontWeight.SemiBold))
        } else {
            drawLoad(lowTop, lowBottom, future)
        }

        // Hours and now
        // Hour labels: every 3 hours, every 6 from three days on; from six days on one label per day.
        val labelHours = when { scenario.days >= 6 -> 24; scenario.days >= 3 -> 6; else -> 3 }
        for (tick in start..end) {
            val time = GameTime(tick)
            if (time.minute == 0 && time.hour % labelHours == 0) {
                val anchor = if (tick == start) 0f else if (tick == end) 1f else 0.5f
                val label = if (labelHours == 24) (if (tick == end) "" else t("Day %lld", time.day - GameTime.START.day + 1)) else time.clockText
                text(label, Offset(if (labelHours == 24) x(tick) + 3f * density else x(tick), size.height - labelBand / 2), anchorX = if (labelHours == 24) 0f else anchor)
                drawLine(Color.Gray.copy(alpha = 0.15f), Offset(x(tick), topTop), Offset(x(tick), lowBottom), 1f)
            }
        }
        drawLine(Palette.ink, Offset(x(now), topTop - 4f * density), Offset(x(now), lowBottom), 1.5f * density)
        hover?.let { tooltip(it, topTop, lowBottom) }
    }

    /** The values at the time under the mouse: a line and a small box. */
    private fun DrawScope.tooltip(at: Offset, top: Float, bottom: Float) {
        if (at.x < left || at.x > right || at.y < top - 20f * density || at.y > bottom) return
        val tick = (start + ((at.x - left) / (right - left) * (end - start)).toInt()).coerceIn(start, end - 1)
        val record = simulation.history.firstOrNull { it.tick == tick }
        val grid = simulation.grid
        val lines = mutableListOf(timeText(tick))
        val cache = record?.cache
        val cooling = scenario.cooling
        if (scenario.caching != null) {
            if (cache != null) {
                lines += t("Cache: %lld of %lld GB, %@ hits", (cache.usedTenthsGb + 5) / 10, cache.capacityGb, Format.wholePercent(cache.hits))
                if (cache.staleTenthsPermille > 0) lines += t("Stale answers: %@", Format.number(cache.staleTenthsPermille / 10.0, 1) + " ‰")
                val kinds = scenario.caching?.kinds.orEmpty()
                for ((k, tenths) in cache.kindTenthsGb.withIndex()) {
                    if (tenths > 0 && k < kinds.size) lines += t("%@: %@ GB", t(kinds[k].title), Format.number(tenths / 10.0, 1))
                }
            }
        } else if (cooling != null) {
            val setpoint = record?.setpoint ?: simulation.policy.setpoint
            lines += t("Outside %@ °C, setpoint %lld °C", Format.number(cooling.outsideTenths(tick) / 10.0, 1), setpoint)
            lines += t("Chiller: %@", Format.wholePercent(cooling.mechanicalPermille(tick, setpoint)))
            if (record != null && record.itPower > 0) lines += t("PUE: %@", Format.number(record.facilityPower.toDouble() / record.itPower, 2))
        } else if (record != null) {
            lines += t("Grid: %lld g/kWh", (record.intensityTenths + 5) / 10)
        } else {
            lines += t("Forecast: %lld g/kWh, ±%lld %%", grid.forecast(tick, now), (grid.band(tick - now) + 5) / 10)
        }
        if (batchLane) {
            val names = simulation.jobs.filter { job ->
                val from = job.startedAt ?: job.plannedStart ?: return@filter false
                val to = job.finishedAt ?: (from + job.spec.span)
                tick in from until to
            }.map { t(it.spec.name) }
            lines += when {
                names.isEmpty() -> t("No job")
                tick < now -> t("Running: %@", names.joinToString(", "))
                else -> t("Planned: %@", names.joinToString(", "))
            }
        } else if (record != null) {
            lines += t("Load %lld, capacity %lld (%lld servers on)", record.demand, record.capacity, record.serversOn)
            if (record.inTime < record.demand) lines += t("In time: %@", Format.wholePercent(record.inTime * 1_000 / maxOf(1, record.demand)))
        } else {
            lines += t("Expected load %lld", simulation.expectedDemand(tick))
        }
        val xLine = x(tick) + (x(tick + 1) - x(tick)) / 2
        drawLine(Palette.accent, Offset(xLine, top), Offset(xLine, bottom), 1.2f * density)
        val layouts = lines.mapIndexed { i, s -> measurer.measure(s, if (i == 0) Type.style(13, FontWeight.SemiBold) else Type.style(12)) }
        val pad = 8f * density
        val w = layouts.maxOf { it.size.width } + 2 * pad
        val h = layouts.sumOf { it.size.height } + 2 * pad
        val boxX = if (xLine + 12f * density + w < size.width) xLine + 12f * density else xLine - 12f * density - w
        val boxY = top
        drawRoundRect(Color.White, Offset(boxX, boxY), Size(w, h), CornerRadius(8f * density))
        drawRoundRect(Palette.divider, Offset(boxX, boxY), Size(w, h), CornerRadius(8f * density), style = Stroke(1f * density))
        var y = boxY + pad
        for (layout in layouts) {
            drawText(layout, topLeft = Offset(boxX + pad, y))
            y += layout.size.height
        }
    }

    /**
     * E3: the memory used, stacked by kind of data (areas), the memory of the running cache servers (step line),
     * the memory expected with the rules now (dotted).
     */
    private fun DrawScope.drawCache(top: Float, bottom: Float, future: List<Int>) {
        val model = scenario.caching ?: return
        val records = simulation.history.mapNotNull { r -> r.cache?.let { r.tick to it } }
        val config = simulation.policy.cache
        val expected = future.map { it to (model.state(config, scenario.interactive.expected(it)).usedGb * 10).toInt() }
        val capacityNow = model.servers(config) * model.gbPerServer
        val maxGb = maxOf(16, (records.map { maxOf(it.second.capacityGb, it.second.usedTenthsGb / 10) } + capacityNow +
            expected.map { it.second / 10 }).maxOrNull() ?: 16)
        val scaleTop = niceCeiling(maxGb).toFloat()
        fun yG(tenths: Int) = bottom - (bottom - top) * (tenths / 10f / scaleTop)
        fun kindColor(k: Int) = Palette.cacheKinds[k % Palette.cacheKinds.size]
        if (records.isNotEmpty()) {
            // Stacked areas, the first kind at the bottom.
            for (k in model.kinds.indices.reversed()) {
                if (records.none { (it.second.kindTenthsGb.getOrElse(k) { 0 }) > 0 }) continue
                val area = Path().apply {
                    moveTo(x(records[0].first), bottom)
                    for ((tick, c) in records) lineTo(x(tick), yG(c.kindTenthsGb.take(k + 1).sum()))
                    lineTo(x(records.last().first + 1), yG(records.last().second.kindTenthsGb.take(k + 1).sum()))
                    lineTo(x(records.last().first + 1), bottom)
                    close()
                }
                drawPath(area, Color.White)
                drawPath(area, kindColor(k).copy(alpha = 0.55f))
            }
            val capacity = Path().apply {
                moveTo(x(records[0].first), yG(records[0].second.capacityGb * 10))
                records.forEach { (tick, c) -> lineTo(x(tick), yG(c.capacityGb * 10)); lineTo(x(tick + 1), yG(c.capacityGb * 10)) }
            }
            drawPath(capacity, Palette.ink.copy(alpha = 0.7f), style = Stroke(1.5f * density))
        }
        if (expected.size > 1 && capacityNow > 0) {
            val line = Path().apply {
                moveTo(x(expected[0].first), yG(expected[0].second))
                expected.drop(1).forEach { lineTo(x(it.first), yG(it.second)) }
            }
            drawPath(line, Palette.ink.copy(alpha = 0.5f), style = Stroke(1.5f * density, pathEffect = PathEffect.dashPathEffect(floatArrayOf(2f * density, 3f * density))))
        }
        // Stale answers: a red mark under the lane.
        for ((tick, c) in records) {
            if (c.staleTenthsPermille > 0) {
                drawRect(Palette.red, Offset(x(tick), bottom + 1f), Size(maxOf(1f, x(tick + 1) - x(tick)), 4f * density))
            }
        }
        // The price change.
        model.priceChange?.let { change ->
            drawLine(Palette.red, Offset(x(change), top), Offset(x(change), bottom), 1.5f * density)
            text(t("Prices change"), Offset(x(change) + 4f * density, top + 30f * density), Type.style(11, FontWeight.SemiBold, Palette.red))
        }
        // One dotted line per cache server.
        var gb = model.gbPerServer
        while (gb <= scaleTop) {
            drawLine(Palette.ink.copy(alpha = 0.25f), Offset(left, yG(gb * 10)), Offset(right, yG(gb * 10)), 1f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(1f * density, 5f * density)))
            text(t("%lld cache servers", gb / model.gbPerServer), Offset(right + 6f * density, yG(gb * 10)))
            gb += model.gbPerServer
        }
        axis(top, bottom, scaleTop.toInt().toString())
        val title = t("Cache memory, GB – areas: used, by kind; line: memory of the running cache servers; dotted: expected; red: stale answers")
        text(title, Offset(left, top - 12f * density), Type.style(12, FontWeight.SemiBold))
        // Legend: the kinds in the cache now, top right of the lane.
        var lx = right
        for (k in model.kinds.indices.reversed()) {
            if (config.ttlMinutes.getOrElse(k) { 0 } <= 0 || capacityNow == 0) continue
            val layout = measurer.measure(t(model.kinds[k].title), Type.style(11))
            lx -= layout.size.width
            drawText(layout, topLeft = Offset(lx, top + 2f * density))
            lx -= 14f * density
            drawRect(kindColor(k).copy(alpha = 0.7f), Offset(lx, top + 4f * density), Size(10f * density, 10f * density))
            lx -= 12f * density
        }
    }

    /** Scenario 6: outside temperature (line, dashed ahead), setpoint (step line), hours the outside air cools (green). */
    private fun DrawScope.drawTemperature(top: Float, bottom: Float) {
        val cooling = scenario.cooling ?: return
        val scaleTop = 40f
        fun yT(tenths: Int) = bottom - (bottom - top) * (tenths / 10f / scaleTop).coerceIn(0f, 1f)
        val setpointAt = { tick: Int -> simulation.history.firstOrNull { it.tick == tick }?.setpoint ?: simulation.policy.setpoint }
        // Hours in which outside air does (part of) the cooling.
        for (tick in start until end) {
            val m = cooling.mechanicalPermille(tick, setpointAt(tick))
            if (m < 1_000) drawRect(Palette.accent.copy(alpha = 0.10f + 0.12f * (1_000 - m) / 1_000), Offset(x(tick), top), Size(x(tick + 1) - x(tick), bottom - top))
        }
        val past = Path().apply {
            moveTo(x(start), yT(cooling.outsideTenths(start)))
            for (tick in start + 1..now) lineTo(x(tick), yT(cooling.outsideTenths(tick)))
        }
        if (now > start) drawPath(past, Palette.red.copy(alpha = 0.85f), style = Stroke(1.8f * density))
        if (now < end) {
            val ahead = Path().apply {
                moveTo(x(now), yT(cooling.outsideTenths(now)))
                for (tick in now + 1..end) lineTo(x(tick), yT(cooling.outsideTenths(minOf(tick, end - 1))))
            }
            drawPath(ahead, Palette.red.copy(alpha = 0.6f), style = Stroke(1.5f * density, pathEffect = PathEffect.dashPathEffect(floatArrayOf(5f * density, 4f * density))))
        }
        val setpoint = Path().apply {
            moveTo(x(start), yT(setpointAt(start) * 10))
            for (tick in start until end) { lineTo(x(tick), yT(setpointAt(tick) * 10)); lineTo(x(tick + 1), yT(setpointAt(tick) * 10)) }
        }
        drawPath(setpoint, Palette.ink.copy(alpha = 0.7f), style = Stroke(1.5f * density))
        for (degrees in listOf(10, 20, 30)) {
            drawLine(Color.Gray.copy(alpha = 0.15f), Offset(left, yT(degrees * 10)), Offset(right, yT(degrees * 10)), 1f)
            text("$degrees", Offset(left - 6f * density, yT(degrees * 10)), anchorX = 1f)
        }
        axis(top, bottom, scaleTop.toInt().toString())
        text(t("Outside temperature, °C – dashed: forecast; black: setpoint; green: the outside air cools"), Offset(left, top - 12f * density), Type.style(12, FontWeight.SemiBold))
    }

    private fun DrawScope.axis(top: Float, bottom: Float, maxLabel: String) {
        drawLine(Color.Gray.copy(alpha = 0.5f), Offset(left, bottom), Offset(right, bottom), 1f)
        text(maxLabel, Offset(left - 6f * density, top), anchorX = 1f)
        text("0", Offset(left - 6f * density, bottom), anchorX = 1f)
        drawLine(Color.Gray.copy(alpha = 0.15f), Offset(left, top), Offset(right, top), 1f,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(3f * density, 3f * density)))
    }

    private fun DrawScope.drawLoad(top: Float, bottom: Float, future: List<Int>) {
        val history = simulation.history
        val expected = future.map { it to simulation.expectedDemand(it) }
        val fleetCapacity = simulation.servers.sumOf { it.kind.capacity }
        // The scale: what happened and what is expected; the whole fleet only if it is not much more than that.
        val dataMax = ((history.map { maxOf(it.demand, it.capacity) } + expected.map { it.second }).maxOrNull() ?: 10) * 11 / 10
        val maxUnits = maxOf(10, if (fleetCapacity <= dataMax * 16 / 10) maxOf(fleetCapacity, dataMax) else dataMax)
        val unitsTop = niceCeiling(maxUnits).toFloat()
        fun yU(units: Int) = bottom - (bottom - top) * (units / unitsTop)
        // Campaigns: marked, although the expected load does not know them.
        for (c in scenario.interactive.campaigns) {
            drawRect(Palette.amber.copy(alpha = 0.16f), Offset(x(c.start), top), Size(x(c.end) - x(c.start), bottom - top))
            text(t("Campaign"), Offset(x(c.start) + 3f * density, top + 7f * density), Type.style(11, FontWeight.SemiBold, Color(0xFFB0741A)))
        }
        if (history.isNotEmpty()) {
            val capacity = Path().apply {
                moveTo(x(history[0].tick), bottom)
                history.forEach { lineTo(x(it.tick), yU(it.capacity)); lineTo(x(it.tick + 1), yU(it.capacity)) }
                lineTo(x(history.last().tick + 1), bottom)
                close()
            }
            drawPath(capacity, Palette.capacity.copy(alpha = 0.6f))
            val demand = Path().apply {
                moveTo(x(history[0].tick), yU(history[0].demand))
                history.drop(1).forEach { lineTo(x(it.tick), yU(it.demand)) }
            }
            drawPath(demand, Palette.demand, style = Stroke(1.8f * density))
        }
        if (expected.size > 1) {
            val line = Path().apply {
                moveTo(x(expected[0].first), yU(expected[0].second))
                expected.drop(1).forEach { lineTo(x(it.first), yU(it.second)) }
            }
            drawPath(line, Palette.demand.copy(alpha = 0.6f), style = Stroke(1.5f * density, pathEffect = PathEffect.dashPathEffect(floatArrayOf(2f * density, 3f * density))))
        }
        // Right axis: how many servers that capacity is (one line every five servers).
        val perServer = simulation.servers.firstOrNull { it.kind.servesInteractive }?.kind?.capacity ?: 0
        if (perServer > 0) {
            val servers = (unitsTop / perServer).toInt()
            val stepServers = when { servers <= 30 -> 5; servers <= 60 -> 10; else -> 20 }
            var count = stepServers
            while (count * perServer <= unitsTop) {
                val y = yU(count * perServer)
                drawLine(Palette.capacity.copy(alpha = 0.45f), Offset(left, y), Offset(right, y), 1f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(1f * density, 5f * density)))
                text(t("%lld servers", count), Offset(right + 6f * density, y))
                count += stepServers
            }
        }
        // Slow or dropped requests: a red mark under the lane.
        for (r in history) {
            if (r.inTime < r.demand) {
                drawRect(Palette.red, Offset(x(r.tick), bottom + 1f), Size(maxOf(1f, x(r.tick + 1) - x(r.tick)), 4f * density))
            }
        }
        axis(top, bottom, unitsTop.toInt().toString())
        text(t("Load (line) and capacity of the running servers (area) – red: slow requests"), Offset(left, top - 12f * density), Type.style(12, FontWeight.SemiBold))
    }

    private fun DrawScope.drawJobs(top: Float, bottom: Float) {
        val jobs = simulation.jobs
        val rowHeight = (bottom - top) / maxOf(1, jobs.size)
        val radius = CornerRadius(3f * density)
        for ((row, job) in jobs.withIndex()) {
            val y0 = top + row * rowHeight
            fun bar(from: Int, to: Int): Pair<Offset, Size> =
                Offset(x(from), y0 + rowHeight * 0.18f) to Size(maxOf(2f, x(to) - x(from)), rowHeight * 0.64f)
            val (wo, ws) = bar(job.spec.arrival, minOf(job.spec.deadline, end))
            drawRoundRect(Palette.amber.copy(alpha = 0.14f), wo, ws, radius)
            drawLine(Palette.red.copy(alpha = 0.7f), Offset(x(job.spec.deadline), y0 + 2f), Offset(x(job.spec.deadline), y0 + rowHeight - 2f), 1.5f * density)
            val dash = Stroke(1.5f * density, pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f * density, 3f * density)))
            job.plannedStart?.let { s ->
                val (o, sz) = bar(s, s + job.spec.span)
                drawRoundRect(if (s > job.spec.latestStart) Palette.red else Palette.amber, o, sz, radius, style = dash)
            }
            job.startedAt?.let { started ->
                val finish = job.finishedAt ?: now
                val bootEnd = minOf(started + ServerKind.BOOT_TICKS, finish)
                val (bo, bs) = bar(started, maxOf(bootEnd, started + 1))
                drawRoundRect(Palette.booting, bo, bs, radius)
                if (finish > bootEnd) {
                    val (ro, rs) = bar(bootEnd, finish)
                    drawRoundRect(if (job.missedDeadline(now)) Palette.red else Palette.amber, ro, rs, radius)
                }
                val state = job.state
                if (job.finishedAt == null && state is BatchJobState.Running) {
                    val (po, ps) = bar(now, state.endsAt)
                    drawRoundRect(Palette.amber, po, ps, radius, style = dash)
                }
            }
            text(t(job.spec.name), Offset(right + 6f * density, y0 + rowHeight / 2), Type.style(11, color = Palette.ink.copy(alpha = 0.8f)))
        }
    }

    /** The next round number at or above a value (1, 2 or 5 × 10ⁿ). */
    private fun niceCeiling(value: Int): Int {
        var magnitude = 1
        while (magnitude * 10 <= value) magnitude *= 10
        for (step in listOf(1, 2, 3, 4, 6, 8, 10)) if (step * magnitude >= value) return step * magnitude
        return 10 * magnitude
    }
}
