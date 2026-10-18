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
            12 -> ScenarioTexts(
                situation = "A week of the web shop in Germany: the shop runs with autoscaling at 50 %, six large batch jobs – backups, reloads, billing – start as soon as they arrive. The board has announced a \"green data center\" and asks for a contract that makes the report green. Three offers are on the table: green certificates, a contract with a wind farm, and 24/7 carbon-free energy.",
                idea = "Not every green is equally green. Certificates change the report, not the electricity. A wind farm delivers clean energy only while the wind blows – the work done in those hours is clean. 24/7 carbon-free energy covers every hour, but it costs. Count in the hour, not on paper.",
                screen = listOf(
                    "The game starts paused. Clock and speed – pause, 1×, 4×, 16×, 64× – and below it your goals: SLA, deadlines, electricity cost and less CO₂, counted in the hour.",
                    "Your racks: grey servers are off, the blue bar shows web requests, the amber bar batch jobs.",
                    "Three tabs: the electricity contract with the CO₂ counted three ways, the shop's capacity rule and the batch rule.",
                    "Carbon intensity of the grid; below it, depending on the tab, the data center's power and the clean energy of the contract in the same hour, the shop's load or the batch jobs.",
                ),
                comparedWith = "compared with the reference operator, who buys the grid mix, runs the shop with autoscaling at 50 % and starts every job at once.",
                rules = "SLA at least 99 %, every job by its deadline, electricity cost at most 10 % above the reference and less CO₂ than the reference, counted in the hour",
                remark = "Green certificates made the report zero and changed nothing in the grid: the hydro plants behind them run anyway. In the report the grey contract even looked worse than the grid's average – the residual mix is what is left after others have claimed the green part [S36]. The wind farm adds clean energy, but only while the wind blows: moving the batch jobs into the windy hours let the data center use more of it. 24/7 carbon-free energy covers every hour – and it was affordable only once the data center needed less energy. Google reports 66 % carbon-free energy in the hour across its data centers [S38]; the GHG Protocol's revision of Scope 2 moves the market-based report towards matching in the hour [S37].",
                term = "24/7 Carbon-Free Energy",
                termText = "Matching electricity consumption with carbon-free generation in the same hour and the same grid, instead of buying certificates for the year. The GHG Protocol knows two ways to report Scope 2: location-based (the grid's average) and market-based (contracts and certificates); only matching in the hour shows what the electricity really was.",
            )
            11 -> ScenarioTexts(
                situation = "A week in January. A high-pressure area lies over Germany: hardly any wind, hardly any sun – a dark doldrums. Coal and gas carry the grid, around 500 g CO₂ per kWh. The forecast expects the wind back on day 5. In the data center: the web shop on 40 CPU servers, six batch jobs, and an AI model that must be trained on 4 GPU servers by the end of the week. The previous shift has left everything as it was: all servers on, every job at once, the training at full power.",
                idea = "Everything you have learned, together. Waiting for clean hours within a day does not help now – every hour is dirty. What helps: use less (size the shop with autoscaling) and move what can wait for days into the wind: the training is due only at the end of the week, three of the batch jobs too. The others cannot wait.",
                screen = listOf(
                    "The game starts paused. Clock and speed – pause, 1×, 4×, 16× – and below it your goals: SLA, every deadline, the training done and less CO₂.",
                    "The hall: 40 CPU servers for the shop and the batch jobs (at most 16 for batch), 4 GPU servers for the training – blue: web requests, amber: batch jobs, violet: training.",
                    "Three tabs – shop, batch, training – each with its rules. Apply applies all of them; an amber note shows changes not applied yet.",
                    "Carbon intensity of the grid with the forecast; below it all three kinds of work: the shop's load and capacity, the batch jobs and the training's progress.",
                ),
                comparedWith = "compared with the reference operator, who leaves all servers on, starts every job at once and trains at full power without a break.",
                rules = "SLA at least 99 %, every job by its deadline, the training done and less CO₂ than the reference",
                remark = "A dark doldrums is the hard case of Carbon Awareness: when every hour is dirty, shifting within the day gains nothing. Two things remained. Using less – the autoscaler saves in every hour, wind or not; it is the lever that never depends on the weather. And waiting for days – only work whose deadline is far enough away can do that: the training and three of the batch jobs, about a quarter of the energy. The forecast promised the wind a day early; a plan made with it put the waiting jobs into a day that was still dirty – plans that look days ahead must be made again when the forecast changes. Germany has a few such weeks every winter; grids with storage, hydropower and links to their neighbours bridge them, and data centers that can wait help them.",
                term = "Carbon Awareness",
                termText = "Doing more when the electricity is clean and less when it is dirty – by Time Shifting, Location Shifting and Demand Shaping. Together with energy efficiency and hardware efficiency it is one of the three principles of the Green Software Foundation; in a dark doldrums all three are needed.",
            )
            10 -> ScenarioTexts(
                situation = "The web shop's batch cluster in Frankfurt: 14 servers in Germany's autumn grid, about 335 g CO₂ per kWh. The company also rents 8 servers in Oslo – Norway's grid runs on hydropower and emits about 30 g, a tenth. Over three days ten jobs arrive: daily reports, two AI trainings, video encoding, customer analytics, a search index and price calculations.",
                idea = "Location Shifting: let the work run where the electricity is clean. But work does not travel alone – its data must go along. Moving terabytes takes hours and energy in the network; a job that talks to a database all the time becomes slow far away from it; and some data may not leave the country at all. Move what needs much computing and little data.",
                screen = listOf(
                    "The game starts paused. Clock and speed – pause, 1×, 4×, 16× – and below it your goals: every job on time, customer data in Germany and less CO₂.",
                    "Both sites side by side: grey servers are off, light blue ones are booting, the amber bar shows a server working on a job.",
                    "Location: for every job its servers, duration, time window and data – and where it runs. Decide before the start; above it the batch rule for both sites.",
                    "Both sites side by side: the carbon intensity of the grid with the forecast (the other site as a thin line), and below it one row per job of that site – light: the time between arrival and deadline, filled: running or done.",
                ),
                comparedWith = "compared with the reference operator, who runs every job in Frankfurt as soon as it arrives.",
                rules = "every job by its deadline, customer data in Germany and less CO₂ than the reference",
                remark = "Location saves more than time here: no hour in the German grid comes close to Norway's hydropower. Look at the energy: it rose – the transfers need energy too – and still the CO₂ fell. Location Shifting saves carbon, not energy. The trainings were the best candidates – hours of computing for little data. The videos were the worst: 30 terabytes through the network cost more CO₂ than Oslo saved, and the transfer took most of a working day. The price calculation needs its database in Frankfurt and misses its deadline far from it; customer data may not leave the country by contract. How much energy a terabyte in the network needs is uncertain – estimates differ by a factor of ten and more [S24] – but the lesson stays: data have weight. And clean electricity is not unlimited: if everyone moves to Norway, it runs short there. Location Shifting works best with capacity that is there anyway.",
                term = "Location Shifting",
                termText = "Moving flexible work to the place where the electricity is cleanest. Together with Time Shifting it is called Demand Shifting – part of the Green Software Foundation principle Carbon Awareness. Its limits: moving the data, the latency to users and other systems, and where data may be stored.",
            )
            9 -> ScenarioTexts(
                situation = "A video streaming service with its servers in Poland. It is summer: the sun makes the middays cleaner, but in the evening – when most people watch – coal carries the grid. The viewers watch on phones (40 %), laptops (25 %) and TVs (35 %). Today every device gets 4K.",
                idea = "The cleanest energy is the energy not used. 4K needs four times the work of HD – and on a phone screen nobody sees the difference. Demand shaping goes one step further: while the grid is dirty, the service lowers the quality a little, as far as the viewers accept it. Their rating is the limit.",
                screen = listOf(
                    "The game starts paused. Clock and speed – pause, 1×, 4×, 16× – and below it your goals: the viewers' rating and less CO₂.",
                    "Your racks: grey servers are off, light blue ones are booting, the blue bar shows how busy a server is.",
                    "Video quality: the highest quality per device with the viewers' rating, the eco mode and its threshold. Below it the situation right now.",
                    "Carbon intensity of the grid with the forecast, the eco threshold and the hours with eco mode on (green); below it the load and capacity.",
                ),
                comparedWith = "compared with the reference operator, who streams 4K to every device.",
                rules = "Viewers' rating at least 93 % and less CO₂ than the reference",
                remark = "Most of the saving needed no compromise: 4K on a phone is work nobody sees. The eco mode adds the rest – in the dirty evening hours, where it saves most and where it is noticed most. SD went too far: the viewers' rating fell below the target. In reality most of the energy of streaming goes into networks and devices, not into the data center [S8]; a lower bitrate saves there as well.",
                term = "Demand Shaping",
                termText = "Adapting the demand to the supply of clean electricity – here through the quality of the service – instead of moving it in time or place. Eco modes of apps and streaming services are the common examples; the users should notice little and be able to choose.",
            )
            8 -> ScenarioTexts(
                situation = "The shop wants a language model for its search, trained by the end of day 3. Eight GPU servers are ready, each up to 10 kW. It is spring in Germany: sunny middays, a calm first day, a windy second day, a calm third. The data science team would start at once, with all servers and without limits.",
                idea = "AI training is the most flexible load there is: it needs a lot of energy, but not at a particular hour, and it can pause at a checkpoint. Three levers: when it runs (the greenest hours of the forecast), how much power each GPU may draw (a power cap saves more energy than it costs in time), and how many servers train together (more are faster, but they spend time waiting for each other).",
                screen = listOf(
                    "The game starts paused. Clock and speed – pause, 1×, 4×, 16× – and below it your goals: the training done by the deadline, and less CO₂.",
                    "The GPU servers: dark ones train, the violet bar shows their power.",
                    "Training run: progress, GPU servers, power cap and when to train – choose, set, apply.",
                    "Carbon intensity of the grid with the forecast; below it when the GPUs train and the progress, ahead the plan with the rules now.",
                ),
                comparedWith = "compared with the reference operator: eight servers, no cap, at once.",
                rules = "Training done by the end of day 3 and less CO₂ than the reference",
                remark = "All three levers work, and they add up. The greenest hours move the work into wind and sun; the power cap saves energy because the last watts of a GPU bring little speed (MIT Lincoln Laboratory measured 12–15 % less energy for about 3 % more time); and fewer servers waste less time waiting for each other – as long as the training still finishes in time. Each pause costs a checkpoint load: a plan that jumps between hours pays for it.",
                term = "Carbon-Aware Training (Power Capping)",
                termText = "Training jobs that do not have to finish at a particular hour are moved into the hours with the cleanest electricity, paused at checkpoints and resumed; together with power capping of the GPUs this is one of the most effective levers for the footprint of AI.",
            )
            7 -> ScenarioTexts(
                situation = "The company runs two data centers, in Warsaw and in Paris, with the same hardware: 24 servers each, bought four years ago. The web shop grows by 15 % a year, at both sites alike. Every year finance approves four new servers for both sites together, and every year the manufacturers bring out a generation that needs less power. From year 3 the old servers are past their six-year life and begin to fail.",
                idea = "New hardware does more work per kilowatt-hour – a new server does the work of two old ones. But making a server emits CO₂ too, about 550 kg. Whether a new server pays off depends on the grid: where electricity is dirty, the energy it saves outweighs its making within weeks; where it is clean, hardly ever. A site in a clean grid needs new servers only when its capacity runs short – and then preferably the newest generation.",
                screen = listOf(
                    "The game starts paused and pauses at the start of every year. Pause and run – a quarter takes two seconds. Below them your goals: the SLA counts at each site.",
                    "Both data centers: an orange mark shows an old server; failed servers are gone.",
                    "Hardware renewal: the year's budget, the generation on offer, and per site its servers, capacity against the peak load, CO₂ so far, and how many new servers it gets.",
                    "Above: CO₂ per quarter and site, electricity and hardware. Below: capacity of each site and the peak load, which grows.",
                ),
                comparedWith = "compared with the reference operator, who buys new servers wherever capacity runs short.",
                rules = "SLA at least 99 % at each site and less CO₂ than the reference",
                remark = "In Warsaw a new server saves many times its embodied emissions; in Paris it saves little, because the electricity is almost free of CO₂ – there it only has to carry the growth and replace the failed servers, and later generations do that better. The question is not “new or old?” but “where does new hardware replace dirty electricity?”. The earlier the budget goes to the dirty grid, the more it saves – as long as the clean site keeps enough capacity.",
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
            107 -> ScenarioTexts(
                situation = "A logistics company's system: customers track their deliveries, dispatchers plan the trucks' routes. It was split into four microservices – tracking, routes, customers and trucks – that call each other with REST and JSON; the routes service also asks a GPS provider and a map service on every request. In the evening up to 15,000 customers a second look where their parcel is – and the tracking takes a third of a second. Once an hour the dispatchers' route optimization needs 200 cores for ten minutes.",
                idea = "In a distributed system, communication is work: every call is serialized, sent, received and deserialized, and every instance runs its runtime even without a request. Fewer and leaner calls, data that is already there and a cut that fits save energy – as long as the customers still see where their truck really is.",
                screen = listOf(
                    "The game starts paused. Clock and speed – pause, 1×, 4×, 16× – and below it your goals: the tracking within 250 ms, data up to date and less CO₂.",
                    "The hall: the servers the services run on; the blue bar shows how busy they are.",
                    "The architecture: a diagram of the services and their calls as your settings would build them, the cut, the communication between the services and how long the providers' answers are reused. Below it what the settings running now measure.",
                    "Carbon intensity of the grid; below it the CPU: tracking, route optimization, and what communication, external calls and idle instances add; the line shows the servers ready.",
                ),
                comparedWith = "compared with the reference operator, who keeps the four microservices with REST, synchronous calls and no cache.",
                rules = "Tracking within 250 ms, positions at most 1 min and traffic at most 5 min old, and less CO₂ than the reference",
                remark = "Communication is work: every remote call is serialized, sent and deserialized, every instance runs its runtime even without a request. Events with a local copy of the master data saved two calls per request, gRPC with Protobuf halves the bytes of JSON [S33], and serverless functions saved idle instances – but only once nobody called them on every request; called synchronously, they cost more than they saved. The modular monolith needs no calls between its modules, but it scales as a whole: it had to keep the capacity for the route optimization ready all day, while the optimization as a service of its own grew for ten minutes and shrank again. That is what microservices are for: cutting along different needs for scaling [S34]. A monolith with only the optimization as a service of its own combines both. The cache made the tracking fast enough in the first place; reused for too long, it shows the customers a truck that has long driven on.",
                term = "Green Software Architecture",
                termText = "Choosing the architecture style, the cut and the communication with their energy in mind: fewer remote calls, compact formats, asynchronous events where data may be a little late, caches for external calls, scale to zero for rarely used parts.",
            )
            106 -> ScenarioTexts(
                situation = "A team of 40 developers works on the web shop. Every commit starts the full pipeline – build, unit and system tests and the slow UI tests – on 16 build agents that run day and night. Six test environments – integration, acceptance and four for previews – run with three replicas each, at night and at the weekend too. On Friday at 14:00 the week's release goes out.",
                idea = "A pipeline is a data center of its own, and most of it waits: agents without a job, environments nobody uses at night. Start only what is needed, run only the tests a change can break, run the slow tests when the grid is clean – and keep the team's feedback fast and the release tested.",
                screen = listOf(
                    "The game starts paused. Clock and speed – pause, 1×, 4×, 16×, 64× – and below it your goals: feedback within 20 minutes, a tested release and less CO₂.",
                    "The hall: the first racks hold the build agents, the others the test environments; the blue bar shows how busy an agent is.",
                    "The pipeline: build agents and cache, tests, when the pipeline starts, and the test environments. Below it one run with your settings.",
                    "Carbon intensity of the grid; below it the servers on – build agents and test environments – and the UI tests at night.",
                ),
                comparedWith = "compared with the reference operator, who leaves agents and environments on and runs all tests with every commit.",
                rules = "Feedback within 20 minutes, UI tests before the release and less CO₂ than the reference",
                remark = "Most of the energy went into waiting: environments at night and at the weekend – they are used on weekdays from 8 to 18, a third of the week – and agents without a job. Agents on demand only help with a remote cache: an agent that starts empty downloads and compiles everything again, and the feedback becomes too slow. Test impact analysis cuts the tests to those a change can break [S31]; the slow UI tests do their job at night, in the greenest hours, as long as they have run before the release. Collecting commits saves runs but makes the team wait – fast feedback is a quality too. Tools such as Eco CI measure what a pipeline uses [S32].",
                term = "Green CI/CD",
                termText = "Running build and test infrastructure only when and as much as it is needed: build agents on demand with a shared cache, test impact analysis, expensive tests at clean times, test environments off when nobody uses them, fewer replicas outside production.",
            )
            105 -> ScenarioTexts(
                situation = "The shop's product pages have grown heavy: photos of 2,000 pixels for every screen, a product video that starts by itself, 100 products per list and six tracking tags from marketing. A visit transfers about 12 MB – more than four times the median web page [S29]. And the page is slow: its largest image appears only after 3.2 s.",
                idea = "Every byte costs energy three times: in the data center, in the network and on the visitor's device. This scenario counts all three, and the data center is the smallest part. Make the pages lighter – and keep them fast. Speed is measured as LCP (Largest Contentful Paint): the time until the largest image or text block of the page is visible. Up to 2.5 s counts as good, measured at the 75th percentile – three of four visits must be at least that fast.",
                screen = listOf(
                    "The game starts paused. Clock and speed – pause, 1×, 4×, 16× – and below it your goals: LCP at most 2.5 s and less CO₂.",
                    "The hall: the shop's servers; the blue bar shows how busy a server is.",
                    "The pages: images, product video, product lists and how visitors are counted. Below it a visit to the pages online now: its weight by part, and the LCP.",
                    "Carbon intensity of the grid; below it the power of the three parts: data center, network and the visitors' devices.",
                ),
                comparedWith = "compared with the reference operator, who leaves the pages as they are.",
                rules = "LCP at most 2.5 s and less CO₂ than the reference",
                remark = "Half of the weight was a video that nobody had asked to watch. Then the images: a modern format, in the size the screen needs, and only when the visitor scrolls to them. Two changes looked green and made the page slow: converting the images on every request instead of once, and lazy loading the first image as well – the LCP waits for exactly that image [S30]. Light pages are fast pages; here the two goals point the same way. The energy per GB is a model value: it spreads the energy of networks and devices over the data they move [S28], and it is disputed – a network does not need much more energy for one more gigabyte. It shows what heavy pages cause on average, not what a single byte costs. Not counted here: phones replaced early because the web has become too heavy for them.",
                term = "Sustainable Web Design",
                termText = "Designing and building web pages so that they transfer and compute as little as possible: images in modern formats and the right size, no media that plays by itself, less third-party code. It counts the network and the users' devices, not only the servers – and it usually makes pages faster.",
            )
            104 -> ScenarioTexts(
                situation = "The web shop has a new assistant in its chat window. The team's prototype sends every question to a large language model, one question at a time – it was the quickest way to get it working. Since then, three in ten questions are \"Where is my order?\", and the assistant runs on GPU servers day and night.",
                idea = "Not every question needs the largest model – some need no model at all. Choose for each kind of question who answers it, and keep the quality of the answers up. A GPU answers several questions almost as fast as one: batching shares the work, but every answer waits a little.",
                screen = listOf(
                    "The game starts paused. Clock and speed – pause, 1×, 4×, 16× – and below it your goals: the quality of the answers, answers in time and less CO₂.",
                    "The hall: the shop's servers and, in the extra rack, the GPU servers for the models (violet: running).",
                    "The shop assistant: who answers each kind of question, with the quality of the answers, and the batching window. Below it the quality and the answers in time with your settings.",
                    "Carbon intensity of the grid; below it the assistant's power by kind of question (colors as in the panel) and the idle rest of the GPU servers.",
                ),
                comparedWith = "compared with the reference operator, who sends every question to the large model, one at a time.",
                rules = "Quality of the answers at least 92 %, 95 % of the answers within 3 s and less CO₂ than the reference",
                remark = "The largest saving needed no AI: a status lookup answers \"Where is my order?\" better than any model, for a five-hundredth of the energy. A small model is good enough where the answer is in the product data; the large one pays off where it adds quality – complaints and advice. Batching is the second lever: the GPU reads the model's weights once per step for the whole batch, and serving systems cut the energy per answer many times this way [S27] – as long as the wait stays below what the customers accept. Reasoning bought three points of quality for complaints, at two and a half times the energy and nine seconds of waiting. The numbers are game values in the range of published measurements [S25, S26]; they differ a lot between models, hardware and the way the models are served – measure your own.",
                term = "Green AI (Right-sizing Models)",
                termText = "Choosing the smallest model that is good enough for a task – or no model at all – and serving it efficiently, for example with batching or a cache for frequent answers. More and more of the energy of AI goes into answering (inference), not into training.",
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
