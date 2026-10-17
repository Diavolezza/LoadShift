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
            7 -> ScenarioTexts(
                situation = "The company runs two data centers, in Warsaw and in Paris, with the same hardware: 24 servers each, bought four years ago. Both carry the same web shop load. Finance has approved a budget for 12 new servers over the next three years – enough to renew one site completely, or both by half.",
                idea = "New hardware does more work per kilowatt-hour – a new server does the work of two old ones. But making a server emits CO₂ too, and an old one retired early wastes the rest of its life. Whether renewing pays off depends on the grid: where electricity is dirty, the energy saved soon outweighs the hardware; where it is clean, the hardware dominates.",
                screen = listOf(
                    "The game starts paused and pauses at the start of every quarter. Clock and speed – pause, 1×, 4×, 16× – and below it your goals. Each game day is a typical day of a quarter.",
                    "The racks of the site you have chosen: an orange mark shows an old server.",
                    "Hardware renewal: the budget and both sites – grid, servers, CO₂ so far, and how many new servers they get this quarter. Click a site to see it in the hall and the timeline.",
                    "Carbon intensity of the chosen site's grid, quarter by quarter, and the load with the capacity of the running servers.",
                ),
                comparedWith = "compared with the reference operator, who keeps the old servers.",
                rules = "SLA at least 99 % and less CO₂ than the reference",
                remark = "In Warsaw a new server saves many times its embodied emissions; in Paris the same server saves little, because the electricity is almost free of CO₂ – there the old servers can run to the end of their life. The question is not “new or old?” but “where does new hardware replace dirty electricity?”. The earlier the budget goes to the dirty grid, the more it saves; every quarter of waiting costs.",
                term = "Embodied Carbon",
                termText = "The emissions of making, shipping and disposing of hardware, as opposed to those of the electricity it uses. The SCI books them over the hardware's life; in a clean grid they can be the larger part. Hardware efficiency means using hardware long and well – and replacing it where the energy saved outweighs them.",
            )
            6 -> ScenarioTexts(
                situation = "The web shop in a German summer, with 40 servers. A heat wave is coming: 28 °C today, 35 °C tomorrow, 37 °C the day after. Autoscaling runs at 50 % target utilization, the cooling keeps 20 °C at the server inlets – the way the last shift left it. Management watches the PUE: the Energy Efficiency Act asks existing data centers for at most 1.5 from July 2027.",
                idea = "Cooling is the biggest part of the facility's own power. Outside air cools for free when it is cooler than the server inlets; in a heat wave the chiller does the work, and the hotter it is outside, the more power it needs. Servers tolerate warmer air than most data centers give them – up to 27 °C – but above 25 °C their fans speed up. And the PUE, facility power divided by IT power, does not always point the right way.",
                screen = listOf(
                    "The game starts paused. Clock and speed – pause, 1×, 4×, 16× – and below it your goals. On the right the PUE right now.",
                    "Your racks: grey servers are off, light blue ones are booting, the blue bar shows how busy a server is.",
                    "Cooling: the setpoint and what cools right now. Below it the capacity rule and the situation right now.",
                    "Outside temperature with the forecast and the setpoint; green: hours in which the outside air cools. Below it the load and capacity.",
                ),
                comparedWith = "compared with the reference operator: autoscaling at 50 %, setpoint 20 °C.",
                rules = "SLA at least 99 % and less CO₂ than the reference",
                remark = "The PUE is a ratio, not a goal. Fewer servers saved the most energy – and raised the PUE, because lighting, UPS and pumps draw the same power for fewer servers. A setpoint of 27 °C gives a better PUE than 25 °C, because the faster fans count as IT power – but it uses more energy. All servers on would have had the best PUE of all. Measure CO₂ or energy per request: the PUE only says how well the building serves the IT, not whether the IT does useful work.",
                term = "PUE (Power Usage Effectiveness)",
                termText = "Facility energy divided by IT energy: 1.0 would mean that all power goes to the servers. The worldwide average is about 1.5, hyperscalers reach 1.1. A warmer setpoint and cooling with outside air (free cooling) lower it.",
            )
            103 -> ScenarioTexts(
                situation = "The web shop serves five kinds of data: product images, catalog pages, exchange rates, search results and, before a transfer, account balances. Today every request computes or fetches its answer anew. The team has ordered four cache servers with 64 GB each, to be on the safe side; nothing is cached yet. On day 2 at 18:00 a sale starts with new prices in the whole catalog – from then on a page with an old price is a stale answer. Autoscaling keeps the web servers in line with the work.",
                idea = "A cache keeps answers for a while, so that the next request is cheap. It works because a few items get most of the requests – the bestsellers, the euro rate. Four things decide: what may be cached at all, how long an entry may live (the TTL – a business question, not a technical one), whether the same thing is asked again, and how much memory is worth its power.",
                screen = listOf(
                    "The game starts paused. Clock and speed – pause, 1×, 4×, 16× – and below it your goals.",
                    "Your racks: grey servers are off, light blue ones are booting, the blue bar shows how busy a server is.",
                    "Caching: the cache servers and the memory used right now, for each kind of data whether it is cached, its TTL and what the business allows, two more measures. Below it the situation right now.",
                    "Cache memory: what the running cache servers hold, what is used and what is expected (dotted). Below it the load and capacity of the web servers.",
                ),
                comparedWith = "compared with the reference operator, who caches nothing.",
                rules = "SLA at least 99 %, at most 0.5 ‰ stale answers and less CO₂ than the reference",
                remark = "A cache is a bet on repetition. It wins where many requests ask for the same thing – images, rates, popular pages – and loses where the answer must be current or is asked only once: a cached account balance is an error, a cached search that nobody repeats only fills memory. Size the memory by what it holds, not by what might come: the hot set fits into one server, more servers only draw power. And a cache must learn when its data change: the TTL bounds the age in normal times, but after a price change only invalidation keeps the answers right – the hard part of caching is not keeping, but forgetting at the right moment. Warming the cache every night sounds diligent, but the pages expire after 15 minutes – long before the first customer comes: work for nothing. HyperLogLog and the materialized view follow the same idea: keep a little, compute less – as long as the business accepts the result.",
                term = "Caching",
                termText = "Keeping results for a while so that they need not be computed or fetched again. The time to live (TTL) says how long an entry counts, the hit rate how often a request finds its answer in the cache. A cache trades memory and freshness for computation.",
            )
            102 -> ScenarioTexts(
                situation = "The web shop is growing: every month 15 % more products, customers and orders – after a year almost five times as much data. Each game day stands for a month. The team spends most of its time on features; for performance it has 2 story points per sprint.",
                idea = "Measured today, some parts of the code hardly matter: a lookup in a list, a query without an index, a nightly job that compares every pair of products. But their work grows with the data – in step with it, or even with its square. Constant savings look bigger today; fixing the parts that grow pays off over the year. The complexity of an algorithm tells you how its work grows.",
                screen = listOf(
                    "The game starts paused and pauses at the start of every sprint. Clock and speed – pause, 1×, 4×, 16× – and below it your goals.",
                    "Your racks: grey servers are off, light blue ones are booting, the blue bar shows how busy a server is.",
                    "Sprint planning: the CPU work of a day by part with its complexity, the backlog with what each item saves today, your story points. Below it the situation right now.",
                    "Carbon intensity of the grid with the forecast, the load so far and the expected load (dotted); the lines on the right show how many servers a load needs.",
                ),
                comparedWith = "compared with the reference operator, whose team changes nothing in the code.",
                rules = "SLA at least 99 % and less CO₂ than the reference",
                remark = "What saves the most today is not what saves the most in a year. A job with O(n²) is a footnote at the start and a large share of the nights after a year; the rewrite in another language saves only a constant share and blocks the team for months. Ask how the work grows, not only how big it is today. And watch for the rebound effect: capacity that is freed fills up quickly with new features.",
                term = "Algorithmic complexity",
                termText = "How the work of an algorithm grows with the amount of data n, written in Big O notation: O(1) stays the same, O(n) grows in step with the data, O(n²) with its square. For small data the difference hardly shows; for growing data it decides.",
            )
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
                remark = "Measure before you optimize: the big items on the team's list hardly mattered, while a one-line change of the log level saved a tenth of the CPU. The checkout is Amdahl's law at work: however much faster a part gets, the whole gains at most its share. Less work per request lowers the energy and – with autoscaling – the number of servers; a data center that needs fewer servers also needs less hardware. The SCI per 1,000 requests shows the effect independent of the load.",
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
                idea = "Autoscaling does what you did by hand in scenario O1: it switches servers on and off for the load. The target utilization decides how much headroom stays free. Little headroom saves energy – but a server takes 30 minutes to boot, and the autoscaler knows only the usual day; a sudden peak finds it unprepared. Headroom all the time costs energy all the time; a minimum for a known event costs it only when it is needed.",
                screen = listOf(
                    "The game starts paused. Clock and speed – pause, 1×, 4×, 16× – and below it your goals.",
                    "Your racks: grey servers are off, light blue ones are booting, the blue bar shows how busy a server is.",
                    "The capacity rule: all on, a fixed number, or autoscaling with a target utilization and, if you want, a minimum for a time window. Choose, set, apply. Below it the situation right now.",
                    "Carbon intensity of the grid with the forecast, the load so far and the expected load (dotted). The campaign is marked – but it is not in the expected load.",
                ),
                comparedWith = "compared with the reference operator, who leaves every server on.",
                rules = "SLA at least 99 % and less CO₂ than the reference",
                remark = "Headroom is insurance: it costs energy every hour, whether the peak comes or not. When a peak is known in advance, scheduled scaling is cheaper – cloud platforms offer it next to the autoscaler. Predictive autoscaling goes one step further and learns such peaks from the past. Part of the 30 minutes is the software itself: a Java service often needs a while to start and warm up. GraalVM Native Image or CRaC (Coordinated Restore at Checkpoint) start it in a fraction of the time – the faster a server is ready, the less headroom the autoscaler needs.",
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
