# Load Shift

A teaching game about Green IT for the browser, Windows, macOS and Linux (Android and iOS later). You run a data
center through days and weeks: servers, workloads, cooling and electricity from a real-world grid mix. Serve your
users reliably and emit as little CO₂ as possible – by switching off idle servers, moving flexible work into clean
hours, cooling wisely and keeping hardware in service.

The game is a simulation, not a race: pause at any time, plan in peace, and let time run faster when nothing
needs to be decided. Each scenario introduces one idea; a debrief compares your result with a reference operator
and shows "what if" with other rules.

Status: step 3 of the concept is under way (`docs/CONCEPT.md`, section 12). Playable: scenario 1 "Night shift"
(switching servers by hand) and scenario 2 "Wait for the sun" (planning batch jobs into clean hours).

The app speaks English and German; everything in this repository is written in English.

## Project layout

| Path | Content |
|---|---|
| `core/` | Kotlin Multiplatform module with the deterministic simulation (no UI) and its tests |
| `composeApp/` | Compose Multiplatform app: web (Wasm) and desktop (JVM) |
| `tools/` | helper scripts (grid curves, string migration) |
| `docs/CONCEPT.md` | game concept, decisions and sources |
| `LoadShift/`, `LoadShiftCore/`, `LoadShift.xcodeproj` | the first version in Swift (iPad and Mac); removed once the Kotlin app runs on iOS |

## Building

Requires JDK 21 or later. The Gradle wrapper downloads everything else.

```
./gradlew :core:allTests                          # simulation tests on the JVM and in Wasm
./gradlew :composeApp:run                         # desktop app
./gradlew :composeApp:wasmJsBrowserDevelopmentRun # web app in the browser
./build.sh ktest | web | serve | desktop          # the same via the build script (also for the watcher)
```

Launch options for screenshots and tests: desktop `-scenario 2 -play -ticks 40 -careful -lang de`, web
`index.html?scenario=2&ticks=40&careful&lang=de`.

## License

MIT, see `LICENSE`. The font Inter is under the SIL Open Font License 1.1 (`tools/fonts/Inter-OFL.txt`).
