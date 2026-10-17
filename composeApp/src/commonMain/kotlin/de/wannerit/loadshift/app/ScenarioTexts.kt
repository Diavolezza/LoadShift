package de.wannerit.loadshift.app

import de.wannerit.loadshift.core.Scenario

/**
 * Texts that differ between scenarios: briefing, the reference operator, the rule for the first star and a closing
 * remark in the debrief (keys of [GERMAN]).
 */
class ScenarioTexts(
    val situation: String,
    val idea: String,
    /** "On the screen": top, middle, right, bottom – numbered like the markers in the preview. */
    val screen: List<String>,
    /** "compared with the reference operator, who …" (end card). */
    val comparedWith: String,
    /** The rule for the first star. */
    val rules: String,
    /** One remark in the debrief. */
    val remark: String,
    /** The technical term for what the player does in the scenario, and what it means. */
    val term: String,
    val termText: String,
) {
    companion object {
        fun of(scenario: Scenario): ScenarioTexts = when (scenario.number) {
            101 -> ScenarioTexts(
                situation = "Back to the web shop – this time to its code. Autoscaling keeps the servers in line with the load, so less CPU work per request means fewer servers. Your team has 8 story points per sprint and a backlog of ideas, each with the team's estimate of what it saves. Four sprints; each game day shows the week after a sprint.",
                idea = "Optimizing by gut feeling is a gamble: the team expects the most from the image resizing and from rewriting the checkout. Whether that is true, only a measurement shows. Profiling costs a few story points and shows where the CPU time really goes – then you fix the hot spots first.",
                screen = listOf(
                    "The game starts paused and pauses at the start of every sprint. Clock and speed – pause, 1×, 4×, 16× – and below it your goals.",
                    "Your racks: grey servers are off, light blue ones are booting, the blue bar shows how busy a server is.",
                    "Sprint planning: where the CPU time per request goes (once measured), the backlog with the team's estimates, your story points. Below it the situation right now.",
                    "Carbon intensity of the grid with the forecast, the load so far and the expected load (dotted); the lines on the right show how many servers a load needs.",
                ),
                comparedWith = "compared with the reference operator, whose team changes nothing in the code.",
                rules = "SLA at least 99 % and less CO₂ than the reference",
                remark = "Measure before you optimize: the big items on the team's list hardly mattered, while a one-line change of the log level saved a tenth of the CPU. Less work per request lowers the energy and – with autoscaling – the number of servers; a data center that needs fewer servers also needs less hardware. The SCI per 1,000 requests shows the effect independent of the load.",
                term = "Profiling",
                termText = "Measuring where a program spends its time and memory, function by function. A profiler shows the hot spots – often not where the team expects them. Without measuring, optimization is guesswork (premature optimization).",
            )
            5 -> ScenarioTexts(
                situation = "The web shop's 24 virtual machines – front ends, batch workers, reporting, two database replicas and caches – run on ten hosts. The previous team spread them evenly: every host runs, none is busy. A host draws about a quarter of its maximum power even when idle.",
                idea = "Fewer hosts, less idle power: pack the VMs tighter. Two things decide whether a VM fits: its RAM and its CPU over the day. By RAM alone five hosts would do – but then the peaks collide. VMs whose peaks come at different times – batch at night, reporting in the morning, the shop in the evening – fit together; VMs that peak at the same time do not.",
                screen = listOf(
                    "The game starts paused. Clock and speed – pause, 1×, 4×, 16× – and below it your goals.",
                    "The hosts as columns: the height is the RAM, the VMs are stacked in it. Below each host its CPU over the day – dotted the expected work, the area what has happened, red dashed the line above which requests get slow. Drag a VM onto a host.",
                    "Hosts in use, RAM, warnings for hosts that will be too busy, the VM kinds with their daily profiles.",
                    "Carbon intensity of the grid with the forecast, the load of all VMs and the capacity of the running hosts.",
                ),
                comparedWith = "compared with the reference operator, who keeps the VMs spread over all ten hosts.",
                rules = "SLA at least 99 % and less CO₂ than the reference",
                remark = "Bin packing is a hard problem (NP-hard): for large fleets the best placement cannot be computed quickly. Simple heuristics like First Fit Decreasing are fast, but with strict rules they leave hosts half empty, and with loose ones the peaks collide – here a careful placement by hand beats them. Real schedulers combine such heuristics with daily profiles and some headroom; spreading for safety is their default.",
                term = "Consolidation",
                termText = "Running the work on as few machines as it needs, so the others can be switched off. Placing items of different sizes into as few bins as possible is called bin packing.",
            )
            4 -> ScenarioTexts(
                situation = "The web shop again, now in winter and with 30 servers. Autoscaling is on: at 80 % target utilization it keeps just enough servers running. On day 2 marketing sends a newsletter at 19:00 – for three hours twice as many people will shop as usual.",
                idea = "Autoscaling does what you did by hand in scenario 1: it switches servers on and off for the load. The target utilization decides how much headroom stays free. Little headroom saves energy – but a server takes 30 minutes to boot, and the autoscaler knows only the usual day; a sudden peak finds it unprepared. Headroom all the time costs energy all the time; a minimum for a known event costs it only when it is needed.",
                screen = listOf(
                    "The game starts paused. Clock and speed – pause, 1×, 4×, 16× – and below it your goals.",
                    "Your racks: grey servers are off, light blue ones are booting, the blue bar shows how busy a server is.",
                    "The capacity rule: all on, a fixed number, or autoscaling with a target utilization and, if you want, a minimum for a time window. Choose, set, apply. Below it the situation right now.",
                    "Carbon intensity of the grid with the forecast, the load so far and the expected load (dotted). The campaign is marked – but it is not in the expected load.",
                ),
                comparedWith = "compared with the reference operator, who leaves every server on.",
                rules = "SLA at least 99 % and less CO₂ than the reference",
                remark = "Headroom is insurance: it costs energy every hour, whether the peak comes or not. When a peak is known in advance, scheduled scaling is cheaper – cloud platforms offer it next to the autoscaler. Predictive autoscaling goes one step further and learns such peaks from the past.",
                term = "Autoscaling",
                termText = "Capacity follows the load automatically: servers are added when the utilization rises above a target and removed when it falls. The target decides the headroom. Scheduled scaling adds capacity for known events in advance.",
            )
            3 -> ScenarioTexts(
                situation = "The web shop's analytics cluster in Great Britain: twelve servers, nine jobs in three days. It is winter, and the British grid depends on the wind. Today is an ordinary winter day; tomorrow a storm front brings plenty of wind power, the day after the wind drops and gas power plants take over.",
                idea = "Planning every job by hand does not scale. A rule does it for you: at a fixed hour, below a threshold, or into the greenest hours of the forecast. But the forecast is uncertain – ±25 % a day ahead in a wind grid – and a rule that waits for clean power may wait in vain. The deadline safety makes sure that no job misses its deadline.",
                screen = listOf(
                    "The game starts paused. Clock and speed – pause, 1×, 4×, 16× – and below it your goals.",
                    "Your batch cluster: grey servers are off, light blue ones are booting, the amber bar shows a server working on a job.",
                    "The batch rule: choose one, set it and apply it. Below it the jobs with their deadlines and what the rule has planned for them.",
                    "The carbon intensity with the forecast, and below it one row per job – light: the time between arrival and deadline, outlined: planned, filled: running or done.",
                ),
                comparedWith = "compared with the reference operator, who starts every job as soon as it arrives.",
                rules = "every job by its deadline and less CO₂ than the reference",
                remark = "A fixed threshold works while the wind blows. When it drops, the jobs wait in vain: without the deadline safety they miss their deadlines, with it they start at the last moment – often in the dirtiest hours. A rule that compares the hours of the forecast adapts to the weather – as long as the plan is made again when the forecast changes. Real schedulers plan again regularly for this reason.",
                term = "Carbon-Aware Scheduling",
                termText = "A scheduler starts jobs by rules that look at the carbon intensity – now or in the forecast. It automates Time Shifting; Google shifts flexible work in its data centers this way, with day-ahead forecasts.",
            )
            2 -> ScenarioTexts(
                situation = "The web shop's batch cluster in Spain: ten servers for reports, backups, imports and conversions. Over two days seven jobs arrive, each with a deadline. Nobody minds when a job runs – as long as it is finished in time.",
                idea = "The same job emits less CO₂ when the grid is clean. In Spain's solar grid the cleanest hours are around midday, not at night. Plan every job into clean hours before its deadline – and keep in mind that only ten servers are there.",
                screen = listOf(
                    "The game starts paused. Clock and speed – pause, 1×, 4×, 16× – and below it your goals. The game also pauses when a job must start within the hour.",
                    "Your batch cluster: grey servers are off, light blue ones are booting, the amber bar shows a server working on a job.",
                    "The jobs with their size and deadline. Plan a start time for each job with plus and minus, or start it now; a planned job starts by itself.",
                    "The carbon intensity with the forecast, and below it one row per job – light: the time between arrival and deadline, outlined: planned, filled: running or done.",
                ),
                comparedWith = "compared with the reference operator, who starts every job as soon as it arrives.",
                rules = "every job by its deadline and less CO₂ than the reference",
                remark = "The night is not clean everywhere: in a solar grid the midday hours are the cleanest. Part of the CO₂ is the hardware's manufacturing – it stays the same whenever the jobs run.",
                term = "Time Shifting",
                termText = "Moving flexible work into the hours when the grid is cleanest. Together with Location Shifting it is called Demand Shifting – part of the Green Software Foundation principle Carbon Awareness.",
            )
            else -> ScenarioTexts(
                situation = "You run the data center of a web shop in Germany. Its 20 servers are sized for the Christmas season. Today is an ordinary autumn day: in the evening many people shop, at night hardly anyone. It is midnight, and the previous shift has left every server on.",
                idea = "An idle server still draws about a quarter of its maximum power. Switch off the servers that are not needed – and switch them on again in time: a server takes 30 minutes before it can work.",
                screen = listOf(
                    "The game starts paused. Clock and speed – pause, 1×, 4×, 16× – and below it your goals.",
                    "Your racks: grey servers are off, light blue ones are booting, the blue bar shows how busy a server is.",
                    "Plus and minus for the servers, and the situation right now.",
                    "Carbon intensity of the grid with the forecast, the load so far and the expected load (dotted); the lines on the right show how many servers a load needs.",
                ),
                comparedWith = "compared with the reference operator, who leaves every server on.",
                rules = "SLA at least 99 % and less CO₂ than the reference",
                remark = "Switching servers off saves energy, not hardware: the embodied emissions stay the same.",
                term = "Rightsizing",
                termText = "Running only as much capacity as the load needs. An idle server still draws power, so servers that are not needed are switched off. Green Software Foundation principle: energy efficiency.",
            )
        }
    }
}
