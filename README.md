# Load Shift

A teaching game about Green IT for iPad, Mac and iPhone. You run a data center through days and weeks:
servers, workloads, cooling and electricity from a real-world grid mix. Serve your users reliably and emit as
little CO₂ as possible – by switching off idle servers, moving flexible work into clean hours, cooling wisely and
keeping hardware in service.

The game is a simulation, not a race: pause at any time, plan in peace, and let time run faster when nothing
needs to be decided. Ten scenarios introduce one idea each; a debrief compares your result with a reference
operator and measures it with the Software Carbon Intensity (SCI, ISO/IEC 21031:2024).

Status: step 3 of the concept is under way (`docs/CONCEPT.md`, section 12). Playable: scenario 1 "Night shift"
(switching servers by hand) and scenario 2 "Wait for the sun" (planning batch jobs into clean hours), with
goals, stars and "what if" in the debrief. The lesson report of the core tests is written to
`build/lesson-report.txt`.

## Project layout

| Path | Content |
|---|---|
| `LoadShiftCore/` | Swift package with the deterministic simulation (no UI) and its tests |
| `LoadShift/` | SwiftUI app for iPad and Mac ("Designed for iPad"); string catalog EN/DE |
| `LoadShift.xcodeproj` | Xcode project (synchronized folder: new files in `LoadShift/` are picked up automatically) |
| `tools/` | helper scripts (string catalog normalizer, later grid curves and App Store tools) |
| `docs/CONCEPT.md` | game concept, decisions and sources |

## Building

```
./build.sh          # test the core and build the app for the iPad simulator
./build.sh run      # also start it in the iPad simulator
./build.sh mac      # build and start it on this Mac with Xcode
./build.sh watch    # wait for build requests from Claude (file build/request)
```

Requires Xcode 16 or later. The version is 0.M.P: `VERSION` holds 0.M (M = implementation step), P counts the
commits within it.

The app speaks English and German; everything in this repository is written in English.

## License

MIT, see `LICENSE`.
