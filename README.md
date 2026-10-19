# Load Shift

A simulation game for teaching Green IT – from code to portfolio. It runs in the browser and as a desktop app for
Windows, macOS and Linux (Android and iOS later). It has three tracks. In **Operations** you run a data center:
switch off idle servers, move flexible work into clean hours, cool wisely, renew hardware where it pays off. In
**Engineering** you improve the software that runs in it: measure before you optimize, cache what repeats, use AI
models only as large as needed. In **Portfolio** you look at the IT of a whole organisation: take stock of its
emissions, choose the measures that pay off first and tidy up the application landscape. Serve your users reliably
and emit as little CO₂ as possible.

The game is a simulation, not a race: pause at any time, plan in peace, and let time run faster when nothing needs
to be decided. Each scenario introduces one idea and names its technical term; a debrief compares your result with
a reference operator and shows "what if" with other rules.

**Play it in the browser: https://diavolezza.github.io/LoadShift/** (built from `main` on every push).

The app speaks English and German (switch on the start page); everything in this repository is written in English.

## Scenarios

| | Operations | Term |
|---|---|---|
| O1 | Night shift | Rightsizing |
| O2 | Wait for the sun | Time Shifting |
| O3 | When the wind drops | Carbon-Aware Scheduling |
| O4 | Headroom | Autoscaling |
| O5 | Consolidation | Consolidation (Bin Packing) |
| O6 | Heat wave | PUE (Power Usage Effectiveness) |
| O7 | Old iron | Embodied Carbon |
| O8 | Training run | Carbon-Aware Training (Power Capping) |
| O9 | Eco mode | Demand Shaping |
| O10 | Two sites | Location Shifting |
| O11 | Green on paper | 24/7 Carbon-Free Energy |
| O12 | Dark doldrums | Carbon Awareness |

| | Engineering | Term |
|---|---|---|
| E1 | How many nines? | Availability Tactics |
| E2 | Measure first | Profiling |
| E3 | Growing data | Algorithmic complexity |
| E4 | Hit or miss | Caching |
| E5 | Data diet | Sustainable Web Design |
| E6 | Chatty services | Green Software Architecture |
| E7 | Constant asking | Polling, Push, Streaming |
| E8 | Right-sized AI | Green AI (Right-sizing Models) |
| E9 | Green pipeline | Green CI/CD |

| | Portfolio | Term |
|---|---|---|
| P1 | Inventory | Carbon Accounting |
| P2 | Where to start | Marginal Abatement Cost Curve (MACC) |
| P3 | Tidy up with TIME | Application Rationalization (TIME) |
| P4 | Leave the cloud? | Cloud Repatriation |

In German the operations scenarios are called B1–B12 ("Betrieb"). A star in one scenario opens the next; teachers can
open all scenarios, show the solutions or reset the progress in the About dialog. A player who is stuck can uncover
the solution on the end card; a result counts at most one star after that. The start page continues with the next open
scenario and shows one tab per track; a glossary collects every technical term. `docs/CONCEPT.md`, section 9.4, maps every scenario to the learning goals of the
iSAQB Advanced Level module GREEN, for courses that follow it. More scenarios are planned (section 9).

## Running the game

### What you need

- **A JDK, version 21 or later**, for example [Eclipse Temurin](https://adoptium.net). Check with `java -version`.
  Nothing else has to be installed: the Gradle wrapper (`gradlew`, `gradlew.bat`) downloads Gradle, Kotlin and all
  libraries on the first build – this needs an internet connection and takes a few minutes.
- For the web version a current browser with WebAssembly GC: Chrome or Edge 119+, Firefox 120+, Safari 18.2+.

### Desktop app

macOS and Linux:

```
./gradlew :composeApp:run
```

Windows:

```
gradlew.bat :composeApp:run
```

### Web app

To try it out, the development server builds the app and opens it in the browser:

```
./gradlew :composeApp:wasmJsBrowserDevelopmentRun        # Windows: gradlew.bat …
```

For other people, build the web app once and put the files on any web server:

```
./gradlew :composeApp:wasmJsBrowserDistribution
```

The files are in `composeApp/build/dist/wasmJs/productionExecutable/` (about 13 MB: `index.html`, `loadshift.js`,
two `.wasm` files, fonts). They must be served over HTTP – opening `index.html` from the file system does not work.
A local test: `cd composeApp/build/dist/wasmJs/productionExecutable && python3 -m http.server 8080`, then open
http://localhost:8080. Any static host works (GitHub Pages, Netlify, Cloudflare Pages, a web server of your own);
the server should deliver `.wasm` files as `application/wasm`. This repository publishes it on GitHub Pages with the workflow `.github/workflows/pages.yml`.

### Installers

```
./gradlew :composeApp:packageDmg      # macOS, run on a Mac
gradlew.bat :composeApp:packageMsi    # Windows, run on Windows
./gradlew :composeApp:packageDeb      # Debian/Ubuntu, run on Linux
```

The installers are in `composeApp/build/compose/binaries/main/`. They are not signed: macOS asks for confirmation
on the first start (right-click → Open), Windows SmartScreen warns once.

### Launch options

For screenshots, tests and teaching: desktop `./gradlew :composeApp:run --args="-scenario 7 -play -ticks 40 -careful -lang de"`,
web `index.html?scenario=7&play&ticks=40&careful&lang=de`. `scenario` opens a scenario's briefing (101–103 are E2–E4),
`play` starts it, `ticks` runs that many 15-minute steps, `careful` plays like a careful player, `lang` sets the
language for this start.

## Development

```
./gradlew :core:allTests     # simulation tests on the JVM and in Wasm; the lesson report is in build/lesson-report.txt
```

`build.sh` (macOS and Linux) wraps the common tasks: `./build.sh ktest | web | serve | desktop`.

| Path | Content |
|---|---|
| `core/` | Kotlin Multiplatform module with the deterministic simulation (no UI) and its tests; `LessonTest` checks that every scenario teaches what it should |
| `composeApp/` | Compose Multiplatform app: web (Wasm) and desktop (JVM) |
| `tools/` | grid curves and fonts |
| `docs/CONCEPT.md` | game concept, model, decisions and sources |

Built with Kotlin 2.4 and Compose Multiplatform 1.12. The first version was written in Swift for iPad and Mac
(tag `v0.3.0`); the Kotlin version replaced it.

## License

MIT, see `LICENSE`. The font Inter is under the SIL Open Font License 1.1 (`tools/fonts/Inter-OFL.txt`).
