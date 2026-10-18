package de.wannerit.loadshift.app

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.foundation.border
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import de.wannerit.loadshift.core.CacheModel
import de.wannerit.loadshift.core.Control
import de.wannerit.loadshift.core.GameTime
import de.wannerit.loadshift.core.Scenario
import de.wannerit.loadshift.core.SpecialGoal
import kotlinx.coroutines.delay

/** The play screen (docs/CONCEPT.md 10.1): top bar with the goals, hall and controls, timeline. */
@Composable
fun GameScreen(game: GameController, onExit: () -> Unit, onNext: ((Scenario) -> Unit)? = null, onOverview: (() -> Unit)? = null) {
    // The clock: one tick per 1/speed seconds while not paused.
    LaunchedEffect(game.speed) {
        val speed = game.speed
        if (speed == Speed.PAUSED) return@LaunchedEffect
        // At most 16 frames a second; faster speeds run several ticks per frame.
        val perFrame = maxOf(1, speed.ticksPerSecond / 16)
        while (true) {
            delay(1_000L * perFrame / speed.ticksPerSecond)
            repeat(perFrame) { if (game.speed == speed) game.step() }
            if (game.speed != speed) break
        }
    }
    @Suppress("UNUSED_VARIABLE") val redraw = game.version
    BoxWithConstraints(Modifier.fillMaxSize().background(Palette.background)) {
        val panelWidth = (maxWidth * 0.32f).coerceIn(300.dp, 380.dp)
        val timelineHeight = (maxHeight * (if (game.isCapstone) 0.46f else 0.3f)).coerceAtLeast(190.dp)
        Column(Modifier.fillMaxSize()) {
            TopBar(game, onExit)
            GoalBar(game)
            HDivider()
            Row(Modifier.fillMaxWidth().weight(1f)) {
                if (Control.PLACEMENT in game.scenario.controls) {
                    PlacementBoard(game, Modifier.weight(1f).fillMaxHeight())
                } else if (game.multiSite) {
                    // O7, O10: both sites side by side.
                    val sites = game.scenario.renewal?.sites ?: game.scenario.location?.sites ?: emptyList()
                    for ((i, site) in sites.withIndex()) {
                        Column(Modifier.weight(1f).fillMaxHeight()) {
                            Txt(t("%@ · grid on average %lld g/kWh", t(site.title), (site.region.annualMean + 5) / 10), Type.style(16, FontWeight.SemiBold), Modifier.padding(start = 16.dp, top = 12.dp))
                            HallView(game.simulationOf(i).servers, game.version, interactive = game.hasRenewal, batch = !game.hasRenewal, modifier = Modifier.weight(1f).fillMaxWidth(), compact = true)
                        }
                    }
                } else {
                    HallView(
                        game.simulation.servers, game.version, game.hasInteractive || game.hasTraining, game.hasBatch, Modifier.weight(1f).fillMaxHeight(),
                        cacheSlots = if (game.scenario.caching != null) CacheModel.MAX_SERVERS else game.scenario.inference?.gpuServers ?: 0,
                        cacheOn = game.scenario.caching?.servers(game.simulation.policy.cache) ?: game.simulation.history.lastOrNull()?.inference?.servers ?: 0,
                        cachePower = game.scenario.caching?.power(game.simulation.policy.cache)
                            ?: game.simulation.history.lastOrNull()?.inference?.let { it.kindWatts.sum() + it.idleWatts } ?: 0,
                        gpuRack = game.hasInference,
                        workLabel = if (game.hasPipeline) t("Build agent at work") else if (game.hasServices) t("Services at work") else null,
                    )
                }
                VDivider()
                ControlPanel(game, Modifier.width(panelWidth).fillMaxHeight())
            }
            HDivider()
            if (game.hasRenewal) QuarterChart(game, Modifier.fillMaxWidth().height(timelineHeight).background(Palette.panel))
            else if (game.scenario.location != null) {
                // O10: both sites side by side, each with the other's grid as a thin line.
                val sites = game.scenario.location!!.sites
                Row(Modifier.fillMaxWidth().height(timelineHeight).background(Palette.panel)) {
                    for (i in sites.indices) {
                        if (i > 0) VDivider()
                        TimelineChart(game.simulationOf(i), game.version, Modifier.weight(1f).fillMaxHeight(),
                            other = t(sites[1 - i].title) to game.simulationOf(1 - i), siteName = t(sites[i].title))
                    }
                }
            } else TimelineChart(game.simulation, game.version, Modifier.fillMaxWidth().height(timelineHeight).background(Palette.panel),
                lane = if (game.isCapstone) Lane.ALL else if (game.scenario.contract != null) when (game.tabs.getOrNull(game.panelTab)) {
                    GameController.PanelTab.SHOP -> Lane.LOAD
                    GameController.PanelTab.BATCH -> Lane.JOBS
                    else -> Lane.CONTRACT
                } else null)
        }
        if (game.showEnd) EndCard(game, onExit, onNext, onOverview)
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
            Column {
                Txt(t("Scenario %@", game.scenario.shownLabel), Type.caption)
                Txt(t(game.scenario.title), Type.style(17, FontWeight.SemiBold), maxLines = 1)
            }
        }
        Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
            val day = minOf(time.day - GameTime.START.day + 1, game.scenario.days)
            Txt(if (game.hasRenewal) t("Quarter %lld, year %lld", (day - 1) % 4 + 1, (day - 1) / 4 + 1) else t("Day %lld", day), Type.caption)
            Txt(time.clockText, Type.style(30, FontWeight.SemiBold).copy(fontFeatureSettings = "tnum"))
        }
        Row(Modifier.align(Alignment.CenterEnd), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(22.dp)) {
        if (!game.hasRenewal) Metric(t("Grid now"), "$intensity g/kWh", Palette.intensity(intensity))
        val totals = game.totals
        Metric(t("CO₂ so far"), if (game.hasRenewal) "${Format.tonnes(totals.emissions)} t" else "${Format.kilograms(totals.emissions)} kg")
        // E4: the shop's own servers are not the topic; the assistant's goals take their place.
        simulation.scenario.contract?.let {
            Metric(t("In the report"), "${Format.kilograms(game.totals.marketOperational)} kg")
        }
        simulation.scenario.services?.let { model ->
            val l = game.totals.meanLatency
            Metric(t("Response time"), t("%lld ms", l), if (l <= model.latencyLimit) Palette.ink else Palette.red)
        }
        simulation.scenario.pipeline?.let { model ->
            val f = game.totals.meanFeedback
            Metric(t("Feedback"), t("%@ min", Format.number(f / 10.0, 1)), if (f <= model.feedbackLimit * 10) Palette.ink else Palette.red)
        }
        simulation.scenario.web?.let { model ->
            Metric(t("Page"), t("%@ MB", Format.number(game.totals.meanPageKb / 1_000.0, 1)))
            val lcp = game.totals.meanLcp
            Metric(t("LCP"), t("%@ s", Format.number(lcp / 10.0, 1)), if (lcp <= model.lcpLimit) Palette.ink else Palette.red)
        }
        if (game.hasInteractive && !game.hasInference && !game.hasWeb) {
            val sla = minOf(totals.sla, totals.worstSiteSla)
            Metric(if (game.hasRenewal) t("SLA (lowest site)") else t("SLA"), Format.percent(sla),
                if (sla >= simulation.scenario.slaTarget) Palette.ink else Palette.red)
        }
        if (game.hasBatch) {
            Metric(t("Jobs done"), "${game.jobsDone} / ${game.jobsTotal}",
                if (totals.deadlinesMissed == 0) Palette.ink else Palette.red)
        }
        simulation.scenario.inference?.let { model ->
            val q = game.totals.meanAnswerQuality
            val o = game.totals.answersOnTime
            Metric(t("Quality"), Format.percent(q), if (q >= model.qualityTarget) Palette.ink else Palette.red)
            Metric(t("In time"), Format.percent(o), if (o >= model.onTimeTarget) Palette.ink else Palette.red)
        }
        simulation.scenario.streaming?.let { model ->
            val rating = simulation.totals.meanRating
            Metric(t("Rating"), Format.percent(rating), if (rating >= model.ratingTarget) Palette.ink else Palette.red)
        }
        simulation.scenario.training?.let { model ->
            Metric(t("Training"), Format.wholePercent(minOf(1_000L, simulation.totals.trainingDone * 1_000 / model.work).toInt()),
                if (simulation.totals.trainingFinishedAt != null) Palette.accent else Palette.ink)
        }
        if (simulation.scenario.cooling != null) {
            Metric(t("PUE now"), if (last == null || last.itPower == 0) "–" else Format.number(last.facilityPower.toDouble() / last.itPower, 2))
        }
        if (!game.hasRenewal) Metric(t("Power"), "${Format.kilowatts(last?.facilityPower ?: 0)} kW")
        }
    }
}

@Composable
private fun SpeedControl(game: GameController) {
    Row(Modifier.background(Palette.background, RoundedCornerShape(10.dp)).padding(3.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        // O7 runs quarter by quarter: pause and run; the others the four speeds.
        val speeds = if (game.hasRenewal) listOf(Speed.PAUSED, Speed.QUARTERS)
            else Speed.entries.filter { it != Speed.QUARTERS && (it != Speed.X64 || game.scenario.days >= 5) }
        for (speed in speeds) {
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
                } else if (speed == Speed.QUARTERS) {
                    Canvas(Modifier.size(14.dp)) {
                        val w = size.width
                        drawPath(androidx.compose.ui.graphics.Path().apply { moveTo(w * 0.15f, 0f); lineTo(w * 0.95f, w / 2); lineTo(w * 0.15f, w); close() }, color)
                    }
                } else {
                    Txt("${speed.ticksPerSecond}×", Type.style(17, FontWeight.SemiBold, color))
                }
            }
        }
    }
}

/** "−70 %" for whole percents, "−50,5 %" otherwise – the goal bar is short of room. */
private fun shortChange(permille: Long): String =
    Format.changePermille(permille).let { if (permille % 10 == 0L) it.replace(Regex("[,.]0(?=[^0-9]*%)"), "") else it }

private fun specialShort(goal: SpecialGoal): String = when (goal) {
    is SpecialGoal.MeanUtilization -> t("utilization ≥ %@", Format.wholePercent(goal.permille))
    is SpecialGoal.Reduction -> t("CO₂ %@", shortChange(-goal.permille.toLong()))
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
    val totals = game.totals
    val change = game.changeSoFar
    Row(
        Modifier.fillMaxWidth().background(Palette.panel).padding(horizontal = 20.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Txt(t("Goals"), Type.style(13, FontWeight.SemiBold, Palette.secondary))
        scenario.staleLimit?.let { limit ->
            GoalItem(totals.stalePerTenThousand <= limit, change != null, t("Stale answers: %@ (at most %@)",
                Format.number(totals.stale.toDouble() / maxOf(1, totals.requests), 2) + " ‰", Format.number(limit / 10.0, 1) + " ‰"))
        }
        if (game.hasInteractive) {
            if (!game.hasInference && !game.hasWeb) GoalItem(minOf(totals.sla, totals.worstSiteSla) >= scenario.slaTarget, change != null,
                if (game.hasRenewal) t("SLA ≥ %@ at each site", Format.percent(scenario.slaTarget)) else t("SLA ≥ %@", Format.percent(scenario.slaTarget)))
            GoalItem((change ?: 0) < 0, change != null,
                if (change == null) t("CO₂ vs. reference") else if (game.isCapstone && !game.isFinished) t("CO₂ expected: %@", Format.changePermille(change))
                else t("CO₂ vs. reference: %@", Format.changePermille(change)))
        }
        scenario.contract?.let { model ->
            val refCost = game.reference.history.getOrNull(game.simulation.history.size - 1)?.contract?.costSoFar ?: 0L
            val within = totals.energyCost * 1_000 <= refCost * model.budgetPermille
            GoalItem(within, refCost > 0, t("Electricity cost ≤ +%@", Format.wholePercent(model.budgetPermille - 1_000)))
        }
        scenario.services?.let { model ->
            GoalItem(totals.meanLatency <= model.latencyLimit, change != null, t("Tracking ≤ %lld ms", model.latencyLimit))
            GoalItem(totals.rulesBroken == 0, true, if (totals.rulesBroken == 0) t("Positions and traffic up to date") else t("Positions or traffic too old"))
            GoalItem((change ?: 0) < 0, change != null,
                if (change == null) t("CO₂ vs. reference") else t("CO₂ vs. reference: %@", Format.changePermille(change)))
        }
        scenario.pipeline?.let { model ->
            GoalItem(totals.meanFeedback <= model.feedbackLimit * 10, change != null, t("Feedback ≤ %lld min", model.feedbackLimit))
            GoalItem(totals.rulesBroken == 0, totals.rulesBroken > 0 || game.isFinished,
                if (totals.rulesBroken == 0) t("Release on Friday with tested UI") else t("Released without UI tests"))
            GoalItem((change ?: 0) < 0, change != null,
                if (change == null) t("CO₂ vs. reference") else t("CO₂ vs. reference: %@", Format.changePermille(change)))
        }
        scenario.web?.let { model ->
            GoalItem(totals.meanLcp <= model.lcpLimit, change != null, t("LCP ≤ %@ s", Format.number(model.lcpLimit / 10.0, 1)))
        }
        scenario.inference?.let { model ->
            GoalItem(totals.meanAnswerQuality >= model.qualityTarget, change != null,
                t("Quality of the answers ≥ %@", Format.percent(model.qualityTarget)))
            GoalItem(totals.answersOnTime >= model.onTimeTarget, change != null,
                t("%@ of the answers within %lld s", Format.percent(model.onTimeTarget), model.limitTenths / 10))
        }
        scenario.streaming?.let { model ->
            GoalItem(totals.meanRating >= model.ratingTarget, change != null,
                t("Viewers' rating %@ (at least %@)", Format.percent(totals.meanRating), Format.percent(model.ratingTarget)))
        }
        if (game.hasTraining) {
            val done = totals.trainingFinishedAt
            GoalItem(done != null, done != null || game.isFinished,
                if (done != null) t("Training done: %@", timeText(done)) else t("Training done by the end of day %lld", scenario.days))
            if (!game.hasInteractive) {
                // O8: CO₂ so far would favour waiting for the wind; CO₂ per percent of training done is fair (like the
                // batch intensity in O2 and O3) – the SCI idea on a small scale: emissions per unit of work.
                // Compared with the reference after the same amount of work, not over its whole run.
                val model = scenario.training!!
                fun perPercent(operational: Long, done: Long) = operational / (done * 100.0 / model.work)
                val mine = if (totals.trainingDone > 0) perPercent(totals.operational, totals.trainingDone) else null
                val refAt = game.reference.history.firstOrNull { it.trainingDone >= totals.trainingDone && it.trainingDone > 0 }
                    ?: game.reference.history.last()
                val refPerPercent = perPercent(refAt.emissionsSoFar - refAt.embodiedSoFar, refAt.trainingDone)
                GoalItem(mine != null && mine <= refPerPercent * 1.001, mine != null,
                    if (mine == null) t("CO₂ per percent of training: none trained yet")
                    else t("CO₂ per percent of training: %@ kg (reference %@ kg)", Format.number(mine / 1e9, 1), Format.number(refPerPercent / 1e9, 1)))
            }
        }
        if (scenario.location != null) {
            GoalItem(totals.rulesBroken == 0, true, if (totals.rulesBroken == 0) t("Customer data stays in Germany") else t("Customer data moved abroad"))
        }
        if (game.hasBatch) {
            val missed = totals.deadlinesMissed
            GoalItem(missed == 0, change != null, if (missed == 0) t("No deadline missed") else t("Deadlines missed: %lld", missed))
            if (!game.hasInteractive) {
                // CO₂ so far would favour waiting; the intensity of the work done is a fair measure.
                val mine = totals.meanBatchIntensityTenths
                val ref = game.referenceTotals.meanBatchIntensityTenths
                GoalItem(mine <= ref, totals.batchEnergy > 0,
                    if (totals.batchEnergy > 0) t("Batch work Ø %lld g/kWh (reference %lld)", (mine + 5) / 10, (ref + 5) / 10)
                    else t("Batch work: no job has run yet"))
            }
        }
        Spacer(Modifier.weight(1f))
        Row(verticalAlignment = Alignment.CenterVertically) {
            StarLabel(2, 13.dp)
            Txt(t("CO₂ %@", shortChange(-scenario.goals.reduction.toLong())), Type.callout)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            StarLabel(3, 13.dp)
            Txt(specialShort(scenario.goals.special), Type.callout)
        }
        StartOver(game)
    }
}

/** Back to the scenario's starting state – rules and choices too; a second click within three seconds confirms. */
@Composable
private fun StartOver(game: GameController) = StartOverChip { game.startOver() }

/** "Start over" with a confirming second click. */
@Composable
fun StartOverChip(onStartOver: () -> Unit) {
    var asking by remember { mutableStateOf(false) }
    LaunchedEffect(asking) {
        if (asking) { delay(3_000); asking = false }
    }
    Txt(
        if (asking) t("Really start over?") else "↺ " + t("Start over"),
        Type.style(13, FontWeight.SemiBold, if (asking) Color.White else Palette.ink),
        Modifier.clip(RoundedCornerShape(50)).background(if (asking) Palette.red else Palette.background)
            .border(1.dp, Palette.divider, RoundedCornerShape(50))
            .clickable { if (asking) { asking = false; onStartOver() } else asking = true }
            .padding(horizontal = 10.dp, vertical = 4.dp),
        maxLines = 1,
    )
}

@Composable
fun GoalItem(ok: Boolean, started: Boolean, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        if (started) CheckMark(ok, warning = !ok) else CheckMark(false)
        Txt(text, Type.callout, maxLines = 1)
    }
}

/** End of the scenario (CONCEPT.md 8.2, 8.3): stars, goals, the result next to the reference, "what if". */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EndCard(game: GameController, onExit: () -> Unit, onNext: ((Scenario) -> Unit)?, onOverview: (() -> Unit)? = null) {
    val scenario = game.scenario
    val you = game.totals
    val ref = game.referenceTotals
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
                    Txt(if (!met.rules) t("Not made") else if (scenario.days == 1) t("Day complete") else t("Scenario complete"), Type.title, Modifier.weight(1f))
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
                    if (game.hasRenewal) {
                        add(t("CO₂ (t)") to (Format.tonnes(you.emissions) to Format.tonnes(ref.emissions)))
                        add(t("of it embodied (t)") to (Format.tonnes(you.embodied) to Format.tonnes(ref.embodied)))
                        add(t("Old servers failed") to ("${you.failedServers}" to "${ref.failedServers}"))
                        add(t("Energy (MWh)") to (Format.number(you.facilityEnergy / 1e9, 0) to Format.number(ref.facilityEnergy / 1e9, 0)))
                    } else {
                        add(t("CO₂ (kg)") to (Format.kilograms(you.emissions) to Format.kilograms(ref.emissions)))
                        add(t("of it embodied (kg)") to (Format.kilograms(you.embodied) to Format.kilograms(ref.embodied)))
                        // E5: the network and the visitors' devices count too.
                        fun energy(x: de.wannerit.loadshift.core.Totals) = x.facilityEnergy + x.networkEnergy + x.deviceEnergy
                        add(t("Energy (kWh)") to (Format.kilowattHours(energy(you)) to Format.kilowattHours(energy(ref))))
                    }
                    if (scenario.cooling != null) add(t("PUE") to (Format.number(you.pue / 1_000.0, 2) to Format.number(ref.pue / 1_000.0, 2)))
                    if (scenario.staleLimit != null) {
                        add(t("Stale answers (‰)") to (Format.number(you.stale.toDouble() / maxOf(1, you.requests), 2) to Format.number(ref.stale.toDouble() / maxOf(1, ref.requests), 2)))
                    }
                    if (scenario.contract != null) {
                        add(t("CO₂ location-based (kg)") to (Format.kilograms(you.locationOperational) to Format.kilograms(ref.locationOperational)))
                        add(t("CO₂ in the report, market-based (kg)") to (Format.kilograms(you.marketOperational) to Format.kilograms(ref.marketOperational)))
                        add(t("CO₂ hourly matched (kg)") to (Format.kilograms(you.operational) to Format.kilograms(ref.operational)))
                        add(t("Electricity cost (€)") to (Format.number(you.energyCost / 1_000.0, 0) to Format.number(ref.energyCost / 1_000.0, 0)))
                    }
                    if (scenario.services != null) {
                        add(t("Response time of the tracking (ms)") to ("${you.meanLatency}" to "${ref.meanLatency}"))
                        add(t("Servers on (mean)") to (Format.number(you.meanServersOn / 1_000.0, 1) to Format.number(ref.meanServersOn / 1_000.0, 1)))
                        add(t("Hours with data too old") to ("${you.rulesBroken}" to "${ref.rulesBroken}"))
                    }
                    if (scenario.pipeline != null) {
                        add(t("Feedback (min)") to (Format.number(you.meanFeedback / 10.0, 1) to Format.number(ref.meanFeedback / 10.0, 1)))
                        add(t("Servers on (mean)") to (Format.number(you.meanServersOn / 1_000.0, 1) to Format.number(ref.meanServersOn / 1_000.0, 1)))
                        add(t("Release with tested UI") to ((if (you.rulesBroken == 0) t("yes") else t("no")) to (if (ref.rulesBroken == 0) t("yes") else t("no"))))
                    }
                    if (scenario.web != null) {
                        add(t("Page weight per visit (MB)") to (Format.number(you.meanPageKb / 1_000.0, 1) to Format.number(ref.meanPageKb / 1_000.0, 1)))
                        add(t("LCP (s)") to (Format.number(you.meanLcp / 10.0, 1) to Format.number(ref.meanLcp / 10.0, 1)))
                        fun dc(x: de.wannerit.loadshift.core.Totals) = Format.number((x.operational - x.networkOperational - x.deviceOperational + x.embodied) / 1e9, 0)
                        add(t("CO₂ data center (kg)") to (dc(you) to dc(ref)))
                        add(t("CO₂ network (kg)") to (Format.number(you.networkOperational / 1e9, 0) to Format.number(ref.networkOperational / 1e9, 0)))
                        add(t("CO₂ visitors' devices (kg)") to (Format.number(you.deviceOperational / 1e9, 0) to Format.number(ref.deviceOperational / 1e9, 0)))
                    }
                    if (game.hasInteractive && !game.hasInference && !game.hasWeb) {
                        add((if (game.hasRenewal) t("SLA (lowest site)") else t("SLA")) to (Format.percent(minOf(you.sla, you.worstSiteSla)) to Format.percent(minOf(ref.sla, ref.worstSiteSla))))
                        add(t("Servers on (mean)") to (Format.number(you.meanServersOn / 1_000.0, 1) to Format.number(ref.meanServersOn / 1_000.0, 1)))
                        add(t("Mean utilization") to (Format.wholePercent(you.meanUtilization) to Format.wholePercent(ref.meanUtilization)))
                    }
                    if (scenario.inference != null) {
                        add(t("Quality of the answers") to (Format.percent(you.meanAnswerQuality) to Format.percent(ref.meanAnswerQuality)))
                        add(t("Answers in time") to (Format.percent(you.answersOnTime) to Format.percent(ref.answersOnTime)))
                        fun gpu(sim: de.wannerit.loadshift.core.Simulation) = sim.history.sumOf { it.inference?.servers ?: 0 }.toDouble() / maxOf(1, sim.history.size)
                        add(t("GPU servers (mean)") to (Format.number(gpu(game.simulation), 1) to Format.number(gpu(game.reference), 1)))
                    }
                    if (scenario.streaming != null) add(t("Viewers' rating") to (Format.percent(you.meanRating) to Format.percent(ref.meanRating)))
                    if (game.hasTraining) {
                        add(t("Training done") to ((you.trainingFinishedAt?.let { timeText(it) } ?: t("not done")) to (ref.trainingFinishedAt?.let { timeText(it) } ?: t("not done"))))
                        add(t("Resumed after a pause") to ("${you.trainingRestarts}" to "${ref.trainingRestarts}"))
                    }
                    if (game.hasBatch) {
                        val n = game.jobsTotal
                        if (game.scenario.location != null) add(t("Data transfers (kWh)") to (Format.number(you.transferEnergy / 1e6, 0) to Format.number(ref.transferEnergy / 1e6, 0)))
                        add(t("Jobs on time") to ("${n - you.deadlinesMissed} / $n" to "${n - ref.deadlinesMissed} / $n"))
                        add(t("Batch work, mean g/kWh") to ("${(you.meanBatchIntensityTenths + 5) / 10}" to "${(ref.meanBatchIntensityTenths + 5) / 10}"))
                    }
                }
                Table(listOf("", t("You"), t("Reference")), rows.map { listOf(it.first, it.second.first, it.second.second) })
                Txt(t("What you did is called %@. %@", texts.term, t(texts.termText)), Type.style(15, color = Palette.secondary))
                Txt(t(texts.remark), Type.style(15, color = Palette.secondary))
                WhatIf(game)
            }
            // The buttons wrap into a second line when the card is too narrow for all of them.
            FlowRow(
                Modifier.fillMaxWidth().padding(horizontal = 30.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                // The next scenario, once a star has opened it.
                val sameTrack = Scenario.ALL.filter { it.track == scenario.track }
                val next = sameTrack.getOrNull(sameTrack.indexOf(scenario) + 1)?.takeIf { Progress.isUnlocked(it) }
                if (next != null && onNext != null) {
                    PrimaryButton(t("To scenario %@", next.shownLabel)) { onNext(next) }
                    SecondaryButton(t("Play again")) { game.restart() }
                } else {
                    PrimaryButton(t("Play again")) { game.restart() }
                }
                SecondaryButton(t("To the briefing"), onClick = onExit)
                if (onOverview != null) SecondaryButton(t("To the overview"), onClick = onOverview)
            }
        }
    }
}

/** A simple table: first column left, the others right. */
@Composable
fun Table(header: List<String>, rows: List<List<String>>, firstWidth: Float = 0.5f) {
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
    val you = game.totals
    val ref = game.referenceTotals
    val alternatives = remember(game.version) { game.whatIf() }
    Card(Modifier.fillMaxWidth(), color = Palette.background, padding = 14.dp) {
        Txt(t("What if …"), Type.subheading)
        Txt(t("The same run with exactly the same load and grid mix – only the rules differ:"), Type.style(15, color = Palette.secondary))
        Spacer(Modifier.height(8.dp))
        val pue = game.scenario.cooling != null
        val labelWeight = if (pue) 0.40f else 0.52f
        Row {
            Spacer(Modifier.weight(labelWeight))
            Txt(t("CO₂ vs. you"), Type.caption.copy(textAlign = androidx.compose.ui.text.style.TextAlign.End), Modifier.weight(0.18f))
            if (pue) Txt(t("PUE"), Type.caption.copy(textAlign = androidx.compose.ui.text.style.TextAlign.End), Modifier.weight(0.12f))
            Txt(if (game.hasBatch) t("Missed") else if (game.hasInference) t("Quality") else if (game.hasWeb) t("LCP") else if (game.hasPipeline) t("Feedback") else if (game.hasServices) t("Response") else t("SLA"), Type.caption.copy(textAlign = androidx.compose.ui.text.style.TextAlign.End), Modifier.weight(0.14f))
            Spacer(Modifier.weight(0.16f))
        }
        // Settings with more stars than the player has reached stay hidden: the second run should not be a recipe.
        val best = maxOf(Progress.stars(game.scenario.number), game.stars)
        val hidden = alternatives.count { game.scenario.stars(it.second, ref) > best }
        for ((label, totals) in alternatives) {
            val secret = game.scenario.stars(totals, ref) > best
            Row(Modifier.padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                if (secret) Txt(t("A setting you have not found yet"), Type.style(15, color = Palette.secondary).copy(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic), Modifier.weight(labelWeight))
                else Txt(label, Type.callout, Modifier.weight(labelWeight))
                Txt(Format.change(totals.emissions, you.emissions), Type.callout.copy(textAlign = androidx.compose.ui.text.style.TextAlign.End), Modifier.weight(0.18f))
                if (pue) Txt(Format.number(totals.pue / 1_000.0, 2), Type.callout.copy(textAlign = androidx.compose.ui.text.style.TextAlign.End), Modifier.weight(0.12f))
                val inference = game.scenario.inference
                val web = game.scenario.web
 val pipe = game.scenario.pipeline
                val svc = game.scenario.services
                if (svc != null) {
                    Txt(t("%lld ms", totals.meanLatency), Type.style(15, color = if (totals.meanLatency <= svc.latencyLimit) Palette.ink else Palette.red).copy(textAlign = androidx.compose.ui.text.style.TextAlign.End), Modifier.weight(0.14f))
                } else if (pipe != null) {
                    Txt(t("%@ min", Format.number(totals.meanFeedback / 10.0, 1)), Type.style(15, color = if (totals.meanFeedback <= pipe.feedbackLimit * 10) Palette.ink else Palette.red).copy(textAlign = androidx.compose.ui.text.style.TextAlign.End), Modifier.weight(0.14f))
                } else if (web != null) {
                    Txt(t("%@ s", Format.number(totals.meanLcp / 10.0, 1)), Type.style(15, color = if (totals.meanLcp <= web.lcpLimit) Palette.ink else Palette.red).copy(textAlign = androidx.compose.ui.text.style.TextAlign.End), Modifier.weight(0.14f))
                } else if (inference != null) {
                    Txt(Format.percent(totals.meanAnswerQuality), Type.style(15, color = if (totals.meanAnswerQuality >= inference.qualityTarget) Palette.ink else Palette.red).copy(textAlign = androidx.compose.ui.text.style.TextAlign.End), Modifier.weight(0.14f))
                } else if (game.hasBatch) {
                    Txt("${totals.deadlinesMissed}", Type.style(15, color = if (totals.deadlinesMissed == 0) Palette.ink else Palette.red).copy(textAlign = androidx.compose.ui.text.style.TextAlign.End), Modifier.weight(0.14f))
                } else {
                    Txt(Format.percent(totals.sla), Type.style(15, color = if (totals.sla >= game.scenario.slaTarget) Palette.ink else Palette.red).copy(textAlign = androidx.compose.ui.text.style.TextAlign.End), Modifier.weight(0.14f))
                }
                Box(Modifier.weight(0.16f), contentAlignment = Alignment.CenterEnd) {
                    StarRow(game.scenario.stars(totals, ref), 11.dp)
                }
            }
        }
        if (hidden > 0) Txt(t("Settings with more stars than you have reached stay hidden until you find them."), Type.caption)
    }
}
