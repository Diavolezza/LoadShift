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
    Column(modifier.padding(28.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            RackIcon(48.dp)
            Column {
                Txt("Load Shift", Type.style(30, FontWeight.Bold))
                Txt(t("Version %@", BuildInfo.VERSION), Type.callout)
            }
        }
        Txt(t("A teaching game about Green IT: run a data center so that it serves its users reliably and emits as little CO₂ as possible – by switching servers, shifting work in time and, later, writing better software."), Type.body)
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Txt("© 2026 Gerhard Wanner · " + t("MIT License"), Type.callout)
            Txt(t("Grid data: Electricity Maps, Grid Review 2025 (life-cycle, imports included)"), Type.caption)
            Txt(t("Font: Inter, SIL Open Font License 1.1"), Type.caption)
            Txt(t("Built with Kotlin and Compose Multiplatform"), Type.caption)
        }
    }
}

/** The campaign: all scenarios with their stars; a scenario opens when the one before has a star. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ScenarioListScreen(onSelect: (Scenario) -> Unit) {
    var about by remember { mutableStateOf(false) }
    BoxWithConstraints(Modifier.fillMaxSize().background(Palette.background)) {
        val side = (maxWidth * 0.05f).coerceAtLeast(32.dp)
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = side, vertical = 48.dp),
            verticalArrangement = Arrangement.spacedBy(28.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                RackIcon(64.dp)
                Column {
                    Txt("Load Shift", Type.style(52, FontWeight.Bold))
                    Txt(t("Run a greener data center"), Type.style(22, color = Palette.secondary))
                }
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(20.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                for (scenario in Scenario.ALL) {
                    ScenarioCard(scenario, Progress.isUnlocked(scenario.number)) { onSelect(scenario) }
                }
            }
            Txt(t("More scenarios follow: headroom and autoscaling, consolidation, cooling, hardware, AI training, eco mode, two sites – and an engineering track: profiling and growing data."), Type.style(20, color = Palette.secondary))
            Txt(t("About Load Shift"), Type.style(16, FontWeight.SemiBold, Palette.accent), Modifier.clip(RoundedCornerShape(6.dp)).clickable { about = true }.padding(vertical = 4.dp, horizontal = 2.dp))
        }
        if (about) {
            Box(
                Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.35f))
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { about = false },
                contentAlignment = Alignment.Center,
            ) {
                AboutContent(Modifier.widthIn(max = 520.dp).background(Palette.panel, RoundedCornerShape(20.dp)))
            }
        }
    }
}

private fun days(n: Int) = if (n == 1) t("1 day") else t("%lld days", n)

@Composable
private fun ScenarioCard(scenario: Scenario, unlocked: Boolean, onClick: () -> Unit) {
    Column(
        Modifier.width(360.dp).alpha(if (unlocked) 1f else 0.6f)
            .clip(RoundedCornerShape(18.dp))
            .background(Palette.panel)
            .clickable(enabled = unlocked, onClick = onClick).padding(22.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Txt("${scenario.number}", Type.style(34, FontWeight.Bold, if (unlocked) Palette.accent else Palette.secondary), Modifier.weight(1f))
            if (unlocked) StarRow(Progress.stars(scenario.number), 22.dp) else LockIcon(22.dp)
        }
        Txt(t(scenario.title), Type.heading)
        Txt(ScenarioTexts.of(scenario).term, Type.style(16, FontWeight.SemiBold, Palette.accent))
        Txt("${t(scenario.setting.region.title)} · ${t(scenario.setting.season.title)} · ${days(scenario.days)}", Type.style(17, color = Palette.secondary))
        if (!unlocked) Txt(t("Earn a star in the scenario before."), Type.callout)
    }
}

/** Briefing before a scenario (CONCEPT.md 9): situation, the one idea, the screen; facts, goals and start beside it. */
@Composable
fun BriefingScreen(scenario: Scenario, onStart: () -> Unit, onBack: () -> Unit) {
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
                    Txt(t("Scenario %lld: %@", scenario.number, t(scenario.title)), Type.display)
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
    val game = remember(scenario) { GameController(scenario) }
    val width = 1366.dp
    val height = 960.dp
    // Marker positions on the full-size screen (GameScreen at 1366 × 960: panel 380 wide, timeline 288 high).
    val markers = listOf(470.dp to 36.dp, 40.dp to 150.dp, (width - 36.dp) to 136.dp, (width - 36.dp) to (height - 258.dp))
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
                GameScreen(game, onExit = {})
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
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Fact(t(scenario.setting.region.title))
            Fact(t(scenario.setting.season.title))
            Fact(if (scenario.days == 1) t("1 day, from 00:00") else t("%lld days, from 00:00", scenario.days))
            if (scenario.interactive.peak > 0) {
                Fact(t("%lld servers, %lld load units each", scenario.serverCount, perServer))
                Fact(t("Evening peak about %lld load units", scenario.interactive.peak))
            } else {
                Fact(t("%lld servers for batch jobs", scenario.serverCount))
                Fact(t("%lld jobs with deadlines", scenario.batchJobs.size))
            }
        }
        HDivider()
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Txt(t("Goals"), Type.heading)
            GoalLine(null, 1, t(texts.rules), Type.style(19))
            GoalLine(null, 2, t("CO₂ %@ or better", Format.changePermille(-scenario.goals.reduction.toLong())), Type.style(19))
            GoalLine(null, 3, specialLong(scenario.goals.special), Type.style(19))
            Txt(t("One star opens the next scenario."), Type.style(16, color = Palette.secondary))
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
