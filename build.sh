#!/bin/bash
# Builds and tests Load Shift (Kotlin Multiplatform, docs/CONCEPT.md section 11).
# Usage:   ./build.sh ktest    test the simulation core (JVM and Wasm); lesson report in build/lesson-report.txt
#          ./build.sh web      build the web app (Wasm) into build/web
#          ./build.sh serve    build the web app and open it in the browser (http://localhost:8080);
#                              extra words become the address options, e.g. "serve scenario=2&lang=de"
#          ./build.sh desktop  build and start the desktop app (extra words are passed to it)
#          ./build.sh commit   commit everything with the message in build/commit-msg.txt and push
#          ./build.sh push     push again after a failed push
#          ./build.sh script   run the one-off script build/script.sh
#          ./build.sh watch    wait for build requests from Claude (file build/request) and run them;
#                              stop with Ctrl-C
set -uo pipefail
cd "$(dirname "$0")"

# Version scheme: 0.M.P. The file VERSION holds "0.M" (M = implementation step of the concept); P counts the
# commits within that version: the first commit gets the tag v0.M.0 and patch 0. The version is written to
# gradle.properties ("version=…") before committing.

build() {
  # First word = mode, the rest are options.
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
      # In the background, so the watcher keeps running.
      nohup ./gradlew :composeApp:run --console=plain -q ${extra:+--args="$extra"} > build/desktop.log 2>&1 &
      echo "✓ the desktop app starts (log: build/desktop.log)"
      return 0 ;;
    commit)
      [ -s build/commit-msg.txt ] || { echo "** build/commit-msg.txt is missing **"; return 1; }
      local base tag patch
      base=$(tr -d '[:space:]' < VERSION)
      tag="v$base.0"
      if git rev-parse -q --verify "refs/tags/$tag" >/dev/null; then
        patch=$(( $(git rev-list --count "$tag"..HEAD) + 1 ))
      else
        patch=0
      fi
      if grep -q '^version=' gradle.properties; then
        sed -i '' -E "s/^version=.*/version=$base.$patch/" gradle.properties
      else
        echo "version=$base.$patch" >> gradle.properties
      fi
      echo "▸ version $base.$patch"
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
    *)
      echo "unknown mode \"$mode\" – see the top of build.sh"
      return 1 ;;
  esac
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
