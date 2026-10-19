package de.wannerit.loadshift.app

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import de.wannerit.loadshift.core.Architecture
import de.wannerit.loadshift.core.AvailabilityResult
import de.wannerit.loadshift.core.BuildingBlock
import de.wannerit.loadshift.core.Scenario
import de.wannerit.loadshift.core.Tactic
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

/** State of E1: a tactic per building block, the decouplings, the selected block; a replay keeps the architecture. */
class AvailabilityController(val scenario: Scenario, fresh: Boolean = false) {
    val model = scenario.availability!!
    var arch by mutableStateOf((if (fresh) null else lastPlans[scenario.number]) ?: model.reference)
        private set
    var selected by mutableIntStateOf(0)
    var finished by mutableStateOf(false)
        private set

    val block: BuildingBlock get() = model.blocks[selected]
    val result: AvailabilityResult get() = model.result(arch)
    val stars: Int get() = model.stars(result, scenario.goals)
    /** Something has been changed (the CO₂ goals are judged from then on). */
    val started: Boolean get() = arch != model.reference || finished

    fun tactic(b: BuildingBlock): Tactic = model.tactic(arch, b.name)

    fun setTactic(b: BuildingBlock, t: Tactic) {
        if (finished) return
        arch = arch.copy(tactics = arch.tactics + (b.name to t))
    }

    fun toggle(id: String) {
        if (finished) return
        arch = arch.copy(decouplings = if (id in arch.decouplings) arch.decouplings - id else arch.decouplings + id)
    }

    fun finish() {
        finished = true
        Progress.record(stars, scenario.number)
        lastPlans[scenario.number] = arch
    }

    fun restart() { finished = false }

    fun startOver() {
        arch = model.reference
        finished = false
    }

    /** The model solution in the screen, to look at and carry out. */
    fun loadSolution() {
        arch = model.example
        finished = false
    }

    fun whatIf(): List<Pair<String, AvailabilityResult>> {
        val own = Architecture(mapOf(
            "Login" to Tactic.WARM_STANDBY, "Policy database" to Tactic.WARM_STANDBY, "Claims intake" to Tactic.WARM_STANDBY,
            "Contract overview" to Tactic.COLD_STANDBY, "Tariff calculator" to Tactic.COLD_STANDBY,
            "Recommendations" to Tactic.SINGLE, "Reporting" to Tactic.SINGLE,
        ))
        return listOf(
            t("Everything active-active, as the board asked") to model.result(model.reference),
            t("Everything warm standby") to model.result(model.all(Tactic.WARM_STANDBY)),
            t("Each block for its own scenario, shared blocks warm") to model.result(own),
            t("The same with the circuit breaker") to model.result(own.copy(decouplings = setOf("fallback"))),
            t("Shared blocks warm, the database cold behind a queue, circuit breaker") to model.result(model.example),
        )
    }

    private companion object {
        val lastPlans = mutableMapOf<Int, Architecture>()
    }
}

fun tacticTitle(t: Tactic) = when (t) {
    Tactic.SINGLE -> t("Single, restarted")
    Tactic.COLD_STANDBY -> t("Cold standby")
    Tactic.WARM_STANDBY -> t("Warm standby")
    Tactic.ACTIVE_ACTIVE -> t("Active-active")
}

private val tacticColor = mapOf(
    Tactic.SINGLE to Color(0xFFB8C0BC), Tactic.COLD_STANDBY to Palette.capacity,
    Tactic.WARM_STANDBY to Palette.amber, Tactic.ACTIVE_ACTIVE to Palette.accent,
)

/** Downtime in a year: minutes, hours or days. */
fun downtimeText(minutes: Double): String = when {
    minutes < 90 -> t("%lld min", kotlin.math.round(minutes).toLong())
    minutes < 48 * 60 -> t("%@ h", Format.number(minutes / 60, 1))
    else -> t("%@ days", Format.number(minutes / 1_440, 1))
}

@Composable
fun AvailabilityScreen(game: AvailabilityController, onExit: () -> Unit, onOverview: (() -> Unit)? = null) {
    BoxWithConstraints(Modifier.fillMaxSize().background(Palette.background)) {
        val panelWidth = (maxWidth * 0.3f).coerceIn(if (maxWidth < 1000.dp) 270.dp else 300.dp, 380.dp)
        val low = maxHeight < 640.dp
        val compact = maxWidth < 1150.dp
        Column(if (low) Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).height(820.dp) else Modifier.fillMaxSize()) {
            AvailabilityTopBar(game, onExit, compact)
            AvailabilityGoals(game, compact)
            HDivider()
            Row(Modifier.fillMaxWidth().weight(1f)) {
                Column(Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState()).padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(Modifier.fillMaxWidth().height(330.dp).background(Palette.panel, RoundedCornerShape(10.dp))) { Diagram(game) }
                    ScenarioTable(game)
                }
                VDivider()
                AvailabilityPanel(game, Modifier.width(panelWidth).fillMaxHeight())
            }
        }
        if (game.finished) AvailabilityEnd(game, onExit, onOverview)
        RotateHint(maxWidth, maxHeight)
    }
}

@Composable
private fun AvailabilityTopBar(game: AvailabilityController, onExit: () -> Unit, compact: Boolean) {
    val r = game.result
    val metrics: @Composable () -> Unit = {
        Metric(t("CO₂ a year"), t("%@ t", tonnes(r.planKg)))
        Metric(t("Compared with today"), Format.changePermille(-r.reductionPermille))
        Metric(t("Servers on"), Format.number(r.serversOn))
        Metric(t("Quality scenarios met"), t("%lld of %lld", (game.model.scenarios.size - r.missed.size).toLong(), game.model.scenarios.size.toLong()),
            if (r.missed.isNotEmpty()) Palette.red else Palette.ink)
    }
    PlayTopBar(game.scenario, onExit, compact, t("A year"), if (game.finished) t("Architecture in place") else t("The architecture"), metrics)
}

/** The top bar of the screens without a clock: back, scenario, what is being planned, the figures. */
@Composable
fun PlayTopBar(scenario: Scenario, onExit: () -> Unit, compact: Boolean, caption: String, title: String, metrics: @Composable () -> Unit) {
    if (compact) {
        Row(
            Modifier.fillMaxWidth().background(Palette.panel).padding(horizontal = 12.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Txt("‹", Type.style(30, FontWeight.SemiBold), Modifier.clip(RoundedCornerShape(8.dp)).clickable(onClick = onExit).padding(horizontal = 4.dp))
            Column {
                Txt(caption, Type.caption)
                Txt(title, Type.style(18, FontWeight.SemiBold))
            }
            Row(Modifier.weight(1f).horizontalScroll(rememberScrollState()), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                metrics()
            }
        }
        return
    }
    Row(
        Modifier.fillMaxWidth().background(Palette.panel).padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(22.dp),
    ) {
        Txt("‹", Type.style(34, FontWeight.SemiBold), Modifier.clip(RoundedCornerShape(8.dp)).clickable(onClick = onExit).padding(horizontal = 6.dp))
        Column(Modifier.widthIn(max = 260.dp)) {
            Txt(t("Scenario %@", scenario.shownLabel), Type.caption)
            Txt(t(scenario.title), Type.style(17, FontWeight.SemiBold), maxLines = 1)
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Txt(caption, Type.caption)
            Txt(title, Type.style(22, FontWeight.SemiBold))
        }
        Spacer(Modifier.weight(1f))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(22.dp)) { metrics() }
    }
}

@Composable
private fun AvailabilityGoals(game: AvailabilityController, compact: Boolean) {
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

/** The building blocks as boxes, coloured by their tactic, with arrows for the synchronous calls of each scenario. */
@Composable
private fun Diagram(game: AvailabilityController) {
    val model = game.model
    val measurer = rememberTextMeasurer()
    val arch = game.arch
    Canvas(Modifier.fillMaxSize().pointerInput(game.finished) {
        detectTapGestures { p ->
            val w = 150f * density; val h = 64f * density
            for ((i, b) in model.blocks.withIndex()) {
                val cx = size.width * b.x / 1_000f; val cy = size.height * b.y / 1_000f
                if (p.x in cx - w / 2..cx + w / 2 && p.y in cy - h / 2..cy + h / 2) game.selected = i
            }
        }
    }) {
        val w = 150f * density; val h = 64f * density
        fun center(name: String): Offset {
            val b = model.blocks.first { it.name == name }
            return Offset(size.width * b.x / 1_000f, size.height * b.y / 1_000f)
        }
        // Calls: from the scenario's first block to each other one it waits for; decoupled calls dashed.
        for (s in model.scenarios) {
            val from = s.uses.first()
            val waits = model.uses(arch, s)
            for (to in s.uses.drop(1)) {
                val a = center(from); val b = center(to)
                val decoupled = to !in waits
                val angle = atan2(b.y - a.y, b.x - a.x)
                // Start and end at the edges of the boxes (roughly).
                fun edge(c: Offset, dir: Float): Offset {
                    val dx = cos(angle) * dir; val dy = sin(angle) * dir
                    val tx = if (dx != 0f) (w / 2) / kotlin.math.abs(dx) else Float.MAX_VALUE
                    val ty = if (dy != 0f) (h / 2) / kotlin.math.abs(dy) else Float.MAX_VALUE
                    val tt = minOf(tx, ty)
                    return Offset(c.x + dx * tt, c.y + dy * tt)
                }
                val p0 = edge(a, 1f); val p1 = edge(b, -1f)
                val color = if (decoupled) Palette.secondary.copy(alpha = 0.45f) else Palette.ink.copy(alpha = 0.7f)
                drawLine(color, p0, p1, 1.8f * density,
                    pathEffect = if (decoupled) PathEffect.dashPathEffect(floatArrayOf(6f * density, 5f * density)) else null)
                val head = 9f * density
                for (sign in listOf(-1f, 1f)) {
                    val ang = angle + kotlin.math.PI.toFloat() + sign * 0.45f
                    drawLine(color, p1, Offset(p1.x + cos(ang) * head, p1.y + sin(ang) * head), 1.8f * density)
                }
                if (decoupled) {
                    val mid = Offset((p0.x + p1.x) / 2, (p0.y + p1.y) / 2)
                    val label = model.decouplings.first { it.scenario == s.name && it.removes == to }
                    val l = measurer.measure(if (label.brokerServers > 0) t("queue") else t("circuit breaker"), Type.style(11, FontWeight.SemiBold, Palette.secondary))
                    drawRoundRect(Color.White, Offset(mid.x - l.size.width / 2f - 4f, mid.y - l.size.height / 2f - 2f),
                        Size(l.size.width + 8f, l.size.height + 4f), CornerRadius(4f))
                    drawText(l, topLeft = Offset(mid.x - l.size.width / 2f, mid.y - l.size.height / 2f))
                }
            }
        }
        for ((i, b) in model.blocks.withIndex()) {
            val c = center(b.name)
            val t0 = model.tactic(arch, b.name)
            val tl = Offset(c.x - w / 2, c.y - h / 2)
            drawRoundRect(tacticColor.getValue(t0).copy(alpha = 0.22f), tl, Size(w, h), CornerRadius(10f * density))
            drawRoundRect(if (i == game.selected) Palette.ink else tacticColor.getValue(t0), tl, Size(w, h), CornerRadius(10f * density),
                style = Stroke((if (i == game.selected) 2.5f else 1.5f) * density))
            val name = measurer.measure(t(b.name), Type.style(13, FontWeight.SemiBold).copy(textAlign = TextAlign.Center),
                constraints = androidx.compose.ui.unit.Constraints(maxWidth = (w - 8f).toInt()))
            drawText(name, topLeft = Offset(c.x - name.size.width / 2f, c.y - h / 2 + 6f * density))
            val tac = measurer.measure(tacticTitle(t0), Type.style(11, color = Palette.secondary))
            drawText(tac, topLeft = Offset(c.x - tac.size.width / 2f, c.y + h / 2 - tac.size.height - 6f * density))
        }
    }
}

/** The quality scenarios: the downtime each may have and the downtime expected with this architecture. */
@Composable
private fun ScenarioTable(game: AvailabilityController) {
    val model = game.model
    val r = game.result
    Column(Modifier.fillMaxWidth().background(Palette.panel, RoundedCornerShape(10.dp)).padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row {
            Txt(t("Quality scenario"), Type.caption, Modifier.weight(1f))
            Txt(t("May be down"), Type.caption.copy(textAlign = TextAlign.End), Modifier.width(110.dp))
            Txt(t("Expected"), Type.caption.copy(textAlign = TextAlign.End), Modifier.width(100.dp))
        }
        for (s in model.scenarios) {
            val down = r.downtime.getValue(s.name)
            val ok = down <= s.maxDowntimeMinutes
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Txt(t(s.name), Type.style(15, FontWeight.SemiBold))
                    Txt(t(s.text), Type.caption)
                    Txt(t("Waits for: %@", model.uses(game.arch, s).joinToString(", ") { t(it) }), Type.style(12, color = Palette.secondary))
                }
                Txt(t("%@ a year", downtimeText(s.maxDowntimeMinutes.toDouble())), Type.callout.copy(textAlign = TextAlign.End), Modifier.width(110.dp))
                Txt(downtimeText(down), Type.style(15, FontWeight.SemiBold, if (ok) Palette.accent else Palette.red).copy(textAlign = TextAlign.End), Modifier.width(100.dp))
            }
        }
        Txt(t("A scenario is available only while every building block it waits for is: their availabilities multiply."), Type.caption)
    }
}

@Composable
private fun AvailabilityPanel(game: AvailabilityController, modifier: Modifier) {
    val model = game.model
    val b = game.block
    val (n, util) = model.sizing(game.arch, b)
    Column(modifier.background(Palette.panel).verticalScroll(rememberScrollState()).padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Txt(t(b.name), Type.heading)
        Txt(t(b.description), Type.style(15, color = Palette.secondary))
        Txt(t("%lld servers, on average %@ busy. It fails about %lld times a year.", n.toLong(), Format.wholePercent(util * 10), model.failuresPerYear.toLong()), Type.callout)
        for (tc in Tactic.entries) {
            val text = when (tc) {
                Tactic.SINGLE -> t("One set of servers; after a failure it is repaired and restarted.")
                Tactic.COLD_STANDBY -> t("A second set is prepared but off; the data are copied. After a failure it is started.")
                Tactic.WARM_STANDBY -> t("A second, smaller set runs and keeps the data current; it takes over after a failure.")
                Tactic.ACTIVE_ACTIVE -> t("Two full sets on two sites share the load; one carries on alone.")
            }
            val facts = t("%@ down per failure · %@ available", downtimeText(model.downtimePerFailure.getValue(tc).toDouble()),
                Format.number(model.availability(tc) * 100, 3) + " %")
            RuleOption(game.tactic(b) == tc, { game.setTactic(b, tc) }, tacticTitle(tc), "$text $facts")
        }
        HDivider()
        Txt(t("Decoupling"), Type.subheading)
        for (d in model.decouplings) {
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).clickable { game.toggle(d.id) }.padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Tick(d.id in game.arch.decouplings)
                    Txt(t(d.title), Type.style(14, FontWeight.SemiBold))
                }
                Txt(t(d.text), Type.caption)
            }
        }
        HDivider()
        Txt(t("Counted is a year of electricity, location-based. A standby that is off draws nothing; a running one does."), Type.caption)
        PrimaryButton(t("Put the architecture in place"), Modifier.fillMaxWidth(), enabled = !game.finished && game.started) { game.finish() }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AvailabilityEnd(game: AvailabilityController, onExit: () -> Unit, onOverview: (() -> Unit)?) {
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
                    StarRow(game.stars)
                }
                Txt("${Format.changePermille(-r.reductionPermille)} CO₂", Type.style(40, FontWeight.Bold, if (r.reductionPermille > 0) Palette.accent else Palette.red))
                Txt(t(texts.comparedWith), Type.style(17, color = Palette.secondary))
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    GoalLine(met.rules, 1, t(texts.rules))
                    GoalLine(met.reduction, 2, t("CO₂ %@ or better", Format.changePermille(-scenario.goals.reduction.toLong())))
                    GoalLine(met.special, 3, specialLong(scenario.goals.special))
                }
                if (!met.rules) Txt(t("No star because: %@", r.missed.joinToString("; ") { name ->
                    val s = model.scenarios.first { it.name == name }
                    t("%@ is down %@ (at most %@)", t(name), downtimeText(r.downtime.getValue(name)), downtimeText(s.maxDowntimeMinutes.toDouble()))
                }), Type.style(16, FontWeight.SemiBold, Palette.red))
                Table(listOf(t("Quality scenario"), t("May be down"), t("Expected")), model.scenarios.map {
                    listOf(t(it.name), downtimeText(it.maxDowntimeMinutes.toDouble()), downtimeText(r.downtime.getValue(it.name)))
                }, firstWidth = 0.5f)
                Table(listOf("", t("today"), t("your architecture")), listOf(
                    listOf(t("CO₂ a year, t"), tonnes(r.referenceKg), tonnes(r.planKg)),
                    listOf(t("Servers on"), Format.number(model.result(model.reference).serversOn), Format.number(r.serversOn)),
                ), firstWidth = 0.5f)
                Txt(t("What you did is called %@. %@", texts.term, t(texts.termText)), Type.style(15, color = Palette.secondary))
                Txt(t(texts.remark), Type.style(15, color = Palette.secondary))
                WhatIfCard(game.whatIf().map { (l, x) -> Triple(l, x.reductionPermille, model.stars(x, scenario.goals)) },
                    maxOf(Progress.stars(scenario.number), game.stars), t("The same portal, other architectures:"), scenario.number) { game.loadSolution() }
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

/** "What if": other plans with their CO₂ change and stars; plans with more stars than reached so far stay hidden (D25). */
@Composable
fun WhatIfCard(rows: List<Triple<String, Long, Int>>, best: Int, subtitle: String, scenario: Int, onLoad: (() -> Unit)? = null) {
    Card(Modifier.fillMaxWidth(), color = Palette.background, padding = 14.dp) {
        Txt(t("What if …"), Type.subheading)
        Txt(subtitle, Type.style(15, color = Palette.secondary))
        Spacer(Modifier.height(8.dp))
        var hidden = 0
        for ((label, reduction, stars) in rows) {
            val secret = stars > best && !Progress.solutionSeen(scenario)
            if (secret) hidden++
            Row(Modifier.padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                if (secret) Txt(t("A setting you have not found yet"), Type.style(15, color = Palette.secondary).copy(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic), Modifier.weight(0.6f))
                else Txt(label, Type.callout, Modifier.weight(0.6f))
                Txt(Format.changePermille(-reduction), Type.callout.copy(textAlign = TextAlign.End), Modifier.weight(0.22f))
                Box(Modifier.weight(0.18f), contentAlignment = Alignment.CenterEnd) { StarRow(stars, 11.dp) }
            }
        }
        if (hidden > 0) Txt(t("Settings with more stars than you have reached stay hidden until you find them."), Type.caption)
        SolutionBar(scenario, hidden, onLoad)
    }
}

/**
 * When a player does not find the solution: uncover the hidden settings of "What if" – and, where the scenario has no
 * clock, load the model solution into the screen. Afterwards a result in that scenario counts at most one star.
 */
@Composable
fun SolutionBar(scenario: Int, hidden: Int, onLoad: (() -> Unit)?, steps: List<String> = emptyList(), clock: Boolean = false,
    differences: List<GameController.Difference>? = null) {
    val seen = Progress.revealed(scenario)
    if (!seen && hidden == 0) return
    Spacer(Modifier.height(8.dp))
    if (!seen) {
        SecondaryButton(t("Show the solution")) { Progress.reveal(scenario) }
        return
    }
    if (differences != null) {
        Txt(t("What your run did differently"), Type.style(15, FontWeight.SemiBold))
        Spacer(Modifier.height(4.dp))
        if (differences.isEmpty()) Txt(t("Your settings at the end match the solution."), Type.callout)
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            for (d in differences) Bullet {
                if (d.mine == null) Txt(d.aspect, Type.callout)
                else {
                    Txt(d.aspect, Type.style(15, FontWeight.SemiBold))
                    Txt(t("You: %@", d.mine), Type.callout)
                    Txt(t("Solution: %@", d.theirs ?: ""), Type.callout)
                }
            }
        }
        Spacer(Modifier.height(14.dp))
    }
    if (steps.isNotEmpty()) {
        Txt(t("The solution"), Type.style(15, FontWeight.SemiBold))
        Spacer(Modifier.height(4.dp))
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            for (step in steps) Bullet { Txt(step, Type.callout) }
        }
        Spacer(Modifier.height(14.dp))
    }
    if (onLoad != null) {
        PrimaryButton(t("Put the solution into the task"), onClick = onLoad)
        Spacer(Modifier.height(8.dp))
        Txt(if (clock) t("A new run starts with the solution in the panel; press play and watch it.")
            else t("The solution goes into the screen; look at it and carry it out."), Type.caption)
    }
}

/** A list item with a hanging indent: the second line starts under the first word, not under the dot. */
@Composable
private fun Bullet(content: @Composable () -> Unit) {
    Row {
        Txt("•", Type.callout, Modifier.width(16.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) { content() }
    }
}
