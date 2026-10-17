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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import de.wannerit.loadshift.core.ServerKind
import de.wannerit.loadshift.core.Units

/** O7: where and when the budget of new servers goes; one card per site, the one chosen is shown in the hall. */
@Composable
fun RenewalPanel(game: GameController) {
    val model = game.scenario.renewal ?: return
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Txt(t("Hardware renewal"), Type.heading)
        Txt(t("Two data centers, each with 24 servers that are four years old – a server lives six years. A new server does the work of two old ones at about the same power, but making it emits about 550 kg CO₂, booked over its six years. An old server retired early books the rest of its 500 kg at once. Each game day is a quarter."), Type.style(15, color = Palette.secondary))
        Txt(t("Budget: %lld of %lld new servers left", maxOf(0, game.budgetLeft), model.budget), Type.style(15, FontWeight.SemiBold))
        for ((i, site) in model.sites.withIndex()) {
            val simulation = game.simulationOf(i)
            val tick = simulation.tick
            val old = simulation.servers.filter { it.kind == ServerKind.CPU_OLD }
            val selected = game.site == i
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                    .background(if (selected) Palette.accent.copy(alpha = 0.08f) else Palette.background)
                    .border(if (selected) 1.5.dp else 0.dp, if (selected) Palette.accent else Palette.background, RoundedCornerShape(12.dp))
                    .clickable { game.site = i }.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Txt(t(site.title), Type.style(16, FontWeight.SemiBold), Modifier.weight(1f))
                    Txt(t("grid on average %lld g/kWh", (site.region.annualMean + 5) / 10), Type.style(13, color = Palette.intensity((site.region.annualMean + 5) / 10)))
                }
                val age = old.firstOrNull()?.ageAt(tick, model.daysPerStep.toLong())
                Txt(
                    if (age != null) t("%lld old servers (%@ years), %lld new", old.size, Format.number(age / 365.0, 1), game.installed(i))
                    else t("%lld new servers", game.installed(i)),
                    Type.callout,
                )
                Txt(t("So far %@ t CO₂, %@ MWh", Format.tonnes(simulation.totals.emissions), Format.number(simulation.totals.facilityEnergy / 1e9, 0)), Type.caption)
                if (game.canRenew) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Txt(t("New servers"), Type.style(14, FontWeight.SemiBold), Modifier.weight(1f))
                        Stepper("${game.renewDraft[i]}", minus = { game.changeRenewal(i, -1) }, plus = { game.changeRenewal(i, 1) })
                    }
                    val retire = (game.renewDraft[i] - game.installed(i)) * model.replacedPerNew
                    if (retire > 0 && age != null) {
                        val left = maxOf(0, Units.LIFESPAN_DAYS - age)
                        val tonnes = retire * ServerKind.CPU_OLD.embodiedKilograms * left.toDouble() / Units.LIFESPAN_DAYS / 1_000
                        Txt(
                            if (left > 0) t("Retires %lld old servers: %@ t embodied CO₂ of their remaining life at once.", retire, Format.number(tonnes, 1))
                            else t("Retires %lld old servers at the end of their life: nothing left to book.", retire),
                            Type.style(12, FontWeight.SemiBold, Palette.old),
                        )
                    }
                }
            }
        }
        if (game.canRenew) {
            PrimaryButton(t("Start the quarter")) { game.startQuarter() }
        } else if (!game.isFinished) {
            Txt(t("The quarter is running – at the start of the next one you decide again."), Type.caption)
        }
    }
}
