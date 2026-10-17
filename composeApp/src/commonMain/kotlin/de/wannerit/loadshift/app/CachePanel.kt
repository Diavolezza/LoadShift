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
import de.wannerit.loadshift.core.CacheModel
import kotlin.math.roundToInt
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Box

/** E3: what to cache, for how long, in how much memory – and two more measures. */
@Composable
fun CachePanel(game: GameController) {
    val model = game.scenario.caching ?: return
    val running = game.simulation.policy.cache
    val load = game.scenario.interactive.expected(minOf(game.simulation.tick, game.scenario.endTick - 1))
    val state = model.state(running, load)
    val hits = state.hits
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Txt(t("Caching"), Type.heading)
        Txt(t("A cache keeps answers so they need not be computed or fetched again. All kinds you tick share the memory of the cache servers; when it is full, the entry used least recently goes first (LRU). A cache server draws power all day. How old an answer may be is a business decision."), Type.style(15, color = Palette.secondary))

        // Cache servers
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Txt(t("Cache servers"), Type.style(15, FontWeight.SemiBold), Modifier.weight(1f))
            Stepper("${game.cacheServers}",
                minus = { game.cacheServers = maxOf(0, game.cacheServers - 1) },
                plus = { game.cacheServers = minOf(CacheModel.MAX_SERVERS, game.cacheServers + 1) })
        }
        Txt(t("%lld GB memory, %lld W all day", game.cacheServers * model.gbPerServer, game.cacheServers * model.serverPower), Type.caption)

        // Memory right now
        if (running.anyCached) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Txt(t("Memory now: %lld of %lld GB used", state.usedGb.roundToInt(), state.capacityGb), Type.style(14, FontWeight.SemiBold))
                Box(Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)).background(Palette.divider)) {
                    Box(Modifier.fillMaxWidth((state.usedGb / state.capacityGb).toFloat().coerceIn(0f, 1f)).height(8.dp)
                        .background(if (state.full) Palette.amber else Palette.accent))
                }
                if (state.full) Txt(t("Full: entries are pushed out before their TTL ends."), Type.style(12, FontWeight.SemiBold, Palette.amber))
            }
        }

        // Kinds of data
        for ((k, kind) in model.kinds.withIndex()) {
            val ttl = game.cacheTtl.getOrElse(k) { 0 }
            val on = ttl > 0
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                    .background(if (on) Palette.accent.copy(alpha = 0.08f) else Palette.background)
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).clickable { game.setTtl(k, if (on) 0 else 15) },
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Tick(on)
                    Txt(t(kind.title), Type.style(15, FontWeight.SemiBold), Modifier.weight(1f))
                    // The kind's color in the timeline.
                    Box(Modifier.size(10.dp).background(Palette.cacheKinds[k % Palette.cacheKinds.size].copy(alpha = 0.7f)))
                    if (state.stay[k] > 0.0) Txt(t("hits now: %@ · %lld GB", Format.wholePercent(hits[k]), state.memoryGb[k].roundToInt()), Type.style(12, FontWeight.SemiBold, Palette.accent))
                }
                Txt(t(kind.note), Type.caption)
                Txt(allowedText(kind.allowedMinutes), Type.caption)
                if (on) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        for (option in CacheModel.TTLS) {
                            val chosen = option == ttl
                            Txt(
                                ttlText(option),
                                Type.style(13, FontWeight.SemiBold, if (chosen) Color.White else Palette.ink),
                                Modifier.clip(RoundedCornerShape(50)).background(if (chosen) Palette.accent else Palette.panel)
                                    .border(1.dp, Palette.divider, RoundedCornerShape(50))
                                    .clickable { game.setTtl(k, option) }.padding(horizontal = 10.dp, vertical = 4.dp),
                            )
                        }
                    }
                    if (ttl > kind.allowedMinutes) {
                        Txt(t("Longer than allowed: some answers will be stale."), Type.style(12, FontWeight.SemiBold, Palette.red))
                    }
                }
            }
        }

        // Two more measures
        Txt(t("Two more measures"), Type.subheading)
        Measure(game.cacheCount, t("Count unique visitors with HyperLogLog"), t("About 2 % error, which the marketing team accepts – instead of keeping every visitor ID.")) { game.cacheCount = !game.cacheCount }
        Measure(game.cacheView, t("Materialized view for the sales dashboard"), t("Stores the result instead of recomputing it every 15 minutes; costs storage.")) { game.cacheView = !game.cacheView }

        ApplyRow(game) { cacheText(it.cache) }
    }
}

@Composable
private fun Measure(on: Boolean, title: String, text: String, toggle: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).clickable(onClick = toggle).padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Tick(on)
        Column(Modifier.weight(1f)) {
            Txt(title, Type.style(15, FontWeight.SemiBold))
            Txt(text, Type.caption)
        }
    }
}

private fun ttlText(minutes: Int): String = when {
    minutes >= 1_440 -> t("1 day")
    minutes >= 60 -> t("%lld h", minutes / 60)
    else -> t("%lld min", minutes)
}

private fun allowedText(minutes: Int): String = when {
    minutes == 0 -> t("Must be current.")
    minutes >= 1_440 -> t("May be up to a day old.")
    else -> t("May be up to %@ old.", ttlText(minutes))
}

/** "1 cache servers, 3 kinds cached, HyperLogLog" */
fun cacheText(c: de.wannerit.loadshift.core.CacheConfig): String {
    val cached = c.ttlMinutes.count { it > 0 }
    val parts = mutableListOf(if (cached == 0 || c.servers == 0) t("no cache") else t("%lld cache servers, %lld kinds cached", c.servers, cached))
    if (c.approximateCount) parts += "HyperLogLog"
    if (c.materializedView) parts += t("materialized view")
    return parts.joinToString(", ")
}
