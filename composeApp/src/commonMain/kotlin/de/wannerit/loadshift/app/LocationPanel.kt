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
import de.wannerit.loadshift.core.BatchMode
import de.wannerit.loadshift.core.GameTime
import de.wannerit.loadshift.core.StayReason

/** O10: which job runs where, and the batch rule. */
@Composable
fun LocationPanel(game: GameController) {
    val model = game.scenario.location ?: return
    val greenest = game.simulation.policy.batch == BatchMode.Greenest
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Txt(t("Location"), Type.heading)
        Txt(t("Before the start you decide which jobs run in Oslo: the grid there is almost free of CO₂, but only %lld servers are free. A job that moves needs its data there first – the transfer takes time, and the network needs energy in the German grid.", model.servers[1]), Type.style(15, color = Palette.secondary))

        Txt(t("Batch rule at both sites"), Type.style(15, FontWeight.SemiBold))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Chip(t("as soon as possible"), !greenest, enabled = !game.isFinished) { game.setLocationRule(false) }
            Chip(t("greenest hours"), greenest, enabled = !game.isFinished) { game.setLocationRule(true) }
        }

        Txt(t("Timeline shows"), Type.style(15, FontWeight.SemiBold))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            for ((i, site) in model.sites.withIndex()) Chip(t(site.title), game.site == i) { game.site = i }
        }

        if (!game.canMove) Txt(t("The jobs are under way – where they run can only be changed before the start (play again)."), Type.caption)
        for ((i, job) in model.jobs.withIndex()) {
            val moved = i in game.moved
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                    .background(if (moved) Palette.accent.copy(alpha = 0.08f) else Palette.background).padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Txt(t(job.spec.name), Type.style(15, FontWeight.SemiBold))
                Txt(t("%lld servers · %lld h · %@ – %@", job.spec.servers, job.spec.duration / GameTime.TICKS_PER_HOUR,
                    timeText(job.spec.arrival), timeText(job.spec.deadline)), Type.caption)
                Txt(t("Data: %@ TB", Format.number(job.dataTenthsTb / 10.0, 1)), Type.caption)
                if (job.stay == StayReason.RESIDENCY) {
                    Txt(t("Customer data: by contract it must stay in Germany."), Type.style(12, FontWeight.SemiBold, Palette.red))
                }
                if (job.remoteDuration > 1_000) {
                    Txt(t("Talks to the live database in Frankfurt all the time: far away it takes %@ times as long.",
                        Format.number(job.remoteDuration / 1_000.0, 0)), Type.style(12, FontWeight.SemiBold, Palette.old))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    for ((s, site) in model.sites.withIndex()) {
                        Chip(t(site.title), (s == 1) == moved, enabled = game.canMove) { if ((s == 1) != moved) game.toggleMove(i) }
                    }
                }
                if (moved) {
                    val hours = model.transferTicks(job) / GameTime.TICKS_PER_HOUR.toDouble()
                    val kWh = model.transferEnergy(job) / 1e6
                    val kg = kWh * model.sites[0].region.annualMean / 10_000.0
                    Txt(t("Transfer: %@ h, %@ kWh in the German grid (about %@ kg CO₂)", Format.number(hours, 1), Format.number(kWh, 0),
                        Format.number(kg, if (kg < 10) 1 else 0)), Type.style(12, FontWeight.SemiBold, Palette.secondary))
                }
            }
        }
    }
}

@Composable
private fun Chip(text: String, chosen: Boolean, enabled: Boolean = true, onClick: () -> Unit) {
    Txt(
        text, Type.style(13, FontWeight.SemiBold, if (chosen) Color.White else if (enabled) Palette.ink else Palette.secondary),
        Modifier.clip(RoundedCornerShape(50)).background(if (chosen) Palette.accent else Palette.panel)
            .border(1.dp, Palette.divider, RoundedCornerShape(50))
            .clickable(enabled = enabled, onClick = onClick).padding(horizontal = 10.dp, vertical = 4.dp),
    )
}
