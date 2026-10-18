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
import de.wannerit.loadshift.core.ServerKind
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
                // Enough to weigh it: the computing at each site, the cost of moving the data, and what binds the job.
                val pue = game.scenario.facility.facilityPower(1_000) / 1_000.0
                val computeKWh = job.spec.servers * job.spec.duration / GameTime.TICKS_PER_HOUR.toDouble() * ServerKind.CPU_CURRENT.maxPower / 1_000.0 * pue
                fun kg(kWh: Double, site: Int) = kWh * model.sites[site].region.annualMean / 10_000.0
                fun kgText(x: Double) = Format.number(x, if (x < 10) 1 else 0)
                Txt(t("Computing: about %@ kWh – in Frankfurt about %@ kg CO₂, in Oslo about %@ kg", Format.number(computeKWh, 0),
                    kgText(kg(computeKWh, 0)), kgText(kg(computeKWh, 1))), Type.caption)
                val hours = model.transferTicks(job) / GameTime.TICKS_PER_HOUR.toDouble()
                val transferKWh = model.transferEnergy(job) / 1e6
                Txt(t("Data: %@ TB – to Oslo %@ h of transfer, %@ kWh in the German grid (about %@ kg CO₂)", Format.number(job.dataTenthsTb / 10.0, 1),
                    Format.number(hours, 1), Format.number(transferKWh, 0), kgText(kg(transferKWh, 0))), Type.caption)
                if (job.stay == StayReason.RESIDENCY) Txt(t("Input: the orders of the last year with the customers' names and addresses – personal data that the contract allows to process only in Germany."), Type.style(12, FontWeight.SemiBold, Palette.old))
                if (job.remoteDuration > 1_000) Txt(t("Reads prices and stock from the live database in Frankfurt while it runs – many thousands of small queries. In Oslo every query crosses the network: about %lld h instead of %lld h.",
                    job.spec.duration * job.remoteDuration / 1_000 / GameTime.TICKS_PER_HOUR, job.spec.duration / GameTime.TICKS_PER_HOUR), Type.style(12, FontWeight.SemiBold, Palette.old))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    for ((s, site) in model.sites.withIndex()) {
                        Chip(t(site.title), (s == 1) == moved, enabled = game.canMove) { if ((s == 1) != moved) game.toggleMove(i) }
                    }
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
