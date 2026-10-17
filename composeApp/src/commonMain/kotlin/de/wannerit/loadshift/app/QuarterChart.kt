package de.wannerit.loadshift.app

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import de.wannerit.loadshift.core.GameTime
import de.wannerit.loadshift.core.Simulation

/**
 * O7: the three years quarter by quarter, for both sites. Above: CO₂ per quarter and site – electricity (dark) and
 * hardware (light). Below: capacity of the installed servers (steps) and the peak load (line, dotted ahead).
 */
@Composable
fun QuarterChart(game: GameController, modifier: Modifier = Modifier) {
    val measurer = rememberTextMeasurer()
    var hover by remember { mutableStateOf<Offset?>(null) }
    val version = game.version
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
        if (version >= 0) {
            val sims = (game.scenario.renewal?.sites?.indices ?: 0 until 1).map { game.simulationOf(it) }
            QuarterDrawing(game, sims, measurer, this, hover).draw()
        }
    }
}

private class QuarterDrawing(val game: GameController, val sims: List<Simulation>, val measurer: TextMeasurer, val scope: DrawScope, val hover: Offset?) {
    val scenario = game.scenario
    val days = scenario.days
    val density = scope.density
    val left = 44f * density
    val right = scope.size.width - 74f * density
    val colors = listOf(Palette.red, Palette.accent)

    fun x(day: Double): Float = left + (right - left) * (day / days).toFloat()

    fun text(s: String, at: Offset, style: TextStyle = Type.small, anchorX: Float = 0f, anchorY: Float = 0.5f) {
        val layout = measurer.measure(s, style)
        scope.drawText(layout, topLeft = Offset(at.x - layout.size.width * anchorX, at.y - layout.size.height * anchorY))
    }

    /** Emissions (operational, embodied) of a site in a quarter, µg; null if the quarter has not started. */
    fun quarter(sim: Simulation, day: Int): Pair<Long, Long>? {
        val records = sim.history.filter { (it.tick - scenario.startTick) / GameTime.TICKS_PER_DAY == day }
        if (records.isEmpty()) return null
        val before = sim.history.lastOrNull { it.tick < records.first().tick }
        val total = records.last().emissionsSoFar - (before?.emissionsSoFar ?: 0)
        val embodied = records.last().embodiedSoFar - (before?.embodiedSoFar ?: 0)
        return (total - embodied) to embodied
    }

    fun draw() = with(scope) {
        val labelBand = 18f * density
        val laneGap = 26f * density
        val usable = size.height - labelBand - laneGap
        val topTop = 16f * density
        val topBottom = topTop + usable * 0.52f - 16f * density
        val lowTop = topBottom + laneGap + 14f * density
        val lowBottom = size.height - labelBand
        val now = (sims.first().tick - scenario.startTick).toDouble() / GameTime.TICKS_PER_DAY

        // CO₂ per quarter and site
        val data = sims.map { sim -> (0 until days).map { quarter(sim, it) } }
        val refData = sims.indices.map { i -> (0 until days).map { quarter(game.referenceOf(i), it) } }
        val maxT = maxOf(1.0, (data + refData).flatten().filterNotNull().maxOfOrNull { (it.first + it.second) / 1e12 } ?: 1.0)
        val scaleTop = niceCeiling(maxT)
        fun yT(t: Double) = topBottom - (topBottom - topTop) * (t / scaleTop).toFloat()
        val slot = (right - left) / days
        val barWidth = slot * 0.32f
        for (day in 0 until days) {
            for ((i, series) in data.withIndex()) {
                val q = series[day] ?: continue
                val x0 = x(day.toDouble()) + slot * 0.14f + i * (barWidth + slot * 0.08f)
                val op = q.first / 1e12
                val em = q.second / 1e12
                drawRect(colors[i].copy(alpha = 0.75f), Offset(x0, yT(op)), Size(barWidth, topBottom - yT(op)))
                drawRect(colors[i].copy(alpha = 0.3f), Offset(x0, yT(op + em)), Size(barWidth, yT(op) - yT(op + em)))
                // The reference operator in the same quarter: a black line across the bar.
                quarter(game.referenceOf(i), day)?.let { r ->
                    val y = yT((r.first + r.second) / 1e12)
                    drawLine(Palette.ink, Offset(x0 - 2f * density, y), Offset(x0 + barWidth + 2f * density, y), 1.5f * density)
                }
            }
        }
        axis(topTop, topBottom, Format.number(scaleTop, 0))
        text(t("CO₂ per quarter, t – dark: electricity, light: hardware; red: Warsaw, green: Paris; black line: reference"), Offset(left, topTop - 12f * density), Type.style(12, FontWeight.SemiBold))

        // Capacity and peak load per site
        val peaks = sims.map { sim -> (0 until days).map { d -> (0 until GameTime.TICKS_PER_DAY).maxOf { sim.scenario.interactive.expected(scenario.startTick + d * GameTime.TICKS_PER_DAY + it) } } }
        val capacities = sims.map { sim -> (0 until days).map { d -> sim.history.lastOrNull { (it.tick - scenario.startTick) / GameTime.TICKS_PER_DAY == d }?.fleetCapacity } }
        val maxU = niceCeiling(maxOf(10.0, (peaks.flatten() + capacities.flatten().filterNotNull()).max().toDouble() * 1.1))
        fun yU(u: Int) = lowBottom - (lowBottom - lowTop) * (u / maxU).toFloat()
        for (i in sims.indices) {
            val steps = Path()
            var started = false
            for (d in 0 until days) {
                val c = capacities[i][d] ?: break
                if (!started) { steps.moveTo(x(d.toDouble()), yU(c)); started = true } else steps.lineTo(x(d.toDouble()), yU(c))
                steps.lineTo(x(d + 1.0), yU(c))
            }
            if (started) drawPath(steps, colors[i].copy(alpha = 0.8f), style = Stroke(2f * density))
        }
        // The peak load is the same at both sites: one line, solid so far, dotted ahead.
        val current = kotlin.math.floor(now).toInt().coerceIn(0, days - 1)
        val points = (0 until days).map { Offset(x(it + 0.5), yU(peaks[0][it])) }
        val past = Path().apply { moveTo(points[0].x, points[0].y); for (d in 1..current) lineTo(points[d].x, points[d].y) }
        val ahead = Path().apply { moveTo(points[current].x, points[current].y); for (d in current + 1 until days) lineTo(points[d].x, points[d].y) }
        drawPath(past, Palette.ink, style = Stroke(1.8f * density))
        if (current < days - 1) drawPath(ahead, Palette.ink.copy(alpha = 0.5f), style = Stroke(1.5f * density, pathEffect = PathEffect.dashPathEffect(floatArrayOf(2f * density, 3f * density))))
        axis(lowTop, lowBottom, maxU.toInt().toString())
        text(t("Capacity of the servers – red: Warsaw, green: Paris; black: peak load of a site, dotted ahead; load units"), Offset(left, lowTop - 12f * density), Type.style(12, FontWeight.SemiBold))

        // Quarters and now
        for (d in 0..days) {
            drawLine(Color.Gray.copy(alpha = if (d % 4 == 0) 0.35f else 0.12f), Offset(x(d.toDouble()), topTop), Offset(x(d.toDouble()), lowBottom), 1f)
            if (d < days) text(t("Q%lld/%lld", d % 4 + 1, d / 4 + 1), Offset(x(d + 0.5), size.height - labelBand / 2), anchorX = 0.5f)
        }
        drawLine(Palette.ink, Offset(x(now), topTop - 4f * density), Offset(x(now), lowBottom), 1.5f * density)
        hover?.let { tooltip(it, topTop, lowBottom, data, capacities, peaks) }
    }

    private fun DrawScope.tooltip(at: Offset, top: Float, bottom: Float, data: List<List<Pair<Long, Long>?>>, capacities: List<List<Int?>>, peaks: List<List<Int>>) {
        if (at.x < left || at.x > right || at.y < top - 20f * density || at.y > bottom) return
        val day = ((at.x - left) / (right - left) * days).toInt().coerceIn(0, days - 1)
        val lines = mutableListOf(t("Quarter %lld, year %lld", day % 4 + 1, day / 4 + 1))
        for ((i, site) in (scenario.renewal?.sites ?: emptyList()).withIndex()) {
            val q = data[i][day]
            lines += if (q == null) t("%@: peak %lld", t(site.title), peaks[i][day])
            else t("%@: %@ t CO₂ (%@ t hardware), capacity %lld, peak %lld", t(site.title), Format.tonnes(q.first + q.second), Format.tonnes(q.second), capacities[i][day] ?: 0, peaks[i][day])
        }
        val layouts = lines.mapIndexed { i, s -> measurer.measure(s, if (i == 0) Type.style(13, FontWeight.SemiBold) else Type.style(12)) }
        val pad = 8f * density
        val w = layouts.maxOf { it.size.width } + 2 * pad
        val h = layouts.sumOf { it.size.height } + 2 * pad
        val xLine = x(day + 0.5)
        drawLine(Palette.ink.copy(alpha = 0.4f), Offset(xLine, top), Offset(xLine, bottom), 1f)
        val boxX = if (xLine + 12f * density + w < size.width) xLine + 12f * density else xLine - 12f * density - w
        drawRoundRect(Color.White, Offset(boxX, top), Size(w, h), CornerRadius(8f * density))
        drawRoundRect(Palette.divider, Offset(boxX, top), Size(w, h), CornerRadius(8f * density), style = Stroke(1f * density))
        var y = top + pad
        for (layout in layouts) {
            drawText(layout, topLeft = Offset(boxX + pad, y))
            y += layout.size.height
        }
    }

    private fun DrawScope.axis(top: Float, bottom: Float, maxLabel: String) {
        drawLine(Color.Gray.copy(alpha = 0.5f), Offset(left, bottom), Offset(right, bottom), 1f)
        text(maxLabel, Offset(left - 6f * density, top), anchorX = 1f)
        text("0", Offset(left - 6f * density, bottom), anchorX = 1f)
    }

    /** The next round number at or above a value. */
    private fun niceCeiling(value: Double): Double {
        var magnitude = 1.0
        while (magnitude * 10 <= value) magnitude *= 10
        for (step in listOf(1, 2, 3, 4, 6, 8, 10)) if (step * magnitude >= value) return step * magnitude
        return 10 * magnitude
    }
}
