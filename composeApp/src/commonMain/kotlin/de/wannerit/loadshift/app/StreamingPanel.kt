package de.wannerit.loadshift.app

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import de.wannerit.loadshift.core.Quality
import de.wannerit.loadshift.core.StreamingConfig
import de.wannerit.loadshift.core.StreamingModel

/** O9: the highest quality per device and the eco mode. */
@Composable
fun StreamingPanel(game: GameController) {
    val model = game.scenario.streaming ?: return
    val simulation = game.simulation
    val tick = minOf(simulation.tick, game.scenario.endTick - 1)
    val grid = simulation.grid.intensity(tick)
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Txt(t("Video quality"), Type.heading)
        Txt(t("The work in the data center grows with the bitrate: 4K needs four times as much as HD, SD less than half of HD. Viewers notice the difference on a TV, much less on a phone. Their mean rating must stay at %@ or above.", Format.wholePercent(model.ratingTarget)), Type.style(15, color = Palette.secondary))
        for ((i, device) in model.devices.withIndex()) {
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Palette.background).padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Txt(t(device.title), Type.style(15, FontWeight.SemiBold), Modifier.weight(1f))
                    Txt(t("%@ of the viewers", Format.wholePercent(device.share)), Type.caption)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    val chosen = game.streamMax.getOrElse(i) { Quality.UHD }
                    for (q in Quality.entries) {
                        Chip("${q.label} · ${Format.wholePercent(device.rating[q.ordinal])}", q == chosen) { game.setMaxQuality(i, q) }
                    }
                }
            }
        }
        Txt(t("The percentages: how the viewers on that device rate the quality."), Type.caption)

        // Eco mode
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).clickable { game.streamEco = !game.streamEco }.padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Tick(game.streamEco)
            Column(Modifier.weight(1f)) {
                Txt(t("Eco mode while the grid is dirty"), Type.style(15, FontWeight.SemiBold))
                Txt(t("Above the threshold every device gets at most the eco quality."), Type.caption)
            }
        }
        if (game.streamEco) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Txt(t("Above"), Type.callout, Modifier.weight(1f))
                val range = StreamingModel.ECO_RANGE
                Stepper(t("%lld g/kWh", game.streamEcoAbove),
                    minus = { game.streamEcoAbove = maxOf(range.first, game.streamEcoAbove - 25) },
                    plus = { game.streamEcoAbove = minOf(range.last, game.streamEcoAbove + 25) })
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                Txt(t("Eco quality"), Type.callout, Modifier.weight(1f))
                for (q in listOf(Quality.HD, Quality.SD)) Chip(q.label, q == game.streamEcoQuality) { game.streamEcoQuality = q }
            }
        }

        // Now
        val running = simulation.policy.streaming
        Card(Modifier.fillMaxWidth(), color = Palette.background, padding = 14.dp) {
            val rows = listOf(
                t("Grid now") to "$grid g/kWh",
                t("Eco mode") to if (model.ecoActive(running, grid)) t("on") else t("off"),
                t("Rating now") to Format.percent(model.rating(running, grid)),
                t("Work per stream") to Format.wholePercent(model.work(running, grid)),
            )
            for ((label, value) in rows) {
                Row(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                    Txt(label, Type.callout, Modifier.weight(1f))
                    Txt(value, Type.style(15, FontWeight.SemiBold))
                }
            }
        }
        ApplyRow(game) { streamingText(it.streaming, model) }
    }
}

@Composable
private fun Chip(text: String, chosen: Boolean, onClick: () -> Unit) {
    Txt(
        text, Type.style(13, FontWeight.SemiBold, if (chosen) Color.White else Palette.ink),
        Modifier.clip(RoundedCornerShape(50)).background(if (chosen) Palette.accent else Palette.panel)
            .border(1.dp, Palette.divider, RoundedCornerShape(50))
            .clickable(onClick = onClick).padding(horizontal = 10.dp, vertical = 4.dp),
    )
}

/** "4K/HD/HD, eco HD above 600 g/kWh" */
fun streamingText(c: StreamingConfig, model: StreamingModel): String {
    val q = model.devices.indices.joinToString("/") { c.maxQuality.getOrElse(it) { Quality.UHD }.label }
    return if (c.eco) t("%@, eco %@ above %lld g/kWh", q, c.ecoQuality.label, c.ecoAbove) else q
}
