package de.wannerit.loadshift.app

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.remember
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import de.wannerit.loadshift.core.Scenario
import de.wannerit.loadshift.core.Track

/** The app's mark: a server rack drawn in the accent color. */
@Composable
fun RackIcon(size: Dp) {
    Canvas(Modifier.size(size)) {
        val w = this.size.width
        val stroke = w * 0.07f
        val h = w * 0.78f
        val top = (this.size.height - h) / 2
        drawRoundRect(Palette.accent, Offset(stroke / 2, top), Size(w - stroke, h), CornerRadius(w * 0.12f), style = Stroke(stroke))
        for (i in 1..2) drawLine(Palette.accent, Offset(stroke, top + h * i / 3), Offset(w - stroke, top + h * i / 3), stroke)
        for (i in 0..2) drawCircle(Palette.accent, stroke * 0.8f, Offset(w * 0.78f, top + h * (i + 0.5f) / 3))
    }
}

/** What Load Shift is, who made it, which data and font it uses (About window on the desktop, overlay on the web). */
@Composable
fun AboutContent(modifier: Modifier = Modifier) {
    // Scrolls when the window is low (a phone in landscape), so that the part for teachers stays reachable.
    Column(modifier.verticalScroll(rememberScrollState()).padding(28.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            RackIcon(48.dp)
            Column {
                Txt("Load Shift", Type.style(30, FontWeight.Bold))
                Txt(t("Version %@", BuildInfo.VERSION), Type.callout)
            }
        }
        Txt(t("A simulation game for teaching Green IT, in three tracks:"), Type.body)
        for ((track, text) in listOf(
            t("Operations") to t("Run a data center so that it serves its users reliably and emits as little CO₂ as possible – switch servers, shift work in time, cool wisely."),
            t("Engineering") to t("Build software that does the same work with less – measure first, cache what repeats, size AI models to the task."),
            t("Portfolio") to t("Find out where the IT emissions of a whole organisation come from and decide what to change first."),
        )) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Txt(track, Type.style(15, FontWeight.SemiBold))
                Txt(text, Type.body)
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Txt("© 2026 Gerhard Wanner · " + t("MIT License"), Type.callout)
            Txt(t("Grid data: Electricity Maps, Grid Review 2025 (life-cycle, imports included)"), Type.caption)
            Txt(t("Font: Inter, SIL Open Font License 1.1"), Type.caption)
            Txt(t("Built with Kotlin and Compose Multiplatform"), Type.caption)
        }
        // All scenarios open: launch option "unlock" (D19), not a switch here.
        ResetProgress()
    }
}

/** All stars gone, for a new player on the same device; a second click within three seconds confirms. */
@Composable
private fun ResetProgress() {
    var asking by remember { mutableStateOf(false) }
    androidx.compose.runtime.LaunchedEffect(asking) {
        if (asking) { kotlinx.coroutines.delay(3_000); asking = false }
    }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Txt(
            if (asking) t("Really delete all stars?") else "↺ " + t("Reset progress"),
            Type.style(15, FontWeight.SemiBold, if (asking) Color.White else Palette.ink),
            Modifier.clip(RoundedCornerShape(50)).background(if (asking) Palette.red else Palette.background)
                .border(1.dp, Palette.divider, RoundedCornerShape(50))
                .clickable { if (asking) { asking = false; Progress.resetAll() } else asking = true }
                .padding(horizontal = 14.dp, vertical = 6.dp),
        )
        Txt(t("Deletes the stars of all scenarios on this device. The scenarios close again."), Type.caption)
    }
}

/** A square check box. */
@Composable
fun CheckBox(on: Boolean) {
    Canvas(Modifier.size(22.dp)) {
        val w = size.width
        if (on) {
            drawRoundRect(Palette.accent, cornerRadius = CornerRadius(w * 0.2f))
            val p = androidx.compose.ui.graphics.Path().apply { moveTo(w * 0.25f, w * 0.52f); lineTo(w * 0.43f, w * 0.7f); lineTo(w * 0.76f, w * 0.32f) }
            drawPath(p, Color.White, style = Stroke(w * 0.12f))
        } else {
            drawRoundRect(Palette.secondary, cornerRadius = CornerRadius(w * 0.2f), style = Stroke(w * 0.08f))
        }
    }
}

/**
 * The campaign: "continue with" the next open scenario, then one tab per track with small cards; locked scenarios
 * only as a narrow row. A scenario opens when the one before has a star.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ScenarioListScreen(onLexicon: () -> Unit = {}, onSelect: (Scenario) -> Unit) {
    var about by remember { mutableStateOf(false) }
    @Suppress("UNUSED_VARIABLE") val redraw = Progress.version
    val next = nextScenario()
    var tab by remember { mutableStateOf(Storage.getInt("tab").takeIf { it in Track.entries.indices } ?: (next?.track?.ordinal ?: 0)) }
    BoxWithConstraints(Modifier.fillMaxSize().background(Palette.background)) {
        // A phone held upright: the buttons go under the title, the card for "continue with" stacks.
        val narrow = maxWidth < 700.dp
        val side = if (narrow) 16.dp else (maxWidth * 0.04f).coerceAtLeast(28.dp)
        val columns = ((maxWidth - side * 2 + 12.dp) / 200.dp).toInt().coerceIn(if (narrow) 1 else 2, 6)
        val cardWidth = (maxWidth - side * 2 - 12.dp * (columns - 1)) / columns - 1.dp
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = side, vertical = if (narrow) 20.dp else 36.dp),
            verticalArrangement = Arrangement.spacedBy(if (narrow) 16.dp else 22.dp),
        ) {
            @Composable
            fun buttons() {
                Txt(t("Glossary"), Type.style(16, FontWeight.SemiBold, Palette.accent),
                    Modifier.clip(RoundedCornerShape(10.dp)).background(Palette.panel).clickable(onClick = onLexicon).padding(horizontal = 14.dp, vertical = 10.dp))
                LanguageSwitch()
                InfoButton { about = true }
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(if (narrow) 12.dp else 18.dp)) {
                RackIcon(if (narrow) 44.dp else 56.dp)
                Column(Modifier.weight(1f)) {
                    Txt("Load Shift", Type.style(if (narrow) 32 else 44, FontWeight.Bold))
                    Txt(t("Green IT – from code to portfolio"), Type.style(if (narrow) 15 else 19, color = Palette.secondary))
                }
                if (!narrow) buttons()
            }
            if (narrow) Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) { buttons() }
            if (next != null) ContinueCard(next, narrow) { onSelect(next) }
            // Tabs, one per track, with the scenarios that have a star; on a phone they scroll sideways.
            Column {
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    for ((track, title) in listOf(Track.OPERATIONS to t("Operations"), Track.ENGINEERING to t("Engineering"), Track.PORTFOLIO to t("Portfolio"))) {
                        val all = Scenario.ALL.filter { it.track == track }
                        val done = all.count { Progress.stars(it.number) > 0 }
                        val on = tab == track.ordinal
                        Column(Modifier.width(androidx.compose.foundation.layout.IntrinsicSize.Max).clip(RoundedCornerShape(8.dp)).clickable { tab = track.ordinal; Storage.setInt("tab", tab) }) {
                            Row(Modifier.padding(horizontal = if (narrow) 10.dp else 18.dp, vertical = 10.dp), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Txt(title, Type.style(19, FontWeight.SemiBold, if (on) Palette.ink else Palette.secondary))
                                Txt(t("%lld / %lld ★", done, all.size), Type.style(14, color = Palette.secondary))
                            }
                            Box(Modifier.fillMaxWidth().height(3.dp).background(if (on) Palette.accent else Color.Transparent))
                        }
                    }
                }
                HDivider()
            }
            val track = Track.entries[tab]
            val scenarios = Scenario.ALL.filter { it.track == track }
            val groups = if (track == Track.OPERATIONS) listOf(
                t("Basics") to scenarios.filter { (it.label.toIntOrNull() ?: 0) <= 6 },
                t("Going deeper") to scenarios.filter { (it.label.toIntOrNull() ?: 0) in 7..11 },
                t("Capstone") to scenarios.filter { (it.label.toIntOrNull() ?: 0) >= 12 },
            ) else listOf(null to scenarios)
            for ((title, group) in groups) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (title != null) Txt(title.uppercase(), Type.style(14, FontWeight.Bold, Palette.secondary))
                    val open = group.filter { Progress.isUnlocked(it) }
                    val locked = group - open.toSet()
                    if (open.isNotEmpty()) FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        for (scenario in open) ScenarioCard(scenario, scenario == next, Modifier.width(cardWidth)) { onSelect(scenario) }
                    }
                    if (locked.isNotEmpty()) FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        for (scenario in locked) LockedChip(scenario)
                    }
                }
            }
        }
        if (about) {
            Box(
                Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.35f))
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { about = false },
                contentAlignment = Alignment.Center,
            ) {
                // A click into the dialog keeps it open; only a click beside it closes it.
                AboutContent(Modifier.padding(16.dp).widthIn(max = 520.dp).clip(RoundedCornerShape(20.dp)).background(Palette.panel, RoundedCornerShape(20.dp))
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {})
            }
        }
    }
}

/** "i" in a circle: about Load Shift, for teachers. */
@Composable
private fun InfoButton(onClick: () -> Unit) {
    Box(
        Modifier.size(42.dp).clip(CircleShape).background(Palette.panel).border(1.5.dp, Palette.accent, CircleShape).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Txt("i", Type.style(22, FontWeight.Bold, Palette.accent).copy(textAlign = TextAlign.Center))
    }
}

/**
 * The scenario to continue with: the first open one without a star in the track played last, else in any track
 * (null when every open scenario has a star).
 */
private fun nextScenario(): Scenario? {
    val last = Scenario.ALL.firstOrNull { it.number == Storage.getInt("lastScenario") }
    fun firstOpen(list: List<Scenario>) = list.firstOrNull { Progress.isUnlocked(it) && Progress.stars(it.number) == 0 }
    return last?.let { l -> firstOpen(Scenario.ALL.filter { it.track == l.track }) } ?: firstOpen(Scenario.ALL)
}

@Composable
private fun ContinueCard(scenario: Scenario, narrow: Boolean, onClick: () -> Unit) {
    if (narrow) {
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Palette.panel).border(2.dp, Palette.accent, RoundedCornerShape(18.dp))
                .clickable(onClick = onClick).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Txt(scenario.shownLabel, Type.style(32, FontWeight.Bold, Palette.accent))
                Column {
                    Txt(t("Continue with").uppercase(), Type.style(12, FontWeight.SemiBold, Palette.secondary))
                    Txt(t(scenario.title), Type.style(20, FontWeight.Bold))
                }
            }
            Txt(ScenarioTexts.of(scenario).term, Type.style(15, FontWeight.SemiBold, Palette.accent))
            Txt(cardFacts(scenario), Type.style(14, color = Palette.secondary))
            Spacer(Modifier.height(6.dp))
            PrimaryButton("▶  " + t("To the briefing"), Modifier.fillMaxWidth(), onClick = onClick)
        }
        return
    }
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Palette.panel).border(2.dp, Palette.accent, RoundedCornerShape(18.dp))
            .clickable(onClick = onClick).padding(horizontal = 24.dp, vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        Column {
            Txt(t("Continue with").uppercase(), Type.style(13, FontWeight.SemiBold, Palette.secondary))
            Txt(scenario.shownLabel, Type.style(44, FontWeight.Bold, Palette.accent))
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Txt(t(scenario.title), Type.style(26, FontWeight.Bold))
            Txt(ScenarioTexts.of(scenario).term, Type.style(15, FontWeight.SemiBold, Palette.accent))
            Txt(cardFacts(scenario), Type.style(15, color = Palette.secondary))
        }
        PrimaryButton("▶  " + t("To the briefing"), large = true, onClick = onClick)
    }
}

/** "Germany · autumn · 2 days", or what a portfolio scenario is about. */
private fun cardFacts(scenario: Scenario): String = scenario.availability?.let { t("%lld building blocks · a year", it.blocks.size) }
    ?: scenario.updates?.let { t("%lld kinds of update · a year", it.kinds.size) }
    ?: scenario.tenancy?.let { t("%lld building blocks · a year", it.components.size) }
    ?: scenario.lifecycle?.let { t("%lld kinds of data · a year", it.kinds.size) }
    ?: scenario.patterns?.let { t("%lld subsystems · a year", it.subsystems.size) }
    ?: scenario.repatriation?.let { t("%lld applications · three years", it.apps.size) }
    ?: scenario.tidy?.let { t("%lld applications · three years", it.apps.size) }
    ?: scenario.abatement?.let { t("%lld measures · three years", it.measures.size) }
    ?: scenario.portfolio?.let { t("%lld applications · a year", it.apps.size) }
    ?: "${t(scenario.setting.region.title)} · ${t(scenario.setting.season.title)} · ${days(scenario.days)}"

private fun days(n: Int) = if (n == 1) t("1 day") else t("%lld days", n)

/** Deutsch | English, kept on the device. */
@Composable
private fun LanguageSwitch() {
    Row(Modifier.background(Palette.panel, RoundedCornerShape(10.dp)).padding(3.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        for ((german, name) in listOf(true to "Deutsch", false to "English")) {
            val selected = Language.german == german
            Txt(
                name, Type.style(16, FontWeight.SemiBold, if (selected) Color.White else Palette.ink),
                Modifier.clip(RoundedCornerShape(8.dp)).background(if (selected) Palette.accent else Color.Transparent)
                    .clickable { Language.choose(german) }.padding(horizontal = 14.dp, vertical = 8.dp),
            )
        }
    }
}

/** The name in the list: B3 in German (Betrieb), O3 in English (Operations); E1 for engineering in both. */
val Scenario.shownLabel: String get() = if (track == Track.OPERATIONS) t("O%@", label) else label

@Composable
private fun ScenarioCard(scenario: Scenario, highlighted: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Column(
        modifier.height(118.dp).clip(RoundedCornerShape(14.dp)).background(Palette.panel)
            .border(if (highlighted) 2.dp else 0.dp, if (highlighted) Palette.accent else Color.Transparent, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick).padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Txt(scenario.shownLabel, Type.style(24, FontWeight.Bold, Palette.accent), Modifier.weight(1f))
            StarRow(Progress.stars(scenario.number), 15.dp)
        }
        Txt(t(scenario.title), Type.style(16, FontWeight.SemiBold), maxLines = 2)
        Spacer(Modifier.weight(1f))
        // One line on the small card: the term without its explanation in brackets ("Green AI (Right-sizing Models)").
        Txt(ScenarioTexts.of(scenario).term.substringBefore(" ("), Type.style(13, FontWeight.SemiBold, Palette.accent), maxLines = 1)
    }
}

/** A locked scenario: a narrow chip with a padlock; what opens it is said when the mouse is not needed – in words. */
@Composable
private fun LockedChip(scenario: Scenario) {
    val before = Scenario.ALL.firstOrNull { it.number == scenario.unlockAfter }?.shownLabel ?: ""
    Row(
        Modifier.background(Palette.panel.copy(alpha = 0.5f), RoundedCornerShape(10.dp)).border(1.dp, Palette.divider, RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        LockIcon(15.dp)
        Txt(scenario.shownLabel, Type.style(14, FontWeight.Bold, Palette.secondary))
        Txt(t(scenario.title), Type.style(14, color = Palette.secondary))
        Txt("· " + t("after %@", before), Type.style(13, color = Palette.secondary))
    }
}

/** Briefing before a scenario (CONCEPT.md 9): situation, the one idea, the screen; facts, goals and start beside it. */
@Composable
fun BriefingScreen(scenario: Scenario, onStart: () -> Unit, onBack: () -> Unit) {
    // The start page continues with the track played last.
    androidx.compose.runtime.LaunchedEffect(scenario) { Storage.setInt("lastScenario", scenario.number) }
    val texts = ScenarioTexts.of(scenario)
    BoxWithConstraints(Modifier.fillMaxSize().background(Palette.background)) {
        val wide = maxWidth > 900.dp
        val side = (maxWidth * 0.05f).coerceAtLeast(32.dp)
        val sideCardWidth = (maxWidth * 0.38f).coerceAtMost(440.dp)
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = side, vertical = 40.dp),
            verticalArrangement = Arrangement.spacedBy(28.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                Txt("‹", Type.style(40, FontWeight.SemiBold), Modifier.clip(RoundedCornerShape(8.dp)).clickable(onClick = onBack).padding(horizontal = 8.dp))
                RackIcon(56.dp)
                Column(Modifier.weight(1f)) {
                    Txt("Load Shift", Type.style(22, FontWeight.SemiBold, Palette.secondary))
                    Txt(t("Scenario %@: %@", scenario.shownLabel, t(scenario.title)), Type.display)
                }
                val best = Progress.stars(scenario.number)
                if (best > 0) Column(horizontalAlignment = Alignment.End) {
                    Txt(t("Best result"), Type.callout)
                    StarRow(best, 30.dp)
                }
            }
            if (wide) {
                Row(horizontalArrangement = Arrangement.spacedBy(40.dp)) {
                    Story(scenario, texts, Modifier.weight(1f))
                    SideCard(scenario, texts, onStart, Modifier.width(sideCardWidth))
                }
            } else {
                Story(scenario, texts, Modifier.fillMaxWidth())
                SideCard(scenario, texts, onStart, Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun Story(scenario: Scenario, texts: ScenarioTexts, modifier: Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(26.dp)) {
        for ((title, text) in listOf(t("Situation") to texts.situation, t("The idea") to texts.idea)) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Txt(title, Type.heading)
                Txt(t(text), Type.bodyLarge)
            }
        }
        Term(texts)
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Txt(t("On the screen"), Type.heading)
            ScreenPreview(scenario, Modifier.fillMaxWidth())
            texts.screen.forEachIndexed { i, text ->
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Marker(i + 1)
                    Txt(t(text), Type.bodyLarge, Modifier.weight(1f))
                }
            }
        }
    }
}

/** The technical term of the scenario, with its meaning: what the player does has a name in the field. */
@Composable
fun Term(texts: ScenarioTexts, modifier: Modifier = Modifier) {
    Column(
        modifier.fillMaxWidth().background(Palette.accent.copy(alpha = 0.08f), RoundedCornerShape(14.dp))
            .border(1.dp, Palette.accent.copy(alpha = 0.35f), RoundedCornerShape(14.dp)).padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Txt(t("Technical term"), Type.style(15, FontWeight.SemiBold, Palette.accent))
        Txt(texts.term, Type.style(24, FontWeight.Bold))
        Txt(t(texts.termText), Type.bodyLarge)
    }
}

/** The play screen as the scenario starts, drawn small: not clickable, with numbered markers for the four areas. */
@Composable
private fun ScreenPreview(scenario: Scenario, modifier: Modifier) {
    // Always the scenario's starting state – never the player's last settings, which may be a solution.
    val portfolio = remember(scenario) { scenario.portfolio?.let { PortfolioController(scenario, fresh = true) } }
    val abatement = remember(scenario) { scenario.abatement?.let { AbatementController(scenario, fresh = true) } }
    val tidy = remember(scenario) { scenario.tidy?.let { TidyController(scenario, fresh = true) } }
    val repatriation = remember(scenario) { scenario.repatriation?.let { RepatriationController(scenario, fresh = true) } }
    val availability = remember(scenario) { scenario.availability?.let { AvailabilityController(scenario, fresh = true) } }
    val updates = remember(scenario) { scenario.updates?.let { UpdatesController(scenario, fresh = true) } }
    val tenancy = remember(scenario) { scenario.tenancy?.let { TenancyController(scenario, fresh = true) } }
    val lifecycle = remember(scenario) { scenario.lifecycle?.let { LifecycleController(scenario, fresh = true) } }
    val patterns = remember(scenario) { scenario.patterns?.let { PatternsController(scenario, fresh = true) } }
    val game = remember(scenario) { if (portfolio == null && abatement == null && tidy == null && repatriation == null && availability == null && updates == null && tenancy == null && lifecycle == null && patterns == null) GameController(scenario, fresh = true) else null }
    val width = 1366.dp
    val height = 960.dp
    // Marker positions on the full-size screen (GameScreen at 1366 × 960: panel 380 wide, timeline 288 high).
    val markers = if (availability != null) listOf(470.dp to 36.dp, 40.dp to 150.dp, 40.dp to 470.dp, (width - 36.dp) to 136.dp)
        else if (updates != null) listOf(470.dp to 36.dp, 40.dp to 150.dp, 40.dp to 560.dp, (width - 36.dp) to 136.dp)
        else if (tenancy != null) listOf(470.dp to 36.dp, 40.dp to 150.dp, 40.dp to 700.dp, (width - 36.dp) to 136.dp)
        else if (lifecycle != null) listOf(470.dp to 36.dp, 40.dp to 150.dp, 40.dp to 640.dp, (width - 36.dp) to 136.dp)
        else if (patterns != null) listOf(470.dp to 36.dp, 40.dp to 150.dp, 40.dp to 640.dp, (width - 36.dp) to 136.dp)
        else if (repatriation != null) listOf(470.dp to 36.dp, 40.dp to 150.dp, 40.dp to (height - 100.dp), (width - 36.dp) to 136.dp)
        else if (tidy != null) listOf(470.dp to 36.dp, 40.dp to 150.dp, 40.dp to (height - 290.dp), (width - 36.dp) to 136.dp)
        else if (abatement != null) listOf(470.dp to 36.dp, 40.dp to 150.dp, 40.dp to 440.dp, (width - 36.dp) to 136.dp)
        else if (portfolio != null) listOf(470.dp to 36.dp, 40.dp to 150.dp, 300.dp to 345.dp, (width - 36.dp) to 136.dp)
        else listOf(470.dp to 36.dp, 40.dp to 150.dp, (width - 36.dp) to 136.dp, (width - 36.dp) to (height - 258.dp))
    BoxWithConstraints(modifier) {
        val scale = maxWidth / width
        Box(
            Modifier.fillMaxWidth().height(height * scale).clipToBounds()
                .border(1.dp, Palette.secondary.copy(alpha = 0.3f), RoundedCornerShape(6.dp)),
        ) {
            Box(
                Modifier.wrapContentSize(Alignment.TopStart, unbounded = true).requiredSize(width, height)
                    .graphicsLayer { scaleX = scale; scaleY = scale; transformOrigin = TransformOrigin(0f, 0f) },
            ) {
                if (availability != null) AvailabilityScreen(availability, onExit = {})
                else if (updates != null) UpdatesScreen(updates, onExit = {})
                else if (tenancy != null) TenancyScreen(tenancy, onExit = {})
                else if (lifecycle != null) LifecycleScreen(lifecycle, onExit = {})
                else if (patterns != null) PatternsScreen(patterns, onExit = {})
                else if (repatriation != null) RepatriationScreen(repatriation, onExit = {})
                else if (tidy != null) TidyScreen(tidy, onExit = {})
                else if (abatement != null) AbatementScreen(abatement, onExit = {})
                else if (portfolio != null) PortfolioScreen(portfolio, onExit = {}) else GameScreen(game!!, onExit = {})
            }
            // On top: takes every click, so the preview is only a picture; the mouse wheel still scrolls the page.
            Box(Modifier.matchParentSize().pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        if (event.type != PointerEventType.Scroll) event.changes.forEach { it.consume() }
                    }
                }
            })
            markers.forEachIndexed { i, (x, y) ->
                Marker(i + 1, Modifier.offset(x * scale - 14.dp, y * scale - 14.dp))
            }
        }
    }
}

/** A numbered circle that links the preview to the list below it. */
@Composable
private fun Marker(number: Int, modifier: Modifier = Modifier) {
    Box(
        modifier.size(28.dp).background(Palette.accent, CircleShape).border(2.dp, Color.White, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Txt("$number", Type.style(15, FontWeight.Bold, Color.White).copy(textAlign = TextAlign.Center))
    }
}

@Composable
private fun SideCard(scenario: Scenario, texts: ScenarioTexts, onStart: () -> Unit, modifier: Modifier) {
    val perServer = scenario.fleet.firstOrNull()?.kind?.capacity ?: 0
    Column(modifier.background(Palette.panel, RoundedCornerShape(20.dp)).padding(28.dp), verticalArrangement = Arrangement.spacedBy(22.dp)) {
        val portfolio = scenario.portfolio
        val abatement = scenario.abatement
        val tidy = scenario.tidy
        val repatriation = scenario.repatriation
        val availability = scenario.availability
        val updates = scenario.updates
        val tenancy = scenario.tenancy
        val lifecycle = scenario.lifecycle
        val patterns = scenario.patterns
        if (patterns != null) Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Fact(t("A mobility platform with %lld subsystems", patterns.subsystems.size))
            Fact(t("Today Event Sourcing, CQRS and events everywhere"))
            Fact(t("Counted: a year, location-based"))
        } else if (tenancy != null) Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Fact(t("An HR software for %lld customer companies", tenancy.groups.sumOf { it.count }))
            Fact(t("%lld building blocks, today one instance of each per customer", tenancy.components.size))
            Fact(t("Counted: a year, location-based"))
        } else if (lifecycle != null) Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Fact(t("An online retailer with %lld years of data", lifecycle.historyYears.toLong()))
            Fact(t("%lld kinds of data, today nothing is deleted", lifecycle.kinds.size))
            Fact(t("Counted: a year, location-based with the making of the storage media"))
        } else if (availability != null) Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Fact(t("%lld building blocks, %lld quality scenarios", availability.blocks.size, availability.scenarios.size))
            Fact(t("Today everything active-active on two sites"))
            Fact(t("Counted: a year, location-based"))
        } else if (updates != null) Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Fact(t("A delivery app on %@ phones", Format.number(updates.phones.toDouble())))
            Fact(t("%lld kinds of update, open and closed", updates.kinds.size))
            Fact(t("Counted: a year – phones, servers and network, location-based"))
        } else if (repatriation != null) Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Fact(t("%lld applications in the cloud in Frankfurt", repatriation.apps.size))
            Fact(t("Four places: two cloud regions, a colocation, the own server room"))
            Fact(t("Costs at least %@ lower", Format.wholePercent(repatriation.savingGoalPermille)))
            Fact(t("Counted: three years, location-based with the making of servers"))
        } else if (tidy != null) Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Fact(t("%lld applications", tidy.apps.size))
            Fact(t("At most %lld projects at a time", tidy.capacity))
            Fact(t("Counted: three years, location-based"))
        } else if (abatement != null) Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Fact(t("%lld measures on a consultant's list", abatement.measures.size))
            Fact(t("Footprint: about %@ t CO₂ a year", tonnes(abatement.footprintKgPerYear)))
            Fact(t("Budget for the measures: %@ €", Format.number(abatement.budget.toDouble())))
            Fact(t("Counted: three years, location-based"))
        } else if (portfolio != null) Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            fun count(h: de.wannerit.loadshift.core.Hosting) = portfolio.apps.count { it.hosting == h }
            Fact(t("%lld applications in the own data center", count(de.wannerit.loadshift.core.Hosting.ON_PREM)))
            Fact(t("%lld applications in the cloud", count(de.wannerit.loadshift.core.Hosting.CLOUD)))
            Fact(t("%lld applications as SaaS", count(de.wannerit.loadshift.core.Hosting.SAAS)))
            Fact(t("Budget for data and measures: %@ €", Format.number(portfolio.budget.toDouble())))
            Fact(t("Counted: a year, location-based"))
        } else Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            val sites = scenario.location?.sites
            Fact(if (sites != null) t("%@ and %@", t(sites[0].region.title), t(sites[1].region.title)) else t(scenario.setting.region.title))
            Fact(t(scenario.setting.season.title))
            Fact(if (scenario.days == 1) t("1 day, from 00:00") else t("%lld days, from 00:00", scenario.days))
            val inference = scenario.inference
            val pipeline = scenario.pipeline
            val services = scenario.services
            val contract = scenario.contract
            if (contract != null) {
                Fact(t("%lld servers, %lld load units each", scenario.serverCount, perServer))
                Fact(t("%lld batch jobs that can wait", scenario.batchJobs.size))
                Fact(t("Electricity: %@ ct/kWh, the budget may rise by %@", Format.number(contract.priceTenthsCent / 10.0, 1), Format.wholePercent(contract.budgetPermille - 1_000)))
            } else if (services != null) {
                Fact(t("A logistics system: four microservices, two external providers"))
                Fact(t("Up to %@ tracking requests a second", Format.number(services.peakRate.toDouble())))
                Fact(t("%lld servers", scenario.serverCount))
            } else if (pipeline != null) {
                Fact(t("%lld developers, %lld build agents", pipeline.developers, pipeline.agents))
                Fact(t("%lld test environments with 3 replicas each", pipeline.environments))
                Fact(t("Release on Friday at 14:00"))
            } else if (scenario.web != null) {
                Fact(t("%lld servers for the shop", scenario.serverCount))
                Fact(t("A visit: about 12 MB"))
                Fact(t("Counted: data center, network and the visitors' devices"))
            } else if (inference != null) {
                Fact(t("A shop assistant: up to %lld questions a second", inference.peakRate))
                Fact(t("%lld GPU servers for the language models", inference.gpuServers))
                Fact(t("%lld kinds of question", inference.kinds.size))
            } else if (scenario.training != null && scenario.interactive.peak > 0) {
                val gpus = scenario.fleet.filter { it.kind == de.wannerit.loadshift.core.ServerKind.GPU_CURRENT }.sumOf { it.count }
                Fact(t("%lld CPU servers for the shop and the batch jobs", scenario.serverCount - gpus))
                Fact(t("%lld GPU servers for the training", gpus))
                Fact(t("%lld jobs with deadlines", scenario.batchJobs.size))
            } else if (scenario.interactive.peak > 0) {
                Fact(t("%lld servers, %lld load units each", scenario.serverCount, perServer))
                Fact(t("Evening peak about %lld load units", scenario.interactive.peak))
            } else {
                val location = scenario.location
                if (location != null) {
                    Fact(t("%lld servers in %@, %lld in %@", location.servers[0], t(location.sites[0].title), location.servers[1], t(location.sites[1].title)))
                    Fact(t("%lld jobs with deadlines", location.jobs.size))
                } else {
                    Fact(t("%lld servers for batch jobs", scenario.serverCount))
                    Fact(t("%lld jobs with deadlines", scenario.batchJobs.size))
                }
            }
        }
        HDivider()
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Txt(t("Goals"), Type.heading)
            GoalLine(null, 1, t(texts.rules), Type.style(19))
            GoalLine(null, 2, t("CO₂ %@ or better", Format.changePermille(-scenario.goals.reduction.toLong())), Type.style(19))
            GoalLine(null, 3, specialLong(scenario.goals.special), Type.style(19))
            // D32: only the scenarios that build on this one wait for its star.
            val opens = Scenario.ALL.filter { it.unlockAfter == scenario.number }.joinToString(", ") { it.shownLabel }
            if (opens.isNotEmpty()) Txt(t("One star opens %@.", opens), Type.style(16, color = Palette.secondary))
        }
        PrimaryButton("▶  " + t("Start"), Modifier.fillMaxWidth(), large = true, onClick = onStart)
    }
}

@Composable
private fun Fact(text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Canvas(Modifier.size(8.dp)) { drawCircle(Palette.accent) }
        Txt(text, Type.style(19))
    }
}

/** A closed padlock. */
@Composable
fun LockIcon(size: Dp) {
    Canvas(Modifier.size(size)) {
        val w = this.size.width
        drawRoundRect(Palette.secondary, Offset(w * 0.15f, w * 0.45f), Size(w * 0.7f, w * 0.5f), CornerRadius(w * 0.08f))
        drawArc(Palette.secondary, 180f, 180f, false, Offset(w * 0.28f, w * 0.12f), Size(w * 0.44f, w * 0.6f), style = Stroke(w * 0.1f))
    }
}
