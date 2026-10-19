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
 * running them, solution takes the solution over and plays it (the stars go to the console), lang=de|en sets the language.
 */
data class LaunchOptions(
    val scenario: Int? = null,
    val play: Boolean = false,
    val ticks: Int = 0,
    val careful: Boolean = false,
    val solution: Boolean = false,
    val language: String? = null,
    /** Every scenario open for this start, whatever the stars (D19). */
    val unlock: Boolean = false,
) {
    companion object {
        /** From "key=value" or "key" pairs (also "-key value" on the command line). */
        fun parse(pairs: Map<String, String?>) = LaunchOptions(
            scenario = pairs["scenario"]?.toIntOrNull(),
            play = "play" in pairs || "ticks" in pairs,
            ticks = pairs["ticks"]?.toIntOrNull() ?: 0,
            careful = "careful" in pairs,
            solution = "solution" in pairs,
            language = pairs["lang"],
            unlock = "unlock" in pairs,
        )
    }
}

private sealed interface Screen {
    data object List : Screen
    data class Briefing(val scenario: Scenario) : Screen
    data class Game(val controller: GameController) : Screen
    data class Portfolio(val controller: PortfolioController) : Screen
    data class Abatement(val controller: AbatementController) : Screen
    data object Lexicon : Screen
    data class Tidy(val controller: TidyController) : Screen
    data class Repatriation(val controller: RepatriationController) : Screen
    data class Availability(val controller: AvailabilityController) : Screen
    data class Updates(val controller: UpdatesController) : Screen
    data class Tenancy(val controller: TenancyController) : Screen
    data class Lifecycle(val controller: LifecycleController) : Screen
    data class Patterns(val controller: PatternsController) : Screen
}

@Composable
fun App(options: LaunchOptions = LaunchOptions()) {
    options.language?.let { Language.german = it == "de" }
    if (options.unlock) Progress.unlockAll = true
    val family = interFamily()
    if (Type.family != family) Type.family = family
    var screen by remember { mutableStateOf(initialScreen(options)) }
    when (val s = screen) {
        Screen.List -> ScenarioListScreen(onLexicon = { screen = Screen.Lexicon }) { screen = Screen.Briefing(it) }
        Screen.Lexicon -> LexiconScreen(onBack = { screen = Screen.List }) { screen = Screen.Briefing(it) }
        is Screen.Briefing -> BriefingScreen(s.scenario, onStart = {
            screen = if (s.scenario.portfolio != null) Screen.Portfolio(PortfolioController(s.scenario))
                else if (s.scenario.abatement != null) Screen.Abatement(AbatementController(s.scenario))
                else if (s.scenario.tidy != null) Screen.Tidy(TidyController(s.scenario))
                else if (s.scenario.repatriation != null) Screen.Repatriation(RepatriationController(s.scenario))
                else if (s.scenario.availability != null) Screen.Availability(AvailabilityController(s.scenario))
                else if (s.scenario.updates != null) Screen.Updates(UpdatesController(s.scenario))
                else if (s.scenario.tenancy != null) Screen.Tenancy(TenancyController(s.scenario))
                else if (s.scenario.lifecycle != null) Screen.Lifecycle(LifecycleController(s.scenario))
                else if (s.scenario.patterns != null) Screen.Patterns(PatternsController(s.scenario)) else Screen.Game(GameController(s.scenario))
        }, onBack = { screen = Screen.List })
        is Screen.Availability -> FitLowScreen { AvailabilityScreen(s.controller, onExit = { screen = Screen.Briefing(s.controller.scenario) }, onOverview = { screen = Screen.List }) }
        is Screen.Updates -> FitLowScreen { UpdatesScreen(s.controller, onExit = { screen = Screen.Briefing(s.controller.scenario) }, onOverview = { screen = Screen.List }) }
        is Screen.Tenancy -> FitLowScreen { TenancyScreen(s.controller, onExit = { screen = Screen.Briefing(s.controller.scenario) }, onOverview = { screen = Screen.List }) }
        is Screen.Patterns -> FitLowScreen { PatternsScreen(s.controller, onExit = { screen = Screen.Briefing(s.controller.scenario) }, onOverview = { screen = Screen.List }) }
        is Screen.Lifecycle -> FitLowScreen { LifecycleScreen(s.controller, onExit = { screen = Screen.Briefing(s.controller.scenario) }, onOverview = { screen = Screen.List }) }
        is Screen.Repatriation -> FitLowScreen { RepatriationScreen(s.controller, onExit = { screen = Screen.Briefing(s.controller.scenario) }, onOverview = { screen = Screen.List }) }
        is Screen.Tidy -> FitLowScreen { TidyScreen(s.controller, onExit = { screen = Screen.Briefing(s.controller.scenario) }, onOverview = { screen = Screen.List }) }
        is Screen.Abatement -> FitLowScreen { AbatementScreen(s.controller, onExit = { screen = Screen.Briefing(s.controller.scenario) }, onOverview = { screen = Screen.List }) }
        is Screen.Portfolio -> FitLowScreen { PortfolioScreen(s.controller, onExit = { screen = Screen.Briefing(s.controller.scenario) }, onOverview = { screen = Screen.List }) }
        is Screen.Game -> FitLowScreen {
            GameScreen(s.controller, onExit = { s.controller.saveDraft(); screen = Screen.Briefing(s.controller.scenario) },
                onNext = { s.controller.saveDraft(); screen = Screen.Briefing(it) },
                onOverview = { s.controller.saveDraft(); screen = Screen.List })
        }
    }
}

private fun initialScreen(options: LaunchOptions): Screen {
    val scenario = Scenario.ALL.firstOrNull { it.number == options.scenario } ?: if (options.play) Scenario.NIGHT_SHIFT else null
    scenario ?: return Screen.List
    if (!options.play) return Screen.Briefing(scenario)
    if (scenario.availability != null) {
        val g = AvailabilityController(scenario)
        if (options.careful) {
            for (b in g.model.blocks) g.setTactic(b, g.model.tactic(g.model.example, b.name))
            for (d in g.model.example.decouplings) g.toggle(d)
            if (options.ticks > 0) g.finish()
        }
        return Screen.Availability(g)
    }
    if (scenario.updates != null) {
        val g = UpdatesController(scenario)
        if (options.careful) {
            for (k in g.model.kinds) {
                g.setOpen(k, g.model.open(g.model.example, k))
                g.setClosed(k, g.model.closed(g.model.example, k))
            }
            g.toggleConditional(); g.toggleBundled()
            if (options.ticks > 0) g.finish()
        }
        return Screen.Updates(g)
    }
    if (scenario.tenancy != null) {
        val g = TenancyController(scenario)
        if (options.careful) {
            for (c in g.model.components) for (gr in g.model.groups) g.set(c, gr, g.model.choice(g.model.example, c, gr))
            if (options.ticks > 0) g.finish()
        }
        return Screen.Tenancy(g)
    }
    if (scenario.patterns != null) {
        val g = PatternsController(scenario)
        if (options.careful) {
            for (s in g.model.subsystems) g.choose(s, g.model.option(g.model.example, s))
            if (options.ticks > 0) g.finish()
        }
        return Screen.Patterns(g)
    }
    if (scenario.lifecycle != null) {
        val g = LifecycleController(scenario)
        if (options.careful) {
            g.loadSolution()
            if (options.ticks > 0) g.finish()
        }
        return Screen.Lifecycle(g)
    }
    if (scenario.repatriation != null) {
        val g = RepatriationController(scenario)
        if (options.careful) {
            // Steady load to the colocation, the rest to Stockholm, the tests off at night.
            for (app in g.model.apps) {
                val m = g.model.example[app.name] ?: continue
                g.place(app, m.place)
                if (m.offOutsideUse) g.toggleOff(app)
            }
            if (options.ticks > 0) g.finish()
        }
        return Screen.Repatriation(g)
    }
    if (scenario.tidy != null) {
        val g = TidyController(scenario)
        if (options.careful) {
            // Like a careful manager: quick wins first, the ERP tuned.
            for ((i, app) in g.model.apps.withIndex()) {
                val c = g.model.example[app.name] ?: continue
                g.selected = i
                g.decide(c.decision)
                g.moveStart(c.start)
                if (c.decommission) g.toggleDecommission()
            }
            g.selected = 0
            if (options.ticks > 0) g.finish()
        }
        return Screen.Tidy(g)
    }
    if (scenario.abatement != null) {
        val a = AbatementController(scenario)
        if (options.careful) {
            // Like a careful manager: the best plan within the budget.
            for (i in a.model.best()) a.toggle(i)
            if (options.ticks > 0) a.finish()
        }
        return Screen.Abatement(a)
    }
    if (scenario.portfolio != null) {
        val p = PortfolioController(scenario)
        if (options.careful) {
            // Like a careful manager: measured power, the cloud recalculated, the two suppliers with much data asked,
            // then the measures with the most expected saving per euro.
            p.chooseKey(de.wannerit.loadshift.core.OnPremKey.POWER)
            p.recalculate()
            p.ask(p.model.indexOf("Video conferencing"))
            p.ask(p.model.indexOf("Cloud file sharing"))
            for (i in p.model.greedyPlan(p.inventory)) p.toggle(i)
            if (options.ticks > 0) p.finish()
        }
        return Screen.Portfolio(p)
    }
    val game = GameController(scenario)
    if (options.solution) {
        // The solution as the app takes it over, played like a player who starts each sprint.
        game.loadSolution()
        repeat(options.ticks) {
            if (!game.simulation.isFinished) {
                if (game.canPlanSprint) game.startSprint()
                game.step()
            }
        }
        println("solution ${scenario.shownLabel}: ${game.stars} stars")
        return Screen.Game(game)
    }
    if (options.careful && game.scenario.contract != null) {
        // Like a careful player: 24/7 carbon-free, autoscaling at 80 %, jobs in the greenest hours.
        game.setPolicy(scenario.referencePolicy.copy(contract = de.wannerit.loadshift.core.Contract.HOURLY,
            capacity = de.wannerit.loadshift.core.CapacityMode.Autoscale(800), batch = BatchMode.Greenest))
        game.advance(options.ticks)
    } else if (options.careful && game.hasServices) {
        // Like a careful player: cache 1 min / 5 min, gRPC, events, serverless functions.
        game.setPolicy(scenario.initialPolicy.copy(services = de.wannerit.loadshift.core.ServiceConfig(de.wannerit.loadshift.core.ServiceCut.MICROSERVICES,
            de.wannerit.loadshift.core.Protocol.GRPC, de.wannerit.loadshift.core.MasterData.EVENTS, 60, 300, true)))
        game.advance(options.ticks)
    } else if (options.careful && game.hasPipeline) {
        // Like a careful player: agents on demand with a remote cache, test impact analysis, UI tests at night,
        // environments on weekdays 7–19 with one replica.
        game.setPolicy(scenario.initialPolicy.copy(pipeline = de.wannerit.loadshift.core.PipelineConfig(de.wannerit.loadshift.core.Runners.ON_DEMAND, true, true,
            de.wannerit.loadshift.core.HeavyTests.NIGHTLY, de.wannerit.loadshift.core.Trigger.EVERY_COMMIT, de.wannerit.loadshift.core.Environments.OFFICE_HOURS, 1)))
        game.advance(options.ticks)
    } else if (options.careful && game.hasWeb) {
        // Like a careful player: AVIF in the right size, lazy below the fold, video on click, short lists, one tag.
        game.setPolicy(scenario.initialPolicy.copy(pages = de.wannerit.loadshift.core.PageConfig(de.wannerit.loadshift.core.ImageFormat.AVIF, true,
            de.wannerit.loadshift.core.LazyLoading.BELOW_FOLD, false, false, de.wannerit.loadshift.core.Tracking.ONE_TAG)))
        game.advance(options.ticks)
    } else if (options.careful && game.hasInference) {
        // Like a careful player: no AI for the order status and greetings, the small model for product questions,
        // batching 1 s.
        val a = de.wannerit.loadshift.core.Answerer.entries
        game.setPolicy(scenario.initialPolicy.copy(inference = de.wannerit.loadshift.core.InferenceConfig(listOf(a[0], a[0], a[1], a[2], a[2]), 1_000)))
        game.advance(options.ticks)
    } else if (options.careful && game.isCapstone) {
        // Like a careful player: autoscaling at 80 %, batch jobs and training in the greenest hours (training capped
        // at 60 %), the plan made again when the forecast is revised.
        game.setPolicy(Policy(de.wannerit.loadshift.core.CapacityMode.Autoscale(800), BatchMode.Greenest,
            training = de.wannerit.loadshift.core.TrainingConfig(4, 600, true)))
        val first = minOf(options.ticks, (scenario.forecastMiss?.revisedAt ?: scenario.endTick) - scenario.startTick)
        game.advance(first)
        if (options.ticks > first) {
            game.replan()
            game.advance(options.ticks - first)
        }
    } else if (options.careful && game.hasBatch && scenario.location == null) {
        // Plan like a careful player: every job into the greenest hours of the forecast.
        game.setPolicy(Policy(scenario.initialPolicy.capacity, BatchMode.Greenest))
        game.advance(options.ticks)
    } else if (options.careful && Control.LOCATION in scenario.controls) {
        // Like a careful player: trainings, reports and search index to Oslo, the greenest hours.
        for (job in listOf(0, 1, 5, 6, 7, 8)) game.toggleMove(job)
        game.setLocationRule(true)
        game.advance(options.ticks)
    } else if (options.careful && Control.STREAMING in scenario.controls) {
        // Like a careful player: HD on phones and laptops, eco mode HD above 550 g/kWh.
        val hd = de.wannerit.loadshift.core.Quality.HD
        game.setPolicy(scenario.initialPolicy.copy(streaming = de.wannerit.loadshift.core.StreamingConfig(
            listOf(hd, hd, de.wannerit.loadshift.core.Quality.UHD), eco = true, ecoAbove = 550)))
        game.advance(options.ticks)
    } else if (options.careful && Control.TRAINING in scenario.controls) {
        // Like a careful player: six servers, cap 60 %, the greenest hours.
        game.setPolicy(scenario.initialPolicy.copy(training = de.wannerit.loadshift.core.TrainingConfig(6, 600, true)))
        game.advance(options.ticks)
    } else if (options.careful && Control.RENEWAL in scenario.controls) {
        // Like a careful player: the whole budget into Warsaw at once.
        game.changeRenewal(0, scenario.renewal?.budgetPerYear ?: 0)
        game.startQuarter()
        game.advance(options.ticks)
    } else if (options.careful && Control.COOLING in scenario.controls) {
        // Like a careful player: autoscaling at 80 %, setpoint 25 °C.
        game.setPolicy(scenario.initialPolicy.copy(capacity = CapacityMode.Autoscale(800), setpoint = 25))
        game.advance(options.ticks)
    } else if (options.careful && Control.CACHING in scenario.controls) {
        // Like a careful player: cached as allowed, one cache server, with HyperLogLog and the materialized view.
        game.setPolicy(scenario.initialPolicy.copy(cache = CacheConfig(1, listOf(1_440, 15, 1, 0, 0), approximateCount = true, materializedView = true, invalidate = true)))
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
        // Like a careful player: less headroom, a minimum for the evening of the campaign.
        game.setPolicy(Policy(CapacityMode.Autoscale(850, ScheduledMinimum(22, 18, 22))))
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
