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
    "CQRS" to "Command Query Responsibility Segregation: separate models for changing data (commands) and for reading it (queries); the read model can be prepared for the queries in advance.",
    "Event Sourcing" to "Storing every change as an event instead of overwriting the state; the state at any moment is replayed from the events.",
    "Lambda and Kappa architecture" to "Two patterns for analytics: Lambda computes the same data in a batch layer and a stream layer; Kappa uses one stream only and replays the stored log for corrections.",
    "Pipes and Filters" to "An architecture pattern in which data flows through a chain of independent steps (filters) connected by pipes; each step does one thing.",
    "Publish/Subscribe" to "Senders publish messages to topics; receivers subscribe to the topics they need and get only those messages.",
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

/** The sources the debriefs cite as [S…] (the full list with notes: docs/CONCEPT.md, section 14). Titles stay as published. */
private val SOURCES = listOf(
    Triple("S8", "G. Kamiya, IEA: The carbon footprint of streaming video: fact-checking the headlines, 2020.", "https://www.iea.org/commentaries/the-carbon-footprint-of-streaming-video-fact-checking-the-headlines"),
    Triple("S25", "C. Elsworth, K. Huang, D. Patterson et al.: Measuring the environmental impact of delivering AI at Google Scale, arXiv:2508.15734, 2025.", "https://arxiv.org/abs/2508.15734"),
    Triple("S26", "J. You: How much energy does ChatGPT use?, Epoch AI, 2025.", "https://epoch.ai/blog/how-much-energy-does-chatgpt-use"),
    Triple("S27", "J. Delavande, R. Pierrard, S. Luccioni: Understanding Efficiency: Quantization, Batching, and Serving Strategies in LLM Energy Use, arXiv:2601.22362, 2026.", "https://arxiv.org/abs/2601.22362"),
    Triple("S28", "Sustainable Web Design Model, version 4 (Wholegrain Digital, Mightybytes, Green Web Foundation and others).", "https://sustainablewebdesign.org/estimating-digital-emissions/"),
    Triple("S29", "HTTP Archive: Web Almanac 2025, Page Weight.", "https://almanac.httparchive.org/en/2025/page-weight"),
    Triple("S30", "web.dev: Largest Contentful Paint (LCP).", "https://web.dev/articles/lcp"),
    Triple("S31", "M. Fowler: The Rise of Test Impact Analysis, martinfowler.com, 2017.", "https://martinfowler.com/articles/rise-test-impact-analysis.html"),
    Triple("S32", "Green Coding Solutions: Eco CI.", "https://www.green-coding.io/projects/eco-ci/"),
    Triple("S33", "K. Maeda: Performance evaluation of object serialization libraries in XML, JSON and binary formats, DICTAP 2012.", null),
    Triple("S34", "N. Bjørndal, M. Mazzara, A. Bucchiarone, N. Dragoni, S. Dustdar: Migration from Monolith to Microservices: Benchmarking a Case Study, Journal of Object Technology 20(3), 2021.", "https://doi.org/10.5381/jot.2021.20.2.a3"),
    Triple("S36", "Association of Issuing Bodies (AIB): European Residual Mixes 2024.", "https://www.aib-net.org/facts/european-residual-mix/2024"),
    Triple("S37", "GHG Protocol: Public consultations on Scope 2, 2025.", "https://ghgprotocol.org/blog/release-ghg-protocol-opens-public-consultations-scope-2-and-electricity-sector-consequential"),
    Triple("S38", "Google: 24/7 carbon-free energy.", "https://blog.google/around-the-globe/google-europe/how-carbon-free-energy-around-the-clock-can-work/"),
)

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
            // The sources the debriefs cite: [S…] has to lead somewhere inside the game.
            Txt(t("Sources"), Type.style(28, FontWeight.Bold))
            Txt(t("The marks in square brackets in the debriefs, for example [S27], refer to these sources."), Type.style(17, color = Palette.secondary))
            val uri = androidx.compose.ui.platform.LocalUriHandler.current
            Column(Modifier.background(Palette.panel, RoundedCornerShape(16.dp)).padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                for ((id, text, link) in SOURCES) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Txt(id, Type.style(15, FontWeight.Bold), Modifier.width(44.dp))
                        Column(Modifier.weight(1f)) {
                            Txt(text, Type.style(15))
                            if (link != null) Txt(link, Type.style(13, color = Palette.accent), Modifier.clickable { uri.openUri(link) })
                        }
                    }
                }
            }
        }
    }
}
