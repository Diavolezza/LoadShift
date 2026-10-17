package de.wannerit.loadshift.app

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import de.wannerit.loadshift.core.Control
import de.wannerit.loadshift.core.GameTime
import de.wannerit.loadshift.core.Scenario
import de.wannerit.loadshift.core.SpecialGoal
import kotlinx.coroutines.delay

/** The play screen (docs/CONCEPT.md 10.1): top bar with the goals, hall and controls, timeline. */
@Composable
fun GameScreen(game: GameController, onExit: () -> Unit, onNext: ((Scenario) -> Unit)? = null) {
    // The clock: one tick per 1/speed seconds while not paused.
    LaunchedEffect(game.speed) {
        val speed = game.speed
        if (speed == Speed.PAUSED) return@LaunchedEffect
        while (true) {
            delay(1_000L / speed.ticksPerSecond)
            game.step()
            if (game.speed != speed) break
        }
    }
    @Suppress("UNUSED_VARIABLE") val redraw = game.version
    BoxWithConstraints(Modifier.fillMaxSize().background(Palette.background)) {
        val panelWidth = (maxWidth * 0.32f).coerceIn(300.dp, 380.dp)
        val timelineHeight = (maxHeight * 0.3f).coerceAtLeast(190.dp)
        Column(Modifier.fillMaxSize()) {
            TopBar(game, onExit)
            GoalBar(game)
            HDivider()
            Row(Modifier.fillMaxWidth().weight(1f)) {
                if (Control.PLACEMENT in game.scenario.controls) {
                    PlacementBoard(game, Modifier.weight(1f).fillMaxHeight())
                } else {
                    HallView(game.simulation.servers, game.version, game.hasInteractive, game.hasBatch, Modifier.weight(1f).fillMaxHeight())
                }
                VDivider()
                ControlPanel(game, Modifier.width(panelWidth).fillMaxHeight())
            }
            HDivider()
            TimelineChart(game.simulation, game.version, Modifier.fillMaxWidth().height(timelineHeight).background(Palette.panel))
        }
        if (game.showEnd) EndCard(game, onExit, onNext)
    }
}

@Composable
private fun TopBar(game: GameController, onExit: () -> Unit) {
    val simulation = game.simulation
    val shown = maxOf(simulation.scenario.startTick, simulation.tick)
    val time = GameTime(shown)
    val intensity = simulation.grid.intensity(minOf(shown, simulation.scenario.endTick - 1))
    val last = simulation.history.lastOrNull()
    // Left the controls, the clock in the middle, the figures on the right: the buttons stay where they are.
    Box(Modifier.fillMaxWidth().background(Palette.panel).padding(horizontal = 20.dp, vertical = 10.dp)) {
        Row(Modifier.align(Alignment.CenterStart), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(22.dp)) {
            Txt("‹", Type.style(34, FontWeight.SemiBold), Modifier.clip(RoundedCornerShape(8.dp)).clickable(onClick = onExit).padding(horizontal = 6.dp))
            SpeedControl(game)
        }
        Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
            Txt(t("Day %lld", time.day - GameTime.START.day + 1), Type.caption)
            Txt(time.clockText, Type.style(30, FontWeight.SemiBold).copy(fontFeatureSettings = "tnum"))
        }
        Row(Modifier.align(Alignment.CenterEnd), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(22.dp)) {
        Metric(t("Grid now"), "$intensity g/kWh", Palette.intensity(intensity))
        Metric(t("CO₂ so far"), "${Format.kilograms(simulation.totals.emissions)} kg")
        if (game.hasInteractive) {
            Metric(t("SLA"), Format.percent(simulation.totals.sla),
                if (simulation.totals.sla >= simulation.scenario.slaTarget) Palette.ink else Palette.red)
        }
        if (game.hasBatch) {
            Metric(t("Jobs done"), "${simulation.totals.jobsDone} / ${simulation.jobs.size}",
                if (simulation.totals.deadlinesMissed == 0) Palette.ink else Palette.red)
        }
        Metric(t("Power"), "${Format.kilowatts(last?.facilityPower ?: 0)} kW")
        }
    }
}

@Composable
private fun SpeedControl(game: GameController) {
    Row(Modifier.background(Palette.background, RoundedCornerShape(10.dp)).padding(3.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        for (speed in Speed.entries) {
            val selected = game.speed == speed
            val enabled = !game.isFinished || speed == Speed.PAUSED
            Box(
                Modifier.size(width = 48.dp, height = 38.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (selected) Palette.accent else Color.Transparent)
                    .clickable(enabled = enabled) { game.changeSpeed(speed) },
                contentAlignment = Alignment.Center,
            ) {
                val color = if (selected) Color.White else if (enabled) Palette.ink else Palette.secondary
                if (speed == Speed.PAUSED) {
                    Canvas(Modifier.size(14.dp)) {
                        val w = size.width
                        drawRect(color, Offset(w * 0.12f, 0f), androidx.compose.ui.geometry.Size(w * 0.26f, w))
                        drawRect(color, Offset(w * 0.62f, 0f), androidx.compose.ui.geometry.Size(w * 0.26f, w))
                    }
                } else {
                    Txt("${speed.ticksPerSecond}×", Type.style(17, FontWeight.SemiBold, color))
                }
            }
        }
    }
}

private fun specialShort(goal: SpecialGoal): String = when (goal) {
    is SpecialGoal.MeanUtilization -> t("utilization ≥ %@", Format.wholePercent(goal.permille))
    is SpecialGoal.Reduction -> t("CO₂ from %@", Format.changePermille(-goal.permille.toLong()))
    is SpecialGoal.BatchIntensity -> t("batch ≤ %lld g/kWh", goal.gramsPerKWh)
    is SpecialGoal.MaxServersOn -> t("≤ %lld hosts", goal.servers)
}

fun specialLong(goal: SpecialGoal): String = when (goal) {
    is SpecialGoal.MeanUtilization -> t("mean utilization at least %@", Format.wholePercent(goal.permille))
    is SpecialGoal.Reduction -> t("CO₂ %@ or better", Format.changePermille(-goal.permille.toLong()))
    is SpecialGoal.BatchIntensity -> t("batch work at %lld g/kWh or less on average", goal.gramsPerKWh)
    is SpecialGoal.MaxServersOn -> t("at most %lld hosts running", goal.servers)
}

/** The goals and where the run stands. */
@Composable
private fun GoalBar(game: GameController) {
    val scenario = game.scenario
    val totals = game.simulation.totals
    val change = game.changeSoFar
    Row(
        Modifier.fillMaxWidth().background(Palette.panel).padding(horizontal = 20.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Txt(t("Goals"), Type.style(13, FontWeight.SemiBold, Palette.secondary))
        if (game.hasInteractive) {
            GoalItem(totals.sla >= scenario.slaTarget, change != null, t("SLA ≥ %@", Format.percent(scenario.slaTarget)))
            GoalItem((change ?: 0) < 0, change != null,
                if (change == null) t("CO₂ vs. reference") else t("CO₂ vs. reference: %@", Format.changePermille(change)))
        }
        if (game.hasBatch) {
            val missed = totals.deadlinesMissed
            GoalItem(missed == 0, change != null, if (missed == 0) t("No deadline missed") else t("Deadlines missed: %lld", missed))
            if (!game.hasInteractive) {
                // CO₂ so far would favour waiting; the intensity of the work done is a fair measure.
                val mine = totals.meanBatchIntensityTenths
                val ref = game.reference.totals.meanBatchIntensityTenths
                GoalItem(mine <= ref, totals.batchEnergy > 0,
                    if (totals.batchEnergy > 0) t("Batch work Ø %lld g/kWh (reference %lld)", (mine + 5) / 10, (ref + 5) / 10)
                    else t("Batch work: no job has run yet"))
            }
        }
        Spacer(Modifier.weight(1f))
        Row(verticalAlignment = Alignment.CenterVertically) {
            StarLabel(2, 13.dp)
            Txt(t("CO₂ from %@", Format.changePermille(-scenario.goals.reduction.toLong())), Type.callout)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            StarLabel(3, 13.dp)
            Txt(specialShort(scenario.goals.special), Type.callout)
        }
    }
}

@Composable
private fun GoalItem(ok: Boolean, started: Boolean, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        if (started) CheckMark(ok, warning = !ok) else CheckMark(false)
        Txt(text, Type.callout, maxLines = 1)
    }
}

/** End of the scenario (CONCEPT.md 8.2, 8.3): stars, goals, the result next to the reference, "what if". */
@Composable
private fun EndCard(game: GameController, onExit: () -> Unit, onNext: ((Scenario) -> Unit)?) {
    val scenario = game.scenario
    val you = game.simulation.totals
    val ref = game.reference.totals
    val met = scenario.goalsMet(you, ref)
    val texts = ScenarioTexts.of(scenario)
    Box(
        Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.35f))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {},
        contentAlignment = Alignment.Center,
    ) {
        Column(
            Modifier.padding(vertical = 40.dp, horizontal = 24.dp).widthIn(max = 620.dp)
                .background(Palette.panel, RoundedCornerShape(20.dp)),
        ) {
            Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()).padding(30.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Txt(if (scenario.days == 1) t("Day complete") else t("Scenario complete"), Type.title, Modifier.weight(1f))
                    StarRow(game.stars)
                }
                Txt("${Format.change(you.emissions, ref.emissions)} CO₂", Type.style(40, FontWeight.Bold, if (you.emissions < ref.emissions) Palette.accent else Palette.red))
                Txt(t(texts.comparedWith), Type.style(17, color = Palette.secondary))
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    GoalLine(met.rules, 1, t(texts.rules))
                    GoalLine(met.reduction, 2, t("CO₂ %@ or better", Format.changePermille(-scenario.goals.reduction.toLong())))
                    GoalLine(met.special, 3, specialLong(scenario.goals.special))
                }
                val rows = buildList {
                    add(t("CO₂ (kg)") to (Format.kilograms(you.emissions) to Format.kilograms(ref.emissions)))
                    add(t("of it embodied (kg)") to (Format.kilograms(you.embodied) to Format.kilograms(ref.embodied)))
                    add(t("Energy (kWh)") to (Format.kilowattHours(you.facilityEnergy) to Format.kilowattHours(ref.facilityEnergy)))
                    if (game.hasInteractive) {
                        add(t("SLA") to (Format.percent(you.sla) to Format.percent(ref.sla)))
                        add(t("Servers on (mean)") to (Format.number(you.meanServersOn / 1_000.0, 1) to Format.number(ref.meanServersOn / 1_000.0, 1)))
                        add(t("Mean utilization") to (Format.wholePercent(you.meanUtilization) to Format.wholePercent(ref.meanUtilization)))
                    }
                    if (game.hasBatch) {
                        val n = game.simulation.jobs.size
                        add(t("Jobs on time") to ("${n - you.deadlinesMissed} / $n" to "${n - ref.deadlinesMissed} / $n"))
                        add(t("Batch work, mean g/kWh") to ("${(you.meanBatchIntensityTenths + 5) / 10}" to "${(ref.meanBatchIntensityTenths + 5) / 10}"))
                    }
                }
                Table(listOf("", t("You"), t("Reference")), rows.map { listOf(it.first, it.second.first, it.second.second) })
                Txt(t("What you did is called %@. %@", texts.term, t(texts.termText)), Type.style(15, color = Palette.secondary))
                Txt(t(texts.remark), Type.style(15, color = Palette.secondary))
                WhatIf(game)
            }
            Row(Modifier.fillMaxWidth().padding(horizontal = 30.dp, vertical = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                // The next scenario, once a star has opened it.
                val sameTrack = Scenario.ALL.filter { it.track == scenario.track }
                val next = sameTrack.getOrNull(sameTrack.indexOf(scenario) + 1)?.takeIf { Progress.isUnlocked(it) }
                if (next != null && onNext != null) {
                    PrimaryButton(t("To scenario %@", next.label)) { onNext(next) }
                    SecondaryButton(t("Play again")) { game.restart() }
                } else {
                    PrimaryButton(t("Play again")) { game.restart() }
                }
                SecondaryButton(t("Back to the briefing"), onClick = onExit)
            }
        }
    }
}

/** A simple table: first column left, the others right. */
@Composable
private fun Table(header: List<String>, rows: List<List<String>>, firstWidth: Float = 0.5f) {
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Row {
            header.forEachIndexed { i, h ->
                Txt(h, Type.style(16, FontWeight.Bold).copy(textAlign = if (i == 0) androidx.compose.ui.text.style.TextAlign.Start else androidx.compose.ui.text.style.TextAlign.End),
                    Modifier.weight(if (i == 0) firstWidth else (1f - firstWidth) / (header.size - 1)))
            }
        }
        HDivider()
        for (row in rows) {
            Row {
                row.forEachIndexed { i, cell ->
                    Txt(cell, Type.style(16).copy(textAlign = if (i == 0) androidx.compose.ui.text.style.TextAlign.Start else androidx.compose.ui.text.style.TextAlign.End),
                        Modifier.weight(if (i == 0) firstWidth else (1f - firstWidth) / (row.size - 1)))
                }
            }
        }
    }
}

/** The same run replayed with other rules: the same load and grid mix, so only the rules differ. */
@Composable
private fun WhatIf(game: GameController) {
    val you = game.simulation.totals
    val ref = game.reference.totals
    val alternatives = remember(game.version) { game.whatIf() }
    Card(Modifier.fillMaxWidth(), color = Palette.background, padding = 14.dp) {
        Txt(t("What if …"), Type.subheading)
        Txt(t("The same run with exactly the same load and grid mix – only the rules differ:"), Type.style(15, color = Palette.secondary))
        Spacer(Modifier.height(8.dp))
        Row {
            Spacer(Modifier.weight(0.52f))
            Txt(t("CO₂ vs. you"), Type.caption.copy(textAlign = androidx.compose.ui.text.style.TextAlign.End), Modifier.weight(0.18f))
            Txt(if (game.hasBatch) t("Missed") else t("SLA"), Type.caption.copy(textAlign = androidx.compose.ui.text.style.TextAlign.End), Modifier.weight(0.14f))
            Spacer(Modifier.weight(0.16f))
        }
        for ((label, totals) in alternatives) {
            Row(Modifier.padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                Txt(label, Type.callout, Modifier.weight(0.52f))
                Txt(Format.change(totals.emissions, you.emissions), Type.callout.copy(textAlign = androidx.compose.ui.text.style.TextAlign.End), Modifier.weight(0.18f))
                if (game.hasBatch) {
                    Txt("${totals.deadlinesMissed}", Type.style(15, color = if (totals.deadlinesMissed == 0) Palette.ink else Palette.red).copy(textAlign = androidx.compose.ui.text.style.TextAlign.End), Modifier.weight(0.14f))
                } else {
                    Txt(Format.percent(totals.sla), Type.style(15, color = if (totals.sla >= game.scenario.slaTarget) Palette.ink else Palette.red).copy(textAlign = androidx.compose.ui.text.style.TextAlign.End), Modifier.weight(0.14f))
                }
                Box(Modifier.weight(0.16f), contentAlignment = Alignment.CenterEnd) {
                    StarRow(game.scenario.stars(totals, ref), 11.dp)
                }
            }
        }
    }
}
