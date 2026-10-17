package de.wannerit.loadshift.app

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import de.wannerit.loadshift.core.Engineering

/** Colors of the parts in the CPU profile. */
private val partColors = listOf(
    Color(0xFFCC4D40), Color(0xFFE8A33A), Color(0xFF8E6CC9), Color(0xFF9AA5A1),
    Color(0xFF5C9EF2), Color(0xFF2A9D8F), Color(0xFFB8C4BF),
)

/** Engineering track: where the CPU time goes, the backlog and the sprint plan. */
@Composable
fun SprintPanel(game: GameController) {
    val scenario = game.scenario
    val backlog = scenario.backlog
    val done = game.sprintDone
    val growth = scenario.growth
    val day = minOf(game.currentDay, scenario.days - 1)
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Txt(t("Sprint %lld of %lld", day + 1, scenario.days), Type.heading)
        Txt(
            if (growth != null) t("Each game day stands for a month, with one sprint at its start. The team has %lld story points per sprint for performance – the rest goes into features. Items bigger than a sprint carry over; started items are continued first.", scenario.sprintPoints)
            else t("Each game day stands for the week after a sprint. Your team has %lld story points per sprint. Pick items from the backlog and start the sprint – their effect counts from this day on. Items that do not fit carry over into the next sprint.", scenario.sprintPoints),
            Type.style(15, color = Palette.secondary),
        )
        if (game.canPlanSprint) Txt(t("Sprint %lld: plan it now – the game is paused.", day + 1), Type.style(14, FontWeight.SemiBold, Palette.accent))

        // The CPU profile
        Card(Modifier.fillMaxWidth(), color = Palette.background, padding = 14.dp) {
            if (growth != null) {
                Txt(t("CPU work of a day"), Type.subheading)
                Txt(t("Data: %@ × the start, growing %@ a month.", Format.number(growth.n(day) / 1_000.0, 2), Format.wholePercent(growth.ratePermille)), Type.callout)
                val profile = growth.profile(day, backlog, done.keys) { scenario.interactive.expected(it) }
                val total = profile.sumOf { it.second }.coerceAtLeast(1)
                Spacer(Modifier.height(4.dp))
                ProfileBar(profile.map { (it.second * 1_000 / total).toInt() })
                Spacer(Modifier.height(4.dp))
                profile.forEachIndexed { i, (title, units) ->
                    val label = if (i == 0) t("Templates, framework and the rest") else {
                        val part = growth.parts[i - 1]
                        val fixed = done.keys.any { backlog[it].fixes == i - 1 }
                        t(title) + " · " + (if (fixed) part.to.label else part.from.label)
                    }
                    LegendRow(i, label, Format.wholePercent((units * 1_000 / total).toInt()))
                }
            } else {
                Txt(t("CPU time per request"), Type.subheading)
                Txt(t("Now %@ of the original.", Format.wholePercent(Engineering.work(backlog, done.keys))), Type.callout)
                Spacer(Modifier.height(4.dp))
                if (game.profiled) {
                    val remaining = scenario.hotSpots.mapIndexed { h, spot ->
                        spot.share - done.keys.filter { backlog[it].hotSpot == h }.sumOf { backlog[it].effect }
                    }
                    ProfileBar(remaining)
                    Spacer(Modifier.height(4.dp))
                    scenario.hotSpots.forEachIndexed { h, spot -> LegendRow(h, t(spot.title), Format.wholePercent(remaining[h])) }
                } else {
                    Txt(t("Where it goes is not measured yet."), Type.style(14, color = Palette.secondary))
                }
            }
        }

        // The backlog
        Txt(t("Backlog"), Type.subheading)
        val carried = game.sprintCarried
        val order = carried + game.sprintDraft
        for ((i, item) in backlog.withIndex()) {
            val isDone = i in done
            val isCarried = i in carried
            val position = order.indexOf(i)
            Row(
                Modifier.fillMaxWidth().alpha(if (isDone) 0.55f else 1f).clip(RoundedCornerShape(10.dp))
                    .background(if (position >= 0) Palette.accent.copy(alpha = 0.08f) else Palette.background)
                    .clickable(enabled = game.canPlanSprint && !isDone && !isCarried) { game.toggleSprintItem(i) }.padding(10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (position >= 0) Order(position + 1) else CheckBox(isDone)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Txt(t(item.title), Type.style(14, FontWeight.SemiBold))
                    if (growth != null) {
                        item.fixes?.let { p ->
                            val part = growth.parts[p]
                            Txt(t("%@: %@ → %@", t(part.title), part.from.label, part.to.label), Type.caption)
                        } ?: Txt(t("faster by a constant factor"), Type.caption)
                        if (!isDone) Txt(t("today: −%@", Format.wholePercent(game.savingToday(i))), Type.style(12, FontWeight.SemiBold, Palette.accent))
                    } else {
                        Txt(if (item.profiling) t("measures where the time goes") else t("team: −%@", Format.wholePercent(item.estimate)), Type.caption)
                        if (game.profiled && !item.profiling) {
                            Txt(t("measured: −%@", Format.wholePercent(item.effect)), Type.style(12, FontWeight.SemiBold, Palette.accent))
                        }
                    }
                    if (isCarried) Txt(t("in progress: %lld of %lld story points left", game.remaining(i), item.effort), Type.style(12, FontWeight.SemiBold, Palette.amber))
                    done[i]?.let { Txt(t("done in sprint %lld", it + 1), Type.caption) }
                }
                Txt(t("%lld SP", item.effort), Type.style(14, FontWeight.SemiBold))
            }
        }
        val points = game.sprintPointsPicked
        val carry = maxOf(0, points - scenario.sprintPoints)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            PrimaryButton(t("Start sprint"), enabled = game.canPlanSprint) { game.startSprint() }
            Txt(
                if (carry > 0) t("%lld of %lld story points – %lld carry over", minOf(points, scenario.sprintPoints), scenario.sprintPoints, carry)
                else t("%lld of %lld story points", points, scenario.sprintPoints),
                Type.style(14, color = Palette.secondary), Modifier.weight(1f),
            )
        }
        if (!game.canPlanSprint && !game.isFinished) Txt(t("Sprint %lld is running.", day + 1), Type.callout)
        if (game.isFinished) Txt(t("All sprints are done."), Type.callout)
    }
}

@Composable
private fun ProfileBar(shares: List<Int>) {
    Canvas(Modifier.fillMaxWidth().height(16.dp).clip(RoundedCornerShape(4.dp))) {
        var x = 0f
        for ((i, share) in shares.withIndex()) {
            val w = size.width * share / 1_000f
            drawRect(partColors[i % partColors.size], Offset(x, 0f), Size(w, size.height))
            x += w
        }
    }
}

@Composable
private fun LegendRow(index: Int, label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(Modifier.size(10.dp).background(partColors[index % partColors.size], RoundedCornerShape(2.dp)))
        Txt(label, Type.caption, Modifier.weight(1f))
        Txt(value, Type.style(12, FontWeight.SemiBold))
    }
}

/** The position of an item in the sprint. */
@Composable
private fun Order(n: Int) {
    Box(Modifier.size(22.dp).background(Palette.accent, CircleShape), contentAlignment = Alignment.Center) {
        Txt("$n", Type.style(12, FontWeight.Bold, Color.White).copy(textAlign = TextAlign.Center))
    }
}
