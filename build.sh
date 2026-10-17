#!/bin/bash
# Tests the simulation core and builds the app for the iPad simulator.
# Usage:   ./build.sh         test and build
#          ./build.sh run     also install and start in the iPad simulator (visible)
#          ./build.sh shot    start the app invisibly (extra words = launch arguments), screenshot, quit
#          ./build.sh mac     let Xcode build and start the iPad app on this Mac ("Designed for iPad", Apple silicon)
#          ./build.sh stop    shut down all simulators
#          ./build.sh commit  commit everything with the message in build/commit-msg.txt and push
#          ./build.sh push    push again after a failed push
#          ./build.sh script  run the one-off script build/script.sh
#          ./build.sh ktest   Kotlin: test the simulation core (JVM and Wasm), lesson report in build/lesson-report.txt
#          ./build.sh web     Kotlin: build the web app (Wasm) into build/web
#          ./build.sh serve   Kotlin: build the web app and open it in the browser (http://localhost:8080)
#          ./build.sh desktop Kotlin: build and start the desktop app
#          ./build.sh watch   wait for build requests from Claude (file build/request) and run them;
#                             stop with Ctrl-C
set -uo pipefail
cd "$(dirname "$0")"

DERIVED=build/DerivedData
APP="$DERIVED/Build/Products/Debug-iphonesimulator/LoadShift.app"
BUNDLE=de.wanner-it.loadshift
# Version scheme: 0.M.P (build B). The file VERSION holds "0.M" (M = implementation step of the concept);
# P counts the commits within that version: the first commit gets the tag v0.M.0 and patch 0.
# B counts all commits.

# Writes the string catalog in Xcode's own form (order and layout), so Xcode does not rewrite it
# after changes made with other tools – otherwise it shows up as changed in git again and again.
normalize_strings() {
  swift tools/strings/normalize.swift LoadShift/Localizable.xcstrings
}

# Newest simulator of the given kind ("iPad", "iPhone") with iOS major version $SIM_IOS (default 26).
find_simulator() {
  xcrun simctl list devices available | awk -v want="${SIM_IOS:-26}" -v kind="$1" '
    /^-- iOS/ { split($3, v, "."); ios = (v[1] == want); found = ""; next }
    /^--/     { ios = 0; next }
    ios && $0 ~ kind && found == "" { match($0, /[0-9A-F-]{36}/); found = substr($0, RSTART, RLENGTH); last = found }
    END { print last }'
}

build() {
  # First word = mode, the rest is passed to the app as launch arguments.
  local mode extra
  read -r mode extra <<< "${1:-}"
  case "$mode" in
    ktest)
      ./gradlew :core:allTests --console=plain -q 2>&1 | grep -E "^e:|FAILED|Test.*>|AssertionError" | head -40
      local result=${PIPESTATUS[0]}
      grep -h -o 'tests="[0-9]*" skipped="[0-9]*" failures="[0-9]*" errors="[0-9]*"' core/build/test-results/*/*.xml \
        | awk -F'"' '{ t += $2; f += $6 + $8 } END { print "▸ " t " tests, " f " failed" }'
      return $result ;;
    web|serve)
      ./gradlew :composeApp:wasmJsBrowserDistribution --console=plain -q 2>&1 | grep -E "^e:|error" | head -40
      [ "${PIPESTATUS[0]}" = 0 ] || { echo "** WEB BUILD FAILED **"; return 1; }
      rm -rf build/web && mkdir -p build/web && cp -R composeApp/build/dist/wasmJs/productionExecutable/. build/web/
      echo "✓ web app in build/web"
      if [ "$mode" = "serve" ]; then
        pkill -f "http.server 8080" >/dev/null 2>&1 || true
        (cd build/web && nohup python3 -m http.server 8080 >/dev/null 2>&1 &)
        sleep 1
        open "http://localhost:8080/index.html${extra:+?$extra}"
        echo "✓ serving on http://localhost:8080"
      fi
      return 0 ;;
    desktop)
      # In the background, so the watcher keeps running; extra words are passed to the app.
      nohup ./gradlew :composeApp:run --console=plain -q ${extra:+--args="$extra"} > build/desktop.log 2>&1 &
      echo "✓ the desktop app starts (log: build/desktop.log)"
      return 0 ;;
    stop)
      xcrun simctl shutdown all
      osascript -e 'tell application id "com.apple.iphonesimulator" to quit' >/dev/null 2>&1
      echo "✓ all simulators stopped"
      return 0 ;;
    commit)
      [ -s build/commit-msg.txt ] || { echo "** build/commit-msg.txt is missing **"; return 1; }
      local base tag patch n
      base=$(tr -d '[:space:]' < VERSION)
      tag="v$base.0"
      if git rev-parse -q --verify "refs/tags/$tag" >/dev/null; then
        patch=$(( $(git rev-list --count "$tag"..HEAD) + 1 ))
      else
        patch=0
      fi
      n=$(( $(git rev-list --count HEAD 2>/dev/null || echo 0) + 1 ))
      sed -i '' -E "s/(MARKETING_VERSION = )[0-9.]+;/\1$base.$patch;/; s/(CURRENT_PROJECT_VERSION = )[0-9]+;/\1$n;/" LoadShift.xcodeproj/project.pbxproj
      echo "▸ version $base.$patch (build $n)"
      normalize_strings
      git add -A && git commit -q -F build/commit-msg.txt || return 1
      if [ "$patch" = 0 ]; then
        git tag -a "$tag" -m "Version $base" && echo "▸ tag $tag"
      fi
      git push 2>&1 && { [ "$patch" != 0 ] || git push origin "$tag" 2>&1; } && git log --oneline -3
      return $? ;;
    push)
      git push 2>&1 && git log --oneline -3
      return $? ;;
    script)
      # Runs a one-off script that Claude put into build/script.sh (build/ is not in git).
      [ -f build/script.sh ] || { echo "** build/script.sh is missing **"; return 1; }
      bash build/script.sh
      return $? ;;
    mac)
      # The unchanged iPad app on an Apple-silicon Mac. macOS starts such an app only the way Xcode does
      # it, so Xcode is told to build and run it ("My Mac (Designed for iPad)", like ⌘R). The first time,
      # macOS asks whether Terminal may control Xcode.
      echo "▸ Xcode builds and starts the app on this Mac"
      osascript - "$PWD/LoadShift.xcodeproj" <<'OSA'
on run argv
  tell application "Xcode"
    activate
    set ws to open POSIX file (item 1 of argv)
    repeat 120 times
      if loaded of ws then exit repeat
      delay 0.5
    end repeat
    set active scheme of ws to scheme "LoadShift" of ws
    set macs to (run destinations of ws whose name contains "Mac")
    if (count of macs) is 0 then error "no Mac run destination – pick My Mac (Designed for iPad) in Xcode once"
    set active run destination of ws to item 1 of macs
    run ws
    return "✓ started in Xcode: " & (name of item 1 of macs)
  end tell
end run
OSA
      return $? ;;
  esac

  echo "▸ testing the simulation core"
  (cd LoadShiftCore && swift test 2>&1 | awk '/error:/ { print } /passed \(([5-9]|[0-9][0-9]+)\.[0-9]+ seconds\)/ { print "  slow: " $0 } /Executed .* tests/ { last = $0 } END { print last }')
  normalize_strings
  echo "▸ building the app for the simulator"
  if ! xcodebuild -project LoadShift.xcodeproj -scheme LoadShift \
      -configuration Debug -destination 'generic/platform=iOS Simulator' \
      -derivedDataPath "$DERIVED" -quiet build; then
    echo "** BUILD FAILED **"
    return 1
  fi
  echo "✓ built: $APP"

  if [ "$mode" = "run" ] || [ "$mode" = "shot" ]; then
    local device simulator_app xcode_app
    device=$(find_simulator iPad)
    [ -n "$device" ] || { echo "** no iPad simulator with iOS ${SIM_IOS:-26} **"; return 1; }
    if ! xcrun simctl list devices booted | grep -q "$device"; then
      xcrun simctl shutdown all >/dev/null 2>&1 || true
      echo "▸ booting simulator $(xcrun simctl list devices | grep "$device" | sed -E 's/ *\(.*//; s/^ *//')"
      xcrun simctl boot "$device"
    fi
    # Simulator window: "Simulator.app" up to Xcode 26, "DeviceHub.app" from Xcode 27.
    xcode_app="$(xcode-select -p)/../.."
    for candidate in "$xcode_app/Contents/Developer/Applications/Simulator.app" "$xcode_app/Contents/Applications/DeviceHub.app"; do
      [ -d "$candidate" ] && simulator_app="$candidate"
    done
    [ "$mode" = "run" ] && open "$simulator_app" --args -CurrentDeviceUDID "$device"
    xcrun simctl install "$device" "$APP"
    xcrun simctl terminate "$device" "$BUNDLE" >/dev/null 2>&1 || true
    if [ "$mode" = "shot" ]; then
      # Screenshots run without music and sound effects.
      # shellcheck disable=SC2086
      xcrun simctl launch "$device" "$BUNDLE" -musicEnabled NO -soundEnabled NO $extra
      sleep 6
    else
      # shellcheck disable=SC2086
      xcrun simctl launch "$device" "$BUNDLE" $extra
      sleep 4
    fi
    xcrun simctl io "$device" screenshot build/screenshot.png >/dev/null 2>&1 && echo "✓ screenshot: build/screenshot.png"
    if [ "$mode" = "shot" ]; then
      xcrun simctl terminate "$device" "$BUNDLE" >/dev/null 2>&1 || true
      xcrun simctl shutdown "$device" >/dev/null 2>&1 || true
      echo "✓ finished, simulator shut down"
    else
      open "$simulator_app"
      echo "✓ running in the simulator"
    fi
  fi
}

if [ "${1:-}" = "watch" ]; then
  mkdir -p build
  echo "Waiting for build requests (build/request). Stop with Ctrl-C."
  while true; do
    if [ -f build/request ]; then
      mode=$(cat build/request)
      rm -f build/request
      echo "$(date '+%H:%M:%S') build request ($mode)"
      # Call the script anew each time so changes to build.sh take effect immediately.
      "./build.sh" "$mode" > build.log 2>&1
      echo "done $(date '+%s')" >> build.log
      tail -2 build.log
    fi
    sleep 2
  done
elif [ -t 1 ]; then
  build "$*" 2>&1 | tee build.log
else
  build "$*"
fi
