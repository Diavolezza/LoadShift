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
import de.wannerit.loadshift.core.Scenario
import de.wannerit.loadshift.core.Tenancy
import de.wannerit.loadshift.core.TenancyComponent
import de.wannerit.loadshift.core.TenancyMiss
import de.wannerit.loadshift.core.TenancyPlan
import de.wannerit.loadshift.core.TenancyResult
import de.wannerit.loadshift.core.TenancyRule
import de.wannerit.loadshift.core.TenantGroup

/** State of E9: the tenancy model per building block and group of customers; a replay keeps the plan. */
class TenancyController(val scenario: Scenario, fresh: Boolean = false) {
    val model = scenario.tenancy!!
    var plan by mutableStateOf((if (fresh) null else lastPlans[scenario.number]) ?: model.reference)
        private set
    var selected by mutableIntStateOf(0)
    var finished by mutableStateOf(false)
        private set

    val component: TenancyComponent get() = model.components[selected]
    val result: TenancyResult get() = model.result(plan)
    val stars: Int get() = model.stars(result, scenario.goals)
    val started: Boolean get() = plan != model.reference || finished

    fun choice(c: TenancyComponent, g: TenantGroup) = model.choice(plan, c, g)

    fun set(c: TenancyComponent, g: TenantGroup, t: Tenancy) {
        if (finished) return
        plan = TenancyPlan(plan.choices + (c.name to ((plan.choices[c.name] ?: emptyMap()) + (g.name to t))))
    }

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

    fun whatIf(): List<Pair<String, TenancyResult>> {
        val ex = model.example
        fun all(t: Tenancy) = model.groups.associate { it.name to t }
        fun except(group: String) = TenancyPlan(ex.choices.mapValues { (_, m) -> m + (group to Tenancy.OWN) })
        return listOf(
            t("Everything shared") to model.result(TenancyPlan(model.components.associate { it.name to all(Tenancy.SHARED) })),
            t("Only the other customers share; their databases on shared servers") to model.result(TenancyPlan(model.components.associate {
                it.name to all(Tenancy.OWN) + (model.groups.first().name to if (Tenancy.BRIDGE in it.options) Tenancy.BRIDGE else Tenancy.SHARED)
            })),
            t("The regulated customers entirely on their own, everything else shared where allowed") to model.result(except(model.regulated)),
            t("The large customer entirely on its own, everything else shared where allowed") to model.result(except(model.large)),
            t("Shared where no quality scenario speaks against it; the others' databases on shared servers") to model.result(ex),
        )
    }

    private companion object {
        val lastPlans = mutableMapOf<Int, TenancyPlan>()
    }
}

fun tenancyTitle(t: Tenancy): String = when (t) {
    Tenancy.OWN -> t("own")
    Tenancy.BRIDGE -> t("own DB")
    Tenancy.SHARED -> t("shared")
}

private fun tenancyText(t: Tenancy, data: Boolean): String = when (t) {
    Tenancy.OWN -> t("Silo: each customer of the group has an instance of its own, with its own servers.")
    Tenancy.BRIDGE -> t("Bridge: the customers share the database servers, but each has a database of its own on them – which costs some memory and work per database.")
    Tenancy.SHARED -> if (data) t("Pool: one database for all customers that share; each row carries the customer's tenant ID.")
        else t("Pool: one instance for all customers that share; each request carries the customer's tenant ID.")
}

fun ruleTitle(r: TenancyRule): String = when (r) {
    TenancyRule.CONTRACT -> t("Contract")
    TenancyRule.MONTH_END -> t("Month end")
    TenancyRule.RESTORE -> t("Restore")
}

fun ruleText(r: TenancyRule): String = when (r) {
    TenancyRule.CONTRACT -> t("The bank and the health insurer: their personnel data – the database and the documents – on servers of their own.")
    TenancyRule.MONTH_END -> t("While the large customer's payroll runs at the end of the month, every other customer works as fast as usual.")
    TenancyRule.RESTORE -> t("A single customer's data can be restored to yesterday's state within 4 hours, without touching the others.")
}

private fun missText(m: TenancyMiss): String = t("%@ (%@)", ruleTitle(m.rule), t(m.component))

@Composable
fun TenancyScreen(game: TenancyController, onExit: () -> Unit, onOverview: (() -> Unit)? = null) {
    BoxWithConstraints(Modifier.fillMaxSize().background(Palette.background)) {
        val panelWidth = (maxWidth * 0.3f).coerceIn(if (maxWidth < 1000.dp) 270.dp else 300.dp, 380.dp)
        val low = maxHeight < 640.dp
        val compact = maxWidth < 1150.dp
        Column(if (low) Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).height(820.dp) else Modifier.fillMaxSize()) {
            val r = game.result
            PlayTopBar(game.scenario, onExit, compact, t("A year"), if (game.finished) t("Plan carried out") else t("The plan")) {
                Metric(t("CO₂ a year"), t("%@ t", tonnes(r.planKg)))
                Metric(t("Compared with today"), Format.changePermille(-r.reductionPermille))
                Metric(t("Servers"), t("%lld of %lld", r.servers.toLong(), r.referenceServers.toLong()))
                Metric(t("Quality scenarios met"), t("%lld of %lld", (TenancyRule.entries.size - TenancyRule.entries.count { r.broken(it) }).toLong(), TenancyRule.entries.size.toLong()),
                    if (r.missed.isNotEmpty()) Palette.red else Palette.ink)
            }
            TenancyGoals(game, compact)
            HDivider()
            Row(Modifier.fillMaxWidth().weight(1f)) {
                Column(Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState()).padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Matrix(game)
                    RuleTable(game)
                }
                VDivider()
                TenancyPanel(game, Modifier.width(panelWidth).fillMaxHeight())
            }
        }
        if (game.finished) TenancyEnd(game, onExit, onOverview)
        RotateHint(maxWidth, maxHeight)
    }
}

@Composable
private fun TenancyGoals(game: TenancyController, compact: Boolean) {
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
        maxLines = 1,
    )
}

/** Building blocks × groups of customers: the tenancy model of each cell, and the servers of each building block. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Matrix(game: TenancyController) {
    val model = game.model
    val r = game.result
    val r0 = model.result(model.reference)
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(Modifier.padding(horizontal = 12.dp), verticalAlignment = Alignment.Bottom) {
            Txt(t("Building block"), Type.caption, Modifier.weight(1.1f))
            for (g in model.groups) Column(Modifier.weight(1f).padding(start = 6.dp)) {
                Txt(t(g.name), Type.style(13, FontWeight.SemiBold))
                Txt(if (g.count == 1) t("1 customer") else t("%lld customers", g.count.toLong()), Type.caption)
            }
            Txt(t("Servers"), Type.caption.copy(textAlign = TextAlign.End), Modifier.width(70.dp))
        }
        for ((i, c) in model.components.withIndex()) {
            val broken = r.missed.any { it.component == c.name }
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                    .background(if (i == game.selected) Palette.accent.copy(alpha = 0.08f) else Palette.panel)
                    .clickable { game.selected = i }.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1.1f)) {
                    Txt(t(c.name), Type.style(15, FontWeight.SemiBold, if (broken) Palette.red else Palette.ink))
                    Txt(t(c.description), Type.caption, maxLines = 2)
                }
                for (g in model.groups) {
                    FlowRow(Modifier.weight(1f).padding(start = 6.dp), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        for (o in c.options) Chip(tenancyTitle(o), game.choice(c, g) == o, !game.finished) { game.selected = i; game.set(c, g, o) }
                    }
                }
                Column(Modifier.width(70.dp), horizontalAlignment = Alignment.End) {
                    Txt("${r.perComponent.getValue(c.name)}", Type.style(15, FontWeight.SemiBold))
                    Txt(t("today %lld", r0.perComponent.getValue(c.name).toLong()), Type.style(11, color = Palette.secondary))
                }
            }
        }
    }
}

/** The three quality scenarios and whether the plan meets them. */
@Composable
private fun RuleTable(game: TenancyController) {
    val r = game.result
    Column(Modifier.fillMaxWidth().background(Palette.panel, RoundedCornerShape(10.dp)).padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Txt(t("Quality scenarios"), Type.style(13, FontWeight.SemiBold))
        for (rule in TenancyRule.entries) {
            val misses = r.missed.filter { it.rule == rule }
            Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                CheckMark(misses.isEmpty(), warning = misses.isNotEmpty())
                Column(Modifier.weight(1f)) {
                    Txt(ruleTitle(rule), Type.style(15, FontWeight.SemiBold, if (misses.isEmpty()) Palette.ink else Palette.red))
                    Txt(ruleText(rule), Type.caption)
                    if (misses.isNotEmpty()) Txt(t("Not met in: %@", misses.joinToString(", ") { t(it.component) }), Type.style(12, FontWeight.SemiBold, Palette.red))
                }
            }
        }
    }
}

@Composable
private fun TenancyPanel(game: TenancyController, modifier: Modifier) {
    val model = game.model
    val c = game.component
    val data = Tenancy.BRIDGE in c.options
    Column(modifier.background(Palette.panel).verticalScroll(rememberScrollState()).padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Txt(t(c.name), Type.heading)
        Txt(t(c.description), Type.style(15, color = Palette.secondary))
        Txt(t("Peak load per customer, in fully busy servers:"), Type.callout)
        for (g in model.groups) Row {
            Txt(t(g.name), Type.callout, Modifier.weight(1f))
            Txt(Format.number(c.peaks.getValue(g.name), 1), Type.style(15, FontWeight.SemiBold))
        }
        Txt(t("On average it uses %@ of its peak. An instance runs at most %@ busy, needs at least %lld servers for availability, and one more for its load balancer, monitoring and backups.",
            Format.wholePercent((c.averageShare * 1_000).toInt()), Format.wholePercent(model.targetPercent * 10), c.minServers.toLong()), Type.caption)
        Txt(t("The peaks of many customers rarely come at the same time: %lld customers together need about %@ of the sum of their peaks.",
            model.groups.first().count.toLong(), Format.wholePercent((model.diversity(model.groups.first().count) * 1_000).toInt())), Type.caption)
        if (c.monthEnd) Txt(t("At the end of the month the payroll run takes every worker and every database connection it gets."), Type.style(13, FontWeight.SemiBold))
        if (c.restore) Txt(t("With one database for all, the backup holds everyone's rows together: bringing back one customer means restoring the whole database into a copy and copying that customer's rows back – about 10 hours. With a database per customer, it is that customer's database alone – about an hour."), Type.style(13, FontWeight.SemiBold))
        HDivider()
        Txt(t("Tenancy models"), Type.subheading)
        for (o in c.options) Column(
            Modifier.fillMaxWidth().background(Palette.background, RoundedCornerShape(12.dp)).padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Txt(tenancyTitle(o).replaceFirstChar { it.uppercase() }, Type.style(15, FontWeight.SemiBold))
            Txt(tenancyText(o, data), Type.caption)
        }
        HDivider()
        Txt(t("Counted is a year: the servers' energy with a PUE of %@, location-based.", Format.number(model.pueHundredths / 100.0, 2)), Type.caption)
        PrimaryButton(t("Carry out the plan"), Modifier.fillMaxWidth(), enabled = !game.finished && game.started) { game.finish() }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TenancyEnd(game: TenancyController, onExit: () -> Unit, onOverview: (() -> Unit)?) {
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
                if (!met.rules) Txt(t("No star because: %@", r.missed.joinToString(", ") { missText(it) }), Type.style(16, FontWeight.SemiBold, Palette.red))
                Table(listOf("", t("today"), t("your plan")), listOf(
                    listOf(t("Servers"), "${r0.servers}", "${r.servers}"),
                    listOf(t("CO₂ a year, t"), tonnes(r0.planKg), tonnes(r.planKg)),
                ), firstWidth = 0.5f)
                Txt(t("What you did is called %@. %@", texts.term, t(texts.termText)), Type.style(15, color = Palette.secondary))
                Txt(t(texts.remark), Type.style(15, color = Palette.secondary))
                WhatIfCard(game.whatIf().map { (l, x) -> Triple(l, x.reductionPermille, model.stars(x, scenario.goals)) },
                    maxOf(Progress.stars(scenario.number), game.stars), t("The same software, other tenancy models:"), scenario.number) { game.loadSolution() }
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
