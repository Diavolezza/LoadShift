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
import de.wannerit.loadshift.core.DataKind
import de.wannerit.loadshift.core.DataPlan
import de.wannerit.loadshift.core.DataResult
import de.wannerit.loadshift.core.Scenario
import de.wannerit.loadshift.core.StorageTier

/** State of E6: per kind of data where older data lives and how long it is kept; a replay keeps the plan. */
class LifecycleController(val scenario: Scenario, fresh: Boolean = false) {
    val model = scenario.lifecycle!!
    var plan by mutableStateOf((if (fresh) null else lastPlans[scenario.number]) ?: model.reference)
        private set
    var selected by mutableIntStateOf(0)
    var finished by mutableStateOf(false)
        private set

    val kind: DataKind get() = model.kinds[selected]
    val result: DataResult get() = model.result(plan)
    val stars: Int get() = model.stars(result, scenario.goals)
    val started: Boolean get() = plan != model.reference || finished
    val requirements: Int get() = model.kinds.sumOf { it.requirements.size + if (it.maxYears != null) 1 else 0 }

    fun setTier(k: DataKind, id: String) { if (!finished) plan = plan.copy(tiers = plan.tiers + (k.name to id)) }
    fun setKeep(k: DataKind, id: String) { if (!finished) plan = plan.copy(keep = plan.keep + (k.name to id)) }

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

    fun whatIf(): List<Pair<String, DataResult>> {
        val ref = model.reference
        val privacy = ref.copy(keep = ref.keep + ("Inactive customer accounts" to "3"))
        val minimum = DataPlan(model.kinds.associate { it.name to "warm" }, model.example.keep)
        return listOf(
            t("As today, the accounts deleted after 3 years") to model.result(privacy),
            t("The same, everything warm") to model.result(privacy.copy(tiers = model.kinds.associate { it.name to "warm" })),
            t("Kept only as long as required, everything warm") to model.result(minimum),
            t("Kept only as long as required, each kind as cold as its requirements allow") to model.result(model.example),
        )
    }

    private companion object {
        val lastPlans = mutableMapOf<Int, DataPlan>()
    }
}

fun tierName(t: StorageTier): String = when (t) {
    StorageTier.HOT -> t("hot")
    StorageTier.WARM -> t("warm")
    StorageTier.COLD -> t("cold")
}

private fun tierText(t: StorageTier, kg: Double, hours: Int): String = when (t) {
    StorageTier.HOT -> t("SSD storage, three copies: read at once. About %@ kg CO₂ per TB and year – the power and the making of the SSDs.", Format.number(kg))
    StorageTier.WARM -> t("HDD storage with erasure coding (the data plus half again): read within a second. About %@ kg CO₂ per TB and year.", Format.number(kg))
    StorageTier.COLD -> t("Archive on tape, two copies: reading takes about %lld hours. About %@ kg CO₂ per TB and year.", hours.toLong(), Format.number(kg))
}

private fun tb(x: Double): String = if (x < 0.05) "0" else Format.number(x, if (x < 10) 1 else 0)

@Composable
fun LifecycleScreen(game: LifecycleController, onExit: () -> Unit, onOverview: (() -> Unit)? = null) {
    BoxWithConstraints(Modifier.fillMaxSize().background(Palette.background)) {
        val panelWidth = (maxWidth * 0.3f).coerceIn(if (maxWidth < 1000.dp) 270.dp else 300.dp, 380.dp)
        val low = maxHeight < 640.dp
        val compact = maxWidth < 1150.dp
        Column(if (low) Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).height(820.dp) else Modifier.fillMaxSize()) {
            val r = game.result
            PlayTopBar(game.scenario, onExit, compact, t("A year"), if (game.finished) t("Plan carried out") else t("The plan")) {
                Metric(t("CO₂ a year"), t("%@ t", tonnes(r.planKg)))
                Metric(t("Compared with today"), Format.changePermille(-r.reductionPermille))
                Metric(t("Stored"), t("%@ TB", tb(r.tb.values.sum())))
                Metric(t("Requirements met"), t("%lld of %lld", (game.requirements - r.missed.size).toLong(), game.requirements.toLong()),
                    if (r.missed.isNotEmpty()) Palette.red else Palette.ink)
            }
            LifecycleGoals(game, compact)
            HDivider()
            Row(Modifier.fillMaxWidth().weight(1f)) {
                Column(Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState()).padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    KindTable(game)
                    TierSplit(game)
                }
                VDivider()
                LifecyclePanel(game, Modifier.width(panelWidth).fillMaxHeight())
            }
        }
        if (game.finished) LifecycleEnd(game, onExit, onOverview)
        RotateHint(maxWidth, maxHeight)
    }
}

@Composable
private fun LifecycleGoals(game: LifecycleController, compact: Boolean) {
    val scenario = game.scenario
    val r = game.result
    Row(
        Modifier.fillMaxWidth().background(Palette.panel).then(if (compact) Modifier.horizontalScroll(rememberScrollState()) else Modifier)
            .padding(horizontal = 20.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Txt(t("Goals"), Type.style(13, FontWeight.SemiBold, Palette.secondary))
        GoalItem(r.missed.isEmpty(), r.missed.isNotEmpty() || game.finished, t("Every requirement met"))
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

/** The six kinds of data: where data older than 90 days lives, how long it is kept, and what is stored then. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun KindTable(game: LifecycleController) {
    val model = game.model
    val r = game.result
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(Modifier.padding(horizontal = 12.dp)) {
            Txt(t("Data"), Type.caption, Modifier.weight(1f))
            Txt(t("Older than 90 days"), Type.caption, Modifier.weight(1.1f))
            Txt(t("Kept"), Type.caption, Modifier.weight(1.1f))
            Txt(t("Stored"), Type.caption.copy(textAlign = TextAlign.End), Modifier.width(92.dp))
        }
        for ((i, k) in model.kinds.withIndex()) {
            val missed = r.missed.any { it.kind == k.name }
            val stored = r.perKind.getValue(k.name)
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                    .background(if (i == game.selected) Palette.accent.copy(alpha = 0.08f) else Palette.panel)
                    .clickable { game.selected = i }.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Txt(t(k.name), Type.style(15, FontWeight.SemiBold, if (missed) Palette.red else Palette.ink))
                    Txt(t("%@ TB a year", tb(k.yearlyTb)), Type.caption)
                }
                FlowRow(Modifier.weight(1.1f), horizontalArrangement = Arrangement.spacedBy(5.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    for (o in model.tierOptions) Chip(t(o.title), model.tier(game.plan, k) == o, !game.finished) { game.selected = i; game.setTier(k, o.id) }
                }
                FlowRow(Modifier.weight(1.1f).padding(start = 8.dp), horizontalArrangement = Arrangement.spacedBy(5.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    for (o in k.keepOptions) Chip(t(o.title), model.keep(game.plan, k) == o, !game.finished) { game.selected = i; game.setKeep(k, o.id) }
                }
                Column(Modifier.width(92.dp), horizontalAlignment = Alignment.End) {
                    for (t0 in StorageTier.entries) if (stored.getValue(t0) > 0.05)
                        Txt(t("%@ %@ TB", tierName(t0), tb(stored.getValue(t0))), Type.style(12, FontWeight.SemiBold))
                    if (stored.values.sum() <= 0.05) Txt(t("nothing"), Type.style(12, FontWeight.SemiBold))
                }
            }
        }
    }
}

/** CO₂ by storage tier, today and with the plan. */
@Composable
private fun TierSplit(game: LifecycleController) {
    val model = game.model
    val r = game.result
    val r0 = model.result(model.reference)
    val colors = mapOf(StorageTier.HOT to Palette.red, StorageTier.WARM to Palette.amber, StorageTier.COLD to Palette.demand)
    fun kg(x: DataResult, t0: StorageTier) = x.tb.getValue(t0) * model.kgPerTbYear.getValue(t0)
    Column(Modifier.fillMaxWidth().background(Palette.panel, RoundedCornerShape(10.dp)).padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Txt(t("CO₂ a year, t – by storage tier; the live data (%@ TB) is always hot", tb(model.recentTb)), Type.style(13, FontWeight.SemiBold))
        val maxKg = r0.planKg.toFloat()
        for ((label, x) in listOf(t("today") to r0, t("plan") to r)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Txt(label, Type.small, Modifier.width(40.dp))
                Row(Modifier.weight(1f).height(16.dp)) {
                    var used = 0f
                    for (t0 in StorageTier.entries) {
                        val share = (kg(x, t0) / maxKg).toFloat()
                        if (share > 0.002f) { Box(Modifier.weight(share).fillMaxHeight().background(colors.getValue(t0))); used += share }
                    }
                    if (1f - used > 0.002f) Spacer(Modifier.weight(1f - used))
                }
                Txt(tonnes(x.planKg), Type.style(12, FontWeight.SemiBold), Modifier.widthIn(min = 40.dp))
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            for (t0 in StorageTier.entries) LegendSwatch(colors.getValue(t0), tierName(t0))
        }
    }
}

@Composable
private fun LifecyclePanel(game: LifecycleController, modifier: Modifier) {
    val model = game.model
    val k = game.kind
    val missed = game.result.missed.filter { it.kind == k.name }.map { it.text }
    Column(modifier.background(Palette.panel).verticalScroll(rememberScrollState()).padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Txt(t(k.name), Type.heading)
        Txt(t(k.description), Type.style(15, color = Palette.secondary))
        Txt(t("%@ TB a year become older than 90 days; the shop has %lld years of them.", tb(k.yearlyTb), model.historyYears.toLong()), Type.callout)
        Txt(t("Requirements"), Type.subheading)
        val texts = k.requirements.map { it.text } + listOfNotNull(k.maxText.takeIf { k.maxYears != null })
        for (text in texts) Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CheckMark(text !in missed, warning = text in missed)
            Txt(t(text), Type.style(14, color = if (text in missed) Palette.red else Palette.ink))
        }
        HDivider()
        Txt(t("Storage tiers"), Type.subheading)
        for (t0 in StorageTier.entries) Column(
            Modifier.fillMaxWidth().background(Palette.background, RoundedCornerShape(12.dp)).padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Txt(tierName(t0).replaceFirstChar { it.uppercase() }, Type.style(15, FontWeight.SemiBold))
            Txt(tierText(t0, model.kgPerTbYear.getValue(t0), model.coldHours), Type.caption)
        }
        Txt(t("Data younger than 90 days stays hot, as do the live databases."), Type.caption)
        HDivider()
        Txt(t("Counted is a year in the steady state, location-based, with the making of the storage media."), Type.caption)
        PrimaryButton(t("Carry out the plan"), Modifier.fillMaxWidth(), enabled = !game.finished && game.started) { game.finish() }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LifecycleEnd(game: LifecycleController, onExit: () -> Unit, onOverview: (() -> Unit)?) {
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
                if (!met.rules) Txt(t("No star because: %@", r.missed.joinToString("; ") { t(it.text) }), Type.style(16, FontWeight.SemiBold, Palette.red))
                Table(listOf("", t("today"), t("your plan")), listOf(
                    listOf(t("Hot, TB"), tb(r0.tb.getValue(StorageTier.HOT)), tb(r.tb.getValue(StorageTier.HOT))),
                    listOf(t("Warm, TB"), tb(r0.tb.getValue(StorageTier.WARM)), tb(r.tb.getValue(StorageTier.WARM))),
                    listOf(t("Cold, TB"), tb(r0.tb.getValue(StorageTier.COLD)), tb(r.tb.getValue(StorageTier.COLD))),
                    listOf(t("CO₂ a year, t"), tonnes(r0.planKg), tonnes(r.planKg)),
                ), firstWidth = 0.5f)
                Txt(t("What you did is called %@. %@", texts.term, t(texts.termText)), Type.style(15, color = Palette.secondary))
                Txt(t(texts.remark), Type.style(15, color = Palette.secondary))
                WhatIfCard(game.whatIf().map { (l, x) -> Triple(l, x.reductionPermille, model.stars(x, scenario.goals)) },
                    maxOf(Progress.stars(scenario.number), game.stars), t("The same data, other plans:"), scenario.number) { game.loadSolution() }
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
