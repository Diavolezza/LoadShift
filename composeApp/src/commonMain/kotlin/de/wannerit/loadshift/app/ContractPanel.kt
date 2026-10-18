package de.wannerit.loadshift.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import de.wannerit.loadshift.core.Contract

/** B12: the electricity contract, and three ways to count the CO₂ so far. */
@Composable
fun ContractPanel(game: GameController) {
    val model = game.scenario.contract ?: return
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Txt(t("Electricity contract"), Type.heading)
        Txt(t("The board wants a green data center. In the report, contracted green energy counts as zero; for the stars, only clean energy that is there in the same hour counts. The cost of electricity may rise by at most %@.",
            Format.wholePercent(model.budgetPermille - 1_000)), Type.style(15, color = Palette.secondary))
        val notes = mapOf(
            Contract.GREY to t("The grid as it is, %@ ct/kWh.", Format.number(model.priceTenthsCent / 10.0, 1)),
            Contract.CERTIFICATES to t("Guarantees of origin from hydro plants that have run for decades, +%@ ct/kWh.", Format.number(Contract.CERTIFICATES.surchargeTenthsCent / 10.0, 1)),
            Contract.PPA to t("A wind farm delivers as much energy over the week as the data center uses without changes – when the wind blows; what is not used in that hour goes to the grid. +%@ ct for each of its kWh.", Format.number(Contract.PPA.surchargeTenthsCent / 10.0, 1)),
            Contract.HOURLY to t("A supplier covers %@ of every hour with wind, sun and storage, +%@ ct/kWh.", Format.wholePercent(model.hourlyShare), Format.number(Contract.HOURLY.surchargeTenthsCent / 10.0, 1)),
        )
        for (c in Contract.entries) {
            RuleOption(game.contractDraft == c, { game.contractDraft = c }, t(c.label), notes.getValue(c))
        }

        // Counted so far
        val totals = game.totals
        val refCost = game.reference.history.getOrNull(game.simulation.history.size - 1)?.contract?.costSoFar ?: 0L
        Card(Modifier.fillMaxWidth(), color = Palette.background, padding = 14.dp) {
            Txt(t("CO₂ so far, counted three ways"), Type.subheading)
            val rows = listOf(
                t("Location-based (the grid's average)") to totals.locationOperational,
                t("In the report (market-based)") to totals.marketOperational,
                t("Hourly matched (what the stars count)") to totals.operational,
            )
            for ((label, value) in rows) {
                Row(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                    Txt(label, Type.callout, Modifier.weight(1f))
                    Txt(t("%@ kg", Format.kilograms(value)), Type.style(15, FontWeight.SemiBold))
                }
            }
            Row(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                Txt(t("Electricity cost so far (reference)"), Type.callout, Modifier.weight(1f))
                Txt(t("%@ € (%@ €)", Format.number(totals.energyCost / 1_000.0, 0), Format.number(refCost / 1_000.0, 0)), Type.callout)
            }
        }
        ApplyRow(game) { contractText(it) }
    }
}

/** "wind farm contract, autoscaling at 80 %, greenest hours" */
fun contractText(p: de.wannerit.loadshift.core.Policy): String = listOf(
    t(p.contract.label), capacityText(p.capacity), ruleText(p),
).joinToString(", ")
