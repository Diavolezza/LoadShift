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

/** E3: what to cache, for how long, in how much memory – and two more measures. */
@Composable
fun CachePanel(game: GameController) {
    val model = game.scenario.caching ?: return
    val running = game.simulation.policy.cache
    val load = game.scenario.interactive.expected(minOf(game.simulation.tick, game.scenario.endTick - 1))
    val hits = model.hitRates(running, load)
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Txt(t("Caching"), Type.heading)
        Txt(t("A cache keeps answers so they need not be computed or fetched again. It saves work while it hits – but the cache servers draw power all the time (one per 64 GB). How old an answer may be is a business decision."), Type.style(15, color = Palette.secondary))

        // Size
        val draft = game.draftCache
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Txt(t("Cache size"), Type.style(15, FontWeight.SemiBold), Modifier.weight(1f))
            val sizes = CacheModel.SIZES
            val i = sizes.indexOf(game.cacheSize).coerceAtLeast(0)
            Stepper("${game.cacheSize} GB",
                minus = { game.cacheSize = sizes[maxOf(0, i - 1)] }, plus = { game.cacheSize = sizes[minOf(sizes.size - 1, i + 1)] })
        }
        Txt(t("%lld cache servers, %lld W all day", model.servers(draft.copy(ttlMinutes = draft.ttlMinutes.map { maxOf(it, 1) })),
            model.power(draft.copy(ttlMinutes = draft.ttlMinutes.map { maxOf(it, 1) }))), Type.caption)

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
                    if (running.ttlMinutes.getOrElse(k) { 0 } > 0) Txt(t("hits now: %@", Format.wholePercent(hits[k])), Type.style(12, FontWeight.SemiBold, Palette.accent))
                }
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

/** "32 GB, 3 kinds cached, HyperLogLog" */
fun cacheText(c: de.wannerit.loadshift.core.CacheConfig): String {
    val cached = c.ttlMinutes.count { it > 0 }
    val parts = mutableListOf(if (cached == 0) t("no cache") else t("%lld GB, %lld kinds cached", c.sizeGb, cached))
    if (c.approximateCount) parts += "HyperLogLog"
    if (c.materializedView) parts += t("materialized view")
    return parts.joinToString(", ")
}
