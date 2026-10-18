package de.wannerit.loadshift.app

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import de.wannerit.loadshift.core.Answerer
import de.wannerit.loadshift.core.InferenceConfig
import de.wannerit.loadshift.core.InferenceModel

/** The short name of an answerer on a chip. */
fun answererShort(a: Answerer): String = when (a) {
    Answerer.NONE -> t("no AI")
    Answerer.SMALL -> t("small")
    Answerer.LARGE -> t("large")
    Answerer.REASONING -> t("large + reasoning")
}

/** E4: who answers each kind of question, and the batching window. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun InferencePanel(game: GameController) {
    val model = game.scenario.inference ?: return
    val simulation = game.simulation
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Txt(t("Shop assistant"), Type.heading)
        Txt(t("Choose for each kind of question who answers it. The mean quality of the answers must stay at %@ or above, and %@ of the answers must come within %lld seconds.",
            Format.wholePercent(model.qualityTarget), Format.wholePercent(model.onTimeTarget), model.limitTenths / 10), Type.style(15, color = Palette.secondary))

        // Who answers: energy and time per answer
        Card(Modifier.fillMaxWidth(), color = Palette.background, padding = 12.dp) {
            Txt(t("Per answer, one question at a time"), Type.style(14, FontWeight.SemiBold))
            for (a in Answerer.entries) {
                Row(Modifier.fillMaxWidth().padding(vertical = 1.dp)) {
                    Txt(t(a.label), Type.caption, Modifier.weight(1f))
                    Txt(t("%@ Wh · %@ s", Format.number(a.milliWattHours / 1_000.0, if (a.milliWattHours < 10) 3 else 2), Format.number(a.tenths / 10.0, 1)), Type.caption)
                }
            }
            Txt(t("Without AI: a status page, a form or a canned reply instead of a model."), Type.caption)
        }

        for ((i, kind) in model.kinds.withIndex()) {
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Palette.background).padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(Modifier.size(10.dp).clip(CircleShape).background(Palette.cacheKinds[i % Palette.cacheKinds.size]))
                    Txt(t(kind.title), Type.style(15, FontWeight.SemiBold), Modifier.weight(1f))
                    Txt(t("%@ of the questions", Format.wholePercent(kind.share)), Type.caption)
                }
                Txt(t(kind.note), Type.caption)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    val chosen = game.aiAnswerers.getOrElse(i) { Answerer.LARGE }
                    for (a in Answerer.entries) {
                        Chip("${answererShort(a)} · ${Format.wholePercent(kind.quality[a.ordinal])}", a == chosen) { game.setAnswerer(i, a) }
                    }
                }
            }
        }
        Txt(t("The percentages: the quality of the answers to that kind of question."), Type.caption)

        // Batching
        Txt(t("Batching"), Type.style(15, FontWeight.SemiBold))
        Txt(t("The GPU servers wait up to this long to answer several questions together: the model's weights are read once for all of them, so a fuller batch needs less energy per answer – but every answer waits that long."), Type.caption)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            for (w in InferenceModel.WINDOWS) Chip(windowText(w), w == game.aiWindow) { game.aiWindow = w }
        }

        // The draft against the rules, and the running settings now
        val draft = game.draftInference
        val quality = model.quality(draft)
        val onTime = model.onTime(draft)
        Card(Modifier.fillMaxWidth(), color = Palette.background, padding = 14.dp) {
            Txt(t("With these settings"), Type.subheading)
            Row(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                Txt(t("Quality of the answers"), Type.callout, Modifier.weight(1f))
                Txt(Format.percent(quality), Type.style(15, FontWeight.SemiBold, if (quality >= model.qualityTarget) Palette.ink else Palette.red))
            }
            Row(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                Txt(t("Answers within %lld s", model.limitTenths / 10), Type.callout, Modifier.weight(1f))
                Txt(Format.percent(onTime), Type.style(15, FontWeight.SemiBold, if (onTime >= model.onTimeTarget) Palette.ink else Palette.red))
            }
            val last = simulation.history.lastOrNull()?.inference
            if (last != null) {
                Row(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                    Txt(t("GPU servers running now"), Type.callout, Modifier.weight(1f))
                    Txt("${last.servers} / ${model.gpuServers}", Type.style(15, FontWeight.SemiBold))
                }
            }
        }
        ApplyRow(game) { inferenceText(it.inference, model) }
    }
}

private fun windowText(ms: Int): String = when {
    ms == 0 -> t("off")
    ms < 1_000 -> "$ms ms"
    else -> "${ms / 1_000} s"
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

/** "no AI / no AI / small / large / large, batching 1 s" */
fun inferenceText(c: InferenceConfig, model: InferenceModel): String {
    val who = model.kinds.indices.joinToString(" / ") { answererShort(model.answerer(c, it)) }
    return if (c.windowMs == 0) who else t("%@, batching %@", who, windowText(c.windowMs))
}
