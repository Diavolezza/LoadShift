package de.wannerit.loadshift.app

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import de.wannerit.loadshift.core.Environments
import de.wannerit.loadshift.core.HeavyTests
import de.wannerit.loadshift.core.PipelineConfig
import de.wannerit.loadshift.core.PipelineModel
import de.wannerit.loadshift.core.Runners
import de.wannerit.loadshift.core.Trigger

/** E9: build agents, caches, tests, triggers and test environments. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PipelinePanel(game: GameController) {
    val model = game.scenario.pipeline ?: return
    val d = game.pipelineDraft
    fun set(c: PipelineConfig) { game.pipelineDraft = c }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Txt(t("Pipeline"), Type.heading)
        Txt(t("%lld developers commit about %lld times a working day. Every commit starts the pipeline on the build agents; %lld test environments serve testing and acceptance. The team needs the result of build and unit tests within %lld minutes, and the UI tests must have run in the 24 hours before Friday's release at 14:00.",
            model.developers, model.commitsPerTick * 40L, model.environments, model.feedbackLimit), Type.style(15, color = Palette.secondary))

        Section(t("Build agents")) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                for (r in Runners.entries) Chip(t(r.label), d.runners == r) { set(d.copy(runners = r)) }
            }
            Txt(t("%lld agents always on, or only as many as the waiting jobs need; an agent started on demand boots for 3 minutes.", model.agents), Type.caption)
            Option(d.remoteCache, t("Remote build cache"), t("Dependencies and build results in a shared store (for example S3) instead of on the agent's disk.")) {
                set(d.copy(remoteCache = !d.remoteCache))
            }
        }
        Section(t("Tests")) {
            Option(d.testImpact, t("Test impact analysis"), t("Runs only the unit and system tests a change can affect.")) {
                set(d.copy(testImpact = !d.testImpact))
            }
            Txt(t("UI and load tests"), Type.caption)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                for (h in HeavyTests.entries) Chip(t(h.label), d.heavy == h) { set(d.copy(heavy = h)) }
            }
        }
        Section(t("When the pipeline starts")) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                for (tr in Trigger.entries) Chip(t(tr.label), d.trigger == tr) { set(d.copy(trigger = tr)) }
            }
            Txt(t("Collected commits share one run – and wait for it."), Type.caption)
        }
        Section(t("Test environments")) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                for (e in Environments.entries) Chip(t(e.label), d.environments == e) { set(d.copy(environments = e)) }
            }
            Txt(t("Testers and business analysts work on weekdays from 8 to 18."), Type.caption)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                for (n in listOf(3, 1)) Chip(t("%lld replicas", n), d.replicas == n) { set(d.copy(replicas = n)) }
            }
            Txt(t("Three replicas per service, as in production, so that resilience can be tested."), Type.caption)
        }

        // One run with the draft
        Card(Modifier.fillMaxWidth(), color = Palette.background, padding = 14.dp) {
            Txt(t("One pipeline run with these settings"), Type.subheading)
            val rows = listOf(
                t("Build") to model.buildMinutes(d), t("Unit tests") to model.unitMinutes(d),
                t("System tests") to model.systemMinutes(d), t("UI and load tests") to model.heavyMinutes(d),
            )
            for ((label, min) in rows) {
                Row(Modifier.fillMaxWidth().padding(vertical = 1.dp)) {
                    Txt(label, Type.callout, Modifier.weight(1f))
                    Txt(t("%@ min", Format.number(min, 1)), Type.callout)
                }
            }
            val feedback = d.trigger.minutes / 2.0 + model.bootMinutes(d) + model.buildMinutes(d) + model.unitMinutes(d)
            Row(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                Txt(t("Feedback without waiting for a free agent"), Type.callout, Modifier.weight(1f))
                Txt(t("%@ min", Format.number(feedback, 1)), Type.style(15, FontWeight.SemiBold, if (feedback <= model.feedbackLimit) Palette.ink else Palette.red))
            }
            game.simulation.history.lastOrNull()?.pipeline?.let { r ->
                Row(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                    Txt(t("Running now"), Type.callout, Modifier.weight(1f))
                    Txt(t("%lld agents, %lld environment servers", r.runnersOn, r.environmentsOn), Type.callout)
                }
            }
        }
        ApplyRow(game) { pipelineText(it.pipeline) }
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Palette.background).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Txt(title, Type.style(15, FontWeight.SemiBold))
        content()
    }
}

@Composable
private fun Option(on: Boolean, title: String, note: String, toggle: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).clickable(onClick = toggle).padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Tick(on)
        Column(Modifier.weight(1f)) {
            Txt(title, Type.style(14, FontWeight.SemiBold))
            Txt(note, Type.caption)
        }
    }
}

@Composable
private fun Chip(text: String, chosen: Boolean, onClick: () -> Unit) {
    Txt(
        text, Type.style(13, FontWeight.SemiBold, if (chosen) Color.White else Palette.ink),
        Modifier.clip(RoundedCornerShape(50)).background(if (chosen) Palette.accent else Palette.panel)
            .border(1.dp, Palette.divider, RoundedCornerShape(50))
            .clickable(onClick = onClick).padding(horizontal = 10.dp, vertical = 4.dp),
    )
}

/** "agents on demand, remote cache, test impact, UI tests every night, every commit, weekdays 7–19, 1 replica" */
fun pipelineText(c: PipelineConfig): String = listOfNotNull(
    t("agents %@", t(c.runners.label)),
    if (c.remoteCache) t("remote cache") else null,
    if (c.testImpact) t("test impact analysis") else null,
    t("UI tests %@", t(c.heavy.label)),
    t(c.trigger.label),
    t("environments %@", t(c.environments.label)),
    t("%lld replicas", c.replicas),
).joinToString(", ")
