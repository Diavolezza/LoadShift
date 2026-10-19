package de.wannerit.loadshift.app

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import de.wannerit.loadshift.core.AbatementResult
import de.wannerit.loadshift.core.Scenario

/** State of P2: the plan, whether the three years are done; a replay starts with the last plan. */
class AbatementController(val scenario: Scenario, fresh: Boolean = false) {
    val model = scenario.abatement!!
    var plan by mutableStateOf((if (fresh) null else lastPlans[scenario.number]) ?: emptySet())
        private set
    var finished by mutableStateOf(false)
        private set

    val cost: Int get() = model.cost(plan)
    val left: Int get() = model.budget - cost
    /** What the consultant's list promises for the plan: the gross savings added up. */
    val promisedKg: Long get() = plan.sumOf { model.consultantKg(it) }
    val result: AbatementResult get() = model.result(plan)
    val stars: Int get() = model.stars(result, scenario.goals)

    fun canAfford(i: Int) = i in plan || model.measures[i].costEuro <= left

    fun toggle(i: Int) {
        if (finished) return
        if (i in plan) plan = plan - i else if (canAfford(i)) plan = plan + i
    }

    /** What a measure of the plan really adds: the plan with it against the plan without it. */
    fun contribution(i: Int): Long = model.savedKg(plan) - model.savedKg(plan - i)

    fun finish() {
        finished = true
        Progress.record(stars, scenario.number)
        lastPlans[scenario.number] = plan
    }

    fun restart() { finished = false }

    fun startOver() {
        plan = emptySet()
        finished = false
    }

    /** The best plan within the budget in the screen, to look at and carry out. */
    fun loadSolution() {
        plan = model.best()
        finished = false
    }

    fun whatIf(): List<Pair<String, AbatementResult>> {
        val certificates = model.measures.indices.filter { model.measures[it].target == null && model.measures[it].onSiteKgPerYear == 0L }.toSet()
        return listOf(
            t("Along the consultant's curve") to model.result(model.alongTheCurve()),
            t("Along the curve, without the certificates") to model.result(model.alongTheCurve() - certificates),
            t("The best plan within the budget") to model.result(model.best()),
        )
    }

    private companion object {
        val lastPlans = mutableMapOf<Int, Set<Int>>()
    }
}

/** P2: the consultant's curve on top, the measures below it, the plan on the right. */
@Composable
fun AbatementScreen(game: AbatementController, onExit: () -> Unit, onOverview: (() -> Unit)? = null) {
    BoxWithConstraints(Modifier.fillMaxSize().background(Palette.background)) {
        val panelWidth = (maxWidth * 0.3f).coerceIn(if (maxWidth < 1000.dp) 270.dp else 300.dp, 380.dp)
        // A low screen (a phone held sideways): the page scrolls and keeps a height everything fits into.
        val low = maxHeight < 640.dp
        val compact = maxWidth < 1150.dp
        Column(if (low) Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).height(720.dp) else Modifier.fillMaxSize()) {
            AbatementTopBar(game, onExit, compact)
            AbatementGoals(game, compact)
            HDivider()
            Row(Modifier.fillMaxWidth().weight(1f)) {
                Column(Modifier.weight(1f).fillMaxHeight()) {
                    Box(Modifier.fillMaxWidth().height(290.dp).background(Palette.panel)) { CostCurve(game) }
                    HDivider()
                    MeasureCards(game, Modifier.weight(1f).fillMaxWidth())
                }
                VDivider()
                AbatementPanel(game, Modifier.width(panelWidth).fillMaxHeight())
            }
        }
        if (game.finished) AbatementEnd(game, onExit, onOverview)
        RotateHint(maxWidth, maxHeight)
    }
}

@Composable
private fun AbatementTopBar(game: AbatementController, onExit: () -> Unit, compact: Boolean = false) {
    if (compact) {
        // A small screen (a phone held sideways): one row, the figures scroll sideways, no scenario name.
        Row(
            Modifier.fillMaxWidth().background(Palette.panel).padding(horizontal = 12.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Txt("‹", Type.style(30, FontWeight.SemiBold), Modifier.clip(RoundedCornerShape(8.dp)).clickable(onClick = onExit).padding(horizontal = 4.dp))
            Column {
                Txt(t("Three years"), Type.caption)
                Txt(if (game.finished) t("Plan carried out") else t("The plan"), Type.style(18, FontWeight.SemiBold))
            }
            Row(Modifier.weight(1f).horizontalScroll(rememberScrollState()), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                Metric(t("According to the list"), t("−%@ t", tonnes(game.promisedKg)))
                Metric(t("Budget left"), euros(game.left))
            }
        }
        return
    }
    Box(Modifier.fillMaxWidth().background(Palette.panel).padding(horizontal = 20.dp, vertical = 10.dp)) {
        Row(Modifier.align(Alignment.CenterStart), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(22.dp)) {
            Txt("‹", Type.style(34, FontWeight.SemiBold), Modifier.clip(RoundedCornerShape(8.dp)).clickable(onClick = onExit).padding(horizontal = 6.dp))
            Column {
                Txt(t("Scenario %@", game.scenario.shownLabel), Type.caption)
                Txt(t(game.scenario.title), Type.style(17, FontWeight.SemiBold), maxLines = 1)
            }
        }
        Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
            Txt(t("Three years"), Type.caption)
            Txt(if (game.finished) t("Plan carried out") else t("The plan"), Type.style(24, FontWeight.SemiBold))
        }
        Row(Modifier.align(Alignment.CenterEnd), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(22.dp)) {
            Metric(t("According to the list"), t("−%@ t", tonnes(game.promisedKg)))
            Metric(t("Budget left"), euros(game.left))
        }
    }
}

@Composable
private fun AbatementGoals(game: AbatementController, compact: Boolean = false) {
    val scenario = game.scenario
    val met = game.model.goalsMet(game.result, scenario.goals)
    Row(
        // On a small screen the goals scroll sideways instead of wrapping letter by letter.
        Modifier.fillMaxWidth().background(Palette.panel).then(if (compact) Modifier.horizontalScroll(rememberScrollState()) else Modifier)
            .padding(horizontal = 20.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Txt(t("Goals"), Type.style(13, FontWeight.SemiBold, Palette.secondary))
        GoalItem(met.rules, game.finished, t("Within the budget and less CO₂ over three years"))
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

/** The consultant's curve: bars sorted by € per tonne, as wide as the tonnes over three years. A click toggles a measure. */
@Composable
private fun CostCurve(game: AbatementController) {
    val model = game.model
    val measurer = rememberTextMeasurer()
    val order = model.consultantOrder
    val totalKg = order.sumOf { model.consultantKg(it) }.toFloat()
    val maxEuro = order.maxOf { model.consultantEuroPerTonne(it) }
    val scaleTop = ((maxEuro + 499) / 500 * 500).toFloat()
    @Suppress("UNUSED_VARIABLE") val redraw = game.plan
    Column(Modifier.fillMaxSize().padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 8.dp)) {
        Txt(t("The consultant's curve: cost per tonne avoided (height) and tonnes over three years (width), from each measure's saving a year"), Type.style(13, FontWeight.SemiBold))
        Canvas(Modifier.fillMaxWidth().weight(1f).padding(top = 6.dp).pointerInput(game.finished) {
            detectTapGestures { p ->
                val left = 52f * density
                val w = size.width - left
                var x = left
                for (i in order) {
                    val bw = w * model.consultantKg(i) / totalKg
                    if (p.x in x..(x + bw)) { game.toggle(i); break }
                    x += bw
                }
            }
        }) {
            val left = 52f * density
            val bottom = size.height - 18f * density
            val top = 8f * density
            val w = size.width - left
            fun y(euro: Long) = bottom - (bottom - top) * (euro / scaleTop)
            // Axis: € per tonne.
            for (v in listOf(0L, scaleTop.toLong() / 2, scaleTop.toLong())) {
                drawLine(Color.Gray.copy(alpha = 0.2f), Offset(left, y(v)), Offset(size.width, y(v)), 1f)
                val layout = measurer.measure(Format.number(v.toDouble()) + " €/t", Type.small)
                drawText(layout, topLeft = Offset(left - layout.size.width - 6f * density, y(v) - layout.size.height / 2))
            }
            var x = left
            for (i in order) {
                val bw = w * model.consultantKg(i) / totalKg
                val inPlan = i in game.plan
                val h = maxOf(2f * density, bottom - y(model.consultantEuroPerTonne(i)))
                drawRect(if (inPlan) Palette.accent else Palette.capacity, Offset(x + 1f, bottom - h), Size(maxOf(1f, bw - 2f), h))
                if (inPlan) drawRect(Palette.ink, Offset(x + 1f, bottom - h), Size(maxOf(1f, bw - 2f), h), style = Stroke(1f * density))
                val label = measurer.measure("${i + 1}", Type.style(11, FontWeight.SemiBold, Palette.ink))
                if (bw > label.size.width + 2) drawText(label, topLeft = Offset(x + (bw - label.size.width) / 2, bottom - h - label.size.height - 2f))
                x += bw
            }
            drawLine(Palette.ink, Offset(left, bottom), Offset(size.width, bottom), 1f * density)
            val axis = measurer.measure(t("tonnes over three years →"), Type.small)
            drawText(axis, topLeft = Offset(size.width - axis.size.width, bottom + 3f * density))
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MeasureCards(game: AbatementController, modifier: Modifier) {
    val model = game.model
    BoxWithConstraints(modifier) {
        val columns = if (maxWidth > 900.dp) 4 else 3
        val cardWidth = (maxWidth - 24.dp - 10.dp * (columns - 1)) / columns - 2.dp
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp)) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                for (i in model.consultantOrder) MeasureCard(game, i, Modifier.width(cardWidth))
            }
        }
    }
}

@Composable
private fun MeasureCard(game: AbatementController, i: Int, modifier: Modifier) {
    val model = game.model
    val m = model.measures[i]
    val inPlan = i in game.plan
    Column(
        modifier.background(Palette.panel, RoundedCornerShape(12.dp))
            .border(if (inPlan) 1.5.dp else 0.dp, if (inPlan) Palette.accent else Color.Transparent, RoundedCornerShape(12.dp))
            .clip(RoundedCornerShape(12.dp)).clickable(enabled = !game.finished && game.canAfford(i)) { game.toggle(i) }.padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(Modifier.size(22.dp).background(if (inPlan) Palette.accent else Palette.capacity, CircleShape), contentAlignment = Alignment.Center) {
                Txt("${i + 1}", Type.style(12, FontWeight.Bold, Color.White).copy(textAlign = TextAlign.Center))
            }
            Txt(t(m.application), Type.caption, Modifier.weight(1f), maxLines = 1)
            Tick(inPlan)
        }
        Txt(t(m.title), Type.style(14, FontWeight.SemiBold, if (game.canAfford(i)) Palette.ink else Palette.secondary))
        Txt(t(m.facts), Type.caption)
        Txt(t("%@ · %@ t a year · %@ €/t", euros(m.costEuro), tonnes(m.grossKgPerYear), Format.number(model.consultantEuroPerTonne(i).toDouble())), Type.style(13, FontWeight.SemiBold))
        if (game.finished && inPlan) {
            val c = game.contribution(i)
            Txt(t("Really %@ t in three years", (if (c < 0) "+" else "−") + tonnes(kotlin.math.abs(c))), Type.style(13, FontWeight.SemiBold, if (c > 0) Palette.accent else Palette.red))
        }
    }
}

@Composable
private fun AbatementPanel(game: AbatementController, modifier: Modifier) {
    val model = game.model
    Column(modifier.background(Palette.panel).verticalScroll(rememberScrollState()).padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Txt(t("Your plan"), Type.heading)
        Txt(t("Click a bar of the curve or a card to put a measure into the plan. All measures start now; each works once its project is done."), Type.style(15, color = Palette.secondary))
        Card(Modifier.fillMaxWidth(), color = Palette.background, padding = 14.dp) {
            for ((label, value) in listOf(
                t("Budget") to euros(model.budget),
                t("Measures") to (if (game.cost > 0) "−" else "") + euros(game.cost),
                t("Left") to euros(game.left),
            )) {
                Row(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                    Txt(label, Type.callout, Modifier.weight(1f))
                    Txt(value, Type.style(15, FontWeight.SemiBold))
                }
            }
        }
        Txt(t("For these measures the consultant's list gives %@ t over three years – %@ of the %@ t the company emits in that time.",
            tonnes(game.promisedKg), Format.wholePercent((game.promisedKg * 1_000 / model.baselineKg).toInt()), tonnes(model.baselineKg)), Type.callout)
        Txt(t("Counted are three years, quarter by quarter, location-based. The stars go by what really changes."), Type.caption)
        PrimaryButton(t("Carry out the plan"), Modifier.fillMaxWidth(), enabled = !game.finished && game.plan.isNotEmpty()) { game.finish() }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AbatementEnd(game: AbatementController, onExit: () -> Unit, onOverview: (() -> Unit)?) {
    val scenario = game.scenario
    val r = game.result
    val met = game.model.goalsMet(r, scenario.goals)
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
                        if (r.costEuro > game.model.budget) add(t("over budget"))
                        if (r.savedKg <= 0) add(t("not less CO₂ over three years"))
                    }
                    if (missed.isNotEmpty()) Txt(t("No star because: %@", missed.joinToString("; ")), Type.style(16, FontWeight.SemiBold, Palette.red))
                }
                Table(listOf("", t("t CO₂")), listOf(
                    listOf(t("Three years without measures"), tonnes(r.baselineKg)),
                    listOf(t("According to the consultant's list"), "−" + tonnes(game.promisedKg)),
                    listOf(t("Really saved, location-based"), (if (r.savedKg < 0) "+" else "−") + tonnes(kotlin.math.abs(r.savedKg))),
                    listOf(t("Spent (€)"), Format.number(r.costEuro.toDouble())),
                ), firstWidth = 0.6f)
                Txt(t("What you did is called %@. %@", texts.term, t(texts.termText)), Type.style(15, color = Palette.secondary))
                Txt(t(texts.remark), Type.style(15, color = Palette.secondary))
                AbatementWhatIf(game)
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

@Composable
private fun AbatementWhatIf(game: AbatementController) {
    val alternatives = remember(game.finished) { game.whatIf() }
    val best = maxOf(Progress.stars(game.scenario.number), game.stars)
    val goals = game.scenario.goals
    Card(Modifier.fillMaxWidth(), color = Palette.background, padding = 14.dp) {
        Txt(t("What if …"), Type.subheading)
        Txt(t("The same budget, other plans:"), Type.style(15, color = Palette.secondary))
        Spacer(Modifier.height(8.dp))
        var hidden = 0
        for ((label, r) in alternatives) {
            val stars = game.model.stars(r, goals)
            val secret = stars > best && !Progress.solutionSeen(game.scenario.number)
            if (secret) hidden++
            Row(Modifier.padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                if (secret) Txt(t("A setting you have not found yet"), Type.style(15, color = Palette.secondary).copy(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic), Modifier.weight(0.6f))
                else Txt(label, Type.callout, Modifier.weight(0.6f))
                Txt(Format.changePermille(-r.reductionPermille), Type.callout.copy(textAlign = TextAlign.End), Modifier.weight(0.22f))
                Box(Modifier.weight(0.18f), contentAlignment = Alignment.CenterEnd) { StarRow(stars, 11.dp) }
            }
        }
        if (hidden > 0) Txt(t("Settings with more stars than you have reached stay hidden until you find them."), Type.caption)
        SolutionBar(game.scenario.number, hidden, { game.loadSolution() })
    }
}
