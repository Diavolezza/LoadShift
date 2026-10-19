package de.wannerit.loadshift.app

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import de.wannerit.loadshift.core.Choice
import de.wannerit.loadshift.core.Decision
import de.wannerit.loadshift.core.Scenario
import de.wannerit.loadshift.core.TidyApp
import de.wannerit.loadshift.core.TidyResult
import kotlin.math.sqrt

/** State of P3: a choice per application, the selected one, whether the three years are done; a replay keeps the plan. */
class TidyController(val scenario: Scenario, fresh: Boolean = false) {
    val model = scenario.tidy!!
    var plan by mutableStateOf((if (fresh) null else lastPlans[scenario.number]) ?: emptyMap())
        private set
    var selected by mutableIntStateOf(0)
    var finished by mutableStateOf(false)
        private set

    val app: TidyApp get() = model.apps[selected]
    fun choice(a: TidyApp): Choice = plan[a.name] ?: Choice()
    val load: IntArray get() = model.load(plan)
    val result: TidyResult get() = model.result(plan)
    val stars: Int get() = model.stars(result, scenario.goals)

    private fun set(c: Choice) {
        if (finished) return
        plan = if (c.decision == Decision.TOLERATE) plan - app.name else plan + (app.name to c)
    }

    fun decide(d: Decision) = set(choice(app).copy(decision = d))
    fun moveStart(by: Int) = set(choice(app).let { it.copy(start = (it.start + by).coerceIn(0, model.quarters - 1)) })
    fun toggleDecommission() = set(choice(app).let { it.copy(decommission = !it.decommission) })

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

    fun whatIf(): List<Pair<String, TidyResult>> = listOf(
        t("Each quadrant's decision, as early as possible, old systems switched off") to model.result(model.byQuadrant(true)),
        t("The same, old systems left running") to model.result(model.byQuadrant(false)),
        t("Quick wins first, the ERP tuned instead of migrated") to model.result(model.example),
    )

    private companion object {
        val lastPlans = mutableMapOf<Int, Map<String, Choice>>()
    }
}

private val decisionColor = mapOf(
    Decision.TOLERATE to Color(0xFFB8C0BC), Decision.INVEST to Palette.accent,
    Decision.MIGRATE to Palette.demand, Decision.ELIMINATE to Palette.amber,
)

fun decisionTitle(d: Decision): String = when (d) {
    Decision.TOLERATE -> t("Tolerate")
    Decision.INVEST -> t("Invest")
    Decision.MIGRATE -> t("Migrate")
    Decision.ELIMINATE -> t("Eliminate")
}

private fun quarterText(q: Int) = t("Q%lld/%lld", q % 4 + 1, q / 4 + 1)

/** P3: the TIME matrix and the project plan on the left, the selected application on the right. */
@Composable
fun TidyScreen(game: TidyController, onExit: () -> Unit, onOverview: (() -> Unit)? = null) {
    BoxWithConstraints(Modifier.fillMaxSize().background(Palette.background)) {
        val panelWidth = (maxWidth * 0.3f).coerceIn(if (maxWidth < 1000.dp) 270.dp else 300.dp, 380.dp)
        // A low screen (a phone held sideways): the page scrolls and keeps a height everything fits into.
        val low = maxHeight < 640.dp
        val compact = maxWidth < 1150.dp
        Column(if (low) Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).height(860.dp) else Modifier.fillMaxSize()) {
            TidyTopBar(game, onExit, compact)
            TidyGoals(game, compact)
            HDivider()
            Row(Modifier.fillMaxWidth().weight(1f)) {
                Column(Modifier.weight(1f).fillMaxHeight()) {
                    Box(Modifier.fillMaxWidth().weight(1f).background(Palette.panel)) { Matrix(game) }
                    HDivider()
                    Box(Modifier.fillMaxWidth().height(300.dp).background(Palette.panel)) { ProjectPlan(game) }
                }
                VDivider()
                TidyPanel(game, Modifier.width(panelWidth).fillMaxHeight())
            }
        }
        if (game.finished) TidyEnd(game, onExit, onOverview)
        RotateHint(maxWidth, maxHeight)
    }
}

@Composable
private fun TidyTopBar(game: TidyController, onExit: () -> Unit, compact: Boolean = false) {
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
                Metric(t("Projects"), "${game.plan.size}")
                val most = game.load.maxOrNull() ?: 0
                Metric(t("Most at a time"), t("%lld of %lld", most, game.model.capacity), if (most > game.model.capacity) Palette.red else Palette.ink)
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
            Metric(t("Projects"), "${game.plan.size}")
            val most = game.load.maxOrNull() ?: 0
            Metric(t("Most at a time"), t("%lld of %lld", most, game.model.capacity), if (most > game.model.capacity) Palette.red else Palette.ink)
        }
    }
}

@Composable
private fun TidyGoals(game: TidyController, compact: Boolean = false) {
    val scenario = game.scenario
    val r = game.result
    val ok = r.overloaded.isEmpty() && r.wronglyEliminated.isEmpty()
    Row(
        // On a small screen the goals scroll sideways instead of wrapping letter by letter.
        Modifier.fillMaxWidth().background(Palette.panel).then(if (compact) Modifier.horizontalScroll(rememberScrollState()) else Modifier)
            .padding(horizontal = 20.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Txt(t("Goals"), Type.style(13, FontWeight.SemiBold, Palette.secondary))
        GoalItem(r.overloaded.isEmpty(), r.overloaded.isNotEmpty() || game.finished, t("At most %lld projects at a time", game.model.capacity))
        GoalItem(r.wronglyEliminated.isEmpty(), r.wronglyEliminated.isNotEmpty() || game.finished, t("Nothing eliminated that must stay"))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            StarLabel(2, 13.dp)
            Txt(t("CO₂ %@", Format.changePermille(-scenario.goals.reduction.toLong())), Type.callout)
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            StarLabel(3, 13.dp)
            Txt(specialLong(scenario.goals.special), Type.callout)
        }
        Spacer(Modifier.weight(1f))
        @Suppress("UNUSED_VARIABLE") val unused = ok
        StartOverChip { game.startOver() }
    }
}

/** The TIME matrix: technical condition to the right, business value up; a bubble per application, as large as its footprint. */
@Composable
private fun Matrix(game: TidyController) {
    val model = game.model
    val measurer = rememberTextMeasurer()
    @Suppress("UNUSED_VARIABLE") val redraw = game.plan to game.selected
    val maxKg = model.apps.maxOf { it.kgPerYear }.toFloat()
    Column(Modifier.fillMaxSize().padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 8.dp)) {
        Txt(t("The applications by business value and technical condition – the size of a bubble is its CO₂ a year; click one to decide"), Type.style(13, FontWeight.SemiBold))
        Canvas(Modifier.fillMaxWidth().weight(1f).padding(top = 6.dp).pointerInput(Unit) {
            detectTapGestures { p ->
                val left = 44f * density
                val w = size.width - left
                val h = size.height - 20f * density
                val hit = model.apps.indices.minByOrNull { i ->
                    val a = model.apps[i]
                    val dx = left + w * a.x / 1_000f - p.x
                    val dy = h * (1 - a.y / 1_000f) - p.y
                    dx * dx + dy * dy
                }
                if (hit != null) game.selected = hit
            }
        }) {
            val left = 44f * density
            val w = size.width - left
            val h = size.height - 20f * density
            // Quadrants: migrate (poor, valuable), invest (good, valuable), eliminate (poor, little value), tolerate (good, little value).
            val quads = listOf(
                Triple(Offset(left, 0f), Decision.MIGRATE, 0f), Triple(Offset(left + w / 2, 0f), Decision.INVEST, 1f),
                Triple(Offset(left, h / 2), Decision.ELIMINATE, 0f), Triple(Offset(left + w / 2, h / 2), Decision.TOLERATE, 1f),
            )
            for ((o, d, align) in quads) {
                drawRect(decisionColor.getValue(d).copy(alpha = 0.07f), o, Size(w / 2, h / 2))
                val label = measurer.measure(decisionTitle(d), Type.style(15, FontWeight.Bold, decisionColor.getValue(d)))
                val x = if (align == 0f) o.x + 8f * density else o.x + w / 2 - label.size.width - 8f * density
                val y = if (o.y == 0f) 6f * density else h - label.size.height - 6f * density
                drawText(label, topLeft = Offset(x, y))
            }
            drawLine(Palette.divider, Offset(left + w / 2, 0f), Offset(left + w / 2, h), 1.5f * density)
            drawLine(Palette.divider, Offset(left, h / 2), Offset(size.width, h / 2), 1.5f * density)
            val xl = measurer.measure(t("technical condition: poor → good"), Type.small)
            drawText(xl, topLeft = Offset(left + w / 2 - xl.size.width / 2, h + 3f * density))
            val yl = measurer.measure(t("value ↑"), Type.small)
            drawText(yl, topLeft = Offset(0f, 0f))
            for ((i, a) in model.apps.withIndex()) {
                val c = Offset(left + w * a.x / 1_000f, h * (1 - a.y / 1_000f))
                val r = (10f + 26f * sqrt(a.kgPerYear / maxKg)) * density
                val d = game.choice(a).decision
                drawCircle(decisionColor.getValue(d).copy(alpha = if (d == Decision.TOLERATE) 0.45f else 0.8f), r, c)
                if (i == game.selected) drawCircle(Palette.ink, r + 3f * density, c, style = Stroke(2f * density))
                val name = measurer.measure(t(a.name), Type.style(11, FontWeight.SemiBold))
                drawText(name, topLeft = Offset(c.x - name.size.width / 2, c.y + r + 2f * density))
            }
        }
    }
}

/** The project plan: a row per project over twelve quarters, and how many run at a time. */
@Composable
private fun ProjectPlan(game: TidyController) {
    val model = game.model
    val rows = model.apps.filter { game.choice(it).decision != Decision.TOLERATE }
    val load = game.load
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Txt(t("Projects – the team runs at most %lld at a time", model.capacity), Type.style(13, FontWeight.SemiBold))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Txt(t("At a time"), Type.caption, Modifier.width(180.dp))
            for (q in 0 until model.quarters) {
                val over = load[q] > model.capacity
                Box(Modifier.weight(1f).padding(1.dp).height(20.dp).background(if (over) Palette.red.copy(alpha = 0.18f) else Palette.background, RoundedCornerShape(4.dp)), contentAlignment = Alignment.Center) {
                    Txt("${load[q]}", Type.style(12, FontWeight.SemiBold, if (over) Palette.red else Palette.ink).copy(textAlign = TextAlign.Center))
                }
            }
        }
        Row {
            Spacer(Modifier.width(180.dp))
            for (q in 0 until model.quarters) Txt(quarterText(q), Type.small.copy(textAlign = TextAlign.Center), Modifier.weight(1f))
        }
        for (a in rows) {
            val c = game.choice(a)
            val n = model.projectQuarters(a, c)
            val selected = model.apps.indexOf(a) == game.selected
            Row(Modifier.clip(RoundedCornerShape(6.dp)).clickable { game.selected = model.apps.indexOf(a) }, verticalAlignment = Alignment.CenterVertically) {
                Txt(t(a.name), Type.style(13, if (selected) FontWeight.Bold else FontWeight.Normal), Modifier.width(180.dp), maxLines = 1)
                for (q in 0 until model.quarters) {
                    val inProject = q >= c.start && q < c.start + n
                    val switchOff = c.decision == Decision.MIGRATE && c.decommission && q == c.start + n - 1
                    val both = c.decision == Decision.MIGRATE && q >= c.start
                    Box(Modifier.weight(1f).padding(1.dp).height(16.dp)) {
                        if (inProject) Box(Modifier.fillMaxSize().background(decisionColor.getValue(c.decision).copy(alpha = if (switchOff) 0.5f else 0.85f), RoundedCornerShape(3.dp)))
                        else if (both && !c.decommission) Box(Modifier.fillMaxWidth().height(3.dp).align(Alignment.Center).background(Palette.red.copy(alpha = 0.6f)))
                    }
                }
            }
        }
        if (rows.isEmpty()) Txt(t("No project yet: everything stays as it is."), Type.caption)
        else Txt(t("Pale: switching off the old system. Red line: old and new system both keep running."), Type.caption)
    }
}

@Composable
private fun TidyPanel(game: TidyController, modifier: Modifier) {
    val model = game.model
    val a = game.app
    val c = game.choice(a)
    Column(modifier.background(Palette.panel).verticalScroll(rememberScrollState()).padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Txt(t(a.name), Type.heading)
        Txt(t(a.description), Type.style(15, color = Palette.secondary))
        Txt(t("Business value %@ · technical condition %@ · %@ t CO₂ a year",
            if (a.highValue) t("high") else t("low"), if (a.goodCondition) t("good") else t("poor"), tonnes(a.kgPerYear)), Type.style(15, FontWeight.SemiBold))
        a.mustKeep?.let { Txt(t(it), Type.style(15, FontWeight.SemiBold)) }
        RuleOption(c.decision == Decision.TOLERATE, { game.decide(Decision.TOLERATE) }, t("Tolerate"), t("Leave it as it is."))
        RuleOption(c.decision == Decision.INVEST, { game.decide(Decision.INVEST) }, t("Invest"),
            t("%@ – %lld quarters, then %@ less CO₂.", t(a.invest.title), a.invest.quarters, Format.wholePercent((100 - a.invest.leftPercent) * 10)))
        RuleOption(c.decision == Decision.MIGRATE, { game.decide(Decision.MIGRATE) }, t("Migrate"),
            t("%@ – %lld quarters. The new system needs %@ of today's CO₂; it runs next to the old one from the start. The old system runs until it is switched off.",
                t(a.migrate.title), a.migrate.quarters, Format.wholePercent(a.migrate.leftPercent * 10))) {
            Row(Modifier.clip(RoundedCornerShape(8.dp)).clickable { game.toggleDecommission() }.padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Tick(c.decommission)
                Txt(t("Then switch off the old system (one more quarter: archive the data)"), Type.style(14))
            }
        }
        RuleOption(c.decision == Decision.ELIMINATE, { game.decide(Decision.ELIMINATE) }, t("Eliminate"),
            t("Move the users and data elsewhere, then switch it off – %lld quarters.", a.eliminateQuarters))
        if (c.decision != Decision.TOLERATE) {
            Txt(t("Start"), Type.subheading)
            Stepper(t("Quarter %lld, year %lld", c.start % 4 + 1, c.start / 4 + 1), { game.moveStart(-1) }, { game.moveStart(1) })
        }
        HDivider()
        Txt(t("Counted are three years, quarter by quarter, location-based. While a project runs, the application stays as it is."), Type.caption)
        PrimaryButton(t("Carry out the plan"), Modifier.fillMaxWidth(), enabled = !game.finished && game.plan.isNotEmpty()) { game.finish() }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TidyEnd(game: TidyController, onExit: () -> Unit, onOverview: (() -> Unit)?) {
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
                        for (q in r.overloaded) add(t("%@: %lld projects (at most %lld)", quarterText(q), game.load[q], game.model.capacity))
                        for (n in r.wronglyEliminated) add(t("%@ eliminated, but it must stay", t(n)))
                        if (r.planKg >= r.baselineKg) add(t("not less CO₂ over three years"))
                    }
                    Txt(t("No star because: %@", missed.joinToString("; ")), Type.style(16, FontWeight.SemiBold, Palette.red))
                }
                Table(listOf("", t("t CO₂")), listOf(
                    listOf(t("Three years without measures"), tonnes(r.baselineKg)),
                    listOf(t("Three years with your plan"), tonnes(r.planKg)),
                ), firstWidth = 0.6f)
                if (r.stillRunning.isNotEmpty()) Txt(t("Old systems still running after their migration: %@", r.stillRunning.joinToString(", ") { t(it) }), Type.style(15, FontWeight.SemiBold))
                QuarterLines(game)
                Txt(t("What you did is called %@. %@", texts.term, t(texts.termText)), Type.style(15, color = Palette.secondary))
                Txt(t(texts.remark), Type.style(15, color = Palette.secondary))
                TidyWhatIf(game)
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

/** CO₂ a year in each quarter: without measures (dashed) and with the plan. */
@Composable
private fun QuarterLines(game: TidyController) {
    val model = game.model
    val base = model.byQuarter(emptyMap())
    val mine = model.byQuarter(game.plan)
    val measurer = rememberTextMeasurer()
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Txt(t("CO₂ a year in each quarter, t – dashed: without measures"), Type.style(13, FontWeight.SemiBold))
        Canvas(Modifier.fillMaxWidth().height(150.dp)) {
            val left = 36f * density
            val bottom = size.height - 16f * density
            val top = 6f * density
            val maxV = (base + mine).maxOrNull()!!.toFloat() * 1.1f
            fun x(q: Int) = left + (size.width - left) * (q + 0.5f) / model.quarters
            fun y(v: Double) = bottom - (bottom - top) * (v / maxV).toFloat()
            for (v in listOf(0.0, maxV / 2.0)) {
                drawLine(Color.Gray.copy(alpha = 0.2f), Offset(left, y(v)), Offset(size.width, y(v)), 1f)
                val l = measurer.measure(Format.number(v / 1_000), Type.small)
                drawText(l, topLeft = Offset(left - l.size.width - 4f * density, y(v) - l.size.height / 2))
            }
            for (q in 0 until model.quarters - 1) {
                drawLine(Palette.secondary, Offset(x(q), y(base[q])), Offset(x(q + 1), y(base[q + 1])), 1.5f * density,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(5f * density, 4f * density)))
                drawLine(Palette.accent, Offset(x(q), y(mine[q])), Offset(x(q + 1), y(mine[q + 1])), 2.5f * density)
            }
            for (q in 0 until model.quarters) {
                drawCircle(Palette.accent, 3f * density, Offset(x(q), y(mine[q])))
                if (q % 4 == 0) {
                    val l = measurer.measure(t("Year %lld", q / 4 + 1), Type.small)
                    drawText(l, topLeft = Offset(x(q) - 6f * density, bottom + 2f * density))
                }
            }
        }
    }
}

@Composable
private fun TidyWhatIf(game: TidyController) {
    val alternatives = remember(game.finished) { game.whatIf() }
    val best = maxOf(Progress.stars(game.scenario.number), game.stars)
    val goals = game.scenario.goals
    Card(Modifier.fillMaxWidth(), color = Palette.background, padding = 14.dp) {
        Txt(t("What if …"), Type.subheading)
        Txt(t("The same portfolio, other plans:"), Type.style(15, color = Palette.secondary))
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
