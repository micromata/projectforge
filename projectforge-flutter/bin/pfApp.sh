#!/usr/bin/env bash
#
# pfApp.sh — dev helper for the ProjectForge app (projectforge-flutter), the counterpart of bin/pfDev.sh.
#
# Works from any working directory. Flutter is taken from the PATH, otherwise from $FLUTTER_HOME or the usual
# install locations in the home directory.
#
set -euo pipefail

# App dir = parent of bin/, resolved from this script's own location.
APP="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
ROOT="$(cd "$APP/.." && pwd)"

usage() {
  cat <<'EOF'
pfApp.sh <command> [slot] [args…] — ProjectForge app dev helper

Commands:
  run [slot] [--android] [args…]
                   flutter run with the server of the slot (:8080+slot) preset in the
                   login; warns if it doesn't answer. Without -d it boots an iOS
                   simulator (if none is running) and runs there. --android: the server as
                   10.0.2.2 (how the Android emulator reaches the Mac). Args are
                   forwarded (e.g. pfApp.sh run 1 -d "iPhone 18 Pro")
  sim [name]       Boot an iOS simulator (default: the first available iPhone) and
                   open Simulator.app
  devices          Devices flutter can run on, and the available iOS simulators
  check            Quality gates: dart format (check only) → flutter analyze → flutter test
  test [args…]     flutter test, args forwarded (e.g. test test/core/pkce_test.dart)
  format           dart format lib test
  gen              The i18n catalogs assets/i18n/generated.*.json (bin/pfDev.sh gen)
  build-ios [--release]
                   Simulator build (debug); --release: flutter build ipa (needs the
                   signing set up in Xcode, see xcode)
  build-android [--release]
                   flutter build apk (debug, or release)
  xcode            Open the iOS project in Xcode (signing, capabilities)
  clean            flutter clean + flutter pub get
  deps             flutter pub get + flutter pub outdated
  doctor           flutter doctor -v
  logs             flutter logs of the running app
  help             Show this help

Slots as in bin/pfDev.sh: a number 1–9 picks the server :8080+n (bin/pfDev.sh run n), none :8080.

Env:
  FLUTTER_HOME     Flutter SDK, if flutter isn't on the PATH
EOF
}

# Puts flutter on the PATH if it isn't there yet.
find_flutter() {
  command -v flutter >/dev/null 2>&1 && return 0
  local dir
  for dir in "${FLUTTER_HOME:-}" "$HOME/development/flutter" "$HOME/workspace/Development/flutter"; do
    if [[ -n "$dir" && -x "$dir/bin/flutter" ]]; then
      export PATH="$dir/bin:$PATH"
      return 0
    fi
  done
  echo "Flutter not found: put it on the PATH or set FLUTTER_HOME" \
    "(git clone -b stable https://github.com/flutter/flutter.git ~/development/flutter)." >&2
  exit 1
}

SLOT=0
parse_slot() {
  if [[ "${1:-}" =~ ^[1-9]$ ]]; then
    SLOT="$1"
  fi
  SPRING_PORT=$((8080 + SLOT))
}

# Shows the booted simulator: Simulator.app up to Xcode 26, DeviceHub.app since Xcode 27. A window that doesn't open
# is no reason to stop, the simulator runs anyway.
open_simulator_app() {
  local xcode
  xcode="$(xcode-select -p)/.."
  open "$xcode/Applications/DeviceHub.app" 2>/dev/null ||
    open "$xcode/Developer/Applications/Simulator.app" 2>/dev/null ||
    open -a Simulator 2>/dev/null ||
    echo "(No simulator window: open it in Xcode, Window → Devices.)"
}

# Boots the iOS simulator named $1 (or the first available iPhone), unless one is running already.
boot_simulator() {
  local name="${1:-}" line udid
  if xcrun simctl list devices booted 2>/dev/null | grep -q "(Booted)"; then
    open_simulator_app
    return 0
  fi
  if [[ -n "$name" ]]; then
    line="$(xcrun simctl list devices available | grep -F -m1 "    $name (" || true)"
  else
    line="$(xcrun simctl list devices available | grep -m1 "    iPhone" || true)"
  fi
  udid="$(grep -oE '[0-9A-F]{8}-([0-9A-F]{4}-){3}[0-9A-F]{12}' <<<"$line" || true)"
  if [[ -z "$udid" ]]; then
    echo "No iOS simulator ${name:+named '$name' }available: install a runtime (xcodebuild -downloadPlatform iOS)." >&2
    exit 1
  fi
  echo "Booting simulator: $(sed -E 's/^ +//; s/ \(.*//' <<<"$line")"
  xcrun simctl boot "$udid"
  open_simulator_app
}

cmd_run() {
  local host=localhost device_given="" args=()
  for arg in "$@"; do
    case "$arg" in
      --android) host=10.0.2.2 ;;
      -d | --device-id | -d=* | --device-id=*) device_given=yes; args+=("$arg") ;;
      *) args+=("$arg") ;;
    esac
  done
  if ! curl -fsS -o /dev/null --max-time 3 "http://localhost:$SPRING_PORT/rsPublic/systemStatus"; then
    local start="bin/pfDev.sh run"
    ((SLOT == 0)) || start+=" $SLOT"
    echo "Warning: no server answers on :$SPRING_PORT (start it with: $start)." >&2
  fi
  if [[ -z "$device_given" && "$host" == localhost && "$(uname)" == Darwin ]]; then
    boot_simulator
    # Explicitly: an iPhone in reach (wireless debugging) is an iOS device as well, flutter would ask which one.
    local udid
    udid="$(xcrun simctl list devices booted | grep -m1 -oE '[0-9A-F]{8}-([0-9A-F]{4}-){3}[0-9A-F]{12}' || true)"
    [[ -z "$udid" ]] || args=(-d "$udid" ${args[@]+"${args[@]}"})
  fi
  cd "$APP" && exec flutter run --dart-define=PF_SERVER_URL="http://$host:$SPRING_PORT" ${args[@]+"${args[@]}"}
}

cmd="${1:-help}"
shift || true
parse_slot "${1:-}"
((SLOT == 0)) || shift

case "$cmd" in
  help | -h | --help)
    usage
    exit 0
    ;;
  sim)
    boot_simulator "${1:-}"
    exit 0
    ;;
  xcode)
    exec open "$APP/ios/Runner.xcworkspace"
    ;;
  gen)
    exec "$ROOT/bin/pfDev.sh" gen "$@"
    ;;
esac

find_flutter
cd "$APP"
case "$cmd" in
  run)
    cmd_run "$@"
    ;;
  devices)
    flutter devices
    echo
    xcrun simctl list devices available 2>/dev/null || true
    ;;
  check)
    dart format --output=none --set-exit-if-changed lib test
    flutter analyze
    exec flutter test
    ;;
  test)
    exec flutter test "$@"
    ;;
  format)
    exec dart format lib test
    ;;
  build-ios)
    if [[ "${1:-}" == --release ]]; then
      exec flutter build ipa "${@:2}"
    fi
    exec flutter build ios --simulator --debug "$@"
    ;;
  build-android)
    if [[ "${1:-}" == --release ]]; then
      exec flutter build apk --release "${@:2}"
    fi
    exec flutter build apk --debug "$@"
    ;;
  clean)
    flutter clean
    exec flutter pub get
    ;;
  deps)
    flutter pub get
    exec flutter pub outdated
    ;;
  doctor)
    exec flutter doctor -v
    ;;
  logs)
    exec flutter logs
    ;;
  *)
    echo "Unknown command: $cmd" >&2
    usage >&2
    exit 1
    ;;
esac
