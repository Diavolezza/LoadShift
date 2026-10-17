package de.wannerit.loadshift.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import de.wannerit.loadshift.core.CoolingModel

/** Scenario 6: the setpoint of the cooling, and what the cooling does right now. */
@Composable
fun CoolingPanel(game: GameController) {
    val cooling = game.scenario.cooling ?: return
    val simulation = game.simulation
    val tick = minOf(simulation.tick, game.scenario.endTick - 1)
    val setpoint = simulation.policy.setpoint
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Txt(t("Cooling"), Type.heading)
        Txt(t("The cooling keeps the air at the server inlets at the setpoint. Outside air cools for free when it is at least 6 °C cooler than the setpoint; otherwise the chiller works – the hotter it is outside, the more power it needs. A warmer setpoint saves chiller power, but above 25 °C the server fans speed up."), Type.style(15, color = Palette.secondary))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Txt(t("Setpoint"), Type.style(15, FontWeight.SemiBold), Modifier.weight(1f))
            val range = CoolingModel.SETPOINTS
            Stepper(t("%lld °C", game.draftSetpoint),
                minus = { game.draftSetpoint = maxOf(range.first, game.draftSetpoint - 1) },
                plus = { game.draftSetpoint = minOf(range.last, game.draftSetpoint + 1) })
        }
        Txt(t("ASHRAE recommends 18 to 27 °C at the server inlets."), Type.caption)
        val mechanical = cooling.mechanicalPermille(tick, setpoint)
        val last = simulation.history.lastOrNull()
        val rows = listOf(
            t("Outside") to "${Format.number(cooling.outsideTenths(tick) / 10.0, 1)} °C",
            t("Cooled by") to when (mechanical) {
                0 -> t("outside air")
                1_000 -> t("chiller")
                else -> t("chiller %@, outside air", Format.wholePercent(mechanical))
            },
            t("PUE now") to (if (last == null || last.itPower == 0) "–" else Format.number(last.facilityPower.toDouble() / last.itPower, 2)),
        )
        Card(Modifier.fillMaxWidth(), color = Palette.background, padding = 14.dp) {
            for ((label, value) in rows) {
                Row(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                    Txt(label, Type.callout, Modifier.weight(1f))
                    Txt(value, Type.style(15, FontWeight.SemiBold))
                }
            }
        }
    }
}
