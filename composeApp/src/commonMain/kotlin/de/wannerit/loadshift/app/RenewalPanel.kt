package de.wannerit.loadshift.app

import androidx.compose.foundation.background
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import de.wannerit.loadshift.core.InteractiveLoad
import de.wannerit.loadshift.core.ServerKind

/** O7: the year's budget, the generation on offer, and per site its grid, servers, capacity and CO₂. */
@Composable
fun RenewalPanel(game: GameController) {
    val model = game.scenario.renewal ?: return
    val day = minOf(game.currentDay, game.scenario.days - 1)
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Txt(t("Hardware renewal"), Type.heading)
        Txt(t("New servers arrive at the start of the next quarter. A new server does the work of two old ones; making it emits about 550 kg CO₂, booked over its six years. Old servers past six years begin to fail. The shop grows by 15 % a year."), Type.style(15, color = Palette.secondary))
        Txt(t("Budget year %lld: %lld of %lld new servers left – what is not spent by the end of the year is gone.", game.year + 1, maxOf(0, game.budgetLeft), model.budgetPerYear), Type.style(15, FontWeight.SemiBold))
        val saving = 1_000 - model.generationPower(day)
        Txt(
            if (saving == 0) t("This year's generation: the first.") else t("This year's generation needs %@ less power than the first.", Format.wholePercent(saving)),
            Type.caption,
        )
        for ((i, site) in model.sites.withIndex()) {
            val simulation = game.simulationOf(i)
            val old = simulation.servers.filter { it.kind == ServerKind.CPU_OLD }
            val capacity = game.capacityOf(i)
            val peak = game.peakOf(i)
            val short = capacity * InteractiveLoad.SLOW_ABOVE / 1_000 < peak
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Palette.background).padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Txt(t(site.title), Type.style(16, FontWeight.SemiBold), Modifier.weight(1f))
                    Txt(t("grid on average %lld g/kWh", (site.region.annualMean + 5) / 10), Type.style(13, color = Palette.intensity((site.region.annualMean + 5) / 10)))
                }
                val age = old.firstOrNull()?.ageAt(simulation.tick, model.daysPerStep.toLong())
                Txt(
                    t("%lld old servers (%@ years), %lld failed; %lld new", old.size, Format.number((age ?: 0) / 365.0, 1),
                        simulation.totals.failedServers, game.renewDraft[i]),
                    Type.callout,
                )
                Txt(
                    t("Capacity %lld, peak this quarter %lld", capacity, peak),
                    Type.style(13, if (short) FontWeight.SemiBold else FontWeight.Normal, if (short) Palette.red else Palette.secondary),
                )
                if (short) Txt(t("Too little: above 85 % utilization requests get slow."), Type.style(12, FontWeight.SemiBold, Palette.red))
                Txt(t("So far %@ t CO₂", Format.tonnes(simulation.totals.emissions)), Type.caption)
                if (game.canRenew) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Txt(t("New servers"), Type.style(14, FontWeight.SemiBold), Modifier.weight(1f))
                        Stepper("${game.renewDraft[i]}", minus = { game.changeRenewal(i, -1) }, plus = { game.changeRenewal(i, 1) })
                    }
                }
            }
        }
        if (!game.isFinished) {
            if (game.speed == Speed.PAUSED) PrimaryButton(t("Run")) { game.startQuarter() }
            Txt(t("The game pauses at the start of every year. You can pause any time and buy; the servers arrive at the start of the next quarter."), Type.caption)
        }
    }
}
