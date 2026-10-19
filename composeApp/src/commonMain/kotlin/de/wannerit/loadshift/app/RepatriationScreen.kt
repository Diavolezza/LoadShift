package de.wannerit.loadshift.app

import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import de.wannerit.loadshift.core.Move
import de.wannerit.loadshift.core.MoveApp
import de.wannerit.loadshift.core.MoveResult
import de.wannerit.loadshift.core.Place
import de.wannerit.loadshift.core.Scenario

/** State of P4: a place per application, the selected one, whether the plan is carried out; a replay keeps the plan. */
class RepatriationController(val scenario: Scenario, fresh: Boolean = false) {
    val model = scenario.repatriation!!
    var plan by mutableStateOf((if (fresh) null else lastPlans[scenario.number]) ?: emptyMap())
        private set
    var selected by mutableIntStateOf(0)
    var finished by mutableStateOf(false)
        private set

    val app: MoveApp get() = model.apps[selected]
    fun move(a: MoveApp): Move = plan[a.name] ?: Move()
    val result: MoveResult get() = model.result(plan)
    val stars: Int get() = model.stars(result, scenario.goals)
    /** A change has been made (the cost goal is judged from then on). */
    val started: Boolean get() = plan.isNotEmpty() || finished

    private fun set(a: MoveApp, m: Move) {
        if (finished) return
        plan = if (m == Move()) plan - a.name else plan + (a.name to m)
    }

    fun place(a: MoveApp, p: Place) {
        if (p == Place.SERVER_ROOM && !model.fitsRoom(plan, a)) return
        set(a, move(a).copy(place = p))
    }

    fun toggleOff(a: MoveApp) = set(a, move(a).let { it.copy(offOutsideUse = !it.offOutsideUse) })

    fun finish() {
        finished = true
        Progress.record(stars, scenario.number)
        lastPlans[scenario.number] = plan
    }

    fun restart() { finished = false }

    fun startOver() {
        plan = emptyMap()
        finished = false
    }

    /** The model solution in the screen, to look at and carry out. */
    fun loadSolution() {
        plan = model.example
        finished = false
    }

    fun whatIf(): List<Pair<String, MoveResult>> = listOf(
        t("Everything stays in the cloud in Frankfurt") to model.result(emptyMap()),
        t("Everything to the colocation") to model.result(model.all(Place.COLOCATION)),
        t("The steady ones to the colocation, the rest stays") to model.result(mapOf(
            "ERP database" to Move(Place.COLOCATION), "Customer data archive" to Move(Place.COLOCATION), "Data warehouse" to Move(Place.COLOCATION))),
        t("Steady load to the colocation, the rest to Stockholm, tests off at night") to model.result(model.example),
    )

    private companion object {
        val lastPlans = mutableMapOf<Int, Map<String, Move>>()
    }
}

/** The short name of a place, for the chips in the table. */
private fun shortPlace(p: Place) = when (p) {
    Place.CLOUD_FRANKFURT -> t("Frankfurt")
    Place.CLOUD_STOCKHOLM -> t("Stockholm")
    Place.COLOCATION -> t("Colocation")
    Place.SERVER_ROOM -> t("Server room")
}

private fun millionEuros(euro: Long) = t("%@ m€", Format.number(euro / 1_000_000.0, 2))

@Composable
fun RepatriationScreen(game: RepatriationController, onExit: () -> Unit, onOverview: (() -> Unit)? = null) {
    BoxWithConstraints(Modifier.fillMaxSize().background(Palette.background)) {
        val panelWidth = (maxWidth * 0.3f).coerceIn(if (maxWidth < 1000.dp) 270.dp else 300.dp, 380.dp)
        // A low screen (a phone held sideways): the page scrolls and keeps a height everything fits into.
        val low = maxHeight < 640.dp
        val compact = maxWidth < 1150.dp
        Column(if (low) Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).height(760.dp) else Modifier.fillMaxSize()) {
            RepatriationTopBar(game, onExit, compact)
            RepatriationGoals(game, compact)
            HDivider()
            Row(Modifier.fillMaxWidth().weight(1f)) {
                Column(Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState()).padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    AppTable(game, Modifier.fillMaxWidth())
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Comparison(game, Modifier.weight(1.1f).background(Palette.panel, RoundedCornerShape(10.dp)).padding(14.dp))
                        Box(Modifier.weight(1f).background(Palette.panel, RoundedCornerShape(10.dp)).padding(14.dp)) { Waterfall(game.result) }
                    }
                }
                VDivider()
                RepatriationPanel(game, Modifier.width(panelWidth).fillMaxHeight())
            }
        }
        if (game.finished) RepatriationEnd(game, onExit, onOverview)
        RotateHint(maxWidth, maxHeight)
    }
}

@Composable
private fun RepatriationTopBar(game: RepatriationController, onExit: () -> Unit, compact: Boolean = false) {
    val r = game.result
    val metrics: @Composable () -> Unit = {
        Metric(t("Costs in three years"), t("%@ (%@)", millionEuros(r.planEuro), Format.changePermille(-r.savingPermille)),
            if (game.started && r.savingPermille < game.model.savingGoalPermille) Palette.red else Palette.ink)
        Metric(t("Location-based with making"), t("%@ t CO₂", tonnes(r.planKg)))
        Metric(t("In the report (market-based)"), t("%@ t CO₂", tonnes(r.reportKg)))
    }
    PlayTopBar(game.scenario, onExit, compact, t("Three years"), if (game.finished) t("Plan carried out") else t("The plan"), metrics)
}

@Composable
private fun RepatriationGoals(game: RepatriationController, compact: Boolean = false) {
    val scenario = game.scenario
    val model = game.model
    val r = game.result
    Row(
        // On a small screen the goals scroll sideways instead of wrapping letter by letter.
        Modifier.fillMaxWidth().background(Palette.panel).then(if (compact) Modifier.horizontalScroll(rememberScrollState()) else Modifier)
            .padding(horizontal = 20.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Txt(t("Goals"), Type.style(13, FontWeight.SemiBold, Palette.secondary))
        GoalItem(r.savingPermille >= model.savingGoalPermille, game.started, t("Costs at least %@ lower", Format.wholePercent(model.savingGoalPermille)))
        GoalItem(r.outsideGermany.isEmpty(), r.outsideGermany.isNotEmpty() || game.finished, t("Customer data stay in Germany"))
        GoalItem(!r.bookingTooSlow, r.bookingTooSlow || game.finished, t("A booking in the plant under %@ s", Format.number(model.bookingLimitMs / 1_000.0)))
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

/** The six applications: their load over a week and a chip per place. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AppTable(game: RepatriationController, modifier: Modifier) {
    val model = game.model
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(Modifier.padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Txt(t("Application"), Type.caption, Modifier.weight(1f))
            Txt(t("Load over a week"), Type.caption, Modifier.width(130.dp))
            Txt(t("Where to?"), Type.caption, Modifier.weight(1.6f))
        }
        for ((i, a) in model.apps.withIndex()) {
            val m = game.move(a)
            val selected = i == game.selected
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                    .background(if (selected) Palette.accent.copy(alpha = 0.08f) else Palette.panel)
                    .clickable { game.selected = i }.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Txt(t(a.name), Type.style(15, FontWeight.SemiBold))
                    Txt(t(a.description), Type.caption, maxLines = 2)
                    if (m.offOutsideUse) Txt(t("Off outside its hours of use"), Type.style(12, FontWeight.SemiBold, Palette.accent))
                    Txt(t("%@ € in three years", Format.number(model.euro(a, m))), Type.style(12, color = Palette.secondary))
                }
                Sparkline(a.week, Modifier.width(130.dp).height(30.dp).padding(end = 12.dp))
                FlowRow(Modifier.weight(1.6f), horizontalArrangement = Arrangement.spacedBy(5.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    for (p in Place.entries) {
                        val on = m.place == p
                        val fits = p != Place.SERVER_ROOM || model.fitsRoom(game.plan, a)
                        Txt(
                            shortPlace(p), Type.style(13, FontWeight.SemiBold, if (on) Color.White else if (fits) Palette.ink else Palette.secondary.copy(alpha = 0.5f)),
                            Modifier.clip(RoundedCornerShape(7.dp)).background(if (on) Palette.accent else Palette.background)
                                .clickable(enabled = fits && !game.finished) { game.selected = i; game.place(a, p) }
                                .padding(horizontal = 9.dp, vertical = 4.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun Sparkline(week: List<Int>, modifier: Modifier) {
    Canvas(modifier) {
        val n = week.size
        val p = Path()
        for ((i, v) in week.withIndex()) {
            val x = size.width * i / (n - 1)
            val y = size.height - 2f - (size.height - 4f) * v / 100f
            if (i == 0) p.moveTo(x, y) else p.lineTo(x, y)
        }
        drawLine(Palette.divider, Offset(0f, size.height - 1f), Offset(size.width, size.height - 1f), 1f)
        drawPath(p, Palette.interactiveLoad, style = Stroke(1.6f * density))
    }
}

/** Today and the plan: in the report, location-based with the making of the servers, and in euros. */
@Composable
private fun Comparison(game: RepatriationController, modifier: Modifier) {
    val r = game.result
    Column(modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        val maxKg = maxOf(r.baselineKg, r.planKg, r.reportKg, r.baselineReportKg).toFloat()
        Bars(t("In the report (market-based), t CO₂"), r.baselineReportKg / maxKg, r.reportKg / maxKg,
            tonnes(r.baselineReportKg), tonnes(r.reportKg), if (r.reportKg > r.baselineReportKg) Palette.red else Palette.accent, Modifier.fillMaxWidth())
        Bars(t("Location-based with making – counts for the stars"), r.baselineKg / maxKg, r.planKg / maxKg,
            tonnes(r.baselineKg), tonnes(r.planKg), if (r.planKg > r.baselineKg) Palette.red else Palette.accent, Modifier.fillMaxWidth())
        val maxEuro = maxOf(r.baselineEuro, r.planEuro).toFloat()
        Bars(t("Costs in three years"), r.baselineEuro / maxEuro, r.planEuro / maxEuro,
            millionEuros(r.baselineEuro), millionEuros(r.planEuro), Palette.demand, Modifier.fillMaxWidth())
    }
}

@Composable
private fun Bars(title: String, today: Float, plan: Float, todayText: String, planText: String, color: Color, modifier: Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Txt(title, Type.style(12, FontWeight.SemiBold, Palette.secondary), maxLines = 2)
        for ((label, share, text, c) in listOf(
            Quad(t("today"), today, todayText, Palette.secondary), Quad(t("plan"), plan, planText, color),
        )) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Txt(label, Type.small, Modifier.width(40.dp))
                Box(Modifier.weight(1f).height(12.dp)) {
                    if (share > 0f) Box(Modifier.fillMaxWidth(share.coerceIn(0.01f, 1f)).fillMaxHeight().background(c, RoundedCornerShape(3.dp)))
                }
                Txt(text, Type.style(12, FontWeight.SemiBold), Modifier.widthIn(min = 64.dp))
            }
        }
    }
}

private data class Quad(val a: String, val b: Float, val c: String, val d: Color)

@Composable
private fun RepatriationPanel(game: RepatriationController, modifier: Modifier) {
    val model = game.model
    val a = game.app
    val m = game.move(a)
    Column(modifier.background(Palette.panel).verticalScroll(rememberScrollState()).padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Txt(t(a.name), Type.heading)
        Txt(t(a.description), Type.style(15, color = Palette.secondary))
        Txt(t("In the cloud today: %lld server equivalents, on average %@ busy, %@ € a year.",
            a.cloudServers, Format.wholePercent(a.cloudUtilPercent * 10), Format.number(a.cloudServers.toDouble() * model.cloudEuroPerServer)), Type.callout)
        Txt(t("On own hardware it needs %lld servers for its peak; on average they are %@ busy.",
            a.ownServers, Format.wholePercent(a.ownUtilPercent * 10)), Type.callout)
        if (a.queriesPerBooking > 0) Txt(t("A booking in the plant sends %lld queries to it, one after the other.", a.queriesPerBooking), Type.style(15, FontWeight.SemiBold))
        if (a.germanyOnly) Txt(t("A contract keeps its data in Germany."), Type.style(15, FontWeight.SemiBold))
        for (s in model.places) {
            val fits = s.place != Place.SERVER_ROOM || model.fitsRoom(game.plan, a)
            val facts = buildList {
                add(t("PUE %@", Format.number(s.pueHundredths / 100.0, 2)))
                add(t("grid %lld g/kWh", s.gramsPerKwh))
                add(t("round trip from the plant %@ ms", Format.number(s.roundTripTenthMs / 10.0, if (s.roundTripTenthMs % 10 == 0) 0 else 1)))
                when (s.place) {
                    Place.COLOCATION -> add(t("new servers at %@ € each, %@ t CO₂ each to make", Format.number(model.serverEuro.toDouble()), Format.number(model.embodiedKg / 1_000.0, 1)))
                    Place.SERVER_ROOM -> add(if (fits) t("%lld free servers, already made", model.roomCapacity - model.roomUsed(game.plan - a.name)) else t("not enough free servers"))
                    else -> add(t("the making of the servers is in the price"))
                }
            }
            val euro = model.euro(a, Move(s.place, m.offOutsideUse)).toLong()
            RuleOption(m.place == s.place, { game.place(a, s.place) }, t(s.title),
                facts.joinToString(" · ") + "\n" + t("Costs in three years: %@ €", Format.number(euro.toDouble())) +
                    (if (s.cloud) "" else " " + t("– plus the team for own hardware, %@ € a year, once for all applications", Format.number(model.teamEuroPerYear.toDouble()))))
        }
        a.usePercent?.let { use ->
            Row(Modifier.clip(RoundedCornerShape(8.dp)).clickable { game.toggleOff(a) }.padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Tick(m.offOutsideUse)
                Txt(t("Off outside its hours of use (on %@ of the time)", Format.wholePercent(use * 10)), Type.style(14))
            }
            if (a.onDemandInCloud) Txt(t("In the cloud it already runs only when needed."), Type.caption)
        }
        HDivider()
        Txt(t("Counted are three years. In the cloud the making of the servers is part of each server equivalent; in the colocation new servers are bought; the server room's are already made. A team for own hardware costs %@ € a year.",
            Format.number(model.teamEuroPerYear.toDouble())), Type.caption)
        PrimaryButton(t("Carry out the plan"), Modifier.fillMaxWidth(), enabled = !game.finished && game.plan.isNotEmpty()) { game.finish() }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RepatriationEnd(game: RepatriationController, onExit: () -> Unit, onOverview: (() -> Unit)?) {
    val scenario = game.scenario
    val model = game.model
    val r = game.result
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
                    StarRow(Progress.counted(game.stars, game.scenario.number))
                }
                Txt("${Format.changePermille(-r.reductionPermille)} CO₂", Type.style(40, FontWeight.Bold, if (r.reductionPermille > 0) Palette.accent else Palette.red))
                Txt(t(texts.comparedWith), Type.style(17, color = Palette.secondary))
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    GoalLine(met.rules, 1, t(texts.rules))
                    GoalLine(met.reduction, 2, t("CO₂ %@ or better", Format.changePermille(-scenario.goals.reduction.toLong())))
                    GoalLine(met.special, 3, specialLong(scenario.goals.special))
                }
                if (!met.rules) {
                    val missed = buildList {
                        if (r.savingPermille < model.savingGoalPermille) add(t("costs only %@ lower", Format.wholePercent(r.savingPermille.toInt())))
                        for (n in r.outsideGermany) add(t("%@ outside Germany", t(n)))
                        if (r.bookingTooSlow) add(t("a booking in the plant takes %@ s", Format.number(r.bookingMs / 1_000.0, 1)))
                    }
                    Txt(t("No star because: %@", missed.joinToString("; ")), Type.style(16, FontWeight.SemiBold, Palette.red))
                }
                Table(listOf("", t("today"), t("your plan")), listOf(
                    listOf(t("In the report (market-based), t CO₂"), tonnes(r.baselineReportKg), tonnes(r.reportKg)),
                    listOf(t("Location-based with making, t CO₂"), tonnes(r.baselineKg), tonnes(r.planKg)),
                    listOf(t("Costs in three years"), millionEuros(r.baselineEuro), millionEuros(r.planEuro)),
                ), firstWidth = 0.5f)
                Waterfall(r)
                Txt(t("What you did is called %@. %@", texts.term, t(texts.termText)), Type.style(15, color = Palette.secondary))
                Txt(t(texts.remark), Type.style(15, color = Palette.secondary))
                RepatriationWhatIf(game)
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

/** Where the change comes from: today, the grid, the servers and their load, the cooling, the making of servers, the plan. */
@Composable
private fun Waterfall(r: MoveResult) {
    val measurer = rememberTextMeasurer()
    val b = r.breakdown
    val steps = listOf(t("Grid") to b.region, t("Servers and load") to b.energy, t("Cooling (PUE)") to b.pue, t("Making servers") to b.embodied)
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Txt(t("Where the change comes from, t CO₂ in three years"), Type.style(13, FontWeight.SemiBold))
        Canvas(Modifier.fillMaxWidth().height(170.dp)) {
            val bottom = size.height - 30f * density
            val top = 18f * density
            var level = r.baselineKg.toDouble()
            val levels = mutableListOf(level)
            for ((_, d) in steps) { level += d; levels += level }
            val maxV = (levels.maxOrNull() ?: 1.0) * 1.08
            fun y(v: Double) = bottom - (bottom - top) * (v / maxV).toFloat()
            val n = steps.size + 2
            val slot = size.width / n
            val w = slot * 0.62f
            fun bar(i: Int, from: Double, to: Double, color: Color, label: String, value: String) {
                val x = slot * i + (slot - w) / 2
                val y0 = y(maxOf(from, to)); val y1 = y(minOf(from, to))
                drawRect(color, Offset(x, y0), Size(w, maxOf(1.5f, y1 - y0)))
                val v = measurer.measure(value, Type.style(11, FontWeight.SemiBold))
                drawText(v, topLeft = Offset(x + w / 2 - v.size.width / 2, y0 - v.size.height - 2f))
                val l = measurer.measure(label, Type.small.copy(textAlign = TextAlign.Center), constraints = androidx.compose.ui.unit.Constraints(maxWidth = slot.toInt()))
                drawText(l, topLeft = Offset(slot * i + slot / 2 - l.size.width / 2, bottom + 4f))
            }
            bar(0, 0.0, r.baselineKg.toDouble(), Palette.secondary.copy(alpha = 0.5f), t("today"), tonnes(r.baselineKg))
            for ((i, s) in steps.withIndex()) {
                val (label, d) = s
                val sign = if (d > 0) "+" else if (d < 0) "−" else "±"
                bar(i + 1, levels[i], levels[i + 1], if (d > 0) Palette.red else Palette.accent, label, sign + tonnes(kotlin.math.abs(d)))
            }
            bar(n - 1, 0.0, r.planKg.toDouble(), Palette.ink, t("your plan"), tonnes(r.planKg))
        }
    }
}

@Composable
private fun RepatriationWhatIf(game: RepatriationController) {
    val alternatives = remember(game.finished) { game.whatIf() }
    val best = maxOf(Progress.stars(game.scenario.number), game.stars)
    val goals = game.scenario.goals
    Card(Modifier.fillMaxWidth(), color = Palette.background, padding = 14.dp) {
        Txt(t("What if …"), Type.subheading)
        Txt(t("The same applications, other places:"), Type.style(15, color = Palette.secondary))
        Spacer(Modifier.height(8.dp))
        Row(Modifier.padding(vertical = 2.dp)) {
            Spacer(Modifier.weight(0.5f))
            Txt(t("CO₂"), Type.caption.copy(textAlign = TextAlign.End), Modifier.weight(0.16f))
            Txt(t("Costs"), Type.caption.copy(textAlign = TextAlign.End), Modifier.weight(0.16f))
            Spacer(Modifier.weight(0.18f))
        }
        var hidden = 0
        for ((label, r) in alternatives) {
            val stars = game.model.stars(r, goals)
            val secret = stars > best && !Progress.solutionSeen(game.scenario.number)
            if (secret) hidden++
            Row(Modifier.padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                if (secret) Txt(t("A setting you have not found yet"), Type.style(15, color = Palette.secondary).copy(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic), Modifier.weight(0.5f))
                else Txt(label, Type.callout, Modifier.weight(0.5f))
                Txt(Format.changePermille(-r.reductionPermille), Type.callout.copy(textAlign = TextAlign.End), Modifier.weight(0.16f))
                Txt(Format.changePermille(-r.savingPermille), Type.callout.copy(textAlign = TextAlign.End), Modifier.weight(0.16f))
                Box(Modifier.weight(0.18f), contentAlignment = Alignment.CenterEnd) { StarRow(stars, 11.dp) }
            }
        }
        if (hidden > 0) Txt(t("Settings with more stars than you have reached stay hidden until you find them."), Type.caption)
        SolutionBar(game.scenario.number, hidden, { game.loadSolution() })
    }
}
