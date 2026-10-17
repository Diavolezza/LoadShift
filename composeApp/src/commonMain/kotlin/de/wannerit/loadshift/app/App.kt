package de.wannerit.loadshift.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import de.wannerit.loadshift.core.BatchMode
import de.wannerit.loadshift.core.CacheConfig
import de.wannerit.loadshift.core.CapacityMode
import de.wannerit.loadshift.core.Control
import de.wannerit.loadshift.core.Engineering
import de.wannerit.loadshift.core.Packing
import de.wannerit.loadshift.core.ScheduledMinimum
import de.wannerit.loadshift.core.GameTime
import de.wannerit.loadshift.core.Policy
import de.wannerit.loadshift.core.Scenario
import de.wannerit.loadshift.core.ServerKind

/**
 * Options from the command line (desktop) or the address (web), for screenshots and tests:
 * scenario=N opens its briefing, play starts it, ticks=N runs N ticks, careful plays like a careful player while
 * running them, lang=de|en sets the language.
 */
data class LaunchOptions(
    val scenario: Int? = null,
    val play: Boolean = false,
    val ticks: Int = 0,
    val careful: Boolean = false,
    val language: String? = null,
) {
    companion object {
        /** From "key=value" or "key" pairs (also "-key value" on the command line). */
        fun parse(pairs: Map<String, String?>) = LaunchOptions(
            scenario = pairs["scenario"]?.toIntOrNull(),
            play = "play" in pairs || "ticks" in pairs,
            ticks = pairs["ticks"]?.toIntOrNull() ?: 0,
            careful = "careful" in pairs,
            language = pairs["lang"],
        )
    }
}

private sealed interface Screen {
    data object List : Screen
    data class Briefing(val scenario: Scenario) : Screen
    data class Game(val controller: GameController) : Screen
}

@Composable
fun App(options: LaunchOptions = LaunchOptions()) {
    options.language?.let { Language.german = it == "de" }
    val family = interFamily()
    if (Type.family != family) Type.family = family
    var screen by remember { mutableStateOf(initialScreen(options)) }
    when (val s = screen) {
        Screen.List -> ScenarioListScreen { screen = Screen.Briefing(it) }
        is Screen.Briefing -> BriefingScreen(s.scenario, onStart = { screen = Screen.Game(GameController(s.scenario)) }, onBack = { screen = Screen.List })
        is Screen.Game -> GameScreen(s.controller, onExit = { s.controller.saveDraft(); screen = Screen.Briefing(s.controller.scenario) },
            onNext = { s.controller.saveDraft(); screen = Screen.Briefing(it) },
            onOverview = { s.controller.saveDraft(); screen = Screen.List })
    }
}

private fun initialScreen(options: LaunchOptions): Screen {
    val scenario = Scenario.ALL.firstOrNull { it.number == options.scenario } ?: if (options.play) Scenario.NIGHT_SHIFT else null
    scenario ?: return Screen.List
    if (!options.play) return Screen.Briefing(scenario)
    val game = GameController(scenario)
    if (options.careful && game.hasBatch) {
        // Plan like a careful player: every job into the greenest hours of the forecast.
        game.setPolicy(Policy(scenario.initialPolicy.capacity, BatchMode.Greenest))
        game.advance(options.ticks)
    } else if (options.careful && Control.COOLING in scenario.controls) {
        // Like a careful player: autoscaling at 80 %, setpoint 25 °C.
        game.setPolicy(scenario.initialPolicy.copy(capacity = CapacityMode.Autoscale(800), setpoint = 25))
        game.advance(options.ticks)
    } else if (options.careful && Control.CACHING in scenario.controls) {
        // Like a careful player: cached as allowed, one cache server, with HyperLogLog and the materialized view.
        game.setPolicy(scenario.initialPolicy.copy(cache = CacheConfig(1, listOf(1_440, 15, 1, 0, 0), approximateCount = true, materializedView = true)))
        game.advance(options.ticks)
    } else if (options.careful && Control.SPRINTS in scenario.controls) {
        // Like a careful team: profiling first, then by the measurements.
        var left = options.ticks
        val plans = if (scenario.growth != null) Engineering.greedy(scenario, value = Engineering.byLastDay(scenario))
        else Engineering.greedy(scenario, profileFirst = true, value = Engineering.byEstimates(scenario))
        for (plan in plans) {
            if (left <= 0) break
            game.planSprint(plan)
            game.startSprint()
            val n = minOf(left, GameTime.TICKS_PER_DAY)
            game.advance(n)
            left -= n
        }
    } else if (options.careful && Control.PLACEMENT in scenario.controls) {
        // Like a careful player: one batch worker per host, the peaks of the others spread – six hosts.
        game.setPolicy(Policy(CapacityMode.Placed(listOf(0, 0, 1, 2, 3, 4, 4, 5, 0, 1, 2, 3, 4, 5, 0, 2, 4, 5, 1, 3, 2, 2, 5, 5))))
        game.advance(options.ticks)
    } else if (options.careful && Control.CAPACITY_RULES in scenario.controls) {
        // Like a careful player: autoscaling with a minimum for the campaign.
        game.setPolicy(Policy(CapacityMode.Autoscale(800, ScheduledMinimum(26, 18, 22))))
        game.advance(options.ticks)
    } else if (options.careful) {
        // Switch like a careful player: enough servers for the next 90 minutes at 80 % utilization.
        repeat(options.ticks) {
            val tick = game.simulation.tick
            val expected = (tick..tick + ServerKind.BOOT_TICKS + GameTime.TICKS_PER_HOUR).maxOf { scenario.interactive.expected(it) }
            game.setWantedServers((expected * 1_000 + 15_999) / 16_000)
            game.advance(1)
        }
    } else {
        game.advance(options.ticks)
    }
    return Screen.Game(game)
}
