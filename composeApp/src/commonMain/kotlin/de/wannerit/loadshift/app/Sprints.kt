package de.wannerit.loadshift.app

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import de.wannerit.loadshift.core.Engineering

/** Colors of the hot spots in the CPU profile. */
private val hotSpotColors = listOf(
    Color(0xFFCC4D40), Color(0xFFE8A33A), Color(0xFF8E6CC9), Color(0xFF9AA5A1),
    Color(0xFF5C9EF2), Color(0xFF2A9D8F), Color(0xFFB8C4BF),
)

/** E1: where the CPU time per request goes, the backlog and the sprint plan. */
@Composable
fun SprintPanel(game: GameController) {
    val scenario = game.scenario
    val backlog = scenario.backlog
    val done = game.sprintDone
    val work = Engineering.work(backlog, done.keys)
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        val sprint = minOf(game.currentDay + 1, scenario.days)
        Txt(t("Sprint %lld of %lld", sprint, scenario.days), Type.heading)
        Txt(t("Each game day stands for the week after a sprint. Your team has %lld story points per sprint. Pick items from the backlog and start the sprint – their effect counts from this day on.", scenario.sprintPoints), Type.style(15, color = Palette.secondary))
        if (game.canPlanSprint) {
            Txt(t("Sprint %lld: plan it now – the game is paused.", sprint), Type.style(14, FontWeight.SemiBold, Palette.accent))
        }

        // The CPU profile
        Card(Modifier.fillMaxWidth(), color = Palette.background, padding = 14.dp) {
            Txt(t("CPU time per request"), Type.subheading)
            Txt(t("Now %@ of the original.", Format.wholePercent(work)), Type.callout)
            Spacer4()
            if (game.profiled) {
                val remaining = scenario.hotSpots.mapIndexed { h, spot ->
                    spot.share - done.keys.filter { backlog[it].hotSpot == h }.sumOf { backlog[it].effect }
                }
                Canvas(Modifier.fillMaxWidth().height(16.dp).clip(RoundedCornerShape(4.dp))) {
                    var x = 0f
                    for ((h, share) in remaining.withIndex()) {
                        val w = size.width * share / 1_000f
                        drawRect(hotSpotColors[h % hotSpotColors.size], Offset(x, 0f), Size(w, size.height))
                        x += w
                    }
                }
                Spacer4()
                for ((h, spot) in scenario.hotSpots.withIndex()) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(Modifier.size(10.dp).background(hotSpotColors[h % hotSpotColors.size], RoundedCornerShape(2.dp)))
                        Txt(t(spot.title), Type.caption, Modifier.weight(1f))
                        Txt(Format.wholePercent(remaining[h]), Type.style(12, FontWeight.SemiBold))
                    }
                }
            } else {
                Txt(t("Where it goes is not measured yet."), Type.style(14, color = Palette.secondary))
            }
        }

        // The backlog
        Txt(t("Backlog"), Type.subheading)
        for ((i, item) in backlog.withIndex()) {
            val isDone = i in done
            val picked = i in game.sprintDraft
            Row(
                Modifier.fillMaxWidth().alpha(if (isDone) 0.55f else 1f).clip(RoundedCornerShape(10.dp))
                    .background(if (picked) Palette.accent.copy(alpha = 0.08f) else Palette.background)
                    .clickable(enabled = game.canPlanSprint && !isDone) { game.toggleSprintItem(i) }.padding(10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                CheckBox(picked || isDone)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Txt(t(item.title), Type.style(14, FontWeight.SemiBold))
                    val estimate = if (item.profiling) t("measures where the time goes") else t("team: −%@", Format.wholePercent(item.estimate))
                    Txt(estimate, Type.caption)
                    if (game.profiled && !item.profiling) {
                        Txt(t("measured: −%@", Format.wholePercent(item.effect)), Type.style(12, FontWeight.SemiBold, Palette.accent))
                    }
                    done[i]?.let { Txt(t("done in sprint %lld", it + 1), Type.caption) }
                }
                Txt(t("%lld SP", item.effort), Type.style(14, FontWeight.SemiBold))
            }
        }
        val points = game.sprintPointsPicked
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            PrimaryButton(t("Start sprint"), enabled = game.canPlanSprint && points <= scenario.sprintPoints) { game.startSprint() }
            Txt(
                if (points > scenario.sprintPoints) t("too many story points") else t("%lld of %lld story points", points, scenario.sprintPoints),
                Type.style(14, color = if (points > scenario.sprintPoints) Palette.red else Palette.secondary), Modifier.weight(1f),
            )
        }
        if (!game.canPlanSprint && !game.isFinished) Txt(t("Sprint %lld is running.", sprint), Type.callout)
        if (game.isFinished) Txt(t("All sprints are done."), Type.callout)
    }
}

@Composable
private fun Spacer4() = androidx.compose.foundation.layout.Spacer(Modifier.padding(2.dp))
