# Load Shift – Game Concept

Load Shift is a teaching game about Green IT for iPad, Mac and iPhone. The player runs a data center through
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
| G5 | Facility efficiency | Cooling costs energy; PUE shows how much. | 5 |
| G6 | Hardware efficiency | High utilization and a long service life; manufacturing emissions count too. | 6 |
| G7 | Demand shaping | When the grid is dirty, do less: lower quality, fewer extras. | 8 |
| G8 | Carbon awareness: location shifting | Where the work runs matters even more than when: Poland emits about 22 times as much per kWh as Norway. It comes last because it needs a second site; scenario 1's debrief already names it as an outlook. | 9 |

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

Ten scenarios, each introducing one idea. Region and season are chosen so that the idea has a clear effect.

| # | Title | Region, season | Length | New controls | Idea |
|---|---|---|---|---|---|
| 1 | Night shift | Germany, autumn | 1 day | servers on/off by hand | Idle power: at night 20 % load, all servers on wastes energy; switching on takes 30 minutes, so plan ahead (G2). |
| 2 | Wait for the sun | Spain (annual profile) | 2 days | batch jobs planned by hand, forecast | Reports and backups move into the midday dip – the night is not clean in a solar grid (G3). |
| 3 | When the wind drops | Great Britain, winter | 3 days | batch rules (fixed hour, greenest hours, threshold), deadline safety | The rules automate what the player planned by hand in scenario 2. A fixed threshold works while the wind blows and fails when it drops: without the deadline safety jobs miss their deadlines, with it they start at the last moment in dirty hours. A rule that compares the hours of the (uncertain) forecast adapts to the weather (G3, D15). |
| 4 | Headroom | Germany, winter | 2 days | capacity rules with autoscaling (target utilization), money, SLA penalty | Autoscaling automates what the player did by hand in scenario 1; a marketing campaign doubles the load: too little headroom costs SLA, too much costs energy (G4). |
| 5 | Heat wave | Germany, summer | 3 days | cooling type, setpoint | PUE rises with the outside temperature; setpoint and free cooling (G5). |
| 6 | Old iron | France and Poland | 3 years (monthly steps) | buying and retiring servers | Replacing hardware pays off in Poland, hardly in France (G6, G1). |
| 7 | Training run | Germany, spring | 3 days | AI training, power cap | Run the training in clean hours and cap the GPUs (G3, G6). |
| 8 | Eco mode | Poland, summer | 2 days | streaming eco mode | Lower quality when the grid is dirty (G7). |
| 9 | Two sites | Norway and Germany | 3 days | moving workloads between sites | Batch and training move to Norway; interactive stays close to the users (latency) (G8). |
| 10 | Dark doldrums | Germany, winter, windless | 7 days | all | Little sun, no wind, high prices – everything together, with a hard SCI target. |

Each scenario has:

- a **briefing**: situation, the one idea, its **technical term** (D16), a small preview of the play screen with
  numbered areas and what they show, facts and goals,
- the play,
- a **debrief** (section 8.2) with a short explanation of the idea and one source.

**Free play** (after scenario 5): choose region, season, length, workload mix and fleet; all unlocked controls.

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
| 9 | Free play, scenario codes, result cards, lexicon | **play test in a lecture** |
| 10 | Look and sound, App Store texts and screenshots | release |

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
  Goals: ★★ −10 %, ★★★ batch work ≤ 140 g/kWh.

| Rule | CO₂ vs. reference | Batch work g/kWh | Deadlines missed | Stars |
|---|---:|---:|---:|---:|
| as soon as possible (reference) | – | 169 | 0 | – |
| greenest hours by the forecast | −14 % | 135 | 0 | ★★★ |
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

**Taken over from the Carbon Rush prototype** (copied, checked for every reference to its origin):
grid curves and their generator, the forecast model, the electricity price formula, the embodied and SCI
bookkeeping, the build script with its watcher, the string catalog tools, the App Store tools, sound effects and
music. Not taken over: maps, paths, the tower-and-creep rules, opponents, network play.

---

## 13 Decisions

| # | Question | Decision |
|---|---|---|
| D1 | Platforms | large screens first (the timeline needs width): browser and desktop, tablets; phones from step 9 (revised by D14) |
| D2 | Hall view or pure dashboard | isometric hall with racks, timeline below, policies on the right |
| D3 | Names of the workloads in the app | Interactive, Batch, AI training, Streaming – English IT terms in both languages |
| D4 | Money | from scenario 4; before that only CO₂ and SLA |
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
