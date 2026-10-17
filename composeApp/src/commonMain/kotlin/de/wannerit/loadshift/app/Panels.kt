package de.wannerit.loadshift.app

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import de.wannerit.loadshift.core.BatchJob
import de.wannerit.loadshift.core.BatchMode
import de.wannerit.loadshift.core.CapacityMode
import de.wannerit.loadshift.core.Policy
import de.wannerit.loadshift.core.BatchJobState
import de.wannerit.loadshift.core.BatchPlanner
import de.wannerit.loadshift.core.Control
import de.wannerit.loadshift.core.GameTime
import de.wannerit.loadshift.core.ServerState

/** The controls on the right (docs/CONCEPT.md 10.1): only what the scenario unlocks. */
@Composable
fun ControlPanel(game: GameController, modifier: Modifier = Modifier) {
    Column(
        modifier.background(Palette.panel).verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        if (Control.BATCH_BY_HAND in game.scenario.controls || Control.BATCH_RULES in game.scenario.controls) {
            BatchPanel(game)
        } else if (Control.CACHING in game.scenario.controls) {
            CachePanel(game)
            NowBox(game)
        } else if (Control.SPRINTS in game.scenario.controls) {
            SprintPanel(game)
            NowBox(game)
        } else if (Control.PLACEMENT in game.scenario.controls) {
            PlacementPanel(game)
        } else if (Control.LOCATION in game.scenario.controls) {
            LocationPanel(game)
        } else if (Control.STREAMING in game.scenario.controls) {
            StreamingPanel(game)
        } else if (Control.TRAINING in game.scenario.controls) {
            TrainingPanel(game)
        } else if (Control.RENEWAL in game.scenario.controls) {
            RenewalPanel(game)
        } else if (Control.COOLING in game.scenario.controls) {
            CoolingPanel(game)
            CapacityRules(game)
            NowBox(game)
        } else if (Control.CAPACITY_RULES in game.scenario.controls) {
            CapacityRules(game)
            NowBox(game)
        } else {
            Txt(t("Capacity"), Type.heading)
            ManualCapacity(game)
            NowBox(game)
        }
    }
}

/** Scenario 1: switch servers on and off by hand; takes effect at once. */
@Composable
fun ManualCapacity(game: GameController) {
    val wanted = game.wantedServers
    val total = game.scenario.serverCount
    val perServer = game.scenario.fleet.firstOrNull()?.kind?.capacity ?: 0
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Txt(t("An idle server still draws about a quarter of its maximum power. Switch off what is not needed – but a server you switch on takes 30 minutes before it can work."), Type.style(15, color = Palette.secondary))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally), verticalAlignment = Alignment.CenterVertically) {
            RoundButton(plus = false, enabled = wanted > 0 && !game.isFinished) { game.setWantedServers(wanted - 1) }
            Column(Modifier.widthIn(min = 110.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Txt(wanted.toString(), Type.style(44, FontWeight.Bold))
                Txt(t("of %lld servers on", total), Type.caption)
            }
            RoundButton(plus = true, enabled = wanted < total && !game.isFinished) { game.setWantedServers(wanted + 1) }
        }
        Txt(t("Capacity: %lld load units", wanted * perServer), Type.subheading.copy(textAlign = TextAlign.Center), Modifier.fillMaxWidth())
        Txt(t("One server handles %lld load units (%lld,000 requests per 15 minutes). Above 85 %% utilization requests get slow.", perServer, perServer), Type.callout)
        Txt(t("Takes effect at once. Switched-off servers stop immediately; switched-on servers boot for 30 minutes."), Type.caption)
    }
}

/** The situation right now: running servers, load and capacity, the expected load in an hour. */
@Composable
fun NowBox(game: GameController) {
    val simulation = game.simulation
    val running = simulation.servers.count { it.state.isOn }
    val booting = simulation.servers.count { it.state is ServerState.Booting }
    val last = simulation.history.lastOrNull()
    val demandNow = last?.demand ?: simulation.scenario.interactive.expected(simulation.tick)
    val capacity = last?.capacity ?: simulation.readyCapacity
    val served = last?.served ?: minOf(demandNow, capacity)
    val inAnHour = simulation.scenario.interactive.expected(simulation.tick + GameTime.TICKS_PER_HOUR)
    val caching = simulation.scenario.caching
    val itPower = simulation.servers.sumOf { it.power() } + (caching?.power(simulation.policy.cache) ?: 0)
    Card(Modifier.fillMaxWidth(), color = Palette.background, padding = 14.dp) {
        Txt(t("Right now"), Type.subheading)
        Spacer(Modifier.padding(4.dp))
        val rows = listOf(
            t("Servers working") to (if (booting > 0) "$running (+$booting)" else "$running"),
            t("Load / capacity") to "$demandNow / $capacity",
            t("Utilization") to (if (capacity == 0) "–" else Format.wholePercent(served * 1_000 / capacity)),
            t("Expected load in 1 h") to "$inAnHour",
            t("IT power") to "${Format.kilowatts(itPower)} kW",
        ) + listOfNotNull(caching?.let { t("Cache servers") to "${it.servers(simulation.policy.cache)}" })
        for ((label, value) in rows) {
            Row(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                Txt(label, Type.callout, Modifier.weight(1f))
                Txt(value, Type.style(15, FontWeight.SemiBold))
            }
        }
    }
}

/** "Day 2, 13:30" */
fun timeText(tick: Int): String {
    val time = GameTime(tick)
    return t("Day %lld, %@", time.day - GameTime.START.day + 1, time.clockText)
}

/** Scenario 2: plan the batch jobs by hand. A planned job starts by itself, if enough servers are free. */
@Composable
fun BatchPanel(game: GameController) {
    val simulation = game.simulation
    val busy = simulation.servers.count { it.job != null }
    val byRule = Control.BATCH_RULES in game.scenario.controls
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        if (byRule) {
            BatchRules(game)
            HDivider()
        }
        Txt(t("Batch jobs"), Type.heading)
        if (!byRule) Txt(t("Each job needs servers for some hours and must be finished by its deadline. When it starts, its servers switch on and boot for 30 minutes first; servers without a job are off."), Type.style(15, color = Palette.secondary))
        Txt(t("Servers in use: %lld of %lld", busy, simulation.batchServerCount), Type.subheading)
        game.urgentJob?.let { urgent ->
            val spec = simulation.jobs[urgent].spec
            Row(
                Modifier.fillMaxWidth().background(Palette.red.copy(alpha = 0.1f), RoundedCornerShape(10.dp)).padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                CheckMark(ok = false, warning = true)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Txt(t("%@ has no start time and must start by %@, or it misses its deadline.", t(spec.name), timeText(spec.latestStart)), Type.callout)
                    Txt(t("OK"), Type.style(15, FontWeight.SemiBold, Palette.accent), Modifier.clip(RoundedCornerShape(6.dp)).clickable { game.dismissUrgent() }.padding(horizontal = 4.dp))
                }
            }
        }
        for (job in simulation.jobs) JobRow(game, job)
    }
}

@Composable
private fun JobRow(game: GameController, job: BatchJob) {
    val spec = job.spec
    val now = game.simulation.tick
    Card(Modifier.fillMaxWidth(), color = Palette.background, padding = 12.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Txt(t(spec.name), Type.subheading, Modifier.weight(1f))
            JobStatus(game, job, now)
        }
        Txt(t("%lld servers · %lld h · due %@", spec.servers, spec.duration / GameTime.TICKS_PER_HOUR, timeText(spec.deadline)), Type.caption)
        if (!job.state.isStarted && Control.BATCH_RULES in game.scenario.controls) {
            Txt(t("start by %@", timeText(spec.latestStart)), Type.caption)
        } else if (!job.state.isStarted) {
            Spacer(Modifier.padding(3.dp))
            val start = job.plannedStart
            if (start != null) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    RoundButton(plus = false, size = 36.dp, enabled = start - GameTime.TICKS_PER_HOUR >= maxOf(now, spec.arrival)) {
                        game.plan(job.id, start - GameTime.TICKS_PER_HOUR)
                    }
                    Column(Modifier.widthIn(min = 110.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Txt(t("Start"), Type.small)
                        Txt(timeText(start), Type.style(15, FontWeight.SemiBold))
                    }
                    RoundButton(plus = true, size = 36.dp, enabled = start + GameTime.TICKS_PER_HOUR <= game.scenario.endTick - spec.span) {
                        game.plan(job.id, start + GameTime.TICKS_PER_HOUR)
                    }
                    Spacer(Modifier.weight(1f))
                    Txt("×", Type.style(17, color = Palette.secondary), Modifier.clip(CircleShape).clickable { game.unplan(job.id) }.padding(6.dp))
                }
                if (start > spec.latestStart) {
                    Txt(t("Too late for the deadline – start by %@.", timeText(spec.latestStart)), Type.style(13, FontWeight.SemiBold, Palette.red))
                } else {
                    val mean = BatchPlanner.meanForecast(spec, start, game.simulation.grid, now)
                    Txt(t("Forecast while it runs: %lld g/kWh · start by %@", (mean + 5) / 10, timeText(spec.latestStart)), Type.caption)
                }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SecondaryButton(t("Plan"), enabled = !game.isFinished) { game.plan(job.id, game.suggestedStart(job.id)) }
                    if (spec.arrival <= now) SecondaryButton(t("Start now"), enabled = !game.isFinished) { game.plan(job.id, now) }
                }
                Txt(t("start by %@", timeText(spec.latestStart)), Type.caption)
            }
        }
    }
}

@Composable
private fun JobStatus(game: GameController, job: BatchJob, now: Int) {
    game.simulation // redraws when the job changes without a tick (planned, unplanned)
    val byRule = Control.BATCH_RULES in game.scenario.controls
    val (text, color) = when (val state = job.state) {
        BatchJobState.Waiting ->
            if (job.spec.arrival > now) t("arrives %@", timeText(job.spec.arrival)) to Palette.secondary
            else (if (byRule) t("waiting") else t("not planned")) to (if (job.missedDeadline(now)) Palette.red else Palette.secondary)
        is BatchJobState.Planned ->
            (if (byRule && state.start > now) t("planned %@", timeText(state.start)) else t("planned")) to Palette.accent
        is BatchJobState.Started -> t("booting") to Palette.demand
        is BatchJobState.Running -> t("runs until %@", GameTime(state.endsAt).clockText) to Palette.amber
        is BatchJobState.Done ->
            if (state.at > job.spec.deadline) t("late, %@", GameTime(state.at).clockText) to Palette.red
            else t("done %@", GameTime(state.at).clockText) to Palette.accent
    }
    Box(Modifier.background(color.copy(alpha = 0.12f), RoundedCornerShape(50)).padding(horizontal = 10.dp, vertical = 3.dp)) {
        Txt(text, Type.style(13, FontWeight.SemiBold, if (color == Palette.amber) Color(0xFFB0741A) else color))
    }
}

/** Scenario 3: a rule decides when the jobs start. The player edits a draft and applies it. */
@Composable
private fun BatchRules(game: GameController) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Txt(t("Batch rule"), Type.heading)
        Txt(t("A rule decides when each job starts – you no longer plan every job by hand. Choose a rule and apply it; you can change it at any time."), Type.style(15, color = Palette.secondary))
        if (game.forecastNews) ForecastNews(game)
        RuleOption(game.draftKind == GameController.RuleKind.ASAP, { game.draftKind = GameController.RuleKind.ASAP }, t("As soon as possible"), t("Every job starts when it arrives."))
        RuleOption(game.draftKind == GameController.RuleKind.HOUR, { game.draftKind = GameController.RuleKind.HOUR }, t("At a fixed hour"), t("Every job starts at the next %@ after it arrives.", GameTime.at(0, game.draftHour).clockText)) {
            Stepper(GameTime.at(0, game.draftHour).clockText,
                minus = { game.draftHour = (game.draftHour + 23) % 24 }, plus = { game.draftHour = (game.draftHour + 1) % 24 })
        }
        RuleOption(game.draftKind == GameController.RuleKind.GREENEST, { game.draftKind = GameController.RuleKind.GREENEST }, t("Greenest hours (forecast)"), t("Every job is planned into the hours with the lowest forecast before its deadline, once, when you apply the rule.")) {
            val active = game.simulation.policy.batch == BatchMode.Greenest
            val planned = game.simulation.plannedWithForecastOf
            if (active && planned != null) {
                Txt(t("Planned with the forecast of %@.", timeText(planned)), Type.caption)
                SecondaryButton(t("Plan again with the latest forecast"), enabled = !game.isFinished) { game.replan() }
            }
        }
        RuleOption(game.draftKind == GameController.RuleKind.THRESHOLD, { game.draftKind = GameController.RuleKind.THRESHOLD }, t("Below a threshold"), t("A job starts as soon as the grid is at or below %lld g/kWh – the value now, not the forecast.", game.draftThreshold)) {
            val range = Policy.THRESHOLD_RANGE
            Stepper("${game.draftThreshold} g/kWh",
                minus = { game.draftThreshold = maxOf(range.first, game.draftThreshold - 10) },
                plus = { game.draftThreshold = minOf(range.last, game.draftThreshold + 10) })
        }
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).clickable { game.draftSafety = !game.draftSafety }.padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Tick(game.draftSafety)
            Column(Modifier.weight(1f)) {
                Txt(t("Deadline safety"), Type.style(15, FontWeight.SemiBold))
                Txt(t("A job that would otherwise miss its deadline starts when there is just enough time left (with a fixed hour and a threshold)."), Type.caption)
            }
        }
        ApplyRow(game) { ruleText(it) }
    }
}

/** "Apply" and what runs now. */
@Composable
fun ApplyRow(game: GameController, describe: (Policy) -> String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        PrimaryButton(t("Apply"), enabled = !game.draftIsActive && !game.isFinished) { game.applyDraft() }
        // A change that is not applied yet is easy to overlook: say so in amber.
        Txt(
            if (game.draftIsActive) t("This rule is active.") else t("Not applied yet – active: %@", describe(game.simulation.policy)),
            Type.style(14, if (game.draftIsActive) FontWeight.Normal else FontWeight.SemiBold, if (game.draftIsActive) Palette.accent else Color(0xFFB0741A)), Modifier.weight(1f),
        )
    }
}

/** Scenario 4: a rule for the capacity – all on, a fixed number, autoscaling with an optional scheduled minimum. */
@Composable
private fun CapacityRules(game: GameController) {
    val perServer = game.scenario.fleet.firstOrNull()?.kind?.capacity ?: 0
    val total = game.scenario.serverCount
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Txt(t("Capacity rule"), Type.heading)
        Txt(t("A rule switches the servers – you no longer do it by hand. One server handles %lld load units; it takes 30 minutes to boot.", perServer), Type.style(15, color = Palette.secondary))
        val kind = game.capKind
        RuleOption(kind == GameController.CapacityKind.ALL_ON, { game.capKind = GameController.CapacityKind.ALL_ON },
            t("All on"), t("All %lld servers run all the time.", total))
        RuleOption(kind == GameController.CapacityKind.FIXED, { game.capKind = GameController.CapacityKind.FIXED },
            t("Fixed number"), t("%lld servers run all the time (%lld load units).", game.capFixed, game.capFixed * perServer)) {
            Stepper(t("%lld servers", game.capFixed),
                minus = { game.capFixed = maxOf(1, game.capFixed - 1) }, plus = { game.capFixed = minOf(total, game.capFixed + 1) })
        }
        RuleOption(kind == GameController.CapacityKind.AUTOSCALE, { game.capKind = GameController.CapacityKind.AUTOSCALE },
            t("Autoscaling"), t("Keeps enough servers on for the expected load of the next hour – or the measured load, if it is higher – at %@ utilization. The rest is headroom.", Format.wholePercent(game.capTarget))) {
            val range = Policy.TARGET_RANGE
            Txt(t("Target utilization"), Type.caption)
            Stepper(Format.wholePercent(game.capTarget),
                minus = { game.capTarget = maxOf(range.first, game.capTarget - 50) }, plus = { game.capTarget = minOf(range.last, game.capTarget + 50) })
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).clickable { game.capMinimumOn = !game.capMinimumOn }.padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Tick(game.capMinimumOn)
                Column(Modifier.weight(1f)) {
                    Txt(t("Minimum in a time window"), Type.style(15, FontWeight.SemiBold))
                    Txt(t("At least %lld servers every day from %@ to %@ (scheduled scaling).", game.capMinimum,
                        GameTime.at(0, game.capFrom).clockText, GameTime.at(0, game.capTo % 24).clockText), Type.caption)
                }
            }
            if (game.capMinimumOn) {
                Stepper(t("%lld servers", game.capMinimum),
                    minus = { game.capMinimum = maxOf(1, game.capMinimum - 1) }, plus = { game.capMinimum = minOf(total, game.capMinimum + 1) })
                Stepper(t("from %@", GameTime.at(0, game.capFrom).clockText),
                    minus = { game.capFrom = maxOf(0, game.capFrom - 1) }, plus = { game.capFrom = minOf(game.capTo - 1, game.capFrom + 1) })
                Stepper(t("to %@", GameTime.at(0, game.capTo % 24).clockText),
                    minus = { game.capTo = maxOf(game.capFrom + 1, game.capTo - 1) }, plus = { game.capTo = minOf(24, game.capTo + 1) })
            }
        }
        ApplyRow(game) { policy ->
            if (Control.COOLING in game.scenario.controls) t("%@, setpoint %lld °C", capacityText(policy.capacity), policy.setpoint)
            else capacityText(policy.capacity)
        }
    }
}

/** "autoscaling at 80 %, at least 26 servers 18:00–22:00" */
fun capacityText(capacity: CapacityMode): String = when (capacity) {
    CapacityMode.AllOn -> t("all on")
    is CapacityMode.Placed -> t("%lld hosts in use", capacity.hosts.toSet().size)
    is CapacityMode.Manual -> t("fixed %lld servers", capacity.servers)
    is CapacityMode.Autoscale -> capacity.minimum?.let {
        t("autoscaling at %@, at least %lld servers %@–%@", Format.wholePercent(capacity.targetUtilization), it.servers,
            GameTime.at(0, it.fromHour).clockText, GameTime.at(0, it.toHour % 24).clockText)
    } ?: t("autoscaling at %@", Format.wholePercent(capacity.targetUtilization))
}

/** The forecast has changed: what that means for the plan, and the way to plan again. */
@Composable
private fun ForecastNews(game: GameController) {
    Row(
        Modifier.fillMaxWidth().background(Palette.amber.copy(alpha = 0.15f), RoundedCornerShape(10.dp)).padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        CheckMark(ok = false, warning = true)
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Txt(t("Forecast update: the wind drops about 12 hours earlier than forecast."), Type.style(15, FontWeight.SemiBold))
            Txt(t("Plans made with the old forecast may now fall into dirty hours. Look at the new forecast below."), Type.callout)
            Txt(t("OK"), Type.style(15, FontWeight.SemiBold, Palette.accent), Modifier.clip(RoundedCornerShape(6.dp)).clickable { game.dismissForecastNews() }.padding(horizontal = 4.dp))
        }
    }
}

/** One rule to choose, with its explanation and, when chosen, its setting. */
@Composable
fun RuleOption(chosen: Boolean, onChoose: () -> Unit, title: String, text: String, setting: (@Composable () -> Unit)? = null) {
    Column(
        Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (chosen) Palette.accent.copy(alpha = 0.08f) else Palette.background, RoundedCornerShape(12.dp))
            .border(if (chosen) 1.5.dp else 0.dp, if (chosen) Palette.accent else Color.Transparent, RoundedCornerShape(12.dp))
            .clickable(onClick = onChoose).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Radio(chosen)
            Txt(title, Type.style(15, FontWeight.SemiBold))
        }
        Txt(text, Type.caption)
        if (chosen && setting != null) setting()
    }
}

@Composable
fun Stepper(value: String, minus: () -> Unit, plus: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        RoundButton(plus = false, size = 34.dp, onClick = minus)
        Txt(value, Type.style(16, FontWeight.SemiBold), Modifier.widthIn(min = 90.dp))
        RoundButton(plus = true, size = 34.dp, onClick = plus)
    }
}

@Composable
private fun Radio(on: Boolean) {
    Canvas(Modifier.size(18.dp)) {
        val r = size.width / 2
        drawCircle(if (on) Palette.accent else Palette.secondary, r * 0.9f, style = Stroke(r * 0.16f))
        if (on) drawCircle(Palette.accent, r * 0.5f)
    }
}

@Composable
fun Tick(on: Boolean) {
    Canvas(Modifier.size(20.dp)) {
        val w = size.width
        if (on) {
            drawRoundRect(Palette.accent, cornerRadius = CornerRadius(w * 0.2f))
            val p = Path().apply { moveTo(w * 0.25f, w * 0.52f); lineTo(w * 0.43f, w * 0.7f); lineTo(w * 0.76f, w * 0.32f) }
            drawPath(p, Color.White, style = Stroke(w * 0.12f))
        } else {
            // Inset by half the stroke, so that a clipping parent does not cut the left edge.
            val s = w * 0.08f
            drawRoundRect(Palette.secondary, Offset(s / 2, s / 2), Size(w - s, w - s), CornerRadius(w * 0.2f), style = Stroke(s))
        }
    }
}

/** "greenest hours, deadline safety on" */
fun ruleText(policy: Policy): String {
    val rule = when (val mode = policy.batch) {
        BatchMode.Asap -> t("as soon as possible")
        is BatchMode.AtHour -> t("at %@", GameTime.at(0, mode.hour).clockText)
        BatchMode.Greenest -> t("greenest hours")
        is BatchMode.Threshold -> t("below %lld g/kWh", mode.gramsPerKWh)
        BatchMode.ByHand -> t("by hand")
    }
    return if (policy.deadlineSafety) t("%@, with deadline safety", rule) else t("%@, without deadline safety", rule)
}
