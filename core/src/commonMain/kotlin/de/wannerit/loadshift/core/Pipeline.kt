package de.wannerit.loadshift.core

import kotlin.math.ceil

/*
 * E6, "Green pipeline" (docs/CONCEPT.md 9.2): a team of 40 developers, a CI/CD pipeline and test environments, for a
 * week. Every commit starts a build with all tests on build agents that run day and night; six test environments run
 * with three replicas each, also at night and at the weekend. The levers follow the usual strategies for CI/CD:
 * scale-to-zero agents (their local cache is lost – a remote build cache keeps it), test impact analysis (only the
 * tests a change can break), the expensive tests at night in the greenest hours, batching commits, environments only
 * when they are used, fewer replicas outside production [S31].
 */

enum class Runners(val label: String) { ALWAYS_ON("always on"), ON_DEMAND("on demand (scale to zero)") }

enum class HeavyTests(val label: String) { EVERY_COMMIT("with every commit"), NIGHTLY("every night, in the greenest hours"), NEVER("never") }

enum class Environments(val label: String) { ALWAYS("day and night"), OFFICE_HOURS("weekdays 7–19"), ON_DEMAND("on demand per acceptance test") }

enum class Trigger(val label: String, val minutes: Int) { EVERY_COMMIT("every commit", 0), EVERY_30_MIN("every 30 minutes", 30), EVERY_2_H("every 2 hours", 120) }

data class PipelineConfig(
    val runners: Runners = Runners.ALWAYS_ON,
    val remoteCache: Boolean = false,
    val testImpact: Boolean = false,
    val heavy: HeavyTests = HeavyTests.EVERY_COMMIT,
    val trigger: Trigger = Trigger.EVERY_COMMIT,
    val environments: Environments = Environments.ALWAYS,
    val replicas: Int = 3,
)

data class PipelineModel(
    val developers: Int = 40,
    /** Commits per tick during working hours (weekdays 8–18). */
    val commitsPerTick: Int = 4,
    val agents: Int = 16,
    val environments: Int = 6,
    /** Feedback limit: minutes from a commit to the result of build and unit tests. */
    val feedbackLimit: Int = 20,
    /** Runner minutes of the full UI and load tests at night. */
    val nightlyMinutes: Int = 180,
    /** Acceptance tests in parallel during the day when environments start on demand. */
    val onDemandEnvironments: Int = 3,
) {
    fun buildMinutes(c: PipelineConfig): Double = if (c.runners == Runners.ALWAYS_ON || c.remoteCache) 3.0 else 14.0
    fun unitMinutes(c: PipelineConfig): Double = if (c.testImpact) 1.5 else 5.0
    fun systemMinutes(c: PipelineConfig): Double = if (c.testImpact) 4.0 else 12.0
    fun heavyMinutes(c: PipelineConfig): Double = if (c.heavy == HeavyTests.EVERY_COMMIT) 25.0 else 0.0

    /** Runner minutes of one pipeline run. */
    fun minutesPerRun(c: PipelineConfig): Double = buildMinutes(c) + unitMinutes(c) + systemMinutes(c) + heavyMinutes(c)

    /** Boot time of an agent started on demand, minutes. */
    fun bootMinutes(c: PipelineConfig): Double = if (c.runners == Runners.ON_DEMAND) 3.0 else 0.0

    /** Test environment servers on at a tick. */
    fun environmentServers(c: PipelineConfig, weekday: Boolean, hour: Int): Int = when (c.environments) {
        Environments.ALWAYS -> environments * c.replicas
        Environments.OFFICE_HOURS -> if (weekday && hour in 7 until 19) environments * c.replicas else 0
        Environments.ON_DEMAND -> if (weekday && hour in 9 until 17) onDemandEnvironments * c.replicas else 0
    }

    fun agentsOn(c: PipelineConfig, demandMinutes: Double): Int =
        if (c.runners == Runners.ALWAYS_ON) agents else minOf(agents, ceil(demandMinutes / MINUTES_PER_TICK).toInt())

    companion object {
        const val MINUTES_PER_TICK = 60.0 / GameTime.TICKS_PER_HOUR
        /** The weekly release: Friday (day 5) at 14:00. */
        const val RELEASE_DAY = 4
        const val RELEASE_HOUR = 14
    }
}

/** E6: runners and environment servers on, runner minutes done, and whether the nightly tests ran. */
data class PipelineRecord(val runnersOn: Int, val environmentsOn: Int, val busyMinutes: Int, val nightly: Boolean)
