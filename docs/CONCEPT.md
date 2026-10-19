# Load Shift – Game Concept

Load Shift is a simulation game for teaching Green IT (web, desktop; later iPad and phones). The player runs a data center through
days and weeks: servers, workloads, cooling and electricity from a real-world grid mix. The goal is to serve the
users reliably and to emit as little CO₂ as possible. The game is a simulation, not a race: the player can pause at
any time, plan in peace and let time run faster when nothing needs to be decided.

Status: concept agreed; decisions in section 13. Values marked as game values are tuned in the play tests.

---

## 1 Teaching goals

The game is built around the principles of green software as the Green Software Foundation describes them [S1]
and the measurement with the Software Carbon Intensity (SCI) [S2]. Every scenario (section 9) teaches one of them,
and the game shows the effect in numbers.

The order follows the order in which the scenarios introduce the goals. Measurement comes first: from the
first debrief on, every result is shown as energy × carbon intensity (plus embodied emissions), so the player has
the yardstick before the first optimisation.

| # | Principle | What the player learns in the game | First in scenario |
|---|---|---|---|
| G1 | Measurement | CO₂ = energy × carbon intensity of the grid, plus the embodied emissions of the hardware. SCI = (operational + embodied emissions) per functional unit. Only measured improvements count; offsets and certificates do not lower the SCI. | 1 (every debrief) |
| G2 | Energy efficiency | Idle servers draw power too. Switch off what is not needed; size capacity to the load. | 1 |
| G3 | Carbon awareness: time shifting | The same work emits less CO₂ when the grid is clean. Flexible work waits for the clean hours. | 2 |
| G4 | Trade-offs | Reliability (SLA), money and CO₂ pull in different directions. There is no setting that wins everywhere. | 4 |
| G5 | Facility efficiency | Cooling costs energy; PUE shows how much. | 6 |
| G6 | Hardware efficiency | High utilization (consolidation) and a long service life; manufacturing emissions count too. | 5 |
| G7 | Demand shaping | When the grid is dirty, do less: lower quality, fewer extras. | 9 |
| G8 | Carbon awareness: location shifting | Where the work runs matters even more than when: Poland emits about 22 times as much per kWh as Norway. It comes last because it needs a second site; scenario 1's debrief already names it as an outlook. | 10 |
| G9 | Software efficiency | Less work per request and per job: measure first (profiling), then fix the hot spots; algorithms and data structures matter more as the data grows. Less work also means fewer servers. | E2 (Engineering track) |

**Audience:** students in a Green IT lecture and people who want to understand why software and operations
decisions change emissions. No prior knowledge is needed; every term is explained where it appears.

---

## 2 Design principles

These principles come from the experience with a real-time prototype that was too hectic to think in.

1. **Time is under the player's control.** Pause, 1×, 4×, 16× and "skip to the next event". Everything can be
   changed while paused. The game pauses by itself when something important happens (section 3.3).
2. **Policies, not clicks.** The player does not micro-manage single requests or single jobs. The player sets
   rules ("run batch work when the grid is below 200 g/kWh, but always before the deadline"), and the simulation
   follows them. This is how real operations work, and it leaves time to think.
3. **One new idea at a time.** Each scenario unlocks only the controls it needs. The full set appears only in the
   last scenarios and in free play.
4. **Find out by playing, compare afterwards.** During the run the game shows the immediate effect of a change
   (servers working, load and capacity, power now) and the information an operator has (forecast, expected load),
   but no forecast of the day's result – otherwise the run would be unnecessary (play test of step 2). After the
   day, the debrief compares the player's result with a reference operator who does nothing clever and shows
   "what if": the same day replayed with other rules (section 8). Where forecasts are uncertain (later scenarios),
   a preview may return, with its uncertainty band, as a lesson about forecasts.
5. **Few, clearly different things.** Four kinds of workload, three kinds of server, two generations. Each has its
   own color and its own shape, readable at phone size.
6. **Real numbers where they exist.** Grid curves, power, PUE and embodied emissions come from published sources
   (section 14). Where the game simplifies, the documentation says so.
7. **Deterministic.** The same scenario with the same decisions always gives the same result. This makes class
   comparisons fair and lets tests check that each lesson works (section 11).

**Deliberately left out:** real-time combat, opponents who attack, paths and maps to defend, placing single
servers on a grid, network topology, green certificates and offsets (the SCI excludes market-based instruments
[S2]).

---

## 3 Time

### 3.1 Clock

- One tick = **15 game minutes**; one game day = 96 ticks.
- Speeds: pause; 1× = one game hour in 4 seconds (a day in about 1.5 minutes); 4×; 16×; "skip to the next
  event". (checked in the first play test, D8)
- A scenario lasts 1 to 7 game days and always starts paused at 00:00 (D10).

### 3.2 Why 15 minutes

The grid curves and load curves change on the scale of hours. A quarter of an hour is fine enough for the
batch queue and the SLA, and coarse enough that a week (672 ticks) can be simulated in a fraction of a second.
This allows the reference operator (section 8) and the forecast preview to run in the background.

### 3.3 Automatic pause (events)

The game pauses and shows a short card when:

- a deadline is at risk (a batch job cannot finish in time with the current policy),
- the SLA falls below its target,
- the forecast changes a lot (for example: wind drops tomorrow, 18:00 forecast up by 40 %),
- a new day begins (optional, on by default in the first scenarios),
- a scenario event happens (marketing campaign, heat wave, server failure).

Each type can be switched off.

---

## 4 Grid mix

The grid model is taken over from the prototype and has been checked against its sources.

### 4.1 Regions

Six European grids with hour-of-day profiles from Electricity Maps' grid review 2025 [S3]; life-cycle carbon
intensities, flow-traced (imports included), in gCO₂e/kWh [S4].

| Region | 2025 mean | Daily shape | Lowest / highest day | Character |
|---|---:|---|---|---|
| Norway | 30 | flat (28–37 in all seasons) | 26 / 40 | hydro; nothing to shift |
| France | 32 | low; evening peak in winter (35–47) | 16 / 72 | nuclear; low and flat |
| Spain | 132 | 94–161, low at midday | 61 / 236 | solar; midday dip |
| Great Britain | 176 | e.g. winter 149 (03:00) – 234 (17:00) | 70 / 348 | wind; weather decides |
| Germany | 335 | e.g. spring 198 (11:00) – 410 (02:00) | 111 / 560 | solar and wind; deep midday dip, big swings between days |
| Poland | 677 | e.g. summer 456 (10:00) – 743 (20:00) | 474 / 886 | coal; high everywhere, shifting still helps |

The full per-season table (trough and peak with hours) and the generator `tools/grid/curves.py` are taken over.

### 4.2 Curve, weather and forecast

- **Curve:** per region and season 24 hourly values, interpolated linearly between the hours.
- **Weather:** every game day draws a day factor from the scenario seed that scales the day's curve (triangular
  distribution around 1; the extremes match the lowest and highest daily mean of 2025). Scenarios can also fix
  the weather ("a windless week").
- **Forecast:** the next 24 hours (later scenarios: 48 hours) as a curve with an uncertainty band that widens with
  the lead time. Half width one day ahead: Norway ±4 %, France ±8 %, Poland ±10 %, Spain ±12 %, Germany ±20 %,
  Great Britain ±25 %. Google's carbon-aware scheduling works with day-ahead forecasts with errors between 0.4 %
  and 26 % depending on grid and lead time [S5].

### 4.3 Electricity price

```
price(t) = base × (0.7 + 0.3 × I(t) / I_mean_region)  +  co2_price × I(t)
```

The first part follows the mix (clean hours are usually cheap hours: a simplified merit order); the second is the
CO₂ price, €82.32 per tonne by default (CBAM certificate price Q3 2026 [S6]). Scenarios can raise it ("what if a
tonne cost €200?"). Base price 10 ct/kWh at the region's mean intensity.

---

## 5 Workloads

Four kinds, each with a clear role in the lessons. The app uses the English names in both languages (D3).

| Workload | Color / shape | Arrives | Flexibility | Unit of work | Lesson |
|---|---|---|---|---|---|
| **Interactive** (web shop, app backend) | blue, circle | continuously, daily curve with an evening peak | none: must be served now | requests | G2, G4 |
| **Batch** (reports, backups, data imports) | amber, square | as jobs with a size and a deadline (e.g. arrives 18:00, due 08:00) | high within the deadline | job | G3 |
| **AI training** | violet, hexagon | one big job over days, can pause and resume (checkpoints) | high, deadline in days | job with progress | G3, G6 |
| **Video streaming** | teal, triangle | continuously, evening peak | quality can be lowered | streams | G7 |

### 5.1 Interactive

- Demand per tick from a daily curve (night about 20 % of the evening peak) times a small random noise from the
  seed; scenarios add events (a marketing campaign doubles demand for three hours).
- Served requests = min(demand, capacity of the running servers that serve interactive work).
- **SLA:** the share of demand served in time over the scenario. Target 99 % by default. Load above 85 % of
  capacity counts as "slow": requests are served, but count only half toward the SLA (simple latency model, game
  value).

### 5.2 Batch

- Each job has a size (server-ticks), an arrival time and a deadline. Jobs wait in a queue (the queue is shown as a
  list with a bar for each job: size, progress and time left).
- **Policy** (one for all batch jobs, later per job type):
  - *As soon as possible* (the reference operator's choice),
  - *Greenest hours before the deadline* (uses the forecast),
  - *Cheapest hours before the deadline*,
  - *Below a threshold* (e.g. 200 g/kWh).
- **Safety rule:** whatever the policy, a job starts when waiting longer would miss the deadline. A missed deadline
  costs money and counts in the debrief.
- Batch work uses free capacity of running servers first; extra servers are switched on for it only if the policy
  allows (switch "may start servers for batch").

### 5.3 AI training

- One job, for example 400 GPU-server-hours, deadline in 3 days. It can only run on GPU servers.
- Policy as for batch, plus **power cap** for GPUs: 100 %, 85 % or 70 %. At 70 % the work per hour drops
  slightly and the energy per unit of work drops more. Reference: MIT Lincoln Laboratory capped GPUs at 150 W in
  BERT training: 12–15 % less energy, about 3 % longer training time; the GPUs ran about 30 °F cooler [S7].
  Game values: 85 % cap → −8 % energy per unit, +2 % time; 70 % cap → −14 % energy per unit, +6 % time.
- Pausing and resuming costs a little (checkpoint: 1 tick without progress after each resume, game value).

### 5.4 Video streaming

- Demand like interactive, peak later in the evening.
- **Eco mode** lowers the quality: −30 % energy per stream (game value) and a small loss of user satisfaction,
  which reduces income a little. Policy: off, always, or above a threshold intensity.
- The IEA puts streaming's footprint in perspective: data center energy is a small part of it; the devices and
  networks dominate [S8]. The game counts only the data center part and says so.

---

## 6 Servers

Three kinds, two generations each. One server = one physical machine; servers stand in racks (section 10).

| Server | Shape / color | Serves | Max power | Idle share | Capacity (game units per tick) | Embodied |
|---|---|---|---:|---:|---:|---:|
| CPU server, old | grey box, two fans | interactive, batch, video | 600 W | 35 % | 10 | 500 kg CO₂e |
| CPU server, current | light box, two fans | interactive, batch, video | 600 W | 26 % | 20 | 550 kg CO₂e |
| ARM server, current | slim box, green stripe | interactive, video | 300 W | 20 % | 12 | 450 kg CO₂e |
| GPU server, old | wide box, violet | AI training | 6.4 kW | 15 % | 1 GPU-server-hour per hour | 1,450 kg CO₂e |
| GPU server, current | wide box, violet, bright | AI training | 10.2 kW | 15 % | 2.5 | 1,800 kg CO₂e |

Derivation (from the prototype's sources): SPECpower – mean full load about 303 W per socket since 2022, idle
25.7 % of full load in 2024 [S9]; Dell PowerEdge R760 life-cycle assessment, manufacturing about 10–15 % of
2,896–4,177 kg [S10]; AWS Graviton up to 60 % less energy at the same performance [S11]; DGX H100 up to 10.2 kW
[S12]; HGX H100 baseboard 1,312 kg cradle-to-gate plus a host of about 500 kg [S13]. Capacities are game values
chosen so that "current" does about twice the work per kWh of "old".

- **Power:** P = P_idle + (P_max − P_idle) × utilization. Off = 0 W.
- **Switching on** takes 2 ticks (30 game minutes, boot and warm-up; game value); switching off is immediate.
- **Autoscaling for interactive and video:** the player sets a **target utilization** (40–90 %). The simulation
  keeps enough servers on to meet the forecast demand at that utilization. Low target = more headroom, safe SLA,
  more idle power; high target = efficient, but risky at peaks (lesson G2/G4).
- **Buying and retiring:** servers are bought between days (or while paused) and arrive the next morning.
  Retiring a server early books the rest of its embodied emissions (section 7.3).

---

## 7 Facility and emissions

### 7.1 Cooling and PUE

| Value | Source |
|---|---|
| Average PUE worldwide 1.52 (2026), 1.54 (2025) | Uptime Institute [S14] |
| Hyperscalers 1.10–1.15; Google fleet 1.09 | [S14], [S15] |
| Germany (EnEfG): new data centers from July 2026 PUE ≤ 1.2; existing ones ≤ 1.5 from July 2027 | [S16] |
| ASHRAE recommended inlet temperature 18–27 °C | [S17] |

Game model:

- The facility has a **cooling type**: room air conditioning (overhead 0.45 per kW of IT power), hot/cold aisle
  containment (0.30), free cooling (0.15 when the outside air is cold enough, otherwise like containment), direct
  liquid cooling (0.05; needed for GPU servers at full power). Plus 7 % for power distribution and lighting.
- **Inlet temperature setpoint** 18–27 °C: each degree above 20 °C lowers the cooling overhead by 4 % (game value),
  but above 25 °C the server fans draw more power (+1 % IT power per degree, game value). The sweet spot depends on
  the outside temperature.
- **Outside temperature** per region and season as a daily curve (sources to be collected, see section 13); free
  cooling works below 18 °C outside.
- **PUE** = (IT energy + cooling + 7 %) / IT energy, per tick; shown as the day average.

### 7.2 Operational emissions

O = facility energy × I(t), summed per tick.

### 7.3 Embodied emissions

- Each server books TE / (6 × 365) per day in service (lifespan 6 years, as Microsoft and Google extended it
  [S18]).
- Retiring a server before its 6 years books the rest at once ("wasted embodied emissions"). Each server has an
  age at scenario start, so a scenario can start with an old fleet.
- The refresh lesson (G6) needs years, not days. Scenario 6 therefore uses a coarse time scale: one step = one
  month, three years in total, with average days instead of single hours (D5).

Worked example (derived): a current CPU server at 50 % utilization draws 378 W; with PUE 1.4 that is 12.7 kWh per
day, in Germany (335 g/kWh) **4.3 kg CO₂e** per day. Its embodied share is 550 kg / 2,190 days = **0.25 kg** per
day (6 %). The same server in France (32 g/kWh): 0.41 kg per day from electricity, so the embodied share is
**38 %** of the total – in a clean grid the hardware dominates. This matches the direction of the Dell R760
assessment, where the use phase is 87–91 % of the total on US and EU grids [S10].

### 7.4 SCI

```
SCI = (O + M) per R        (GSF SCI v1.1, ISO/IEC 21031:2024 [S2])
```

- **R** (functional unit) per scenario: 1,000 interactive requests, one batch job, one training run, or one hour of
  streaming. The debrief shows the SCI per unit for each workload and in total.
- E includes idle servers and the facility overhead; I is location-based; M as in 7.3.

---

## 8 Money and evaluation

### 8.1 Money

- **Income:** per served interactive request and streaming hour; per batch job finished in time; a fixed fee for the
  training run if it finishes before the deadline.
- **Costs:** electricity (4.3), servers bought, penalties for SLA below target and missed deadlines.
- The budget is not the main goal, but it keeps the trade-offs honest: "switch everything off" is not possible,
  and "buy the best hardware" has a price. Money comes in with scenario 4; before that only CO₂ and SLA count (D4).

### 8.2 The reference operator

For every scenario the game also simulates a **reference operator** with the same seed in the background: all
servers on, batch as soon as possible, no eco mode, setpoint 20 °C, no power cap. Because the simulation is
deterministic, both runs see exactly the same weather and demand.

The debrief compares:

- CO₂ total (operational and embodied), SCI per unit, energy, PUE, SLA, deadlines met, money – player vs reference,
  as numbers and as a difference in percent.
- **Day chart:** carbon intensity of the grid, with the player's power draw on top and batch/training runs as bars;
  the reference operator's power as a thin line.
- **One sentence of insight**, chosen from the largest difference ("Your batch jobs ran at 142 g/kWh on average,
  the reference at 318 g/kWh: −55 % CO₂ for the same work.").

### 8.3 Stars

- ★ the scenario's rules are met (SLA target, less CO₂ than the reference; later also deadlines and a budget not
  negative at the end). One star opens the next scenario.
- ★★ CO₂ at least X % below the reference (per scenario).
- ★★★ a special goal of the scenario (e.g. mean utilization, PUE ≤ 1.2, no wasted embodied emissions, SCI below a
  value).
- The second and third star need the first, not each other. The thresholds are set with the lesson tests: a
  scripted good operator reaches three stars, a rough one one or two, the reference operator none. The aim is that
  a first run usually opens the next scenario and all stars take two or three runs.

### 8.4 Comparison in class

Like a challenge code: a scenario plus a seed gives a short code (5 characters). Everyone who plays the same code
plays the same week. The result card can be shared (text with code, SCI, CO₂, SLA, stars). No server and no
account are needed; the lecturer collects the cards. No online ranking at first (D6).

---

## 9 Scenarios (campaign)

Three tracks (D17, D26): **Operations** – running the data center –, **Engineering** – the software that runs in
it – and **Portfolio** – all applications of a company, in its own data center, in the cloud and as SaaS.
Each scenario introduces one idea. Region and season are chosen so that the idea has a clear effect. The
tracks are independent of each other (D32): each can be the first one.

### 9.1 Operations

| # | Title | Region, season | Length | New controls | Idea |
|---|---|---|---|---|---|
| 1 | Night shift | Germany, autumn | 1 day | servers on/off by hand | Idle power: at night 20 % load, all servers on wastes energy; switching on takes 30 minutes, so plan ahead (G2). Term: Rightsizing. |
| 2 | Wait for the sun | Spain (annual profile) | 2 days | batch jobs planned by hand, forecast | Reports and backups move into the midday dip – the night is not clean in a solar grid (G3). Term: Time Shifting. |
| 3 | When the wind drops | Great Britain, winter | 3 days | batch rules (fixed hour, greenest hours, threshold), deadline safety | The rules automate what the player planned by hand in scenario 2. A fixed threshold works while the wind blows and fails when it drops: without the deadline safety jobs miss their deadlines, with it they start at the last moment in dirty hours. A rule that compares the hours of the (uncertain) forecast adapts to the weather – if the plan is made again when the forecast changes (G3, D15). Term: Carbon-Aware Scheduling. |
| 4 | Headroom | Germany, winter | 2 days | capacity rules: all on, fixed, autoscaling with target utilization and a scheduled minimum | Autoscaling automates what the player did by hand in scenario 1. A newsletter on day 2 doubles the load for three hours; the autoscaler knows only the usual day and reacts too late (30 minutes boot time). Headroom all the time costs energy all the time; a minimum for the known event costs it only when needed (G4). Term: Autoscaling (with Scheduled Scaling). |
| 5 | Consolidation | Germany, autumn | 1 day | placing VMs on hosts by hand (two dimensions: CPU and RAM) | Bin packing: fewer hosts running means less idle power. Pack by the peaks, not the means; VMs with opposite daily profiles fit together; often the RAM is full before the CPU; packing too tight lets the peaks collide (G6, D18). Term: Consolidation (Bin Packing). |
| 6 | Heat wave | Germany, summer | 3 days | inlet setpoint (18–27 °C), capacity rule | Outside air cools for free while it is 6 K below the setpoint; in the heat the chiller works, the hotter the more. A warmer setpoint saves chiller power, above 25 °C the server fans eat it up: the sweet spot is 25 °C. The PUE trap: fewer servers save the most energy but raise the PUE (fixed facility power); 27 °C and all servers on give the best PUE and more CO₂. The stars count CO₂ (G5, D21). Term: PUE (Power Usage Effectiveness). |
| 7 | Old iron | Warsaw and Paris | 3 years (12 quarters, one game day each) | where and when new servers go: a budget of four a year | Two data centers with the same old fleet (24 servers, four years old). The shop grows by 15 % a year; every year a generation that needs 17 % less power; from year 3 the old servers fail. A new server does the work of two old ones; making it emits 550 kg, booked over six years. In the Polish grid a new server saves many times that, in the French grid hardly anything: new hardware first where the grid is dirty, at the clean site only what the capacity needs, as late as possible (G6, G1, D22). Term: Embodied Carbon. |
| 8 | Training run | Germany, spring (calm, windy, calm) | 3 days | GPU servers in parallel, power cap, schedule (without a break or greenest hours) | A model needs a fixed amount of GPU work by the end of day 3. The greenest hours move the work into wind and sun; a power cap saves energy for a little time [S7]; fewer servers waste less time waiting for each other. A pause costs a checkpoint load (G3, G6). Term: Carbon-Aware Training (Power Capping). |
| 9 | Eco mode | Poland, summer | 2 days | highest quality per device (4K, HD, SD), eco mode with threshold and eco quality | A streaming service: phones 40 %, laptops 25 %, TVs 35 %; 4K needs four times the work of HD. HD on phones and laptops costs hardly any rating; an eco mode lowers TVs to HD while the grid is dirty – in the evening, when most watch. The viewers' rating must stay at 93 % (G7). Term: Demand Shaping. |
| 10 | Two sites | Frankfurt and Oslo, autumn | 3 days | where each job runs (before the start), batch rule for both sites | Ten batch jobs; Oslo's grid emits a tenth of Frankfurt's, but has only 8 servers. A moved job needs its data there first: the transfer takes hours and network energy in the German grid. Move what computes much and has little data (trainings, reports, the search index); the product videos (30 TB) cost more in the network than Oslo saves, the price calculation talks to the live database in Frankfurt and takes three times as long far from it, customer data must stay in Germany. Location saves CO₂, not energy (G8, D23). Term: Location Shifting. |
| 12 (shown as 11) | Green on paper | Germany, autumn, windy and calm days | 7 days | electricity contract (grid mix, green certificates, wind farm PPA with the week's volume, 24/7 carbon-free 90 % in every hour), capacity rule, batch rule | The same data center counted three ways: location-based, market-based as in the report (certificates and contracted volumes count zero, the rest at the residual mix [S36]) and matched in the hour – the stars count the last. Certificates make the report zero and change nothing; the wind farm helps in windy hours, more so with the batch jobs moved there; 24/7 carbon-free energy covers every hour but is affordable only with less energy (budget +10 %) (iSAQB GREEN, LZ 2-1, 2-2, 7-3; GHG Protocol Scope 2 [S37]). Term: 24/7 Carbon-Free Energy. |
| 11 (shown as 12) | Dark doldrums | Germany, winter, windless until day 6 | 7 days | all: capacity rule for the shop, batch rule, training (servers, cap, schedule) – in tabs | Every hour is dirty, so shifting within the day gains nothing. What remains: using less (autoscaling, independent of the weather) and waiting for days – the training (due at the end of the week) and three of six batch jobs can wait for the wind, the others cannot. The forecast expects the wind a day early until day 4, 12:00; plans made with it must be made again. Each lever alone gives one star, two give two, all three (with the plan made again and the training capped) three (G1–G4, D24). Term: Carbon Awareness. |

**Scenario 5 in more detail.** Ten hosts (current CPU servers with RAM as a second capacity) and about 24 VMs of
the web shop, each with a RAM size and a daily CPU profile: shop front ends peak in the evening, batch VMs at
night, reporting in the morning. The player drags the VMs onto hosts before the day starts; hosts without a VM are
off. During the day a host whose VMs together need more CPU than it has makes requests slow (SLA). The debrief's
"what if" shows the classic heuristics – one VM per host, First Fit Decreasing by mean CPU, by peak CPU, by RAM,
and by peak CPU and RAM together – so the player sees that bin packing is a hard problem (NP-hard) where a simple
heuristic gets close. A later rule ("place automatically") follows the pattern of D11 and D13.

### 9.2 Engineering

The time scale is coarser: one step is a sprint (a week) or a month; within a step the day simulation runs as
before. The player has a backlog of improvements and a team with a fixed capacity per step. Each item changes the
work per request or per job; the effect shows in the SCI per 1,000 requests and in the servers needed.

| # | Title | Setting | Length | New controls | Idea |
|---|---|---|---|---|---|
| E1 | How many nines? | an insurer's customer portal: seven building blocks, five quality scenarios with the downtime each may have in a year; today everything active-active on two sites ("99.99 % for everything") | a year, no clock | per building block an availability tactic (single and restarted, cold standby, warm standby, active-active); claims into a queue; a circuit breaker for the recommendations | Quality scenarios instead of one figure for everything (LG 3-1). A scenario waits for every building block it calls synchronously, so their availabilities multiply: the shared login and policy database need more than any single function. Decoupling breaks the chain – the queue frees the claims from the database and evens out the peaks, the circuit breaker frees the contract overview from the recommendations – and redundancy is then needed only where a scenario asks for it (LG 3-2: resilience against energy efficiency). Term: Availability Tactics [S44]. |
| E2 | Measure first | web shop, Germany | 4 weeks (weekly sprints) | engineering backlog, team capacity, profiling | The team's estimates of where the energy goes are wrong. One sprint of profiling shows the real hot spots (for example an N+1 database query and JSON serialization, not the image service everybody suspects). Optimizing by guess wastes sprints; less work per request also means fewer servers and less hardware (G9, G1). Term: Profiling. |
| E3 | Growing data | web shop, Germany | 12 months (one game day each) | backlog with algorithmic complexity, carry-over | The data grows every month. A batch job with O(n²) and a lookup in a list instead of a hash map get more expensive month by month; a database index stops a query from scanning everything. Fixing early saves the most; waiting costs energy and, at some point, hardware (G9, G6). Term: Algorithmic Complexity. |
| E4 | Hit or miss | web shop, Germany | 2 days | cache servers, per kind of data cached or not and its TTL, two more measures | Five kinds of data with different popularity, size, cost and business limits on their age: product images (a day), catalog pages (15 min), exchange rates (1 min), search results (1 h, but most searches are typed only once), the account balance before a transfer (must be current). All cached kinds share the memory of the cache servers (LRU). The team has ordered four cache servers "to be on the safe side". A cache saves work while it hits, but every cache server draws power all day; a TTL longer than allowed brings stale answers (0 stars); search results fill half the memory and hardly hit. Two more measures follow the same idea: counting unique visitors with HyperLogLog instead of keeping every ID, and a materialized view for the sales dashboard (G9, G1). Term: Caching. |
| E5 | Data diet | web shop, Germany | 2 days | image format and sizes, lazy loading, product video, list length, how visitors are counted | A visit weighs 12 MB. Counted with the network and the visitors' devices (energy per GB after the Sustainable Web Design Model [S28]), the data center is the smallest part. The video that plays by itself is half the weight; modern formats in the right size and lazy loading below the fold do the rest. Two traps make the page slow: converting images on every request, and lazy loading the first image. Rule: LCP at most 2.5 s [S30] (G9, G1). Term: Sustainable Web Design. |
| E6 | Chatty services | logistics system, Germany | 2 days | cut (microservices, modular monolith, monolith with the route optimization as a service of its own), protocol (REST with JSON, gRPC with Protobuf), master data (synchronous calls, events with a local copy), serverless functions for the customer and truck services, how long GPS positions and traffic information are reused | Communication is work: every remote call serializes and deserializes, every instance runs its runtime without load. The modular monolith saves the calls but scales as a whole – it keeps the capacity for the hourly route optimization (200 cores for ten minutes) ready from 6 to 20, while a service of its own scales for its ten minutes; microservices reach three stars with events, gRPC and serverless functions, the monolith with the optimization cut out – serverless only pays for services that are rarely called. The cache for the external providers makes the tracking fast enough; reused too long, the data are too old. Rules: tracking within 250 ms on average, positions at most 1 min and traffic at most 5 min old (iSAQB GREEN, LZ 6-1, 6-2, 6-4). Term: Green Software Architecture. |
| E7 | Constant asking | a delivery app on 400,000 phones; five kinds of update (delivery status, courier on the map, offers, support chat, unread messages) | a year, no clock | per kind one way while the app is open (polling every n seconds, on opening, long polling, stream, push) and one while it is closed (nothing, refresh every 15 minutes, connection kept open, push); conditional requests; one background refresh for all | Counted with the phones and the network, the servers are the smallest part. Every background wake-up switches the radio on for seconds [S45, S46]; unchanged answers sent in full fill the network. Rare changes suit push, frequent ones a stream while the app is open, and what nobody needs while it is closed needs no refresh. A WebSocket kept open in the background meets every requirement and is the most expensive plan (LG 6-2: call frequency and data volume). The current app fails the requirement for "The courier is at the door". Term: Polling, Push, Streaming. |
| E8 | Right-sized AI | web shop, Germany | 2 days | per kind of question who answers it (no AI, small model, large model, large model with reasoning), batching window | The team's prototype sends every question of the shop assistant to the large model, one at a time. A status lookup answers "Where is my order?" better than any model; a small model is good enough where the answer is in the product data; the large model pays off for complaints and advice. Batching shares the GPU's work among questions but every answer waits; reasoning adds a little quality and a long wait. Rules: answer quality at least 92 %, 95 % of the answers within 3 s (G9, G1). Term: Green AI (Right-sizing Models). |
| E9 | Green pipeline | a team of 40 developers, Germany | 7 days (Monday to Sunday) | build agents (always on or on demand), remote build cache, test impact analysis, UI and load tests (every commit, nightly in the greenest hours, never), trigger (every commit, every 30 minutes, every 2 hours), test environments (day and night, weekdays 7–19, on demand) and their replicas | Most of a pipeline waits: environments at night and at the weekend, agents without a job. Agents on demand lose their local cache – without a remote cache the feedback gets too slow; batching commits saves runs but makes the team wait; without UI tests the release goes out untested. Rules: feedback within 20 minutes, UI tests in the 24 hours before Friday's release (iSAQB GREEN, LZ 9-1 to 9-4). Term: Green CI/CD. |

Backlog items (game values, to be tuned in the lesson tests): database index, removing an N+1 query, a cache
(saves work, but the cache server draws power too), compressing images, pagination instead of loading
everything, a faster algorithm or data structure, rewriting a CPU-bound service in a compiled language. For the
last one the game cites Pereira et al. [S19] and the critique of such rankings [S20] and says what the numbers do not show: they come from CPU-bound
benchmarks; for a typical web service waiting on the database the language matters much less.

**Not in the game:** writing or reading real code and measuring it. That belongs in exercises of the lecture
(for example measuring Java code with RAPL-based tools); measured numbers from such exercises can become the values
of backlog items.

### 9.3 Portfolio

The third track looks at a company's application portfolio instead of one system (D26). There is no clock: the
player decides for a year at a time, and the result is counted for the year, location-based. Each application has
a hosting path, a yearly cost, a short description and one measure with a one-off cost and an effect in percent of
its footprint. The player does not see the real footprint – only what the sources of data show, and how good they
are. Better data cost money from the same budget as the measures.

| # | Title | Setting | Length | New controls | Idea |
|---|---|---|---|---|---|
| P1 | Inventory | 18 applications: 6 in the own data center (Stuttgart, 1,000 MWh a year, 400 g/kWh), 6 with a cloud provider (Frankfurt), 6 as SaaS; budget 150,000 € | a year | how the own data center's metered energy is split (by servers in the CMDB, free; by CPU time from the monitoring, 5,000 €; measured power per server, 20,000 €), cloud recalculated from the usage data location-based (8,000 €) instead of the provider's market-based tool, data asked from each SaaS supplier (2,000 € each, one supplier sends none), one measure per application | Each hosting path has its own source of data, and each counts in its own way. The provider's tool counts its certificates: the cloud looks almost clean, and the largest single emitter (an analytics cluster) looks like nothing. Servers as the key make many small servers look big; CPU time hides idle servers that are on around the clock (test environments). Spend-based numbers make the expensive CRM look large and the cheap video conferencing and file sharing small. The plan follows the data; the stars count what really changes. Term: Carbon Accounting. |
| P2 | Where to start | the same company, 955 t a year with the employees' PCs; budget 200,000 € | three years in quarters | sixteen measures from a consultant's list, drawn as a marginal abatement cost curve (€ per tonne over three years from each measure's gross saving a year); a click on a bar or a card puts a measure into the plan | The curve ranks measures one at a time. What it leaves out decides the result: certificates come first on the curve (1,200 t for 40,000 €) and change nothing location-based; two measures on the same servers (test environments off at night and in the cloud on demand; the analytics platform moved and rightsized) do not add up; a project of four quarters saves only after it; 600 laptops instead of desktop PCs emit more in their making than they save in three years. Term: Marginal Abatement Cost Curve (MACC) [S41]. |
| P3 | Tidy up with TIME | thirteen applications by business value and technical condition | three years in quarters | per application tolerate, invest, migrate (optionally switching off the old system, one quarter more) or eliminate, and a start quarter; three projects at a time | Gartner's TIME model gives each quadrant a decision; over three years the timing decides as much. A migration runs old and new side by side until the old one is switched off – left running, it saves nothing. The new ERP (six quarters plus one to switch off) costs more in its parallel run than it saves within three years; tuning the old one saves more. Quick wins (eliminating unused systems) first. The mail archive is of little value but must be kept by law. Term: Application Rationalization (TIME). |
| P4 | Leave the cloud? | six applications with a cloud provider in Frankfurt; the CFO wants the costs at least 20 % lower; a colocation in Stuttgart and the own server room with six free servers | three years | per application a place – cloud Frankfurt or Stockholm, colocation, own server room – and, for the nightly runs and the test environments, off outside the hours of use | In the market-based report the cloud stands at zero, so every move out of it looks worse. Location-based it depends on the grid, how busy the servers are, the PUE and the making of new servers. Steady load (ERP database, archive) can move for the costs at about the same CO₂; the web shop, sized for its campaigns on own hardware, would idle most of the year. The largest saving is another cloud region – but the plant's ERP sends 200 queries one after the other per booking, too far for Stockholm, and the customer data must stay in Germany. Term: Cloud Repatriation [S42, S43]. |

**As built (P1).** The first star needs the inventory within ±15 % of the real footprint (so the cloud must be
recalculated) and the budget kept; ★★ −20 %, ★★★ −25 % next year. With the free data a careful plan (the
measures with the most expected saving per euro) reaches −12.5 % and no star; with the cloud recalculated −24.6 %
(★★); CPU time instead of servers −22.7 % (★★); measured power and the cloud recalculated −25.6 % to −26 % (★★★);
the best plan with perfect knowledge and no data bought −30 %. Buying every source of data is not needed – the
suppliers' data change one or two decisions. The spend-based factor (120 g CO₂e per €) and the provider's
remaining share (3 %) are game values; spend-based factors per industry come from environmentally extended
input-output tables [S39], the dual reporting of Scope 2 from the GHG Protocol [S40].

**As built (P2).** The saving is counted per quarter and emission source: what is left after all measures that work by
then, minus the embodied CO₂ of new hardware, plus energy made on site. ★ within the budget and less CO₂; ★★ −24 %,
★★★ −26 % over three years. Along the consultant's curve −22.8 % (★), with or without the certificates; the best plan
−26.8 % (★★★) – it keeps both measures for the test environments, since the second still adds a little.

**As built (P3).** Footprint per application and quarter: unchanged while its project runs; an investment cuts it
afterwards; a migration adds the new system from the start of the project and removes the old one a quarter after
its end – only if switching off is chosen; an elimination removes it. ★ needs at most three projects in every quarter
and nothing eliminated that must stay (high business value, or the law); ★★ −25 %, ★★★ −30 % over three years. Each
quadrant's decision as early as possible: −18.8 % with the old systems switched off, −2.1 % without (★); quick wins
first and the ERP tuned: −32 % (★★★); the same with the ERP migrated: −27.4 % (★★); without switching off: −25.9 %
(★★).

**As built (P4).** Per application and place over three years: servers × hours × (120 W + 240 W × utilization) × PUE
× grid, plus the making of the servers (1.5 t each, three fifths of a five-year life; none for the server room's
existing servers). In the cloud an application is counted for the server equivalents it is given today; on own
hardware for the servers its peak needs. The report counts the cloud at zero (market-based with the provider's
certificates), own sites with the grid. Costs: 11,000 € a server equivalent a year in the cloud; in the colocation
8,000 € a server plus 2,600 € a year and the electricity; in the server room 1,200 € a year and the electricity; a
team for own hardware 55,000 € a year as soon as anything runs there. Rules for ★: costs at least 20 % lower, the
customer data in Germany, a booking in the plant under one second; ★★ −10 %, ★★★ −20 % CO₂. Everything to the
colocation: +38.5 % CO₂ and only −15 % costs (no star); the steady ones to the colocation: +8 %, −21 % (★); steady
load to the colocation, the rest to Stockholm, tests off at night: −25 %, −23 % (★★★); the ERP in Stockholm: a
booking takes 2.8 s (no star). The end card splits the change into grid, servers and load, cooling (PUE) and making
of servers (D29).

**Further portfolio scenarios (planned).** P5
*Cost as a proxy?* – steering by cost alone, and where it leads astray. P6 *Rebound* – savings make an
application cheaper, its use grows.

### 9.4 Scenarios and the iSAQB module GREEN

For courses along the iSAQB Advanced Level module GREEN [S35] (learning goals LG, blocks 1–9). Labels as shown in
the app (O = operations, B in German).

| Scenario | Learning goals |
|---|---|
| O1 Night shift | LG 1-4 energy efficiency, LG 7-1 power-load relation, LG 7-4 overprovisioning |
| O2 Wait for the sun | LG 1-4 CO₂ emission efficiency, LG 7-5 time shifting |
| O3 When the wind drops | LG 7-5 time shifting, LG 3-2 deadlines against green hours |
| O4 Headroom | LG 7-4 overprovisioning, LG 7-5 autoscaling, LG 3-2 availability against efficiency |
| O5 Consolidation | LG 1-4 hardware efficiency, LG 7-1, LG 7-2 virtual machines |
| O6 Heat wave | LG 7-1 PUE |
| O7 Old iron | LG 1-3, LG 1-4 hardware efficiency, LG 7-1 |
| O8 Training run | LG 8-1 AI's footprint, LG 7-5 time shifting |
| O9 Eco mode | LG 6-4 demand shaping, LG 3-2 quality against efficiency, LG 1-3 data traffic |
| O10 Two sites | LG 7-5 location shifting, LG 7-4 geographical distribution |
| O11 Green on paper | LG 2-2 GHG Protocol Scope 2, LG 7-1 REF, LG 7-3 greenwashing risks |
| O12 Dark doldrums | LG 1-4 all three fields of action, LG 7-5, LG 8-1 |
| E1 How many nines? | LG 3-1 quality scenarios, LG 3-2 resilience against energy efficiency, LG 6-1 |
| E2 Measure first | LG 4-2 narrowing down hotspots, LG 4-3, LG 5-1 sensible starting points |
| E3 Growing data | LG 5-2 complexity, LG 5-3 data handling |
| E4 Hit or miss | LG 5-2 caches, LG 3-2 freshness against efficiency |
| E5 Data diet | LG 6-4 Web Sustainability Guidelines, LG 1-3 data traffic, LG 3-2 |
| E6 Chatty services | LG 6-1 architecture styles, LG 6-2 communication |
| E7 Constant asking | LG 6-2 communication (call frequency, data volume), LG 1-3 drivers of IT energy (devices and network) |
| E8 Right-sized AI | LG 8-2, LG 8-3, LG 8-4 |
| E9 Green pipeline | LG 9-1 to LG 9-4 |
| P1 Inventory | LG 2-2 Scope 2, LG 4-3 providers' carbon tools, LG 4-4 measuring in enterprise environments, LG 7-3 |
| P2 Where to start | LG 2-3 reduction targets and measures in the company, LG 1-4 hardware efficiency, LG 2-2 market-based against location-based |
| P3 Tidy up with TIME | LG 2-3 energy efficiency as a field of action in the company, LG 7-2 deployment models, LG 7-4 operational antipatterns (systems left running) |
| P4 Leave the cloud? | LG 7-2 deployment models, LG 1-4 hardware efficiency, LG 4-3 providers' carbon tools, LG 2-2 market-based against location-based |

Each scenario has:

- a **briefing**: situation, the one idea, its **technical term** (D16), a small preview of the play screen with
  numbered areas and what they show, facts and goals,
- the play,
- a **debrief** (section 8.2) with a short explanation of the idea and one source.

**Free play** (after scenario 6): choose region, season, length, workload mix and fleet; all unlocked controls.

---

## 10 Screen

### 10.1 Layout (large screens, landscape)

```
┌──────────────────────────────────────────────────────────────────────┐
│ Day 2  14:15   ⏸ 1× 4× 16× ⏭      CO₂ 182 kg   SLA 99.4 %   € 1,240  │
├───────────────────────────────────────────────┬──────────────────────┤
│                                               │ Policies (tabs)      │
│        Data center hall (isometric)           │  Capacity            │
│        racks with servers: off / idle /       │  Batch               │
│        busy / hot; queue bars next to it      │  Cooling             │
│                                               │  …only unlocked ones │
├───────────────────────────────────────────────┴──────────────────────┤
│ Timeline: grid intensity (past + forecast band), power draw, load,    │
│ batch/training runs as bars; drag to look back                        │
└──────────────────────────────────────────────────────────────────────┘
```

- **Hall:** the isometric style of the prototype, but racks instead of single machines: each server is a slot
  with a clear state color. Tapping a rack shows its servers, power and utilization. No placement on cells – racks
  fill in order (D2).
- **Timeline:** the most important view for the lessons: it shows at a glance when the grid is clean and what the
  data center does at that time.
- **Controls:** the right column, one section per unlocked topic. Switching by hand takes effect at once; rules
  show which one is active and take effect with "Apply". A box "Right now" shows the immediate situation.
- **iPhone** (from step 9, D1): the hall becomes a compact status row; timeline and policies as tabs.

### 10.2 Look

- Isometric drawing style ("architecture diagram") from the prototype, light background, no neon.
- Workload colors and shapes as in section 5; server shapes as in section 6. A distinguishability check at the
  smallest size (iPhone SE) is a done-criterion of the UI step.
- Lexicon with every term (SCI, PUE, embodied emissions, carbon intensity, SLA …), reachable from every number.

---

## 11 Architecture and tests

- **Technology (D14):** Kotlin Multiplatform with Compose Multiplatform, so the same code runs in the browser
  (Wasm), on Windows, macOS and Linux (desktop, JVM) and later on Android and iOS. The browser matters most for
  teaching: a link or QR code in the lecture, no installation, the scenario code in the address.
- **`:core`** (Kotlin, common code, no UI): deterministic simulation with integer arithmetic (Long for energy and
  emissions), seeded random numbers, `Scenario`, `Policy`, `Simulation.step()`. The same results on every
  platform; the tests run on the JVM and in Wasm. Runs headless in tests and in the background for the reference
  operator and "what if".
- **`:composeApp`** (Compose Multiplatform, foundation only, font Inter): screens on the simulation state;
  German texts keyed by the English text (`Strings.kt`).
- The first version was written in Swift/SwiftUI (iPad and Mac, versions up to 0.3.0, tag `v0.3.0`); its core was
  ported 1:1 – the lesson report of the Kotlin core reproduces its numbers exactly. The Swift code was removed
  afterwards.
- **Lesson tests:** for every scenario the tests run the reference operator and a scripted "good operator" and
  check that (1) the reference meets the SLA (the scenario is fair), (2) the good operator is at least X % better in
  CO₂ (the lesson works), (3) a deliberately bad policy shows the expected failure (e.g. a fixed batch window misses a
  deadline in scenario 3). A test report (`build/lesson-report.txt`) lists the numbers for balancing.
- **Save games:** the scenario code plus the list of policy changes with their tick (the simulation is
  deterministic, so this replays the game exactly).

---

## 12 Implementation steps

After each step the game is played by a person before the next step begins (**play-test gate**).

| Step | Content | Done when |
|---|---|---|
| 0 | Repo, build script with watcher, Xcode project, empty core package, README, license | builds and tests run on the Mac – **done** (version 0.0) |
| 1 | Core: clock, grid curves and forecast, interactive load, servers with power, autoscaling, SLA, emissions | headless run of scenario 1 prints plausible numbers; tests green – **done** (version 0.1) |
| 2 | Minimal UI: top bar with speed control, timeline, capacity tab, simple hall | scenario 1 playable; **play test** – done; revised after the play test (version 0.2.1): start at 00:00, switching by hand, goals and stars, "what if" |
| 3 | Batch with policies and deadline safety; reference operator; debrief | scenarios 2 and 3 playable; **play test** – scenario 2 built (version 0.3), scenario 3 built (version 0.3.6) |
| 4 | Money, events, automatic pause | scenario 4; **play test** |
| 5 | Cooling and PUE, outside temperature | scenario 5; **play test** |
| 6 | Embodied emissions, buying/retiring, monthly time scale, SCI | scenario 6; **play test** |
| 7 | AI training with power cap; streaming eco mode | scenarios 7 and 8; **play test** |
| 8 | Two sites | scenarios 9 and 10; **play test** |
| 9 | Free play, scenario codes, result cards, lexicon | **play test in a lecture** – glossary built (D28); result codes built and removed again (not needed); free play open |
| 10 | Look and sound, App Store texts and screenshots | release |

**As built in step 9, part 1** (D28): a **glossary** reached from the start page – every scenario's technical term
with its explanation and the scenarios that teach it (open ones start from there), plus six general terms (carbon
intensity, CO₂e, idle power, location-based and market-based, Scope 1–3, SCI). Result codes bound to the player's
name were built and taken out again: the courses do not need them.

**As built in step 1** (simulation core, then in Swift; ported to Kotlin in `:core`, D14):

- Units: power in W, energy in mWh (1 W for one tick = 250 mWh), carbon intensity in tenths of g/kWh, emissions
  in µg CO₂e, shares in per mille; integers only.
- Grid, weather and forecast as in section 4, on 15-minute ticks (`Grid.kt`, curves from
  `tools/grid/curves.py`).
- Interactive load: one work unit = 1,000 requests. Daily curve in per mille of the evening peak (hours 0–23):
  380, 300, 240, 210, 200, 220, 300, 420, 540, 620, 660, 690, 720, 700, 670, 660, 680, 730, 800, 900, 1,000,
  960, 780, 560; noise ±5 % per tick. A current CPU server handles 20 units per tick at full load (about 22
  requests per second, game value).
- SLA: of the work served, the part above 85 % of the capacity counts half (slow).
- Capacity policies: all on (reference), manual (a number of servers), autoscale (target utilization 40–90 %).
  The autoscaler plans for the highest expected demand within the boot time plus one hour, uses the most
  efficient servers first and keeps a surplus server running until its replacement has booted.
- Facility fixed to room air conditioning (PUE 1.52) until step 5.
- Embodied emissions are booked for every server owned, on or off: switching off saves energy, not hardware.
- Scenario 1 (Germany, autumn, 20 current CPU servers, peak 240 units, seed 1 – a day with a high carbon
  intensity of 480 g/kWh on average), `build/lesson-report.txt`:

| Policy | CO₂ kg | of it embodied | kWh | SLA | Servers on (mean) | vs. reference |
|---|---:|---:|---:|---:|---:|---:|
| all on (reference) | 113.6 | 5.0 | 226 | 100 % | 20.0 | – |
| autoscale 70 % | 91.8 | 5.0 | 177 | 100 % | 11.4 | −19 % |
| autoscale 90 % | 85.0 | 5.0 | 163 | 99.3 % | 8.9 | −25 % |
| manual, 10 servers | 85.6 | 5.0 | 167 | 95.5 % | 10.0 | −25 %, misses the SLA at the evening peak |

**As built in step 2** (app, revised after the first play test):

- Briefing before the scenario (situation, idea, goals with stars, what is on the screen, best result so far),
  then the play screen: top bar with day, clock, speed (pause, 1×, 4×, 16×) and the figures grid now, CO₂ so far,
  SLA, facility power; below it a goal bar (SLA target, CO₂ compared with the reference so far, what the second
  and third star need); isometric hall with racks of ten servers (off grey, booting amber, on dark with a green
  load bar); timeline with the carbon intensity so far (colored area), the forecast with its band, load and
  capacity, expected load, red marks for slow requests.
- Scenario 1: plus and minus for the servers, effective at once, and a box "Right now" (servers working, load /
  capacity, utilization, expected load in an hour, IT power). The capacity rules (all on, fixed, autoscaling) with
  "Active" and "Apply" existed in the Swift version and are built again with scenario 4. `Simulation.projected()`
  stays in the core for later.
- End card: stars and the three goals, the result next to the reference operator's (CO₂, of it embodied, energy,
  SLA, mean servers on, mean utilization), and "what if": the same day replayed with all servers on, half and 70 %
  of the fleet all day, and autoscaling at 70 %. Best stars are kept on the device; one star opens the next
  scenario.
- Scenario 1 numbers with the start at 00:00 (`build/lesson-report.txt`):

| Operator | CO₂ kg | vs. reference | SLA | Mean utilization | Stars |
|---|---:|---:|---:|---:|---:|
| all on (reference) | 115.3 | – | 100 % | 35 % | – |
| by hand, careful (every 15 min for the next 90 min at 80 %) | 88.6 | −23 % | 100 % | 72 % | ★★★ |
| by hand, twice: 8 servers at night, 16 from 06:00 | 99.4 | −14 % | 100 % | 50 % | ★ |
| fixed 10 servers | 86.7 | −25 % | 95.5 % | 68 % | – |
| autoscaling 70 % (scenario 4) | 92.7 | −20 % | 100 % | 63 % | ★★★ |

- Second play test: the capacity of a server is shown in the panel ("Capacity: 400 load units", one server = 20
  load units) and in the briefing (fleet, evening peak); the load lane of the timeline has dotted lines with
  "5 servers", "10 servers" … on the right. The briefing uses two columns on wide screens and larger type; its goal
  lines have no check circles. "What if" says what is the same: the load and the grid mix.
- Launch arguments for screenshots: `-play`, `-ticks N`, `-fixed N`, `-careful 1`, `-autoscale P`.

**As built in step 3, part 1** (scenario 2):

- Core: `BatchJobSpec` (servers, hours, arrival, deadline), `BatchJob` with the states waiting, planned, started
  (servers booting), running, done. A job switches its servers on when it starts, works after the boot time and
  releases them when done; a planned job waits if not enough servers are free. Deadlines missed are counted
  once. Batch modes: by hand, as soon as possible (reference), at a fixed hour, greenest (`BatchPlanner`: the
  start with the lowest mean forecast intensity before the latest start, capacity-aware, tightest deadline first;
  planned again when a job arrives). Each scenario has its own reference policy; in scenario 2 servers without a
  job are off for everybody, so the comparison shows the effect of the timing alone.
- First star now also needs every deadline met; special goal "batch work at most X g/kWh on average".
- Scenario 2: Spain (annual profile, 94 g/kWh at 14:00, 161 at 21:00), 2 days, 10 CPU servers, 7 jobs (daily
  report ×2, backup ×2, data import, image conversion, search index). Goals: ★★ −10 %, ★★★ batch work ≤ 83 g/kWh.

| Operator | CO₂ kg | vs. reference | Batch work g/kWh | Deadlines missed | Stars |
|---|---:|---:|---:|---:|---:|
| as soon as possible (reference) | 14.1 | – | 104 | 0 | – |
| greenest by forecast | 11.9 | −16 % | 79 | 0 | ★★★ |
| every job at 11:00 | 12.4 | −12 % | 85 | 0 | ★★ |
| every job at 01:00 (nightly batch) | 14.9 | +6 % | 114 | 0 | – |
| nothing planned | – | – | – | 7 | – |

  The embodied emissions of the ten servers (5 kg in two days) are a large share in a clean grid; they are the
  same whenever the jobs run.
- App: scenario list (cards with stars, locked until the scenario before has a star); briefing per scenario
  (`ScenarioTexts`); batch panel with one row per job (size, deadline, status, start time with plus and minus,
  "Start now", forecast mean while it would run, latest start, warning when planned too late); the game pauses
  once per job when an unplanned job must start within the hour; the timeline's lower lane shows one row per job
  (window from arrival to deadline, planned outline, booting and running bars, deadline mark); hall bars blue
  for web requests, amber for batch jobs, light blue for booting; goal bar for batch: deadlines and the mean
  intensity of the batch work compared with the reference (CO₂ so far would favour waiting); end card with jobs
  on time, mean g/kWh and "what if" (as soon as possible, at 01:00, at 11:00, by the forecast).
- The dotted server lines in the load lane are lighter after the second play test.

**As built in step 3, part 2** (scenario 3 and play-test changes):

- Core: batch rule `Threshold(g/kWh)` (starts when the intensity now is at or below it) and `Policy.deadlineSafety`
  (fixed hour and threshold start a job at its latest start at the latest; switchable). A changed rule plans the
  jobs not started yet anew. `WeatherMode.Days`: one weather factor per day. A changed capacity policy applies at
  once, also while paused, with the same result as at the next tick.
- Scenario 3: Great Britain, winter, 3 days, weather 1.0 / 0.55 (storm) / 1.7 (windless), 12 CPU servers, 9 jobs.
  Goals: ★★ −10 %, ★★★ batch work ≤ 145 g/kWh. After the first play test (too easy: greenest hours plus safety
  gave three stars at once) the forecast gets the timing wrong until day 2, 12:00: it expects the wind to drop 12
  hours later than it does (`ForecastMiss`). Then the game pauses with the news. The greenest-hours rule plans once,
  when it is applied; "Plan again with the latest forecast" plans the open jobs anew. Only a player who plans
  again after the update reaches the third star (rolling planning).

| Rule | CO₂ vs. reference | Batch work g/kWh | Deadlines missed | Stars |
|---|---:|---:|---:|---:|
| as soon as possible (reference) | – | 169 | 0 | – |
| greenest hours, planned once | −8 % | 151 | 0 | ★ |
| greenest hours, planned again at the forecast update | −11 % | 142 | 0 | ★★★ |
| at 03:00, with safety | −4 % | 160 | 0 | ★ |
| at 03:00, without safety | −16 % | 144 | 1 | – |
| below 140 g/kWh, with safety | −1 % | 166 | 0 | ★ |
| below 140 g/kWh, without safety | −32 % | 114 | 3 | – |

- App: rules panel with a draft (four rules, hour and threshold with plus and minus, deadline safety) and
  "Apply"; the job list shows what the rule has planned. "What if" for scenario 3: as soon as possible, 03:00,
  below 150 with and without safety, greenest hours.
- Briefing: the technical term of the scenario in a box (Rightsizing, Time Shifting, Carbon-Aware Scheduling), also
  on the scenario card and in the debrief; a live preview of the play screen in its start state with markers ①–④.
  End card: "To scenario N" once it is open.

**As built in step 4, part 1** (scenario 4, without money – D4 revised):

- Core: `Campaign` in the interactive load (not in the expected demand); the autoscaler also reacts to the demand
  of the last tick; `ScheduledMinimum` (at least N servers every day in a time window); `Simulation.replay` with
  policy changes at given ticks.
- Scenario 4: Germany, winter, 2 days, 30 CPU servers, peak 240 units, newsletter on day 2 from 19:00 to 22:00
  (twice the load). Start: autoscaling at 80 %. Goals: ★★ −30 %, ★★★ mean utilization ≥ 70 %.

| Operator | CO₂ vs. reference | SLA | Mean utilization | Stars |
|---|---:|---:|---:|---:|
| all on (reference) | – | 100 % | 25 % | – |
| autoscaling 80 % (as found) | −34 % | 98.3 % | 72 % | – |
| autoscaling 60 % (headroom all the time) | −28 % | 99.3 % | 57 % | ★ |
| 80 % + minimum 26 servers 18–22 every day | −31 % | 99.9 % | 66 % | ★★ |
| 80 % + minimum 26 servers 18–22, set on day 2 | −33 % | 99.9 % | 71 % | ★★★ |
| 80 %, all on from day 2 18:30 | −31 % | 100 % | 66 % | ★★ |

- App: capacity rules panel (all on, fixed number, autoscaling with target and minimum window; draft and
  "Apply"), the campaign marked in the load lane, "what if" with the alternatives above. Slogan "Green IT – from
  code to data center".

**As built in step 4, part 2** (scenario 5, timeline tooltips):

- Core: `VmKind` (RAM, CPU peak, daily profile: shop front end evening, batch worker night, reporting morning,
  database replica, cache), `CapacityMode.Placed` (host per VM; hosts without a VM are off), per-host CPU with
  slow requests above 85 %, `Packing` (spread, First Fit Decreasing with three checks: RAM only, sum of the peaks,
  daily profiles), special goal "at most N servers on".
- Scenario 5: Germany, autumn, 1 day, 10 hosts (256 GB, 20 load units), 24 VMs (1,248 GB: by RAM five hosts
  would do, by CPU they do not). As found: spread over all ten hosts. Goals: ★★ −10 %, ★★★ at most 6 hosts.
  Retuned after the play test so that five hosts fit the RAM but miss the SLA, and six fit with a little air.

| Placement | Hosts | CO₂ vs. reference | SLA | Stars |
|---|---:|---:|---:|---:|
| spread (reference) | 10 | – | 100 % | – |
| FFD, RAM only | 5 | −37 % | 69 % | – |
| FFD, daily profiles up to 85 % | 9 | −4 % | 100 % | ★ |
| FFD, daily profiles up to 90 % | 8 | −9 % | 99.2 % | ★ |
| by hand: one batch worker per host, peaks spread | 6 | −18 % | 99.5 % | ★★★ |
| by hand, 5 hosts (RAM fits) | 5 | −24 % | 92.9 % | – |

- App: the hall is replaced by a board of hosts as bins (RAM as height, VMs stacked, CPU chart per host with the
  slow line); drag a VM onto a host, or click it and then the host; fixed once the day runs; warnings for hosts whose expected peak is above
  85 %. The placement is kept for the next run. Timeline tooltips with the values at the time under the mouse.

**As built in step 4, part 3** (E2, scenario list):

- Core: `Policy.workPermille` (CPU work per request; demand and autoscaler scale with it), `BacklogItem`,
  `HotSpot`, `Engineering` (work after the items done, a greedy team by estimates or by measurements), SCI per
  1,000 requests counts the requests (not the smaller work), `Scenario.track`, `label`, `unlockAfter`.
- E2 "Measure first": the web shop with autoscaling at 70 %, 4 days, each day the week after a sprint; 8 story
  points per sprint; backlog: profiling (3), cache resized images (3, team −30 %, real −7 %), rewrite the checkout
  (8, −40 % / −4 %), framework upgrade (5, −10 % / −3 %), cache catalog responses (8, −10 % / −18 %), fix N+1
  queries (5, −5 % / −24 %), log level INFO (1, −1 % / −10 %). Goals: ★★ −30 %, ★★★ −40 %.

| Team | CO₂ vs. reference | Stars |
|---|---:|---:|
| changes nothing (reference) | – | – |
| by its estimates | −27 % | ★ |
| profiling first, then by the measurements | −41 % | ★★★ |
| lucky guess without profiling | −49 % | ★★★ |

- App: sprint panel (CPU profile once measured, backlog with estimates and measurements, story points, "Start
  sprint"), the game pauses at every sprint; the last run's plans are offered again. Scenario list in two sections
  (Operations, Engineering) with labels; "Open all scenarios" switch (D19; since D32 a launch option).

**As built in step 4, part 4** (play test of E2, E3):

- Sprints: items that do not fit into a sprint carry over; started items are continued first (`Team`). "Start
  sprint" also starts the clock at 16× – a sprint shows its effect over a whole day, 1× would show nothing.
- Core: `Policy.done` (items finished) instead of a work factor; `GrowthModel` with parts that grow by their
  complexity (`Complexity`: O(1), O(log n), O(n), O(n log n), O(n²)) and nightly jobs.
- E3 "Growing data": data +15 % a month, 12 game days = 12 months, 2 story points per sprint for performance, 80
  servers with autoscaling. Growing parts: product lookup in a list O(n) → hash map O(1); order history without
  an index O(n) → O(log n); search index rebuilt every night O(n²) → incremental O(n). Constant cuts: string
  handling, JSON library, image service rewrite (8 SP). Goals: ★★ −15 %, ★★★ −24 %.

| Team | CO₂ vs. reference | Stars |
|---|---:|---:|
| changes nothing (reference) | – | – |
| biggest saving today first (the rewrite blocks the team) | −8 % | ★ |
| by today's saving per story point | −22 % | ★★ |
| the growing parts first, the quadratic one before all | −25 % | ★★★ |

- Timeline: the scale follows the data, server lines every 5, 10 or 20 servers, one label per day from six days on.

**As built in step 4, part 5** (E4, ideas from the Green IT lecture; reworked after the first play test):

- Core: `CacheModel` with kinds of data (share of the requests, cost per request, number of items, item size,
  allowed age, share asked only once) and work no cache can save (cart, checkout, login). Requests follow a Zipf
  distribution over the items [S21]: with the request rate R, the time T an entry stays and n items, item i is in the
  cache with probability a/(a + i), a = R·T/H(n). The cache then holds a·(H(n+a) − H(a)) items and hits
  (H(n) − H(n+a) + H(a)) / H(n), `H(x) ≈ ln x + γ + 1/(2x)`. Requests asked only once never hit but are stored too.
- All cached kinds share the memory of the cache servers (64 GB each, 180 W all day, embodied emissions like any
  server). If the TTLs need more memory than there is, the least recently used entries go first: all entries stay
  the same time t, or their TTL if shorter, with t such that the memory is just full (LRU, Che's approximation
  [S23]). A hit still costs 2 % of the work, a miss 8 % more (look up, serialize, store). A TTL longer than the
  business allows makes that share of the hits stale.
- `Policy.cache` (`CacheConfig`: cache servers, TTL per kind, HyperLogLog, materialized view); `Scenario.caching`,
  `Scenario.staleLimit` (‰ of the requests); goal and end-card row "Stale answers". The panel shows the memory used
  right now and per kind its hit rate and memory; the hall shows the cache servers in their own rack (violet).
- HyperLogLog [S22]: the exact count of unique visitors keeps every ID and costs work per request; the estimate
  with about 2 % error costs a fraction. Materialized view: the sales dashboard reads a stored result instead of
  aggregating every 15 minutes.
- E4 "Hit or miss": 2 days, 30 web servers with autoscaling at 70 %, four cache servers ordered, nothing cached;
  goals ★★ −32 %, ★★★ −40 %, at most 1 ‰ stale.

| Cache | CO₂ vs. reference | Stars |
|---|---:|---:|
| none (reference) | – | – |
| everything for a day, or the balance cached | – | 0 (stale) |
| everything allowed, search too, 4 servers (the team's plan) | −24 % | ★ |
| as allowed without search, 2 servers | −30 % | ★ |
| as allowed, 1 server (with or without search) | −33 % | ★★ |
| as allowed, 4 servers, HyperLogLog and view | −31 % | ★ |
| as allowed, 1 server, HyperLogLog and view | −41 % | ★★★ |

- E2: twelve backlog items, 50 story points for 32 in four sprints – some items are left at the end (lazy loading,
  session cache, TLS session reuse, JVM update, precomputed shipping costs). By the estimates −22 % ★, profiling
  first −46 % ★★★.
- E2 and E3: the last run's sprint plans are not offered again – the plan is the player's.
- E4: the upper lane of the timeline shows the cache memory (memory of the running cache servers, used, expected)
  instead of the grid's carbon intensity, which the player cannot act on there.

**As built in step 4, part 6** (E4 after the second play test; start page):

- E4: on day 2 at 18:00 the prices change in the catalog. Catalog pages cached before are wrong until they expire:
  τ minutes after the change a share 1 − τ/T of the hits is wrong (T the time an entry stays). "Invalidate the
  catalog pages" removes them at once; the hit rate then climbs back within T. Stale answers are limited to 0.5 ‰:
  without invalidation 0.9 ‰ – no star; catalog pages for 1 min instead: correct, but −33 % (★★).
- E4: a measure that should not be ticked: "warm the cache every night" (03:00–07:00, all catalog page variants
  in advance) – the pages expire after 15 minutes, long before the first customer: −37 % instead of −40 % (★★).
  ★★★ now from −38 %.
- E4: red marks under the cache lane where answers were stale; the end card says "Not made" without the first star;
  a draft that is not applied says so in amber.
- Start page: operations scenarios are labelled B1… in German and O1… in English (E2… in both); a language switch
  (Deutsch | English), kept on the device; "Open all scenarios" moved into the About dialog ("For teachers"; since D32 a launch option).

**As built in step 5, part 1** (scenario 6, "Heat wave"):

- Core: `CoolingModel` – outside temperature per day (lowest at 05:00, highest at 15:00, cosine in between),
  free cooling while the outside air is at least 6 K below the setpoint, the chiller fully above setpoint − 1 K,
  linear in between. Chiller power per kW IT: 0.38 at 30 °C outside and 20 °C setpoint, +3 % per K outside, −5 % per
  K setpoint (at least 0.12); free cooling 0.06; distribution 6 %; a fixed 1.5 kW for lighting, UPS and pumps. Server
  fans: +3 % IT power per K above 25 °C. Game values in the range of [S14] and [S17].
- `Policy.setpoint`, `Scenario.cooling`, `Control.COOLING`; the record of a tick keeps the setpoint.
- App: cooling panel (setpoint, outside temperature, chiller share, PUE now) above the capacity rule; "PUE now" in
  the top bar; the upper lane of the timeline shows the outside temperature, the setpoint and the hours the outside
  air cools; PUE in the end card and in "what if".
- Heat wave: 28, 35, 37 °C; 40 servers, peak 300 units; reference autoscaling at 50 %, 20 °C. Goals ★★ −13 %,
  ★★★ −16.5 %.

| Rules | CO₂ vs. reference | PUE | Stars |
|---|---:|---:|---:|
| 50 %, 20 °C (reference) | – | 1.64 | – |
| 50 %, 25 °C | −5.5 % | 1.53 | ★ |
| 50 %, 27 °C | −3.7 % | 1.48 | ★ |
| 80 %, 20 °C | −12.4 % | 1.68 | ★ |
| 80 %, 27 °C | −15.6 % | 1.52 | ★★ |
| 80 %, 25 °C | −17.1 % | 1.58 | ★★★ |
| all on, 27 °C | +25.9 % | 1.40 | – |

- Scenario 4: the debrief adds the start time of the software (JVM; GraalVM Native Image, CRaC) – the faster a
  server is ready, the less headroom the autoscaler needs.
- End card: "To the overview" next to "To the briefing".

**As built in step 5, part 2** (scenario 7, "Old iron"; reworked after the first play test):

- Core: `RenewalModel` (sites, a game day stands for 91 real days, four game days a year, a budget of four new
  servers a year for both sites – what is not spent is gone –, each year's generation 20 % more work per kWh, old
  servers past their six-year life fail with 8 % a quarter, the same ones in every run). `InteractiveLoad` grows by
  15 % a year. `Scenario.forSite` gives each site its grid; the grid changes its season every quarter. Embodied
  emissions are booked only during a server's life (scaled by 91 per game day, like the energy). `Policy.renewed`:
  new servers bought for a site; `Policy.buyWhenShort`: the reference operator buys at the start of a quarter
  whenever the quarter's peak would exceed 75 % of the capacity. The SLA counts per site (`Totals.worstSiteSla`).
- App: one simulation per site, stepping together; both halls side by side; pause and run (a quarter in two
  seconds), pausing at the start of every year; servers bought while paused arrive at the start of the next
  quarter. The chart shows CO₂ per quarter and site (electricity and hardware, the reference as a line) and the
  capacity of both sites against the growing peak load.
- Goals: SLA ≥ 99 % at each site, ★★ −20 %, ★★★ −28 %.

| New servers | CO₂ vs. reference | Stars |
|---|---:|---:|
| none | +8.9 % (SLA missed) | 0 |
| all twelve in Warsaw | −31.3 % (SLA in Paris missed) | 0 |
| Paris first, Warsaw in year 3 | 0.0 % | 0 |
| two and two every year | −14.0 % | ★ |
| Warsaw first, Paris in year 3 | −26.1 % | ★★ |
| Warsaw first, Paris only two, in the last quarters | −29.4 % | ★★★ |
- Debriefs: E2 names Amdahl's law (the checkout), E3 the rebound effect (freed capacity fills up with features).
- Planned from the same lecture: scenario 6 "Heat wave" with the PUE trap (switching servers off raises the PUE,
  but CO₂ falls – the score counts CO₂, not PUE); a note on JVM start time (Native Image, CRaC) in scenario 4,
  where slow starts force a minimum of servers.

**Taken over from the Carbon Rush prototype** (copied, checked for every reference to its origin):
grid curves and their generator, the forecast model, the electricity price formula, the embodied and SCI
bookkeeping, the build script with its watcher, the string catalog tools, the App Store tools, sound effects and
music. Not taken over: maps, paths, the tower-and-creep rules, opponents, network play.

**As built in step 5, part 3** (scenario 8, "Training run"):

- Core: `TrainingModel` (work 240 server-hours; GPUs draw 90 % of their dynamic range without a cap; speed at a cap
  50/60/70/80/90 % = 70/87/93/97/100 % – below 60 % the energy per work rises again, 60 % is the sweet spot; each additional server loses 4 % throughput; resuming costs one tick).
  `Policy.training` (`TrainingConfig`: servers, cap, greenest). Greenest hours: the training runs now if this tick is
  among the greenest of the ticks left that it still needs (a tenth and two ticks in reserve), by the forecast made
  now. The first star needs the training done by the deadline. Weather: calm, windy, calm.
- App: training panel (progress, servers, cap, schedule, hours left against hours to the deadline); the lower lane
  shows when the GPUs train, the progress, and ahead the plan with the rules now (`Simulation.projectedHistory`).
- During the run the goal bar shows the CO₂ per percent of training done, compared with the reference after the
  same amount of work – CO₂ so far would favour waiting for the wind (like the batch intensity in scenarios 2 and 3;
  the SCI idea on a small scale: emissions per unit of work).
- Goals ★★ −20 %, ★★★ −27 %.

| Rules | CO₂ vs. reference | Stars |
|---|---:|---:|
| 8 servers, no cap, at once (reference) | – | – |
| 8, no cap, greenest hours | −15.8 % | ★ |
| 8, cap 70 %, at once | −12.7 % | ★ |
| 8, cap 70 %, greenest hours | −23.9 % | ★★ |
| 6, cap 60 %, greenest hours | −29.5 % | ★★★ |
| 4, cap 70 %, at once | (not done in time) | 0 |

**As built in step 5, part 4** (scenario 9, "Eco mode"):

- Core: `StreamingModel` – devices with a share of the viewers and a rating of 4K/HD/SD (phones 100/100/80 %,
  laptops 100/95/60 %, TVs 100/80/40 %), work per stream 15 % fixed plus the bitrate (HD a quarter of 4K, SD a
  tenth), rating target 93 %. `Policy.streaming` (`StreamingConfig`: highest quality per device, eco mode, threshold,
  eco quality). The eco mode looks at the grid now. `Totals.meanRating`; the first star needs the rating.
- App: quality panel (chips per device with the rating, eco mode, threshold, the situation now); the carbon lane
  shows the threshold and the hours with eco mode on; rating in the top bar, goals and end card.
- Goals ★★ −45 %, ★★★ −56 %.

| Rules | CO₂ vs. reference | Rating | Stars |
|---|---:|---:|---:|
| 4K everywhere (reference) | – | 100 % | – |
| HD on phones and laptops | −39.6 % | 98.7 % | ★ |
| HD everywhere | −60.6 % | 91.7 % | 0 |
| the same plus eco HD above 650 g/kWh | −52.0 % | 95.0 % | ★★ |
| the same plus eco HD above 550 g/kWh | −57.3 % | 93.2 % | ★★★ |
| the same plus eco SD above 650 g/kWh | −59.1 % | 78.9 % | 0 |

**As built in step 5, part 5** (scenario 10, "Two sites"):

- Core: `LocationModel` – two sites (Frankfurt, Germany, 14 servers; Oslo, Norway, 8 servers) and ten
  `LocationJob`s with data size, a duration far from the database (`remoteDuration`, the price calculation 3×) and a
  reason to stay (`StayReason.RESIDENCY`, customer analytics). `Scenario.forLocation(site, moved)` builds the site's
  scenario: Frankfurt keeps the jobs that stay and books the transfers (`Transfer`, 4 TB/h, 5 kWh/TB [S24], in the
  German grid, at the tick they happen); Oslo gets the moved jobs, arriving once their data are there.
  `Totals.transferEnergy` and `Totals.rulesBroken`; the first star needs no broken rule.
- App: the two halls side by side; location panel (batch rule for both sites, which site the timeline shows, a card
  per job with servers, hours, window, data, the notes on residency and database, the site switch and the transfer
  with hours, kWh and kg CO₂); the switch works only before the start. The carbon lane draws the other site's grid as
  a thin line. Goal "customer data stays in Germany"; end card with the transfer energy.
- Goals ★★ −35 %, ★★★ −45 %.

| Jobs in Oslo, rule | CO₂ vs. reference | Missed | Transfer kWh | Stars |
|---|---:|---:|---:|---:|
| none, as soon as possible (reference) | – | 0 | 0 | – |
| none, greenest hours | −8.4 % | 0 | 0 | ★ |
| the two trainings, as soon as possible | −33.0 % | 0 | 7 | ★ |
| trainings, reports, search index, as soon as possible | −41.7 % | 0 | 25 | ★★ |
| the same, greenest hours | −48.6 % | 0 | 25 | ★★★ |
| the same plus the product videos | −0.7 % | 0 | 175 | ★ |
| the same plus a price calculation | −46.3 % | 2 | 26 | 0 |
| the same plus the customer analytics | −44.9 % | 0 | 35 | 0 (rule) |

Energy rises with every move (the transfers), CO₂ falls: the scenario shows that Location Shifting saves carbon,
not energy.

**As built in step 5, part 6** (scenario 11, "Dark doldrums", the capstone of the Operations track):

- Core: no new model – the scenario combines the shop (40 CPU servers, peak 240), six batch jobs on the same
  servers (`Scenario.batchLimit`: at most 16 for batch at a time, the rest belong to the shop) and a training on 4
  GPU servers (`TrainingModel(work = 90_000)`, due at the end of day 7). Weather `Days(1300, 1400, 1450, 1400,
  1350, 600, 450, 1300)`: dark doldrums until day 5, wind on days 6 and 7, calm again after the week. `ForecastMiss`
  with a negative shift: until day 4, 12:00 the forecast sees the weather a day early. `TickRecord.training` marks
  the ticks the GPUs train (the training lane no longer infers it from the IT power).
- App: the control panel has three tabs (shop, batch, training) over one draft; the timeline's lower lane follows
  the tab (load, jobs, training). The CO₂ goal compares the expected end with the rules now – CO₂ so far would
  flatter a training that waits. The forecast update comes without a pause or news (unlike scenario 3): noticing that the
  dashed line has moved, and planning again, is part of the task.
- Goals ★★ −40 %, ★★★ −50.5 %.

| Shop, batch, training | CO₂ vs. reference | Stars |
|---|---:|---:|
| all on, at once, 4 GPUs uncapped at once (reference) | – | – |
| autoscaling 80 %, at once, at once | −23.9 % | ★ |
| all on, at once, training 4 / 60 % / greenest | −24.3 % | ★ |
| all on, greenest (planned again), at once | −2.8 % | ★ |
| autoscaling 80 %, at once, 4 / 60 % / greenest | −48.3 % | ★★ |
| autoscaling 80 %, greenest planned once, 4 / 60 % / greenest | −50.1 % | ★★ |
| autoscaling 80 %, greenest planned again, 4 / 100 % / greenest | −50.0 % | ★★ |
| autoscaling 80 %, greenest planned again, 4 / 60 % / greenest | −52.3 % | ★★★ |
| autoscaling 70 %, the same | −51.0 % | ★★★ |
| autoscaling 60 %, the same | −49.4 % | ★★ |

**As built in step 5, part 7** (E8, "Right-sized AI"):

- Core: `InferenceModel` – five kinds of question with a share and an answer quality per answerer (`Answerer`: no
  AI 0.002 Wh / 0.3 s, small model 0.12 Wh / 0.8 s, large model 1.0 Wh / 2.0 s, large model with reasoning 2.5 Wh
  / 9 s per answer without batching – game values in the range of [S25]–[S27]); the questions follow the shop's
  daily curve, 12 a second at the peak. Batching: the batch is the questions that arrive in the window (at most 32);
  the energy per answer falls to 0.1 + 0.9 / b^0.8 of the unbatched value [S27], every answer waits the window. GPU
  servers (up to 6, 9.3 kW at full load) switch with the power; idle rest and embodied emissions count. `Policy.inference`,
  `Totals.meanAnswerQuality` and `answersOnTime`; the first star needs both.
- App: panel with the energy and time per answer, a card per kind of question (chips with the quality per answerer),
  the batching window and the quality and answers in time of the draft; the hall's extra rack holds the GPU servers
  (violet); the timeline's lower lane stacks the assistant's power by kind of question.
- Goals ★★ −70 %, ★★★ −76 %. The quantization of the models is left out on purpose: measured savings depend so much on
  the implementation that int8 decoding can even need more energy than float32 [S27].

| Who answers (status, greetings, product, complaints, advice), batching | CO₂ vs. reference | Quality | In time | Stars |
|---|---:|---:|---:|---:|
| large everywhere, one at a time (reference) | – | 95.9 % | 100 % | – |
| small everywhere | −79.3 % | 88.3 % | 100 % | 0 |
| no AI, no AI, large, large, large | −36.1 % | 95.9 % | 100 % | ★ |
| no AI, no AI, small, large, large | −59.9 % | 93.5 % | 100 % | ★ |
| large everywhere, batching 1 s | −66.7 % | 95.9 % | 100 % | ★ |
| no AI, no AI, large, large, large, batching 1 s | −72.5 % | 95.9 % | 100 % | ★★ |
| no AI, no AI, small, large, large, batching 1 s | −76.8 % | 93.5 % | 100 % | ★★★ |
| the same, batching 2 s | −80.3 % | 93.5 % | 69.9 % | 0 |
| the same with 1 s, reasoning for complaints | −61.2 % | 93.9 % | 84.9 % | 0 |
| no AI, no AI, small, large, small, batching 1 s | −80.5 % | 91.2 % | 100 % | 0 |

**As built in step 5, part 8** (E5, "Data diet"):

- Core: `WebModel` and `PageConfig` (image format JPEG / WebP / AVIF converted once / AVIF converted on every
  request, sizes for the screen, lazy loading off / below the fold / all images, product video plays by itself or on
  click, 100 or 24 products per list, six tracking tags / one analytics tag / counting on the server). A visit is ten
  requests; its weight (KB) and its LCP (75th percentile) follow from the settings (game values; the reference page
  weighs 12.2 MB, four to five times the median page of 2025 [S29], and needs 3.2 s). Network and device energy per GB:
  59 and 80 Wh (operational part of [S28]), in the same grid; they count in the emissions (`Totals.networkOperational`,
  `deviceOperational`). The settings change the work per request on the shop's servers too (AVIF on every request
  +80 %, short lists −15 %).
- App: pages panel with the settings and "a visit to the pages online now" (weight by part and LCP of the running
  pages – measured, not predicted); the timeline's lower lane stacks the power of data center, network and devices;
  page weight and LCP in the top bar; end card with the CO₂ of the three parts.
- Goals ★★ −55 %, ★★★ −76 %.

| Images, video, lists, counting | CO₂ vs. reference | MB per visit | LCP | Stars |
|---|---:|---:|---:|---:|
| JPEG one size, autoplay, 100, six tags (reference) | – | 12.2 | 3.2 s | – |
| video on click | −42.8 % | 6.5 | 2.9 s | 0 |
| video on click, one tag | −50.0 % | 5.6 | 2.4 s | ★ |
| the same, 24 per list | −57.8 % | 4.7 | 2.1 s | ★★ |
| the same, AVIF in the right size, lazy below the fold | −78.5 % | 1.9 | 1.5 s | ★★★ |
| AVIF without sizes for the screen | −70.2 % | 3.0 | 1.7 s | ★★ |
| AVIF converted on every request | −74.5 % | 1.7 | 2.8 s | 0 |
| lazy loading for all images | −79.8 % | 1.7 | 2.6 s | 0 |

**As built in step 5, part 9** (E9, "Green pipeline"):

- Core: `PipelineModel` and `PipelineConfig`. Four commits per tick on weekdays from 8 to 18; a run needs build 3 min
  (14 without a cache: an agent on demand starts empty unless the cache is remote), unit tests 5 (1.5 with test impact
  analysis), system tests 12 (4), UI tests 25 per commit or 180 runner minutes once a night in the greenest tick between
  22:00 and 06:00 (by the forecast). Runner minutes queue on the agents (16 always on, or as many as needed); feedback
  = half the trigger interval + boot (3 min on demand) + build + unit tests + the wait in the queue. Test environments:
  6 × replicas servers day and night, on weekdays 7–19, or 3 × replicas on demand from 9 to 17. The first 16 servers
  of the fleet are the agents, the rest the environments; the simulation switches them directly. Friday's release at
  14:00 without UI tests in the 24 hours before counts as a broken rule.
- App: pipeline panel (agents, cache, tests, trigger, environments and replicas; one run with the draft and its
  feedback without queue), timeline lane with the servers on and the nightly UI tests, weekday labels.
- Goals ★★ −60 %, ★★★ −78 %.

| Agents, cache, TIA, UI tests, trigger, environments, replicas | CO₂ vs. reference | Feedback | Stars |
|---|---:|---:|---:|
| always on, –, –, every commit, every commit, day and night, 3 (reference) | – | 8.0 min | – |
| environments weekdays 7–19 | −27.1 % | 8.0 min | ★ |
| UI tests at night | −9.9 % | 8.0 min | ★ |
| always on, TIA, nightly, weekdays 7–19, 1 replica | −51.0 % | 4.5 min | ★ |
| on demand, remote cache, TIA, nightly, weekdays 7–19, 1 replica | −80.9 % | 7.5 min | ★★★ |
| the same without remote cache | −74.7 % | 18.5 min | ★★ |
| the same with 3 replicas | −71.8 % | 7.5 min | ★★ |
| on demand without cache, everything else as the reference | −19.0 % | 22.0 min | 0 |
| on demand, cache, TIA, nightly, every 30 minutes | −85.3 % | 22.5 min | 0 |
| on demand, cache, TIA, UI tests never | −81.6 % | 7.5 min | 0 (release) |

**As built in step 5, part 12** (E1 "How many nines?" and E7 "Constant asking", D30): two scenarios without a clock,
each with its own screen, like the portfolio track.

- E1: `AvailabilityModel`. Each building block fails six times a year; one failure costs 240 min (single), 45 (cold
  standby), 10 (warm standby) or 1 (active-active). A scenario's downtime is 1 − the product of the availabilities of
  the blocks it waits for. Power: single n servers; cold standby + 5 % for copies; warm standby + half the servers at
  15 %; active-active 2n servers at half the load. The queue removes the policy database from "Report a claim", sizes
  the intake for the average (3 servers at 50 % instead of 6 at 25 %) and adds a broker of 2 servers; the circuit
  breaker removes the recommendations from "See contracts". ★ every scenario met; ★★ −20 %, ★★★ −28 %.

| Architecture | CO₂ a year | vs. today | Stars |
|---|---:|---:|---:|
| everything active-active (today) | 88 t | – | ★ |
| everything warm standby | 77 t | −12.8 % | ★ |
| everything cold standby | 58 t | −33.4 % | 0 |
| each block only for its own scenario | 58 t | −33.4 % | 0 |
| shared blocks warm, no decoupling | 67 t | −24.1 % | ★★ |
| the same with the circuit breaker | 64 t | −26.9 % | ★★ |
| shared warm, database cold behind the queue, circuit breaker | 61 t | −30.2 % | ★★★ |

- E7: `UpdatesModel`. Phone: 0.5 J a request while open, 6 J a wake-up while closed, 0.01 J a KB; server: 0.4 J a
  full answer, 0.05 J a short one, 3 J an hour of open connection, PUE 1.4; network 0.06 kWh/GB; push suits at most one
  change in two minutes. The rest of the app (40 t a year) stays the same. ★ every kind as fresh as required; ★★ −30 %,
  ★★★ −45 %.

| Plan | CO₂ a year | vs. today | Stars |
|---|---:|---:|---:|
| today (the arrival notice too late) | 81 t | – | 0 |
| push for the delivery status | 81 t | ±0 | ★ |
| + conditional requests and one background refresh | 50 t | −37.7 % | ★★ |
| a WebSocket for everything, open and closed | 110 t | +36.5 % | ★ |
| push for everything | 41 t | −49.3 % | 0 (map, chat) |
| push for rare changes, streams for frequent ones, no refresh nobody needs | 40 t | −50.1 % | ★★★ |

**As built in step 5, part 11** (scenario 12, "Green on paper", shown as B11 before the capstone, which is now B12;
the numbers stay so that saved stars keep their scenario):

- Core: `ContractModel` and `Policy.contract`. Per tick: location-based emissions (facility energy × grid intensity);
  the clean energy of the contract in that tick (PPA: the week's volume – the reference's consumption – spread over the
  ticks by the wind, here `(max(0, 480 g − intensity))²`; 24/7: 90 % of the consumption; certificates: none); the
  operational emissions count only the energy not covered in the same tick ("hourly matched"); market-based emissions
  count contracted volumes (certificates: all; PPA: up to its volume; 24/7: 90 %) as zero and the rest at the residual
  mix of 650 g/kWh (game value; Germany's residual mix is about twice its average [S36]). Cost: 25 ct/kWh plus the
  contract's surcharge (certificates +0.3 ct per kWh used, PPA +1.5 ct per kWh of its volume, 24/7 +4 ct per kWh
  used); the first star needs the cost within +10 % of the reference's.
- App: tabs generalized (electricity, shop, batch); contract panel with the CO₂ counted three ways and the cost so far;
  the lower lane follows the tab (power and clean energy in the same hour, load, jobs); "in the report" in the top bar.
- Goals ★★ −60 %, ★★★ −75 %.

| Contract, capacity, batch | CO₂ in the hour | In the report | Cost | Stars |
|---|---:|---:|---:|---:|
| grid mix, autoscaling 50 %, at once (reference) | – | 1,325 kg | 100 % | – |
| certificates | ±0 % | 0 kg | 101 % | 0 |
| grid mix, autoscaling 80 % | −10.4 % | 1,175 kg | 89 % | ★ |
| wind farm | −55.2 % | 0 kg | 106 % | ★ |
| wind farm, greenest hours | −63.9 % | 0 kg | 106 % | ★★ |
| wind farm, autoscaling 80 %, greenest hours | −70.6 % | 0 kg | 95 % | ★★ |
| 24/7 carbon-free | −80.8 % | 133 kg | 116 % | 0 (budget) |
| 24/7 carbon-free, autoscaling 80 % | −81.8 % | 118 kg | 103 % | ★★★ |

**As built in step 5, part 10** (E6, "Chatty services"):

- Core: `ServiceModel` and `ServiceConfig`. Tracking requests follow the daily curve, 15,000 a second at the peak.
  CPU per request: 10 ms own work, 4 ms per remote hop with JSON or 1.5 ms with gRPC (half the bytes [S33]), 0.3 ms
  in the monolith; external calls 3 ms each when they go out, 0.2 ms from the cache (calls out: trucks or regions per
  reuse time, at most one per request); 6 ms per call to a serverless function. Instances that always run (two per
  microservice, two for Kafka with events, one for the cache; two for the monolith; none for serverless functions)
  cost 600 ms of CPU a second each. Servers: the work at 70 % of 32 cores, at least one per four instances; the
  simulation switches them directly. Response time: 30 ms + 12 ms per JSON hop (5 with gRPC) + 25 ms per function
  call + 120 ms (GPS) and 150 ms (traffic) for each call that goes out. Data older than allowed count as a broken rule
  for every hour.
- App: architecture panel with a diagram of the draft (boxes and calls, sync and events, cache, functions), the
  settings, the CPU per request and the response time at the peak; timeline lane with the CPU split into own work and
  overhead.
- Revised after the play test: the panel shows what the running settings measure instead of predicting the draft; the
  route optimization (200 cores for ten minutes an hour, 6 to 20) shows independent scaling – the monolith keeps its
  capacity ready, a service of its own scales for it.
- Goals ★★ −38 %, ★★★ −45.5 %.

| Cut, protocol, master data, functions, reuse GPS / traffic | CO₂ vs. reference | Response | Stars |
|---|---:|---:|---:|
| microservices, JSON, sync, –, none / none (reference) | – | 336 ms | – |
| the same with reuse 1 min / 5 min | −16.6 % | 66 ms | ★ |
| reuse 5 min / 1 h | −16.6 % | 66 ms | 0 (data too old) |
| reuse, gRPC | −39.1 % | 45 ms | ★ |
| reuse, events | −40.5 % | 42 ms | ★★ |
| reuse, gRPC, events | −48.0 % | 35 ms | ★★ |
| reuse, gRPC, events, functions | −48.9 % | 35 ms | ★★★ |
| reuse, functions with synchronous calls | +19.0 % | 116 ms | 0 |
| modular monolith with reuse | −53.5 % | 30 ms | ★★★ |
| modular monolith without reuse | −36.6 % | 300 ms | 0 |

---

## 13 Decisions

| # | Question | Decision |
|---|---|---|
| D1 | Platforms | large screens first (the timeline needs width): browser and desktop, tablets; phones from step 9 (revised by D14) |
| D2 | Hall view or pure dashboard | isometric hall with racks, timeline below, policies on the right |
| D3 | Names of the workloads in the app | Interactive, Batch, AI training, Streaming – English IT terms in both languages |
| D4 | Money | later (revised 17.10.): in scenario 4 lost requests would cost far more than electricity, so money would only say "always keep headroom"; it comes with a scenario where the electricity price matters |
| D5 | Second time scale for the hardware lesson | monthly steps, only in scenario 6 and in free play as "fleet planning" |
| D6 | Online ranking for classes | not at first; shareable result cards with the scenario code |
| D7 | Outside temperature per region and season | collected in step 5 |
| D8 | Speeds | 1× = an hour in 4 s, 4×, 16×; checked in the first play test |
| D9 | Order of the teaching goals | the order in which the scenarios introduce them, measurement first (section 1) |
| D10 | Start time | every scenario starts at 00:00 |
| D11 | Scenario 1 | switching by hand only; autoscaling comes with scenario 4 |
| D12 | Preview of the day's result | not during the run; "what if" in the debrief instead (principle 4) |
| D13 | Scenario 2 | batch jobs planned by hand; batch rules come with scenario 3 (same pattern as D11) |
| D14 | Technology | Kotlin Multiplatform + Compose Multiplatform instead of Swift: web (Wasm) and desktop first, Android and iOS later; close to Java for lecturer and students |
| D15 | Lesson of scenario 3 | relative rule (greenest hours of the forecast) beats an absolute threshold; the deadline safety is a switch, so its effect can be seen |
| D16 | Technical terms | every scenario names the term for what the player does (briefing, scenario card, debrief), in English in both languages |
| D17 | Engineering | a second track "Engineering" (E2 profiling, E3 growing data, E4 caching) next to the operations scenarios; backlog with team capacity, coarser time steps; no real code in the game |
| D18 | Bin packing | scenario 5 "Consolidation": VMs on hosts by hand, two dimensions (CPU, RAM), heuristics in "what if"; the operations scenarios after it move up by one |
| D19 | Order of the scenarios | a star opens the next scenario (the story), but a switch "Open all scenarios" opens everything for courses that take the topics in another order; stars still count |
| D20 | Caching | its own Engineering scenario E4 instead of a backlog item; the TTL is a business rule per kind of data, breaking it gives stale answers and no stars; all kinds share the memory of whole cache servers (LRU), so the decisions are what to cache and how many servers; HyperLogLog and the materialized view as extra measures |
| D21 | PUE | shown everywhere in scenario 6, but the stars count CO₂: the scenario is built so that the best PUE and the least CO₂ need different decisions (fewer servers raise the PUE, faster fans lower it) |
| D22 | Hardware renewal (scenario 7) | quarterly steps (a game day stands for a quarter) instead of monthly steps (revises D5); two sites with a yearly budget, growth, new generations and failures, so that decisions come up over the three years; the question is where new hardware replaces dirty electricity, while the clean site keeps enough capacity |
| D23 | Location Shifting (scenario 10) | the player decides per job before the start, not during the game: moving work between sites is a planning decision, and the data must be moved first; the limits come from the jobs themselves (data size, database latency, data residency), so that "everything to Norway" is wrong and the choice needs judgement |
| D24 | Capstone (scenario 11) | stars by CO₂ reduction, not a hard SCI target; the three kinds of work share one draft in tabs, so the controls stay those of the scenarios that introduced them; the forecast update is the one new twist, because a plan days ahead is where a forecast error costs most |
| D25 | "What if" and replays | settings in the debrief's "what if" that reach more stars than the player's best so far are shown without their label ("a setting you have not found yet") – the numbers stay, the recipe does not; a "start over" button in the goal bar returns to the scenario's starting rules and choices (a second click confirms) |
| D26 | Portfolio track | a third track for a company's applications (P1 "Inventory" first); no clock, a year at a time; the data the player buys come from the same budget as the measures; the stars count the real footprint, location-based; open from the start (D32; before: after E2 "Measure first"); the P1 briefing explains location-based and market-based itself |
| D27 | Order of the Engineering scenarios | along the blocks of the iSAQB module GREEN: quality goals (E1 How many nines?), code (E2–E4), the page and the architecture (E5 Data diet, E6 Chatty services, E7 Constant asking), AI (E8 Right-sized AI), the development process (E9 Green pipeline); the scenario numbers in the code stay (104–107), so stars kept on a device stay with their scenario – only the labels and the order of opening changed |
| D28 | Glossary and results in a course | a glossary of all technical terms on the start page; no result codes or ranking – the courses do not need them (revises D6) |
| D29 | P4 "Leave the cloud?" | three years, so that new servers are written off over a fair time; the own server room stays, limited to six existing servers (reuse against a poor PUE); no green-power contract as an option – the report next to the location-based figure shows the same lesson as B11, and the debrief names it |
| D30 | E1 "How many nines?" and E7 "Constant asking" | two architecture scenarios, sorted in along the GREEN blocks of D27: quality goals first (E1, block 3), polling and push after the services (E7, block 6); the others move on (E2 Measure first … E9 Green pipeline). Scenario numbers stay (108, 109; 101–107), so stars kept on a device stay with their scenario. Like the portfolio track without a clock, decided for a year |
| D31 | Showing the solution | on the end card "Show the solution" uncovers the hidden settings of "What if" (revises D25 for players who are stuck) and shows, in scenarios with a clock, what the player's run did differently from the solution (the rules at the end of the run, sprint plans, jobs moved, servers bought, planning again after a forecast change) and the solution step by step; "Put the solution into the task" starts again with it: without a clock the model solution in the screen, with a clock the solution of `Solutions` (core) in the panel – LessonTest checks three stars for each, and the launch option "solution" plays the app's own takeover (all 19 three stars). A solution must be one a player can follow by hand: rules set once at the start where possible (B4: autoscaling at 85 % with at least 22 servers 18:00–22:00, not a change on day 2), servers by hand once an hour on the full hour (B1), and LessonTest checks that it still earns three stars when played a little late (B1 30 minutes, the new plan in the capstone two hours); in the takeover, sprint plans fill in at the start of each sprint and servers by hand switch by themselves. Stars count as usual, also with the solution; no switch for teachers. The comparison lists each aspect with "You:" and "Solution:" on lines of their own. With fewer than three stars, "Play again" comes first on the end card. The end cards without a clock have no "Look at the plan" – "Play again" shows the same plan, open to change. Graded hints are left for later |
| D32 | Opening by content | the three tracks do not depend on each other; inside a track a star opens a scenario only where it builds on another one: Operations B1 → B2 → B3 → B4 (by hand, batch, rules with a forecast, autoscaling), then B5–B11 after B4, the capstone B12 after B8 (training); Engineering all open, E3 after E2 (the same sprints, now with growth); Portfolio P1, P3, P4 open, P2 after P1 (it starts from the inventory). Opening everything is the launch option "unlock" (desktop `./build.sh desktop unlock`, web `?unlock`), for this start only – no switch in the About dialog (revises D19) |

---

## 14 Sources

| # | Source |
|---|---|
| S1 | Green Software Foundation: *Green Software Practitioner* – principles (carbon efficiency, energy efficiency, carbon awareness, hardware efficiency, measurement). https://learn.greensoftware.foundation/ |
| S2 | Green Software Foundation: *Software Carbon Intensity (SCI) Specification* v1.1.0; ISO/IEC 21031:2024. https://sci.greensoftware.foundation/ |
| S3 | Electricity Maps: *Electricity Grid Review 2025* – Germany, France, Norway, Poland, Spain, Great Britain. https://www.electricitymaps.com/grid-in-review-2025/germany (and …/france, …/norway, …/poland, …/spain, …/great-britain) |
| S4 | Electricity Maps: *Emission factors* (life-cycle factors by default). https://app.electricitymaps.com/docs/concepts/emission-factors |
| S5 | Radovanovic et al. (Google): *Carbon-Aware Computing for Datacenters*, arXiv:2106.11750, 2021. https://arxiv.org/pdf/2106.11750 |
| S6 | carboncredits.com: *EU CBAM certificate price for Q3 2026: €82.32 per tonne*, 5 Oct 2026. https://carboncredits.com/eu-cbam-certificate-carbon-price-q3-2026/ |
| S7 | MIT Lincoln Laboratory: *AI models are devouring energy. Tools to reduce consumption are here, if data centers will adopt*, 22 Sep 2023 (power cap 150 W: 12–15 % less energy, about 3 % longer). https://www.ll.mit.edu/news/ai-models-are-devouring-energy-tools-reduce-consumption-are-here-if-data-centers-will-adopt |
| S8 | G. Kamiya, IEA: *The carbon footprint of streaming video: fact-checking the headlines*, Dec 2020. https://www.iea.org/commentaries/the-carbon-footprint-of-streaming-video-fact-checking-the-headlines |
| S9 | *16 Years of SPEC Power: An Analysis of x86 Energy Efficiency Trends*, TU Dresden, arXiv:2411.07062, 2024. https://arxiv.org/html/2411.07062v1 |
| S10 | Dell Technologies: *PowerEdge R760 Life Cycle Assessment*. https://www.delltechnologies.com/asset/en-us/products/servers/briefs-summaries/poweredge-r760-life-cycle-assessment.pdf |
| S11 | AWS: *Graviton sustainability*. https://aws.amazon.com/ec2/graviton/graviton-sustainability |
| S12 | NVIDIA: *DGX H100 datasheet* (up to 10.2 kW). https://www.MEGWARE.com/fileadmin/user_upload/LandingPage%20NVIDIA/nvidia-dgx-h100-datasheet.pdf |
| S13 | NVIDIA: *HGX H100 Product Carbon Footprint Summary*, Jul 2025 (1,312 kg CO₂e cradle-to-gate). https://images.nvidia.com/aem-dam/Solutions/documents/HGX-H100-PCF-Summary.pdf |
| S14 | Uptime Institute: *Global Data Center Survey 2026* (PUE 1.52) and *2025* (PUE 1.54). https://datacenter.uptimeinstitute.com/rs/711-RIA-145/images/UptimeInstitute.GlobalDataCenterSurvey.2026.pdf , https://intelligence.uptimeinstitute.com/resource/uptime-institute-global-data-center-survey-2025 |
| S15 | Google Cloud: *Measuring the environmental impact of AI inference*, 21 Aug 2025 (fleet PUE 1.09). https://cloud.google.com/blog/products/infrastructure/measuring-the-environmental-impact-of-ai-inference |
| S16 | CMS: *Der Bundestag verabschiedet Energieeffizienzgesetz – das erwartet Rechenzentrumsbetreiber*. https://cms.law/de/deu/legal-updates/der-bundestag-verabschiedet-energieeffizienzgesetz-das-erwartet-rechenzentrumsbetreiber |
| S17 | ASHRAE TC 9.9, *Thermal Guidelines for Data Processing Environments*, 5th ed., 2021, summarised by GreenCalculus. https://greencalculus.com/standards/ashrae-tc-9-9-thermal-guidelines/ |
| S18 | Network World: *Microsoft extends Azure server lifetimes by 50%*, 2022; DatacenterDynamics: *Google increases server life to six years*, 2022. https://www.networkworld.com/article/3670737/microsoft-extends-azure-server-lifetimes-by-50.html , https://datacenterdynamics.com/en/news/google-increases-server-life-to-six-years-will-save-billions-of-dollars |
| S19 | R. Pereira et al.: *Ranking programming languages by energy efficiency*, Science of Computer Programming, 2021. https://web.fe.up.pt/~jacome/downloads/SCP21.pdf |
| S20 | *It's Not Easy Being Green: On the Energy Efficiency of Programming Languages*, arXiv:2410.05460 (the language matters less than the implementation and the runtime setup). https://arxiv.org/abs/2410.05460 |
| S21 | L. Breslau, P. Cao, L. Fan, G. Phillips, S. Shenker: *Web Caching and Zipf-like Distributions: Evidence and Implications*, IEEE INFOCOM 1999. |
| S22 | P. Flajolet, É. Fusy, O. Gandouet, F. Meunier: *HyperLogLog: the analysis of a near-optimal cardinality estimation algorithm*, AofA 2007. |
| S23 | H. Che, Y. Tung, Z. Wang: *Hierarchical Web caching systems: modeling, design and experimental results*, IEEE JSAC 20(7), 2002 (the characteristic time of an LRU cache). |
| S24 | J. Aslan, K. Mayers, J. G. Koomey, C. France: *Electricity Intensity of Internet Data Transmission: Untangling the Estimates*, Journal of Industrial Ecology 22(4), 2018 (0.06 kWh/GB in 2015, halving about every two years; other studies differ by orders of magnitude, and the energy of fixed networks grows less than proportionally with the data). The game uses 5 kWh/TB for a transfer between data centers. |
| S25 | C. Elsworth, K. Huang, D. Patterson et al.: *Measuring the environmental impact of delivering AI at Google Scale*, arXiv:2508.15734, 2025 (median Gemini text prompt 0.24 Wh including idle machines and overhead; 33 times less within a year through model improvements, quantization, distilled models and better utilization). https://arxiv.org/abs/2508.15734 |
| S26 | J. You: *How much energy does ChatGPT use?*, Epoch AI, 2025 (about 0.3 Wh for a typical GPT-4o query; reasoning models write about 2.5 times as many tokens). https://epoch.ai/blog/how-much-energy-does-chatgpt-use |
| S27 | J. Delavande, R. Pierrard, S. Luccioni: *Understanding Efficiency: Quantization, Batching, and Serving Strategies in LLM Energy Use*, arXiv:2601.22362, 2026 (serving with batching cuts the energy per request of an 8B model 12.5 to 100 times; int8 decoding can need more energy than float32). https://arxiv.org/abs/2601.22362 |
| S28 | Sustainable Web Design Model, version 4 (Wholegrain Digital, Mightybytes, Green Web Foundation and others): energy per GB – data centers 0.055, networks 0.059, user devices 0.080 kWh/GB operational, plus embodied parts; the authors say the model overestimates and that data transfer is a disputed proxy. https://sustainablewebdesign.org/estimating-digital-emissions/ |
| S29 | HTTP Archive: *Web Almanac 2025, Page Weight* (median home page 2.56 MB on mobile, 2.86 MB on desktop; images about a third). https://almanac.httparchive.org/en/2025/page-weight |
| S30 | web.dev: *Largest Contentful Paint (LCP)* (good: at most 2.5 s at the 75th percentile; a lazily loaded LCP image delays it). https://web.dev/articles/lcp |
| S31 | M. Fowler: *The Rise of Test Impact Analysis*, martinfowler.com, 2017. https://martinfowler.com/articles/rise-test-impact-analysis.html |
| S32 | Green Coding Solutions: *Eco CI* (energy estimation for GitHub Actions and GitLab pipelines, based on SPECpower). https://www.green-coding.io/projects/eco-ci/ |
| S33 | K. Maeda: *Performance evaluation of object serialization libraries in XML, JSON and binary formats*, Second International Conference on Digital Information and Communication Technology and its Applications, 2012 (Protobuf about half the size of JSON, faster to serialize and deserialize). |
| S34 | N. Bjørndal, M. Mazzara, A. Bucchiarone, N. Dragoni, S. Dustdar: *Migration from Monolith to Microservices: Benchmarking a Case Study*, Journal of Object Technology 20(3), 2021. https://doi.org/10.5381/jot.2021.20.2.a3 |
| S35 | iSAQB: *Curriculum for the Advanced Level module GREEN* (learning goals on architecture styles, communication, CI/CD). https://github.com/isaqb-org/curriculum-green |
| S36 | Association of Issuing Bodies (AIB): *European Residual Mixes 2024* (residual mixes after the disclosure of guarantees of origin; average 452 g/kWh; Germany's residual mix well above its production mix). https://www.aib-net.org/facts/european-residual-mix/2024 |
| S37 | GHG Protocol: *Public consultations on Scope 2* (2025): hourly matching and deliverability for market-based reporting, dual reporting kept, final standard expected 2027. https://ghgprotocol.org/blog/release-ghg-protocol-opens-public-consultations-scope-2-and-electricity-sector-consequential |
| S38 | Google: *24/7 carbon-free energy* (hourly CFE score of its data centers, 66 % in the latest report). https://blog.google/around-the-globe/google-europe/how-carbon-free-energy-around-the-clock-can-work/ |
| S39 | US EPA: *Supply Chain Greenhouse Gas Emission Factors v1.3 by NAICS-6* (spend-based factors from an environmentally extended input-output model). https://catalog.data.gov/dataset/supply-chain-greenhouse-gas-emission-factors-v1-2-by-naics-6 |
| S40 | GHG Protocol: *Scope 2 Guidance* (2015): Scope 2 reported both location-based and market-based. https://ghgprotocol.org/scope-2-guidance |
| S41 | McKinsey & Company: *Pathways to a Low-Carbon Economy – Version 2 of the Global Greenhouse Gas Abatement Cost Curve*, 2009 (the first version 2007). https://www.mckinsey.com/capabilities/sustainability/our-insights/pathways-to-a-low-carbon-economy |
| S42 | AWS: *Customer Carbon Footprint Tool now includes location-based emissions* (June 2025); Scope 3 since October 2025. https://aws.amazon.com/about-aws/whats-new/2025/06/customer-carbon-footprint-tool-location-based-emissions/ |
| S43 | Uptime Institute: *Global Data Center Survey 2025* – average PUE 1.54. https://intelligence.uptimeinstitute.com/resource/uptime-institute-global-data-center-survey-2025 |
| S44 | L. Bass, P. Clements, R. Kazman: *Software Architecture in Practice*, 4th ed., Addison-Wesley 2021 – quality attribute scenarios and availability tactics. |
| S45 | N. Balasubramanian, A. Balasubramanian, A. Venkataramani: *Energy Consumption in Mobile Phones: A Measurement Study and Implications for Network Applications*, IMC 2009 – the tail energy of the radio after each transfer. |
| S46 | J. Huang et al.: *A Close Examination of Performance and Power Characteristics of 4G LTE Networks*, MobiSys 2012. |
