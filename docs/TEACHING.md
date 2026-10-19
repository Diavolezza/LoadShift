# Teaching with Load Shift

Load Shift is a simulation game for teaching Green IT. Each scenario takes about 15 to 30 minutes and introduces one
idea with its technical term. The players decide, the simulation shows the consequences, and the debrief explains
them and shows "what if" with other decisions. Stars are feedback for the players, not a grade.

## Getting started

- **In the browser:** https://diavolezza.github.io/LoadShift/ – nothing to install, no sign-up. The progress stays in
  the browser on that device; nothing is sent anywhere.
- **Language:** English or German, switched on the start page, or with `?lang=de` / `?lang=en` in the address.
- **All scenarios open:** add `?unlock` to the address (for example `https://diavolezza.github.io/LoadShift/?unlock`),
  or start the desktop app with `./build.sh desktop unlock`. Without it, a scenario opens with a star only where it
  builds on another one; the three tracks are independent.
- **One scenario directly:** `?scenario=N` opens its briefing (numbers below).
- **Stuck players:** the end card offers "Show the solution": what the run did differently, the solution step by
  step, and "Put the solution into the task".
- **A new group on the same device:** About (i) → "Reset progress".

## The scenarios

| Track | Scenario (number for `?scenario=`) | Term |
|---|---|---|
| Operations | B1 Night shift (1) · B2 Wait for the sun (2) · B3 When the wind drops (3) · B4 Headroom (4) | Rightsizing · Time Shifting · Carbon-Aware Scheduling · Autoscaling |
| | B5 Consolidation (5) · B6 Heat wave (6) · B7 Old iron (7) · B8 Training run (8) | Consolidation · PUE · Embodied Carbon · Carbon-Aware Training |
| | B9 Eco mode (9) · B10 Two sites (10) · B11 Green on paper (12) · B12 Dark doldrums (11) | Demand Shaping · Location Shifting · 24/7 Carbon-Free Energy · Carbon Awareness |
| Engineering | E1 How many nines? (108) · E2 Measure first (101) · E3 Growing data (102) · E4 Hit or miss (103) | Availability Tactics · Profiling · Algorithmic complexity · Caching |
| | E5 Data diet (105) · E6 Keep everything? (111) · E7 Chatty services (107) · E8 Constant asking (109) | Sustainable Web Design · Data Lifecycle Management · Green Software Architecture · Polling, Push, Streaming |
| | E9 One per customer? (110) · E10 Right-sized AI (104) · E11 Green pipeline (106) · E12 One pattern for everything? (112) | Multi-tenancy · Green AI · Green CI/CD · Architecture patterns |
| Portfolio | P1 Inventory (201) · P2 Where to start (202) · P3 Tidy up with TIME (203) · P4 Leave the cloud? (204) | Carbon Accounting · MACC · TIME · Cloud Repatriation |

The German operations scenarios are called B1–B12 ("Betrieb"); in English the game shows them as O1–O12.

## Suggested sessions

| Course | Scenarios | 90 minutes |
|---|---|---|
| Introduction to Green IT | B1, B2, B4, B7 | idle power, time shifting, headroom, embodied carbon |
| Software engineering | E2, E3, E4, E5 | measure first, complexity, caching, the weight of a web page |
| Software architecture | E1, E6, E9, E12 (and E7, E8) | quality scenarios against energy: availability, data lifecycle, tenancy, patterns |
| IT management / EAM | P1, P2, P3, P4 | where the emissions are, what to do first, TIME, cloud or not |

`docs/CONCEPT.md`, section 9.4, maps every scenario to the learning goals of the iSAQB Advanced Level module GREEN.

## Running a session

1. **Briefing together (5 min):** read the situation and the idea; make sure everyone knows the goals. The idea
   states facts, not the solution.
2. **Play in pairs (15–25 min):** two people at one screen talk about their decisions – that is where the learning
   happens. Pause the clock to think; speed it up when nothing needs to be decided.
3. **Compare (10 min):** collect the results of the pairs. Which decisions made the difference? Who met the rules
   but missed the reduction, and why?
4. **Debrief (5–10 min):** read the debrief and the "what if" table together. The term and its explanation are the
   bridge to the lecture.

Questions that work for most scenarios: What did the reference do, and why is it not stupid? Which goal pulled
against which? What would you need to know in a real project that the game told you for free?

## What the numbers are

The models are simplified but plausible: grid data from Electricity Maps (life-cycle, imports included), power and
embodied emissions from published figures, quoted in `docs/CONCEPT.md` (sources S1 onward). The games are meant to
show directions and orders of magnitude, not to replace a measurement.
