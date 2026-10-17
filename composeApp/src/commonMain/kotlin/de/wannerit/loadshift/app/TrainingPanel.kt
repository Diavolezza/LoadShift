package de.wannerit.loadshift.app

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import de.wannerit.loadshift.core.GameTime
import de.wannerit.loadshift.core.ServerKind
import de.wannerit.loadshift.core.TrainingConfig
import de.wannerit.loadshift.core.TrainingModel

/** O8: how many GPU servers train, with which power cap, and when. */
@Composable
fun TrainingPanel(game: GameController) {
    val model = game.scenario.training ?: return
    val simulation = game.simulation
    val totals = simulation.totals
    val draft = game.draftTraining
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Txt(t("Training run"), Type.heading)
        Txt(t("The model needs a fixed amount of GPU work by the end of day 3. You decide how many GPU servers train together, how much power each may draw, and when they train. A pause is possible at a checkpoint; resuming costs 15 minutes of loading it."), Type.style(15, color = Palette.secondary))

        // Progress
        val done = minOf(1_000L, totals.trainingDone * 1_000 / model.work).toInt()
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Txt(t("Progress: %@ · resumed %lld times", Format.wholePercent(done), totals.trainingRestarts), Type.style(14, FontWeight.SemiBold))
            Box(Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)).background(Palette.divider)) {
                Box(Modifier.fillMaxWidth(done / 1_000f).height(8.dp).background(Palette.accent))
            }
        }

        // Servers
        Row(verticalAlignment = Alignment.CenterVertically) {
            Txt(t("GPU servers"), Type.style(15, FontWeight.SemiBold), Modifier.weight(1f))
            Stepper("${game.trainServers}",
                minus = { game.trainServers = maxOf(TrainingModel.SERVERS.first, game.trainServers - 1) },
                plus = { game.trainServers = minOf(TrainingModel.SERVERS.last, game.trainServers + 1) })
        }
        val efficiency = 1_000 - model.scalingLoss * (game.trainServers - 1)
        Txt(t("Together they do %@ times the work of one server – each one %@, because they wait for each other.",
            Format.number(game.trainServers * efficiency / 1_000.0, 1), Format.wholePercent(efficiency)), Type.caption)

        // Power cap
        Txt(t("Power cap"), Type.style(15, FontWeight.SemiBold))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            for (cap in TrainingModel.CAPS) {
                val chosen = cap == game.trainCap
                Txt(
                    if (cap == 1_000) t("none") else Format.wholePercent(cap),
                    Type.style(13, FontWeight.SemiBold, if (chosen) Color.White else Palette.ink),
                    Modifier.clip(RoundedCornerShape(50)).background(if (chosen) Palette.accent else Palette.panel)
                        .border(1.dp, Palette.divider, RoundedCornerShape(50))
                        .clickable { game.trainCap = cap }.padding(horizontal = 10.dp, vertical = 4.dp),
                )
            }
        }
        val kind = ServerKind.GPU_CURRENT
        val watts = kind.idlePower + (kind.maxPower - kind.idlePower) * model.draw(game.trainCap) / 1_000
        Txt(t("Each GPU server draws %@ kW while training and works at %@ of full speed.",
            Format.kilowatts(watts), Format.wholePercent(model.speed(game.trainCap))), Type.caption)

        // When
        RuleOption(!game.trainGreenest, { game.trainGreenest = false }, t("Without a break"), t("Trains from now on until the model is done."))
        RuleOption(game.trainGreenest, { game.trainGreenest = true }, t("Greenest hours"),
            t("Trains in the hours with the lowest forecast that are needed before the deadline – with a reserve of a tenth."))

        // Estimate
        val left = maxOf(0L, model.work - totals.trainingDone)
        val perTick = model.perTick(draft)
        val hours = if (perTick == 0) 0.0 else left.toDouble() / perTick / GameTime.TICKS_PER_HOUR
        val hoursLeft = (game.scenario.endTick - simulation.tick) / GameTime.TICKS_PER_HOUR
        Txt(
            t("With these settings about %@ hours of training are left; %lld hours remain until the deadline.", Format.number(hours, 1), hoursLeft),
            Type.style(13, if (hours > hoursLeft) FontWeight.SemiBold else FontWeight.Normal, if (hours > hoursLeft) Palette.red else Palette.secondary),
        )
        ApplyRow(game) { trainingText(it.training) }
    }
}

/** "6 servers, cap 70 %, greenest hours" */
fun trainingText(c: TrainingConfig): String =
    t("%lld servers", c.servers) + ", " + (if (c.cap == 1_000) t("no cap") else t("cap %@", Format.wholePercent(c.cap))) + ", " +
        (if (c.greenest) t("greenest hours") else t("without a break"))
