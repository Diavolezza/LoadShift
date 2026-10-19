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
import de.wannerit.loadshift.core.Closed
import de.wannerit.loadshift.core.Open
import de.wannerit.loadshift.core.Scenario
import de.wannerit.loadshift.core.UpdateKind
import de.wannerit.loadshift.core.UpdatePlan
import de.wannerit.loadshift.core.UpdatesResult

/** State of E7: how each kind of update reaches the app, open and closed, and the two switches; a replay keeps the plan. */
class UpdatesController(val scenario: Scenario, fresh: Boolean = false) {
    val model = scenario.updates!!
    var plan by mutableStateOf((if (fresh) null else lastPlans[scenario.number]) ?: model.reference)
        private set
    var selected by mutableIntStateOf(0)
    var finished by mutableStateOf(false)
        private set

    val kind: UpdateKind get() = model.kinds[selected]
    val result: UpdatesResult get() = model.result(plan)
    val stars: Int get() = model.stars(result, scenario.goals)
    val started: Boolean get() = plan != model.reference || finished

    fun open(k: UpdateKind) = model.open(plan, k)
    fun closed(k: UpdateKind) = model.closed(plan, k)

    fun setOpen(k: UpdateKind, o: Open) { if (!finished) plan = plan.copy(open = plan.open + (k.name to o)) }
    fun setClosed(k: UpdateKind, c: Closed) { if (!finished) plan = plan.copy(closed = plan.closed + (k.name to c)) }
    fun toggleConditional() { if (!finished) plan = plan.copy(conditional = !plan.conditional) }
    fun toggleBundled() { if (!finished) plan = plan.copy(bundled = !plan.bundled) }

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

    fun whatIf(): List<Pair<String, UpdatesResult>> {
        val ref = model.reference
        val pushStatus = ref.copy(closed = ref.closed + ("Delivery status" to Closed.PUSH))
        return listOf(
            t("As today, with push for the delivery status") to model.result(pushStatus),
            t("The same with conditional requests and one background refresh") to model.result(pushStatus.copy(conditional = true, bundled = true)),
            t("A WebSocket for everything, open and closed") to model.result(UpdatePlan(model.kinds.associate { it.name to Open.Stream },
                mapOf("Delivery status" to Closed.SOCKET, "Offers" to Closed.SOCKET, "Unread messages" to Closed.SOCKET))),
            t("Push where changes are rare, a stream where they are many, nothing in the background that nobody needs") to model.result(model.example),
        )
    }

    private companion object {
        val lastPlans = mutableMapOf<Int, UpdatePlan>()
    }
}

fun openTitle(o: Open): String = when (o) {
    is Open.Poll -> t("every %lld s", o.seconds.toLong())
    Open.OnOpen -> t("on opening")
    Open.LongPoll -> t("long polling")
    Open.Stream -> t("stream")
    Open.Push -> t("push")
}

fun closedTitle(c: Closed): String = when (c) {
    Closed.NONE -> t("nothing")
    Closed.REFRESH -> t("every 15 min")
    Closed.SOCKET -> t("connection")
    Closed.PUSH -> t("push")
}

/** What a way does while the app is open. */
private fun openText(o: Open): String = when (o) {
    is Open.Poll -> t("The app asks every %lld seconds whether anything has changed – also when nothing has.", o.seconds.toLong())
    Open.OnOpen -> t("The app loads the data once, when it is opened.")
    Open.LongPoll -> t("The app asks, and the server holds back the answer until something changes or a minute is over; then the app asks again.")
    Open.Stream -> t("While the app is open, one connection (WebSocket) stays open, and the server sends each change at once.")
    Open.Push -> t("The server hands each change to the phone platform's push service, which delivers it – usually within seconds, without a guarantee, and throttled when messages come often.")
}

/** What a way does while the app is closed. */
private fun closedText(c: Closed, keepalive: Int): String = when (c) {
    Closed.NONE -> t("No updates until the app is opened.")
    Closed.REFRESH -> t("The operating system wakes the app every 15 minutes to ask.")
    Closed.SOCKET -> t("A connection stays open in the background too; a sign of life every %lld minutes wakes the phone.", keepalive.toLong())
    Closed.PUSH -> t("The push service wakes the phone only when something has changed.")
}

private fun secondsText(s: Int?): String = when {
    s == null -> "–"
    s < 120 -> t("%lld s", s.toLong())
    else -> t("%lld min", (s / 60).toLong())
}

@Composable
fun UpdatesScreen(game: UpdatesController, onExit: () -> Unit, onOverview: (() -> Unit)? = null) {
    BoxWithConstraints(Modifier.fillMaxSize().background(Palette.background)) {
        val panelWidth = (maxWidth * 0.3f).coerceIn(if (maxWidth < 1000.dp) 270.dp else 300.dp, 380.dp)
        val low = maxHeight < 640.dp
        val compact = maxWidth < 1150.dp
        Column(if (low) Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).height(820.dp) else Modifier.fillMaxSize()) {
            val r = game.result
            PlayTopBar(game.scenario, onExit, compact, t("A year"), if (game.finished) t("Plan carried out") else t("The plan")) {
                Metric(t("CO₂ a year"), t("%@ t", tonnes(r.planKg)))
                Metric(t("Compared with today"), Format.changePermille(-r.reductionPermille))
                Metric(t("Of it on the phones"), t("%@ t", tonnes(r.phonesKg)))
                Metric(t("Fresh enough"), t("%lld of %lld", (game.model.kinds.size - r.missed.size).toLong(), game.model.kinds.size.toLong()),
                    if (r.missed.isNotEmpty()) Palette.red else Palette.ink)
            }
            UpdatesGoals(game, compact)
            HDivider()
            Row(Modifier.fillMaxWidth().weight(1f)) {
                Column(Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState()).padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    KindTable(game)
                    Split(game)
                }
                VDivider()
                UpdatesPanel(game, Modifier.width(panelWidth).fillMaxHeight())
            }
        }
        if (game.finished) UpdatesEnd(game, onExit, onOverview)
        RotateHint(maxWidth, maxHeight)
    }
}

@Composable
private fun UpdatesGoals(game: UpdatesController, compact: Boolean) {
    val scenario = game.scenario
    val r = game.result
    Row(
        Modifier.fillMaxWidth().background(Palette.panel).then(if (compact) Modifier.horizontalScroll(rememberScrollState()) else Modifier)
            .padding(horizontal = 20.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Txt(t("Goals"), Type.style(13, FontWeight.SemiBold, Palette.secondary))
        GoalItem(r.missed.isEmpty(), r.missed.isNotEmpty() || game.finished, t("Every kind of update as fresh as required"))
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

/** The five kinds of update: how they reach the app while it is open and while it is closed, and how fresh they are. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun KindTable(game: UpdatesController) {
    val model = game.model
    val r = game.result
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(Modifier.padding(horizontal = 12.dp)) {
            Txt(t("Update"), Type.caption, Modifier.weight(1f))
            Txt(t("While the app is open"), Type.caption, Modifier.weight(1.3f))
            Txt(t("While it is closed"), Type.caption, Modifier.weight(1.1f))
            Txt(t("Fresh within"), Type.caption.copy(textAlign = TextAlign.End), Modifier.width(92.dp))
        }
        for ((i, k) in model.kinds.withIndex()) {
            val missed = k.name in r.missed
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                    .background(if (i == game.selected) Palette.accent.copy(alpha = 0.08f) else Palette.panel)
                    .clickable { game.selected = i }.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Txt(t(k.name), Type.style(15, FontWeight.SemiBold))
                    Txt(t(k.description), Type.caption, maxLines = 3)
                }
                FlowRow(Modifier.weight(1.3f), horizontalArrangement = Arrangement.spacedBy(5.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    for (o in k.openOptions) Chip(openTitle(o), game.open(k) == o, !game.finished) { game.selected = i; game.setOpen(k, o) }
                }
                FlowRow(Modifier.weight(1.1f).padding(start = 8.dp), horizontalArrangement = Arrangement.spacedBy(5.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    for (c in k.closedOptions) Chip(closedTitle(c), game.closed(k) == c, !game.finished) { game.selected = i; game.setClosed(k, c) }
                }
                Column(Modifier.width(92.dp), horizontalAlignment = Alignment.End) {
                    val color = if (missed) Palette.red else Palette.ink
                    Txt(t("open %@", secondsText(r.openSeconds[k.name])), Type.style(12, FontWeight.SemiBold, color))
                    if (k.closedOptions.size > 1) Txt(t("closed %@", secondsText(r.closedSeconds[k.name])), Type.style(12, FontWeight.SemiBold, color))
                }
            }
        }
    }
}

/** Where the CO₂ comes from: the phones, the servers, the network, the rest of the app – today and with the plan. */
@Composable
private fun Split(game: UpdatesController) {
    val r = game.result
    val r0 = game.model.result(game.model.reference)
    val parts = listOf(t("Phones") to Palette.demand, t("Servers") to Palette.amber, t("Network") to Palette.cache, t("Rest of the app") to Color(0xFFB8C0BC))
    Column(Modifier.fillMaxWidth().background(Palette.panel, RoundedCornerShape(10.dp)).padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Txt(t("CO₂ a year, t – the rest of the app (browsing, ordering) stays as it is"), Type.style(13, FontWeight.SemiBold))
        val maxKg = maxOf(r0.planKg, r.planKg).toFloat()
        for ((label, x) in listOf(t("today") to r0, t("plan") to r)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Txt(label, Type.small, Modifier.width(40.dp))
                Row(Modifier.weight(1f).height(16.dp)) {
                    val values = listOf(x.phonesKg, x.serversKg, x.networkKg, x.otherKg)
                    for ((v, p) in values.zip(parts)) {
                        val share = v / maxKg
                        if (share > 0.002f) Box(Modifier.weight(share).fillMaxHeight().background(p.second))
                    }
                    val rest = 1f - values.sum() / maxKg
                    if (rest > 0.002f) Spacer(Modifier.weight(rest))
                }
                Txt(tonnes(x.planKg), Type.style(12, FontWeight.SemiBold), Modifier.widthIn(min = 40.dp))
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            for ((label, color) in parts) LegendSwatch(color, label)
        }
    }
}

@Composable
private fun UpdatesPanel(game: UpdatesController, modifier: Modifier) {
    val model = game.model
    val k = game.kind
    Column(modifier.background(Palette.panel).verticalScroll(rememberScrollState()).padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Txt(t(k.name), Type.heading)
        Txt(t(k.description), Type.style(15, color = Palette.secondary))
        Txt(t("%@ times a year; each time open %lld min, closed %lld min; %@ changes; an answer is %lld KB.",
            Format.number(k.periodsPerYear.toDouble()), k.openMinutes.toLong(), k.closedMinutes.toLong(),
            Format.number(k.changesTenths / 10.0, if (k.changesTenths % 10 == 0) 0 else 1), k.payloadKb.toLong()), Type.callout)
        Txt(t("Required: while open %@, while closed %@.",
            k.openFreshness?.let { t("within %@", secondsText(it)) } ?: t("current when opened"),
            k.closedFreshness?.let { t("within %@", secondsText(it)) } ?: t("nothing")), Type.style(15, FontWeight.SemiBold))
        HDivider()
        Txt(t("While the app is open"), Type.subheading)
        for (o in k.openOptions) {
            val fresh = model.openSeconds(game.plan.copy(open = game.plan.open + (k.name to o)), k)
            RuleOption(game.open(k) == o, { game.setOpen(k, o) }, openTitle(o).replaceFirstChar { it.uppercase() },
                openText(o) + " " + (fresh?.let { t("Fresh within %@.", secondsText(it)) } ?: t("Fresh only when opened.")))
        }
        if (k.closedOptions.size > 1) {
            Txt(t("While it is closed"), Type.subheading)
            Txt(t("A phone that is woken up switches its radio on for some seconds – about %@ J each time, whether there is news or not.", Format.number(model.wakeJ)), Type.caption)
            for (c in k.closedOptions) {
                val fresh = model.closedSeconds(game.plan.copy(closed = game.plan.closed + (k.name to c)), k)
                RuleOption(game.closed(k) == c, { game.setClosed(k, c) }, closedTitle(c).replaceFirstChar { it.uppercase() },
                    closedText(c, model.keepaliveMinutes) + " " + (fresh?.let { t("Fresh within %@.", secondsText(it)) } ?: t("Nothing reaches the closed app.")))
            }
        }
        HDivider()
        Txt(t("For all kinds"), Type.subheading)
        for ((on, title, text, toggle) in listOf(
            Quadruple(game.plan.conditional, t("Conditional requests"), t("The app sends the version it has (ETag); if nothing has changed, the answer is a short \"304 Not Modified\" instead of the full data."), game::toggleConditional),
            Quadruple(game.plan.bundled, t("One background refresh for all"), t("Kinds that refresh in the background share one wake-up every 15 minutes and one combined answer, instead of each waking the phone on its own."), game::toggleBundled),
        )) {
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).clickable { toggle() }.padding(vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Tick(on)
                    Txt(title, Type.style(14, FontWeight.SemiBold))
                }
                Txt(text, Type.caption)
            }
        }
        HDivider()
        Txt(t("Counted is a year: the phones' energy, the servers' with a PUE of %@, the network's by the data sent, location-based.", Format.number(model.pueHundredths / 100.0, 2)), Type.caption)
        PrimaryButton(t("Carry out the plan"), Modifier.fillMaxWidth(), enabled = !game.finished && game.started) { game.finish() }
    }
}

private data class Quadruple<A, B, C, D>(val a: A, val b: B, val c: C, val d: D)

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun UpdatesEnd(game: UpdatesController, onExit: () -> Unit, onOverview: (() -> Unit)?) {
    val scenario = game.scenario
    val model = game.model
    val r = game.result
    val r0 = model.result(model.reference)
    val met = model.goalsMet(r, scenario.goals)
    val texts = ScenarioTexts.of(scenario)
    var closed by remember { mutableStateOf(false) }
    if (closed) {
        Box(Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.BottomCenter) {
            PrimaryButton(t("Show the result")) { closed = false }
        }
        return
    }
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
                if (!met.rules) Txt(t("No star because: %@", t("not fresh enough: %@", r.missed.joinToString(", ") { t(it) })), Type.style(16, FontWeight.SemiBold, Palette.red))
                Table(listOf("", t("today"), t("your plan")), listOf(
                    listOf(t("Phones, t CO₂"), tonnes(r0.phonesKg), tonnes(r.phonesKg)),
                    listOf(t("Servers, t CO₂"), tonnes(r0.serversKg), tonnes(r.serversKg)),
                    listOf(t("Network, t CO₂"), tonnes(r0.networkKg), tonnes(r.networkKg)),
                    listOf(t("Rest of the app, t CO₂"), tonnes(r0.otherKg), tonnes(r.otherKg)),
                ), firstWidth = 0.5f)
                Txt(t("What you did is called %@. %@", texts.term, t(texts.termText)), Type.style(15, color = Palette.secondary))
                Txt(t(texts.remark), Type.style(15, color = Palette.secondary))
                WhatIfCard(game.whatIf().map { (l, x) -> Triple(l, x.reductionPermille, model.stars(x, scenario.goals)) },
                    maxOf(Progress.stars(scenario.number), game.stars), t("The same app, other ways to learn about changes:"), scenario.number) { game.loadSolution() }
            }
            FlowRow(
                Modifier.fillMaxWidth().padding(horizontal = 30.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                PrimaryButton(t("Play again")) { game.restart() }
                SecondaryButton(t("Look at the plan")) { closed = true }
                SecondaryButton(t("To the briefing"), onClick = onExit)
                if (onOverview != null) SecondaryButton(t("To the overview"), onClick = onOverview)
            }
        }
    }
}
