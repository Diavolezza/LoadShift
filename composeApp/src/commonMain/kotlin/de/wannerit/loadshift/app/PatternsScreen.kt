package de.wannerit.loadshift.app

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import de.wannerit.loadshift.core.PatternOption
import de.wannerit.loadshift.core.PatternPlan
import de.wannerit.loadshift.core.PatternResult
import de.wannerit.loadshift.core.Scenario
import de.wannerit.loadshift.core.Subsystem

/** State of E12: an architecture pattern per subsystem; a replay keeps the plan. */
class PatternsController(val scenario: Scenario, fresh: Boolean = false) {
    val model = scenario.patterns!!
    var plan by mutableStateOf((if (fresh) null else lastPlans[scenario.number]) ?: model.reference)
        private set
    var selected by mutableIntStateOf(0)
    var finished by mutableStateOf(false)
        private set

    val subsystem: Subsystem get() = model.subsystems[selected]
    val result: PatternResult get() = model.result(plan)
    val stars: Int get() = model.stars(result, scenario.goals)
    val started: Boolean get() = plan != model.reference || finished

    fun option(s: Subsystem) = model.option(plan, s)
    fun choose(s: Subsystem, o: PatternOption) { if (!finished) plan = PatternPlan(plan.choices + (s.name to o.id)) }

    fun finish() {
        finished = true
        Progress.record(stars, scenario.number)
        lastPlans[scenario.number] = plan
    }

    fun restart() { finished = false }

    fun startOver() {
        plan = model.reference
        finished = false
    }

    /** The model solution in the screen, to look at and carry out. */
    fun loadSolution() {
        plan = model.example
        finished = false
    }

    fun whatIf(): List<Pair<String, PatternResult>> {
        val ex = model.example
        fun with(sub: String, id: String) = PatternPlan(ex.choices + (sub to id))
        return listOf(
            t("The simplest pattern everywhere") to model.result(PatternPlan(model.subsystems.associate { it.name to it.options.minBy { o -> model.kg(o) }.id })),
            t("Each pattern where it fits, but the apps get every event") to model.result(with("Delay notifications", "all")),
            t("Each pattern where it fits, but Event Sourcing for the bookings too") to model.result(with("Booking", "es-cqrs")),
            t("Each pattern where it fits, but the Lambda architecture kept") to model.result(with("Analytics", "lambda")),
            t("Each pattern where it fits") to model.result(ex),
        )
    }

    private companion object {
        val lastPlans = mutableMapOf<Int, PatternPlan>()
    }
}

@Composable
fun PatternsScreen(game: PatternsController, onExit: () -> Unit, onOverview: (() -> Unit)? = null) {
    BoxWithConstraints(Modifier.fillMaxSize().background(Palette.background)) {
        val panelWidth = (maxWidth * 0.3f).coerceIn(if (maxWidth < 1000.dp) 270.dp else 300.dp, 380.dp)
        val low = maxHeight < 640.dp
        val compact = maxWidth < 1150.dp
        Column(if (low) Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).height(820.dp) else Modifier.fillMaxSize()) {
            val r = game.result
            PlayTopBar(game.scenario, onExit, compact, t("A year"), if (game.finished) t("Plan carried out") else t("The plan")) {
                Metric(t("CO₂ a year"), t("%@ t", tonnes(r.planKg)))
                Metric(t("Compared with today"), Format.changePermille(-r.reductionPermille))
                Metric(t("Servers"), Format.number(r.servers, if (r.servers % 1.0 == 0.0) 0 else 1))
                Metric(t("Quality scenarios met"), t("%lld of %lld", (game.model.qualities.size - r.missed.size).toLong(), game.model.qualities.size.toLong()),
                    if (r.missed.isNotEmpty()) Palette.red else Palette.ink)
            }
            PatternGoals(game, compact)
            HDivider()
            Row(Modifier.fillMaxWidth().weight(1f)) {
                Column(Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState()).padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    SubsystemTable(game)
                    QualityList(game)
                }
                VDivider()
                PatternPanel(game, Modifier.width(panelWidth).fillMaxHeight())
            }
        }
        if (game.finished) PatternEnd(game, onExit, onOverview)
        RotateHint(maxWidth, maxHeight)
    }
}

@Composable
private fun PatternGoals(game: PatternsController, compact: Boolean) {
    val scenario = game.scenario
    val r = game.result
    Row(
        Modifier.fillMaxWidth().background(Palette.panel).then(if (compact) Modifier.horizontalScroll(rememberScrollState()) else Modifier)
            .padding(horizontal = 20.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Txt(t("Goals"), Type.style(13, FontWeight.SemiBold, Palette.secondary))
        GoalItem(r.missed.isEmpty(), r.missed.isNotEmpty() || game.finished, t("Every quality scenario met"))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            StarLabel(2, 13.dp)
            Txt(t("CO₂ %@", Format.changePermille(-scenario.goals.reduction.toLong())), Type.callout)
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            StarLabel(3, 13.dp)
            Txt(specialLong(scenario.goals.special), Type.callout)
        }
        Spacer(Modifier.weight(1f))
        StartOverChip { game.startOver() }
    }
}

@Composable
private fun Chip(label: String, on: Boolean, enabled: Boolean, onClick: () -> Unit) {
    Txt(
        label, Type.style(13, FontWeight.SemiBold, if (on) Color.White else Palette.ink),
        Modifier.clip(RoundedCornerShape(7.dp)).background(if (on) Palette.accent else Palette.background)
            .clickable(enabled = enabled, onClick = onClick).padding(horizontal = 9.dp, vertical = 4.dp),
    )
}

/** The six subsystems: the pattern of each and its CO₂ a year. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SubsystemTable(game: PatternsController) {
    val model = game.model
    val r = game.result
    val r0 = model.result(model.reference)
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(Modifier.padding(horizontal = 12.dp)) {
            Txt(t("Subsystem"), Type.caption, Modifier.weight(1f))
            Txt(t("Architecture pattern"), Type.caption, Modifier.weight(1.4f))
            Txt(t("CO₂ a year"), Type.caption.copy(textAlign = TextAlign.End), Modifier.width(92.dp))
        }
        for ((i, s) in model.subsystems.withIndex()) {
            val failing = game.option(s).fails.isNotEmpty()
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                    .background(if (i == game.selected) Palette.accent.copy(alpha = 0.08f) else Palette.panel)
                    .clickable { game.selected = i }.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Txt(t(s.name), Type.style(15, FontWeight.SemiBold, if (failing) Palette.red else Palette.ink))
                    Txt(t(s.description), Type.caption, maxLines = 3)
                }
                FlowRow(Modifier.weight(1.4f).padding(start = 8.dp), horizontalArrangement = Arrangement.spacedBy(5.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    for (o in s.options) Chip(t(o.title), game.option(s) == o, !game.finished) { game.selected = i; game.choose(s, o) }
                }
                Column(Modifier.width(92.dp), horizontalAlignment = Alignment.End) {
                    Txt(t("%@ t", tonnes(r.perSubsystemKg.getValue(s.name))), Type.style(15, FontWeight.SemiBold))
                    Txt(t("today %@ t", tonnes(r0.perSubsystemKg.getValue(s.name))), Type.style(11, color = Palette.secondary))
                }
            }
        }
    }
}

/** The quality scenarios and whether the plan meets them. */
@Composable
private fun QualityList(game: PatternsController) {
    val r = game.result
    Column(Modifier.fillMaxWidth().background(Palette.panel, RoundedCornerShape(10.dp)).padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Txt(t("Quality scenarios"), Type.style(13, FontWeight.SemiBold))
        for (q in game.model.qualities) {
            val missed = q.id in r.missed
            Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                CheckMark(!missed, warning = missed)
                Column(Modifier.weight(1f)) {
                    Txt(t(q.name), Type.style(15, FontWeight.SemiBold, if (missed) Palette.red else Palette.ink))
                    Txt(t(q.text), Type.caption)
                }
            }
        }
    }
}

@Composable
private fun PatternPanel(game: PatternsController, modifier: Modifier) {
    val model = game.model
    val s = game.subsystem
    Column(modifier.background(Palette.panel).verticalScroll(rememberScrollState()).padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Txt(t(s.name), Type.heading)
        Txt(t(s.description), Type.style(15, color = Palette.secondary))
        HDivider()
        Txt(t("Architecture patterns"), Type.subheading)
        for (o in s.options) {
            RuleOption(game.option(s) == o, { game.choose(s, o) }, t(o.title),
                t(o.text) + " " + t("About %@ t CO₂ a year.", tonnes(model.kg(o).toLong())))
        }
        HDivider()
        Txt(t("A server for a year: about %@ kWh, with a PUE of %@ and %lld g CO₂ per kWh about %@ t CO₂. A TB of storage: about %lld kg a year.",
            Format.number(model.serverKwh.toDouble()), Format.number(model.pueHundredths / 100.0, 2), model.gramsPerKwh.toLong(),
            Format.number(model.serverKg / 1_000, 1), model.kgPerTb.toLong()), Type.caption)
        Txt(t("Counted is a year: servers, storage and – for the notifications – phones and network, location-based."), Type.caption)
        PrimaryButton(t("Carry out the plan"), Modifier.fillMaxWidth(), enabled = !game.finished && game.started) { game.finish() }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PatternEnd(game: PatternsController, onExit: () -> Unit, onOverview: (() -> Unit)?) {
    val scenario = game.scenario
    val model = game.model
    val r = game.result
    val r0 = model.result(model.reference)
    val met = model.goalsMet(r, scenario.goals)
    val texts = ScenarioTexts.of(scenario)
    Box(
        Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.35f))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {},
        contentAlignment = Alignment.Center,
    ) {
        Column(Modifier.padding(vertical = 40.dp, horizontal = 24.dp).widthIn(max = 640.dp).background(Palette.panel, RoundedCornerShape(20.dp))) {
            Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()).padding(30.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Txt(if (!met.rules) t("Not made") else t("Scenario complete"), Type.title, Modifier.weight(1f))
                    StarRow(game.stars)
                }
                Txt("${Format.changePermille(-r.reductionPermille)} CO₂", Type.style(40, FontWeight.Bold, if (r.reductionPermille > 0) Palette.accent else Palette.red))
                Txt(t(texts.comparedWith), Type.style(17, color = Palette.secondary))
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    GoalLine(met.rules, 1, t(texts.rules))
                    GoalLine(met.reduction, 2, t("CO₂ %@ or better", Format.changePermille(-scenario.goals.reduction.toLong())))
                    GoalLine(met.special, 3, specialLong(scenario.goals.special))
                }
                if (!met.rules) Txt(t("No star because: %@", r.missed.joinToString(", ") { id -> t(model.qualities.first { it.id == id }.name) }),
                    Type.style(16, FontWeight.SemiBold, Palette.red))
                Table(listOf("", t("today"), t("your plan")), listOf(
                    listOf(t("Servers"), Format.number(r0.servers, 0), Format.number(r.servers, if (r.servers % 1.0 == 0.0) 0 else 1)),
                    listOf(t("Storage, TB"), Format.number(r0.storageTb, 0), Format.number(r.storageTb, 0)),
                    listOf(t("CO₂ a year, t"), tonnes(r0.planKg), tonnes(r.planKg)),
                ), firstWidth = 0.5f)
                Txt(t("What you did is called %@. %@", texts.term, t(texts.termText)), Type.style(15, color = Palette.secondary))
                Txt(t(texts.remark), Type.style(15, color = Palette.secondary))
                WhatIfCard(game.whatIf().map { (l, x) -> Triple(l, x.reductionPermille, model.stars(x, scenario.goals)) },
                    maxOf(Progress.stars(scenario.number), game.stars), t("The same platform, other patterns:"), scenario.number) { game.loadSolution() }
            }
            FlowRow(
                Modifier.fillMaxWidth().padding(horizontal = 30.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                PrimaryButton(t("Play again")) { game.restart() }
                SecondaryButton(t("To the briefing"), onClick = onExit)
                if (onOverview != null) SecondaryButton(t("To the overview"), onClick = onOverview)
            }
        }
    }
}
