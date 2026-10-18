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
/** What the lower lane shows when a scenario has more than one kind of work (O11: it follows the panel's tab). */
enum class Lane { LOAD, JOBS, TRAINING, ALL, CONTRACT }

/** E7: the week starts on Monday. */
private val WEEKDAYS = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")

@Composable
fun TimelineChart(simulation: Simulation, version: Int, modifier: Modifier = Modifier, other: Pair<String, Simulation>? = null, lane: Lane? = null, siteName: String? = null) {
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
        if (version >= 0) TimelineDrawing(simulation, measurer, this, hover, other, lane, siteName).draw()
    }
}

private class TimelineDrawing(val simulation: Simulation, val measurer: TextMeasurer, val scope: DrawScope, val hover: Offset?, val other: Pair<String, Simulation>? = null, val lane: Lane? = null, val siteName: String? = null) {
    val scenario = simulation.scenario
    val start = scenario.startTick
    val end = scenario.endTick
    val now = minOf(simulation.tick, end)
    val batchLane = lane == Lane.JOBS || lane == Lane.ALL || (lane == null && scenario.interactive.peak == 0 && simulation.jobs.isNotEmpty())
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
        val topBottom = topTop + usable * (if (lane == Lane.ALL) 0.34f else 0.52f) - 16f * density
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
                // O10: the other site's grid as a thin line – the gap between the two is what moving a job saves.
                val otherLine = other?.second?.let { o -> (start..end).map { it to if (it <= now) o.grid.intensityTenths(it) else o.grid.forecastTenths(it, now) } }
                val maxTenths = maxOf(10, (past.map { it.second } + forecast.map { it.second * (1_000 + it.third) / 1_000 } +
                    (otherLine?.map { it.second } ?: emptyList())).maxOrNull() ?: 10)
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
                if (otherLine != null) {
                    val color = Palette.accent
                    val line = Path().apply {
                        moveTo(x(otherLine[0].first), yI(otherLine[0].second))
                        otherLine.drop(1).forEach { lineTo(x(it.first), yI(it.second)) }
                    }
                    drawPath(line, color.copy(alpha = 0.85f), style = Stroke(1.5f * density))
                    text(other!!.first, Offset(right + 6f * density, yI(otherLine.last().second)), Type.style(11, FontWeight.SemiBold, color))
                }
                // O9: the eco threshold and the hours the eco mode was on.
                scenario.streaming?.let { model ->
                    val c = simulation.policy.streaming
                    if (c.eco) {
                        val y = yI(c.ecoAbove * 10)
                        drawLine(Palette.accent, Offset(left, y), Offset(right, y), 1.5f * density, pathEffect = PathEffect.dashPathEffect(floatArrayOf(5f * density, 4f * density)))
                        text(t("eco above %lld", c.ecoAbove), Offset(right + 6f * density, y), Type.style(11, FontWeight.SemiBold, Palette.accent))
                    }
                    for (r in simulation.history) if (r.eco) {
                        drawRect(Palette.accent, Offset(x(r.tick), topBottom + 1f), Size(maxOf(1f, x(r.tick + 1) - x(r.tick)), 4f * density))
                    }
                }
                axis(topTop, topBottom, scaleTop.toInt().toString())
                val caption = if (scenario.streaming != null) t("Carbon intensity of the grid, g CO₂/kWh – dashed: forecast; green: eco mode on") else t("Carbon intensity of the grid, g CO₂/kWh – dashed: forecast")
                text(if (siteName != null) "$siteName · $caption" else caption, Offset(left, topTop - 12f * density), Type.style(12, FontWeight.SemiBold))
            }
        }

        // Lower lane
        if (lane == Lane.ALL && scenario.contract != null) {
            // O11 "Green on paper": the contract's clean energy, the shop's load and the batch jobs, one under the other.
            val gap = 24f * density
            val h = lowBottom - lowTop - 2 * gap
            val contractBottom = lowTop + h * 0.36f
            val loadTop = contractBottom + gap
            val loadBottom = loadTop + h * 0.28f
            drawContract(lowTop, contractBottom)
            drawLoad(loadTop, loadBottom, future)
            drawJobs(loadBottom + gap, lowBottom)
            text(t("Batch jobs – light: arrival to deadline, outlined: planned, filled: running or done"), Offset(left, loadBottom + gap - 12f * density), Type.style(12, FontWeight.SemiBold))
        } else if (lane == Lane.CONTRACT) {
            drawContract(lowTop, lowBottom)
        } else if (scenario.services != null) {
            drawServices(lowTop, lowBottom)
        } else if (scenario.pipeline != null) {
            drawPipeline(lowTop, lowBottom)
        } else if (scenario.web != null) {
            drawWeb(lowTop, lowBottom)
        } else if (scenario.inference != null) {
            drawInference(lowTop, lowBottom, future)
        } else if (lane == Lane.ALL) {
            // O11: the shop's load, the batch jobs and the training, one under the other.
            val gap = 24f * density
            val h = lowBottom - lowTop - 2 * gap
            val loadBottom = lowTop + h * 0.30f
            val jobsTop = loadBottom + gap
            val jobsBottom = jobsTop + h * 0.42f
            drawLoad(lowTop, loadBottom, future)
            drawJobs(jobsTop, jobsBottom)
            text(t("Batch jobs – light: arrival to deadline, outlined: planned, filled: running or done"), Offset(left, jobsTop - 12f * density), Type.style(12, FontWeight.SemiBold))
            drawTraining(jobsBottom + gap, lowBottom)
        } else if (lane == Lane.TRAINING || (lane == null && scenario.training != null)) {
            drawTraining(lowTop, lowBottom)
        } else if (batchLane) {
            drawJobs(lowTop, lowBottom)
            text(t("Batch jobs – light: arrival to deadline, outlined: planned, filled: running or done"), Offset(left, lowTop - 12f * density), Type.style(12, FontWeight.SemiBold))
        } else {
            drawLoad(lowTop, lowBottom, future)
        }

        // Hours and now
        // Hour labels: every 3 hours, every 6 from three days on; from six days on one label per day.
        var labelHours = when { scenario.days >= 6 -> 24; scenario.days >= 3 -> 6; else -> 3 }
        // Narrow charts (O10: two side by side): fewer labels, at least about 56 dp apart.
        while (labelHours < 24 && (right - left) / (scenario.days * 24f / labelHours) < 56f * density) labelHours *= 2
        for (tick in start..end) {
            val time = GameTime(tick)
            if (time.minute == 0 && time.hour % labelHours == 0) {
                val anchor = if (tick == start) 0f else if (tick == end) 1f else 0.5f
                val dayNumber = time.day - GameTime.START.day + 1
                val label = if (labelHours == 24) (if (tick == end) "" else if (scenario.pipeline != null) t(WEEKDAYS[(dayNumber - 1) % 7]) else if (scenario.renewal != null) t("Q%lld/%lld", (dayNumber - 1) % 4 + 1, (dayNumber - 1) / 4 + 1) else t("Day %lld", dayNumber)) else time.clockText
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
        } else if (scenario.training != null) {
            if (record != null) lines += t("%lld GPU servers on, progress %@", record.serversOn,
                Format.wholePercent(minOf(1_000L, record.trainingDone * 1_000 / (scenario.training?.work ?: 1)).toInt()))
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

    /**
     * O8: when the GPUs train (blue: training, light: loading the checkpoint after a pause; ahead dashed: the plan with
     * the rules now) and the progress (line, 100 % at the top).
     */
    private fun DrawScope.drawTraining(top: Float, bottom: Float) {
        val model = scenario.training ?: return
        val history = simulation.history
        val plan = if (now < end) simulation.projectedHistory(simulation.policy) else emptyList()
        val barTop = top + (bottom - top) * 0.55f
        fun bars(records: List<de.wannerit.loadshift.core.TickRecord>, before: Long, planned: Boolean) {
            var last = before
            for (r in records) {
                if (r.training) {
                    val progressed = r.trainingDone > last
                    val color = (if (progressed) Palette.trainingLoad else Palette.booting).copy(alpha = if (planned) 0.45f else 1f)
                    drawRect(color, Offset(x(r.tick), barTop), Size(maxOf(1f, x(r.tick + 1) - x(r.tick)), bottom - barTop))
                }
                last = r.trainingDone
            }
        }
        bars(history, 0, false)
        bars(plan, simulation.totals.trainingDone, true)
        fun yP(done: Long) = barTop - (barTop - top) * (minOf(done, model.work.toLong()).toFloat() / model.work)
        val line = Path().apply {
            moveTo(x(start), yP(0))
            history.forEach { lineTo(x(it.tick + 1), yP(it.trainingDone)) }
        }
        drawPath(line, Palette.accent, style = Stroke(2f * density))
        if (plan.isNotEmpty()) {
            val ahead = Path().apply {
                moveTo(x(now), yP(simulation.totals.trainingDone))
                plan.forEach { lineTo(x(it.tick + 1), yP(it.trainingDone)) }
            }
            drawPath(ahead, Palette.accent.copy(alpha = 0.6f), style = Stroke(1.5f * density, pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f * density, 3f * density))))
        }
        drawLine(Palette.accent.copy(alpha = 0.3f), Offset(left, top), Offset(right, top), 1f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(3f * density, 3f * density)))
        text("100 %", Offset(right + 6f * density, top))
        drawLine(Color.Gray.copy(alpha = 0.5f), Offset(left, bottom), Offset(right, bottom), 1f)
        text(t("Training – green: progress, violet: GPUs training, light blue: loading after a pause; dashed: the plan with the rules now"), Offset(left, top - 12f * density), Type.style(12, FontWeight.SemiBold))
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

    /** B12: the data center's power (line) and the clean energy there for it in the same hour (green). */
    private fun DrawScope.drawContract(top: Float, bottom: Float) {
        val records = simulation.history.mapNotNull { r -> r.contract?.let { r.tick to it } }
        val maxW = maxOf(1_000, records.maxOfOrNull { maxOf(it.second.demandWatts, it.second.cleanWatts) } ?: 1_000)
        val scaleTop = niceCeiling((maxW + 999) / 1_000).toFloat()
        fun yW(w: Int) = bottom - (bottom - top) * (w / 1_000f / scaleTop)
        if (records.isNotEmpty()) {
            val clean = Path().apply {
                moveTo(x(records[0].first), bottom)
                for ((tick, r) in records) { val y = yW(r.cleanWatts); lineTo(x(tick), y); lineTo(x(tick + 1), y) }
                lineTo(x(records.last().first + 1), bottom)
                close()
            }
            drawPath(clean, Palette.accent.copy(alpha = 0.35f))
            val demand = Path().apply {
                moveTo(x(records[0].first), yW(records[0].second.demandWatts))
                for ((tick, r) in records) { val y = yW(r.demandWatts); lineTo(x(tick), y); lineTo(x(tick + 1), y) }
            }
            drawPath(demand, Palette.ink.copy(alpha = 0.8f), style = Stroke(1.5f * density))
        }
        axis(top, bottom, "${scaleTop.toInt()} kW")
        text(t("Power of the data center (line) and clean energy from the contract in the same hour (green), kW"), Offset(left, top - 12f * density), Type.style(12, FontWeight.SemiBold))
    }

    /** E5: CPU in cores, stacked: the services' own work and the rest (communication, external calls, idle instances). */
    private fun DrawScope.drawServices(top: Float, bottom: Float) {
        val records = simulation.history.mapNotNull { r -> r.services?.let { r.tick to it } }
        val maxCores = maxOf(1, (records.maxOfOrNull { maxOf(it.second.cpuMs, (it.second.servers * de.wannerit.loadshift.core.ServiceModel.SERVER_MS * 0.7).toInt()) } ?: 1_000) / 1_000)
        val scaleTop = niceCeiling(maxCores).toFloat()
        fun yC(ms: Int) = bottom - (bottom - top) * (ms / 1_000f / scaleTop)
        if (records.isNotEmpty()) {
            for (layer in 2 downTo 0) {
                val area = Path().apply {
                    moveTo(x(records[0].first), bottom)
                    for ((tick, r) in records) {
                        val y = yC(when (layer) { 0 -> r.cpuMs - r.overheadMs - r.optimizerMs; 1 -> r.cpuMs - r.overheadMs; else -> r.cpuMs })
                        lineTo(x(tick), y); lineTo(x(tick + 1), y)
                    }
                    lineTo(x(records.last().first + 1), bottom)
                    close()
                }
                drawPath(area, Color.White)
                drawPath(area, (when (layer) { 0 -> Palette.interactiveLoad; 1 -> Palette.trainingLoad; else -> Palette.amber }).copy(alpha = 0.65f))
            }
            // Servers ready (capacity at 70 %), as a line.
            fun ready(r: de.wannerit.loadshift.core.ServiceRecord) = (r.servers * de.wannerit.loadshift.core.ServiceModel.SERVER_MS * 0.7).toInt()
            val line = Path().apply {
                moveTo(x(records[0].first), yC(ready(records[0].second)))
                for ((tick, r) in records) { val y = yC(ready(r)); lineTo(x(tick), y); lineTo(x(tick + 1), y) }
            }
            drawPath(line, Palette.ink.copy(alpha = 0.6f), style = Stroke(1.2f * density))
        }
        axis(top, bottom, t("%lld cores", scaleTop.toInt()))
        text(t("CPU – blue: tracking, violet: route optimization, amber: communication, external calls, idle instances; line: servers ready"), Offset(left, top - 12f * density), Type.style(12, FontWeight.SemiBold))
    }

    /** E7: build agents and test environment servers on, stacked; a violet mark for the nightly UI tests. */
    private fun DrawScope.drawPipeline(top: Float, bottom: Float) {
        val records = simulation.history.mapNotNull { r -> r.pipeline?.let { r.tick to it } }
        val scaleTop = niceCeiling(simulation.servers.size).toFloat()
        fun yS(n: Int) = bottom - (bottom - top) * (n / scaleTop)
        if (records.isNotEmpty()) {
            for (layer in 1 downTo 0) {
                val area = Path().apply {
                    moveTo(x(records[0].first), bottom)
                    for ((tick, r) in records) {
                        val y = yS(if (layer == 0) r.runnersOn else r.runnersOn + r.environmentsOn)
                        lineTo(x(tick), y); lineTo(x(tick + 1), y)
                    }
                    lineTo(x(records.last().first + 1), bottom)
                    close()
                }
                drawPath(area, Color.White)
                drawPath(area, (if (layer == 0) Palette.interactiveLoad else Palette.capacity).copy(alpha = 0.7f))
            }
            for ((tick, r) in records) if (r.nightly) {
                drawRect(Palette.trainingLoad, Offset(x(tick), top), Size(maxOf(2f, x(tick + 1) - x(tick)), bottom - top))
            }
        }
        axis(top, bottom, t("%lld servers", scaleTop.toInt()))
        text(t("Servers on – blue: build agents, light blue: test environments; violet: UI tests at night"), Offset(left, top - 12f * density), Type.style(12, FontWeight.SemiBold))
    }

    /** E4: the power of a visit's three parts, stacked: data center, network, visitors' devices. */
    private fun DrawScope.drawWeb(top: Float, bottom: Float) {
        val records = simulation.history.mapNotNull { r -> r.web?.let { r to it } }
        val colors = listOf(Palette.demand, Palette.amber, Palette.trainingLoad)
        fun parts(r: de.wannerit.loadshift.core.TickRecord, w: de.wannerit.loadshift.core.WebRecord) = listOf(r.facilityPower, w.networkWatts, w.deviceWatts)
        val maxW = maxOf(1_000, records.maxOfOrNull { (r, w) -> parts(r, w).sum() } ?: 1_000)
        val scaleTop = niceCeiling((maxW + 999) / 1_000).toFloat()
        fun yW(watts: Int) = bottom - (bottom - top) * (watts / 1_000f / scaleTop)
        if (records.isNotEmpty()) {
            for (layer in 2 downTo 0) {
                val area = Path().apply {
                    moveTo(x(records[0].first.tick), bottom)
                    for ((r, w) in records) {
                        val y = yW(parts(r, w).take(layer + 1).sum())
                        lineTo(x(r.tick), y); lineTo(x(r.tick + 1), y)
                    }
                    lineTo(x(records.last().first.tick + 1), bottom)
                    close()
                }
                drawPath(area, Color.White)
                drawPath(area, colors[layer].copy(alpha = 0.6f))
            }
        }
        axis(top, bottom, "${scaleTop.toInt()} kW")
        text(t("Power, kW – blue: data center, amber: network, violet: visitors' devices"), Offset(left, top - 12f * density), Type.style(12, FontWeight.SemiBold))
    }

    /** E6: the shop assistant's power, stacked by kind of question; grey the idle rest of the GPU servers. */
    private fun DrawScope.drawInference(top: Float, bottom: Float, future: List<Int>) {
        val model = scenario.inference ?: return
        val records = simulation.history.mapNotNull { r -> r.inference?.let { r.tick to it } }
        val config = simulation.policy.inference
        val peak = scenario.interactive.peak
        fun curve(tick: Int) = if (peak == 0) 0 else scenario.interactive.expected(tick) * 1_000 / peak
        // Expected power with the rules now (W), for the rest of the run.
        val expected = future.map { tick ->
            val energy = model.energy(config, curve(tick))
            val gpu = model.kinds.indices.filter { model.answerer(config, it).gpu }.sumOf { energy[it] } / 250.0
            val servers = model.servers(gpu)
            tick to (energy.sum() / 250.0 + maxOf(0.0, servers * de.wannerit.loadshift.core.ServerKind.GPU_CURRENT.idlePower - gpu)).toInt()
        }
        val maxW = maxOf(1_000, (records.map { it.second.kindWatts.sum() + it.second.idleWatts } + expected.map { it.second }).maxOrNull() ?: 1_000)
        val scaleTop = niceCeiling((maxW + 999) / 1_000).toFloat()
        fun yW(watts: Int) = bottom - (bottom - top) * (watts / 1_000f / scaleTop)
        fun kindColor(k: Int) = Palette.cacheKinds[k % Palette.cacheKinds.size]
        if (records.isNotEmpty()) {
            // Stacked areas: the kinds of question from the bottom, the idle rest on top.
            val layers = model.kinds.size + 1
            for (layer in (0 until layers).reversed()) {
                fun upTo(r: de.wannerit.loadshift.core.InferenceRecord) = r.kindWatts.take(layer + 1).sum() + if (layer == model.kinds.size) r.idleWatts else 0
                val area = Path().apply {
                    moveTo(x(records[0].first), bottom)
                    for ((tick, r) in records) { lineTo(x(tick), yW(upTo(r))); lineTo(x(tick + 1), yW(upTo(r))) }
                    lineTo(x(records.last().first + 1), bottom)
                    close()
                }
                drawPath(area, Color.White)
                drawPath(area, if (layer == model.kinds.size) Color.Gray.copy(alpha = 0.35f) else kindColor(layer).copy(alpha = 0.6f))
            }
        }
        if (expected.size > 1) {
            val line = Path().apply {
                moveTo(x(expected[0].first), yW(expected[0].second))
                expected.drop(1).forEach { lineTo(x(it.first), yW(it.second)) }
            }
            drawPath(line, Palette.ink.copy(alpha = 0.5f), style = Stroke(1.5f * density, pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f * density, 3f * density))))
        }
        axis(top, bottom, "${scaleTop.toInt()} kW")
        text(t("Shop assistant, kW – colors: the kinds of question as in the panel; grey: GPU servers idle; dashed: expected with the rules now"),
            Offset(left, top - 12f * density), Type.style(12, FontWeight.SemiBold))
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
            // On a low chart the names would lie on top of each other; the tooltip still names the jobs.
            if (rowHeight >= 11f * density) text(t(job.spec.name), Offset(right + 6f * density, y0 + rowHeight / 2), Type.style(11, color = Palette.ink.copy(alpha = 0.8f)))
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
