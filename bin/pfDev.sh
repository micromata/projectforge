#!/usr/bin/env bash
#
# pfDev.sh — small dev helper for ProjectForge (Next.js branch).
#
# Dispatches common dev chores to the right toolchain: Gradle at the repo root
# and the npm scripts in projectforge-next/. Works from any working directory.
#
set -euo pipefail

# Repo root = parent of bin/, resolved from this script's own location.
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
NEXT="$ROOT/projectforge-next"
GRADLEW="$ROOT/gradlew"

usage() {
  cat <<'EOF'
pfDev.sh <command> [args…] — ProjectForge dev helper

Commands:
  gen              Prepare/generate: source headers, i18n sort, next message
                   catalogs + field metadata
                   (:projectforge-application:developmentMainForRelease)
  bootRun          Build (skip tests), then run the app in dev mode
  dev              next dev (projectforge-next)
  build            next build (projectforge-next)
  e2e [args…]      Playwright e2e tests; args are forwarded
                   (e.g. pfDev.sh e2e book-edit --headed)
  e2e:ui [args…]   Playwright e2e tests in UI mode
  check            Next quality gates: typecheck → lint → format:check
  help             Show this help

Env:
  PROJECTFORGE_HOME  Base dir for bootRun (default: ~/ProjectForge)
EOF
}

cmd="${1:-help}"
shift || true

case "$cmd" in
  gen)
    exec "$GRADLEW" :projectforge-application:developmentMainForRelease "$@"
    ;;
  bootRun)
    "$GRADLEW" build -x test
    JAVA_TOOL_OPTIONS="-XX:ReservedCodeCacheSize=256m -Dprojectforge.base.dir=${PROJECTFORGE_HOME:-$HOME/ProjectForge}" \
      exec "$GRADLEW" :projectforge-application:bootRun "$@"
    ;;
  dev)
    cd "$NEXT" && exec npm run dev "$@"
    ;;
  build)
    cd "$NEXT" && exec npm run build "$@"
    ;;
  e2e)
    cd "$NEXT" && exec npm run e2e -- "$@"
    ;;
  e2e:ui)
    cd "$NEXT" && exec npm run e2e:ui -- "$@"
    ;;
  check)
    cd "$NEXT" && npm run typecheck && npm run lint && exec npm run format:check
    ;;
  help | -h | --help)
    usage
    ;;
  *)
    echo "Unknown command: $cmd" >&2
    usage
    exit 1
    ;;
esac
