package de.wannerit.loadshift.app

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import de.wannerit.loadshift.core.Scenario

/** A term of the glossary: its name (English in both languages, D16), what it means, and the scenarios that teach it. */
private class Entry(val term: String, val text: String, val scenarios: List<Scenario>)

/** Terms the scenarios use without being about them. */
private val GENERAL = listOf(
    "Carbon intensity" to "How much CO₂e the generation of one kilowatt-hour of electricity emits, in g CO₂e/kWh. It changes from hour to hour with the share of wind, sun, water, coal and gas in the grid.",
    "Circuit Breaker" to "A pattern for calls between building blocks: after repeated failures the caller stops calling for a while and answers without the other block – for example with less content – instead of failing too.",
    "Quality scenario" to "A requirement for a quality attribute, written as a small story: a source, a stimulus, the part of the system it hits, the response and a measure – for example: after a storm many customers report damage (stimulus), the portal takes every report (response), down at most 4.4 hours a year (measure).",
    "Long Polling" to "A client asks, and the server holds the request open until there is something new or a time limit is reached; then the client asks again. Fewer requests than polling, but each waits on the server.",
    "Push notification" to "A message the server hands to the phone platform's push service (for example APNs or Firebase Cloud Messaging); the service delivers it to the device, even when the app is closed. Usually within seconds, but without a guarantee, and throttled when there are many.",
    "WebSocket" to "A connection between client and server that stays open, so that both can send at any time (RFC 6455). Good for frequent changes while an app is open; kept open in the background, it needs signs of life that wake the device.",
    "Erasure coding" to "A way to store data safely without full copies: the data is cut into pieces and extra pieces are computed from them, so that it survives the loss of some disks – often with half again as much space instead of two or three times.",
    "Tenant" to "A customer of a software as a service, with its own users and data. A tenant ID tells the customers apart in shared instances and databases.",
    "Colocation" to "A data center in which many companies rent space, power and cooling; each brings its own servers and runs them itself.",
    "CO₂e (CO₂ equivalent)" to "Greenhouse gases counted together: each gas weighted by how much it warms the climate compared with CO₂.",
    "Idle power" to "What a server draws while it does nothing – often a quarter to a half of its maximum. It is why switching off unused servers saves so much.",
    "Location-based and market-based" to "The two ways the GHG Protocol reports the emissions of bought electricity (Scope 2): location-based with the mean of the grid where the electricity is used, market-based with the contracts and certificates bought.",
    "Scope 1, 2 and 3" to "The three scopes of the GHG Protocol: the own combustion (1), bought energy (2) and everything else in the value chain (3) – for IT for example the making of hardware and the services bought.",
    "Software Carbon Intensity (SCI)" to "ISO/IEC 21031: the CO₂e of software per functional unit – energy times carbon intensity plus embodied emissions, per request, per user or per job. It falls only with less energy, cleaner energy or less hardware, not with offsets.",
)

private fun entries(): List<Entry> {
    val byTerm = Scenario.ALL.groupBy { ScenarioTexts.of(it).term }
    val fromScenarios = byTerm.map { (term, scenarios) -> Entry(term, ScenarioTexts.of(scenarios.first()).termText, scenarios) }
    return (fromScenarios + GENERAL.map { Entry(it.first, it.second, emptyList()) }).sortedBy { it.term.lowercase() }
}

/** The glossary: every technical term of the game, with the scenarios that teach it (open ones can be started from here). */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LexiconScreen(onBack: () -> Unit, onOpen: (Scenario) -> Unit) {
    BoxWithConstraints(Modifier.fillMaxSize().background(Palette.background)) {
        val side = (maxWidth * 0.04f).coerceAtLeast(28.dp)
        val columns = if (maxWidth > 1000.dp) 2 else 1
        val cardWidth = (maxWidth - side * 2 - 16.dp * (columns - 1)) / columns - 1.dp
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = side, vertical = 36.dp), verticalArrangement = Arrangement.spacedBy(22.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                Txt("‹", Type.style(40, FontWeight.SemiBold), Modifier.clip(RoundedCornerShape(8.dp)).clickable(onClick = onBack).padding(horizontal = 8.dp))
                Column {
                    Txt("Load Shift", Type.style(19, FontWeight.SemiBold, Palette.secondary))
                    Txt(t("Glossary"), Type.style(44, FontWeight.Bold))
                }
            }
            Txt(t("Every technical term of the game, with what it means and where you meet it. The terms stay in English, as they are used in the field."), Type.style(17, color = Palette.secondary))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                for (e in entries()) {
                    Column(Modifier.width(cardWidth).background(Palette.panel, RoundedCornerShape(16.dp)).padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Txt(e.term, Type.style(20, FontWeight.Bold))
                        Txt(t(e.text), Type.style(16))
                        if (e.scenarios.isNotEmpty()) FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            for (s in e.scenarios) {
                                val open = Progress.isUnlocked(s)
                                Row(
                                    Modifier.clip(RoundedCornerShape(50)).background(if (open) Palette.accent.copy(alpha = 0.1f) else Palette.background)
                                        .clickable(enabled = open) { onOpen(s) }.padding(horizontal = 10.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp),
                                ) {
                                    if (!open) LockIcon(12.dp)
                                    Txt(t("Scenario %@: %@", s.shownLabel, t(s.title)), Type.style(14, FontWeight.SemiBold, if (open) Palette.accent else Palette.secondary))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
