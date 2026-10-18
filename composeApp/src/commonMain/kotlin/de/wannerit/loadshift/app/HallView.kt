package de.wannerit.loadshift.app

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.dp
import de.wannerit.loadshift.core.Server
import de.wannerit.loadshift.core.ServerKind
import de.wannerit.loadshift.core.ServerState

/**
 * The data center hall in isometric view (docs/CONCEPT.md 10.1, D2): racks of ten servers each. A server is a
 * slab on the rack's front: light grey when off, light blue while booting, dark when on – its bar shows how busy it
 * is, blue for interactive work, amber for a batch job.
 */
@Composable
fun HallView(
    servers: List<Server>, version: Int, interactive: Boolean, batch: Boolean, modifier: Modifier = Modifier,
    cacheSlots: Int = 0, cacheOn: Int = 0, cachePower: Int = 0,
    /** E4: the extra rack holds GPU servers for the language models instead of cache servers. */
    gpuRack: Boolean = false,
    /** O7: two halls side by side – a short status line only. */
    compact: Boolean = false,
) {
    val on = servers.count { it.state.isPowered }
    val itPower = servers.sumOf { it.power() } + cachePower
    Column(modifier.padding(16.dp)) {
        Canvas(Modifier.fillMaxWidth().weight(1f)) {
            // The server list is the same object after every tick; reading the version redraws the hall.
            if (version >= 0) drawHall(servers, cacheSlots, cacheOn, if (gpuRack) Palette.trainingLoad else Palette.cache)
        }
        Row(
            Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            if (compact) {
                if (batch) LegendSwatch(Palette.slab, t("Batch job"), Palette.batchLoad) else LegendSwatch(Palette.old, t("Old server"))
                Spacer(Modifier.weight(1f))
                Txt(t("%lld of %lld servers on", on, servers.size), Type.callout, maxLines = 1)
                return@Row
            }
            LegendSwatch(Palette.off, t("Off"))
            LegendSwatch(Palette.booting, t("Booting"))
            if (interactive && servers.any { it.kind.servesInteractive }) LegendSwatch(Palette.slab, t("Web requests"), Palette.interactiveLoad)
            if (servers.any { it.kind == ServerKind.GPU_CURRENT }) LegendSwatch(Palette.slab, t("Training"), Palette.trainingLoad)
            if (batch) LegendSwatch(Palette.slab, t("Batch job"), Palette.batchLoad)
            if (cacheSlots > 0) LegendSwatch(if (gpuRack) Palette.trainingLoad else Palette.cache, if (gpuRack) t("GPU server for the models") else t("Cache server"))
            if (servers.any { it.kind == ServerKind.CPU_OLD }) LegendSwatch(Palette.old, t("Old server"))
            Spacer(Modifier.weight(1f))
            Txt(
                if (cacheSlots > 0 && gpuRack) t("%lld of %lld servers on + %lld GPU servers · %@ kW IT power", on, servers.size, cacheOn, Format.kilowatts(itPower))
                else if (cacheSlots > 0) t("%lld of %lld servers on + %lld cache servers · %@ kW IT power", on, servers.size, cacheOn, Format.kilowatts(itPower))
                else t("%lld of %lld servers on · %@ kW IT power", on, servers.size, Format.kilowatts(itPower)),
                Type.callout, maxLines = 1,
            )
        }
    }
}

private const val SLOTS_PER_RACK = 10
private const val RACK_WIDTH = 1.0
private const val RACK_DEPTH = 0.7
private const val RACK_HEIGHT = 2.4
private const val RACK_GAP = 0.45

/** Isometric projection of floor coordinates (x to the right back, y to the left front, z up). */
private fun iso(x: Double, y: Double, z: Double) = Offset(((x - y) * 0.866).toFloat(), ((x + y) * 0.5 - z).toFloat())

private fun DrawScope.drawHall(servers: List<Server>, cacheSlots: Int, cacheOn: Int, extraColor: Color) {
    val serverRacks = maxOf(1, (servers.size + SLOTS_PER_RACK - 1) / SLOTS_PER_RACK)
    // E3: one more rack for the cache servers.
    val racks = serverRacks + if (cacheSlots > 0) 1 else 0
    val length = racks * RACK_WIDTH + (racks - 1) * RACK_GAP
    val fx0 = -0.6; val fx1 = length + 0.6; val fy0 = -0.6; val fy1 = RACK_DEPTH + 0.9
    // Fit the projected floor and racks into the canvas.
    val corners = listOf(iso(fx0, fy0, 0.0), iso(fx1, fy0, 0.0), iso(fx0, fy1, 0.0), iso(fx1, fy1, 0.0),
        iso(0.0, 0.0, RACK_HEIGHT), iso(length, 0.0, RACK_HEIGHT))
    val minX = corners.minOf { it.x }; val maxX = corners.maxOf { it.x }
    val minY = corners.minOf { it.y }; val maxY = corners.maxOf { it.y }
    val scale = minOf(size.width / (maxX - minX), size.height / (maxY - minY)) * 0.94f
    val offset = Offset((size.width - (maxX - minX) * scale) / 2 - minX * scale, (size.height - (maxY - minY) * scale) / 2 - minY * scale)
    fun o(x: Double, y: Double, z: Double): Offset { val q = iso(x, y, z); return Offset(q.x * scale + offset.x, q.y * scale + offset.y) }
    fun p(x: Double, y: Double, z: Double): Pair<Float, Float> = o(x, y, z).let { it.x to it.y }
    fun polygon(vararg points: Pair<Float, Float>) = Path().apply { moveTo(points[0].first, points[0].second); points.drop(1).forEach { lineTo(it.first, it.second) }; close() }

    // Floor with a light grid.
    drawPath(polygon(p(fx0, fy0, 0.0), p(fx1, fy0, 0.0), p(fx1, fy1, 0.0), p(fx0, fy1, 0.0)), Color(0xFFE3EBE6))
    var x = fx0
    while (x <= fx1 + 0.001) { drawLine(Color.White.copy(alpha = 0.7f), o(x, fy0, 0.0), o(x, fy1, 0.0), 1f); x += 0.5 }
    var y = fy0
    while (y <= fy1 + 0.001) { drawLine(Color.White.copy(alpha = 0.7f), o(fx0, y, 0.0), o(fx1, y, 0.0), 1f); y += 0.5 }

    for (rack in 0 until racks) {
        val x0 = rack * (RACK_WIDTH + RACK_GAP); val x1 = x0 + RACK_WIDTH
        val y0 = 0.0; val y1 = RACK_DEPTH; val top = RACK_HEIGHT
        drawPath(polygon(p(x0, y1, 0.0), p(x1, y1, 0.0), p(x1 + 0.25, y1 + 0.25, 0.0), p(x0 + 0.25, y1 + 0.25, 0.0)), Color.Black.copy(alpha = 0.06f))
        drawPath(polygon(p(x0, y0, top), p(x1, y0, top), p(x1, y1, top), p(x0, y1, top)), Color(0xFFF2F2F2))
        drawPath(polygon(p(x0, y1, 0.0), p(x1, y1, 0.0), p(x1, y1, top), p(x0, y1, top)), Color(0xFFC7C7C7))
        drawPath(polygon(p(x1, y0, 0.0), p(x1, y1, 0.0), p(x1, y1, top), p(x1, y0, top)), Color(0xFFADADAD))

        // Servers on the front, from the bottom up.
        val slotHeight = (top - 0.2) / SLOTS_PER_RACK
        if (rack == serverRacks) {
            // Cache servers: violet while they run, grey when off.
            for (slot in 0 until cacheSlots) {
                val z0 = 0.1 + slot * slotHeight + 0.025; val z1 = z0 + slotHeight - 0.05
                val slab = polygon(p(x0 + 0.07, y1, z0), p(x1 - 0.07, y1, z0), p(x1 - 0.07, y1, z1), p(x0 + 0.07, y1, z1))
                drawPath(slab, if (slot < cacheOn) extraColor else Palette.off)
            }
            continue
        }
        for (slot in 0 until SLOTS_PER_RACK) {
            val index = rack * SLOTS_PER_RACK + slot
            if (index >= servers.size) break
            val server = servers[index]
            val z0 = 0.1 + slot * slotHeight + 0.025; val z1 = z0 + slotHeight - 0.05
            val sx0 = x0 + 0.07; val sx1 = x1 - 0.07
            val slab = polygon(p(sx0, y1, z0), p(sx1, y1, z0), p(sx1, y1, z1), p(sx0, y1, z1))
            // O7: an old server gets a mark on its left.
            val markOld = server.kind == ServerKind.CPU_OLD
            when (server.state) {
                ServerState.Off -> drawPath(slab, Palette.off)
                is ServerState.Booting -> drawPath(slab, Palette.booting)
                ServerState.On -> {
                    drawPath(slab, Palette.slab)
                    val width = (sx1 - sx0 - 0.08) * maxOf(server.utilization, 20) / 1_000
                    val bz0 = z0 + (z1 - z0) * 0.3; val bz1 = z1 - (z1 - z0) * 0.3
                    drawPath(
                        polygon(p(sx0 + 0.04, y1, bz0), p(sx0 + 0.04 + width, y1, bz0), p(sx0 + 0.04 + width, y1, bz1), p(sx0 + 0.04, y1, bz1)),
                        if (server.job != null) Palette.batchLoad else if (server.kind == ServerKind.GPU_CURRENT) Palette.trainingLoad else Palette.interactiveLoad,
                    )
                }
            }
            if (markOld) drawPath(polygon(p(sx0, y1, z0), p(sx0 + 0.05, y1, z0), p(sx0 + 0.05, y1, z1), p(sx0, y1, z1)), Palette.old)
        }
    }
}
