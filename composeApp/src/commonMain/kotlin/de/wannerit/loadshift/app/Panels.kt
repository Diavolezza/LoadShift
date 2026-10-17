package de.wannerit.loadshift.app

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import de.wannerit.loadshift.core.BatchJob
import de.wannerit.loadshift.core.BatchJobState
import de.wannerit.loadshift.core.BatchPlanner
import de.wannerit.loadshift.core.Control
import de.wannerit.loadshift.core.GameTime
import de.wannerit.loadshift.core.ServerState
import de.wannerit.loadshift.core.Simulation

/** The controls on the right (docs/CONCEPT.md 10.1): only what the scenario unlocks. */
@Composable
fun ControlPanel(game: GameController, modifier: Modifier = Modifier) {
    Column(
        modifier.background(Palette.panel).verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        if (Control.BATCH_BY_HAND in game.scenario.controls) {
            BatchPanel(game)
        } else {
            Txt(t("Capacity"), Type.heading)
            ManualCapacity(game)
            NowBox(game.simulation)
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
fun NowBox(simulation: Simulation) {
    val running = simulation.servers.count { it.state.isOn }
    val booting = simulation.servers.count { it.state is ServerState.Booting }
    val last = simulation.history.lastOrNull()
    val demandNow = last?.demand ?: simulation.scenario.interactive.expected(simulation.tick)
    val capacity = last?.capacity ?: simulation.readyCapacity
    val served = last?.served ?: minOf(demandNow, capacity)
    val inAnHour = simulation.scenario.interactive.expected(simulation.tick + GameTime.TICKS_PER_HOUR)
    val itPower = simulation.servers.sumOf { it.power() }
    Card(Modifier.fillMaxWidth(), color = Palette.background, padding = 14.dp) {
        Txt(t("Right now"), Type.subheading)
        Spacer(Modifier.padding(4.dp))
        val rows = listOf(
            t("Servers working") to (if (booting > 0) "$running (+$booting)" else "$running"),
            t("Load / capacity") to "$demandNow / $capacity",
            t("Utilization") to (if (capacity == 0) "–" else Format.wholePercent(served * 1_000 / capacity)),
            t("Expected load in 1 h") to "$inAnHour",
            t("IT power") to "${Format.kilowatts(itPower)} kW",
        )
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
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Txt(t("Batch jobs"), Type.heading)
        Txt(t("Each job needs servers for some hours and must be finished by its deadline. When it starts, its servers switch on and boot for 30 minutes first; servers without a job are off."), Type.style(15, color = Palette.secondary))
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
                    Txt(t("OK"), Type.style(15, FontWeight.SemiBold, Palette.accent), Modifier.clickable { game.dismissUrgent() })
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
            JobStatus(job, now)
        }
        Txt(t("%lld servers · %lld h · due %@", spec.servers, spec.duration / GameTime.TICKS_PER_HOUR, timeText(spec.deadline)), Type.caption)
        if (!job.state.isStarted) {
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
                    Txt("×", Type.style(17, color = Palette.secondary), Modifier.clickable { game.unplan(job.id) }.padding(6.dp))
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
private fun JobStatus(job: BatchJob, now: Int) {
    val (text, color) = when (val state = job.state) {
        BatchJobState.Waiting ->
            if (job.spec.arrival > now) t("arrives %@", timeText(job.spec.arrival)) to Palette.secondary
            else t("not planned") to (if (job.missedDeadline(now)) Palette.red else Palette.secondary)
        is BatchJobState.Planned -> t("planned") to Palette.accent
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
