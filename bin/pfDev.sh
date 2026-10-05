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
pfDev.sh <command> [slot] [args…] — ProjectForge dev helper

Commands:
  gen              Prepare/generate: source headers, i18n sort, next message
                   catalogs + field metadata, changelog (site + next, from
                   changelog/changelog{,.de}.json), i18n key usage
                   (:projectforge-application:developmentMainForRelease)
  run [slot]       Build (skip tests), then run the app in dev mode
  setup <slot> [pw] Create a slot instance (see below); its first run fills the
                   database with test data (admin password pw, random if omitted)
  reset <slot>     Delete the home dir of a slot instance (asks first)
  dev [slot] […]   next dev (projectforge-next) on :3000+slot against :8080+slot;
                   -p <n> picks another port (pfDev.sh dev -p 3005)
  build            next build (projectforge-next)
  e2e [slot] […]   Playwright e2e tests; args are forwarded
                   (e.g. pfDev.sh e2e book-edit --headed). Against Spring, the
                   Next export is refreshed first (processResources, a no-op if
                   current); skipped with --dev / --port <n> (dev server)
  e2e:ui [slot] …  Playwright e2e tests in UI mode
  check            Next quality gates: typecheck → lint → format:check
  release <X.Y.Z> [--skip-tests]
                   Release X.Y.Z: checks changelog/changelog.json (the release on top,
                   tagged X.Y.Z-RELEASE; X.Y.0 also needs a news X.Y), sets the version,
                   runs gen and the build, commits and tags, then commits the next
                   X.Y.(Z+1)-SNAPSHOT. Nothing is pushed.
  publish <X.Y.Z>  Pushes the release and creates its GitHub release (notes generated
                   from the changelog, jar attached); asks before each step
  help             Show this help

Slots (1–9) run independent instances side by side, e.g. one per worktree:
  pfDev.sh setup 1 Creates the home ~/ProjectForge-1 from
                   ~/ProjectForge/projectforge.properties: own HSQLDB, mails and
                   gateway push off. Once per slot.
  pfDev.sh run 1   Spring on :8081 with that home and session cookie
                   JSESSIONID_1; the first run initializes the database with
                   test data and prints the admin login
  pfDev.sh dev 1   next dev on :3001, proxying to :8081
  pfDev.sh e2e 1 … e2e against :8081 (or :3001 with --dev), credentials from
                   ~/ProjectForge-1/testAccounts.txt
Without a slot everything is as before: :8080/:3000, ~/ProjectForge and its
configured database.

Env:
  PROJECTFORGE_HOME  Base dir of the main instance (default: ~/ProjectForge)
EOF
}

MAIN_HOME="${PROJECTFORGE_HOME:-$HOME/ProjectForge}"

# Consumes a leading slot number (1–9) from the args and derives ports, home dir and cookie name.
# Slot 0 (no number given) is the main instance with its unchanged defaults.
SLOT=0
parse_slot() {
  if [[ "${1:-}" =~ ^[1-9]$ ]]; then
    SLOT="$1"
  fi
  SPRING_PORT=$((8080 + SLOT))
  NEXT_PORT=$((3000 + SLOT))
  if ((SLOT == 0)); then
    PF_HOME="$MAIN_HOME"
    COOKIE=JSESSIONID
  else
    PF_HOME="$HOME/ProjectForge-$SLOT"
    COOKIE="JSESSIONID_$SLOT"
  fi
}

# Creates the home dir of a slot instance: the main instance's configuration with its own HSQLDB.
# The database, JCR and search index of the main instance are not copied — they belong to its DB.
init_slot_home() {
  local src="$MAIN_HOME"
  if [[ ! -f "$src/projectforge.properties" ]]; then
    echo "No $src/projectforge.properties to derive the slot instance from." >&2
    exit 1
  fi
  echo "Creating $PF_HOME from $src (own HSQLDB, mails and gateway push off)."
  mkdir -p "$PF_HOME"
  cp "$src/projectforge.properties" "$PF_HOME/"
  for item in config.xml resources styles; do
    # -c: APFS clone, instant and without extra space; plain copy where that isn't supported.
    [[ -e "$src/$item" ]] && { cp -cR "$src/$item" "$PF_HOME/" 2>/dev/null || cp -R "$src/$item" "$PF_HOME/"; }
  done
  cat >>"$PF_HOME/projectforge.properties" <<'EOF'

# ---------------------------------------------------------------------------------------------------
# Slot instance (bin/pfDev.sh): the settings below override the copied ones above (last one wins).
# Port, domain and session cookie name are passed by pfDev.sh as system properties.
# ---------------------------------------------------------------------------------------------------
spring.datasource.url=jdbc:hsqldb:file:${projectforge.base.dir}/database/projectforge;shutdown=true
spring.datasource.driver-class-name=org.hsqldb.jdbc.JDBCDriver
spring.datasource.username=sa
spring.datasource.password=
mail.session.pfmailsession.emailEnabled=false
projectforge.gateway.push.enabled=false
EOF
}

SETUP_PENDING=pfDev-setup.pending

# Waits for the slot instance and runs its initial setup with test data, the admin password taken from
# $SETUP_PENDING. Run in the background by `run`, so its output lands between the server's.
init_database() {
  local base="http://localhost:$SPRING_PORT/rsPublic/setup" status="" password result
  for _ in $(seq 1 300); do
    status="$(curl -sf "$base/status" || true)"
    [[ -n "$status" ]] && break
    sleep 2
  done
  if [[ -z "$status" ]]; then
    echo "pfDev.sh: no answer from $base/status, database not initialized." >&2
    return 1
  fi
  if [[ "$status" != *'"alreadyInitialized":true'* ]]; then
    password="$(<"$PF_HOME/$SETUP_PENDING")"
    echo "pfDev.sh: initializing the database with test data…"
    result="$(curl -sf -X POST "$base" -H 'Content-Type: application/json' \
      -d "{\"setupTarget\":\"TEST_DATA\",\"username\":\"admin\",\"password\":\"$password\",\"passwordRepeat\":\"$password\"}")"
    if [[ "$result" != *'"success":true'* ]]; then
      echo "pfDev.sh: setup failed: $result" >&2
      return 1
    fi
    echo
    echo "pfDev.sh: database initialized. Login: admin / $password — E2E accounts in $PF_HOME/testAccounts.txt"
    echo
  fi
  rm -f "$PF_HOME/$SETUP_PENDING"
}

require_slot() {
  if ((SLOT == 0)); then
    echo "$1 needs a slot number (1–9)." >&2
    exit 1
  fi
}

# Sets the version of all modules (the single source is gradle.properties).
set_version() {
  perl -pi -e "s/^version=.*/version=$1/" "$ROOT/gradle.properties"
}

confirm() {
  local answer
  read -r -p "$1 [y/N] " answer
  [[ "$answer" == [yY] ]]
}

RELEASE_USAGE="Usage: pfDev.sh release X.Y.Z [--skip-tests] / pfDev.sh publish X.Y.Z"

release() {
  local version="${1:-}" skip_tests=false
  [[ "${2:-}" == --skip-tests ]] && skip_tests=true
  if [[ ! "$version" =~ ^([0-9]+)\.([0-9]+)\.([0-9]+)$ ]]; then
    echo "$RELEASE_USAGE" >&2
    exit 1
  fi
  local next="${BASH_REMATCH[1]}.${BASH_REMATCH[2]}.$((10#${BASH_REMATCH[3]} + 1))-SNAPSHOT" tag="$version-RELEASE" start
  cd "$ROOT"
  if [[ -n "$(git status --porcelain)" ]]; then
    echo "The working tree isn't clean, commit or stash first." >&2
    exit 1
  fi
  if git rev-parse -q --verify "refs/tags/$tag" >/dev/null; then
    echo "The tag $tag exists already." >&2
    exit 1
  fi
  git fetch --quiet
  if git rev-parse -q --verify '@{upstream}' >/dev/null 2>&1 && (($(git rev-list --count 'HEAD..@{upstream}') > 0)); then
    echo "The branch is behind its upstream, pull first." >&2
    exit 1
  fi
  # Fails with every problem of the changelog, before anything is changed.
  "$GRADLEW" -p "$ROOT" :projectforge-application:checkReleaseChangelog -PreleaseVersion="$version"
  # Shown before the first question, so a release can't count backwards by mistake.
  local current latest
  current="$(sed -n 's/^version=//p' gradle.properties)"
  latest="$(git tag -l '[0-9]*-RELEASE' --sort=-v:refname | head -1)"
  echo "Current version: $current, latest release: ${latest:-none}"
  for older in "${current%-SNAPSHOT}" "${latest%-RELEASE}"; do
    if [[ -n "$older" && "$(printf '%s\n' "$older" "$version" | sort -V | tail -1)" != "$version" ]]; then
      echo "Warning: $version is lower than $older."
    fi
  done
  confirm "Release $version from $(git branch --show-current) (then $next)?" || exit 1
  start="$(git rev-parse --short HEAD)"
  trap 'echo "pfDev.sh release failed. To start over: git reset --hard $start && git tag -d $tag (if created)." >&2' ERR
  set_version "$version"
  "$GRADLEW" -p "$ROOT" :projectforge-application:developmentMainForRelease --rerun
  git add -A
  git commit -q -m "release: $version"
  git tag -a "$tag" -m "ProjectForge $version"
  # Built from the tagged commit, so the jar's build.properties show it (and not a dirty tree).
  if $skip_tests; then "$GRADLEW" -p "$ROOT" build -x test; else "$GRADLEW" -p "$ROOT" build; fi
  set_version "$next"
  git commit -q -m "chore: next development version $next" -- gradle.properties
  trap - ERR
  # Files touched by the build itself (e.g. an npm lock file) are not part of the release.
  if [[ -n "$(git status --porcelain)" ]]; then
    echo "Note: the build changed files that weren't committed:"
    git status --short
  fi
  echo "Released $version (tag $tag), now on $next. Nothing is pushed yet: pfDev.sh publish $version"
}

publish() {
  local version="${1:-}"
  if [[ ! "$version" =~ ^[0-9]+\.[0-9]+\.[0-9]+$ ]]; then
    echo "$RELEASE_USAGE" >&2
    exit 1
  fi
  local tag="$version-RELEASE" notes="$ROOT/build/release-notes-$version.md"
  local jar="$ROOT/projectforge-application/build/libs/projectforge-application-$version.jar"
  cd "$ROOT"
  if ! git rev-parse -q --verify "refs/tags/$tag" >/dev/null; then
    echo "There is no tag $tag, run pfDev.sh release $version first." >&2
    exit 1
  fi
  for file in "$notes" "$jar"; do
    if [[ ! -f "$file" ]]; then
      echo "$file is missing, it's built by pfDev.sh release $version." >&2
      exit 1
    fi
  done
  echo "----- $notes -----"
  cat "$notes"
  echo "-----"
  confirm "Push $(git branch --show-current) and the tag $tag?" || exit 1
  git push --follow-tags
  # gh would create a missing tag on the default branch, so the pushed tag is checked first.
  if ! git ls-remote --exit-code --tags origin "refs/tags/$tag" >/dev/null; then
    echo "The tag $tag isn't on origin." >&2
    exit 1
  fi
  confirm "Create the GitHub release $tag with $(basename "$jar")?" || exit 1
  gh release create "$tag" "$jar" --title "ProjectForge $version" --notes-file "$notes" --latest
}

cmd="${1:-help}"
shift || true
parse_slot "${1:-}"
((SLOT == 0)) || shift

case "$cmd" in
  gen)
    exec "$GRADLEW" -p "$ROOT" :projectforge-application:developmentMainForRelease "$@"
    ;;
  run)
    opts="-XX:ReservedCodeCacheSize=256m -Dprojectforge.base.dir=$PF_HOME"
    if ((SLOT > 0)); then
      if [[ ! -d "$PF_HOME" ]]; then
        echo "Slot $SLOT isn't set up yet, run \`pfDev.sh setup $SLOT\` first." >&2
        exit 1
      fi
      if lsof -nP -iTCP:"$SPRING_PORT" -sTCP:LISTEN >/dev/null 2>&1; then
        echo "Slot $SLOT is already running (:$SPRING_PORT)." >&2
        exit 1
      fi
      # The main instance keeps whatever its projectforge.properties says.
      opts+=" -Dserver.port=$SPRING_PORT -Dprojectforge.domain=http://localhost:$SPRING_PORT -Dserver.servlet.session.cookie.name=$COOKIE"
    fi
    "$GRADLEW" -p "$ROOT" build -x test
    # First start after `setup`: fill the fresh database as soon as the instance answers.
    [[ -f "$PF_HOME/$SETUP_PENDING" ]] && init_database &
    JAVA_TOOL_OPTIONS="$opts" \
      exec "$GRADLEW" -p "$ROOT" :projectforge-application:bootRun "$@"
    ;;
  setup)
    require_slot setup
    if [[ -d "$PF_HOME" ]]; then
      echo "$PF_HOME already exists; \`pfDev.sh reset $SLOT\` first to start over." >&2
      exit 1
    fi
    init_slot_home
    # Read by the first `run`, which initializes the database with it (the setup needs a running instance).
    (umask 077 && echo "${1:-Pf-$(openssl rand -hex 8)-1x}" >"$PF_HOME/$SETUP_PENDING")
    echo "Done. Start it with \`pfDev.sh run $SLOT\` (:$SPRING_PORT); the first start fills the database with test data."
    ;;
  reset)
    require_slot reset
    if [[ ! -d "$PF_HOME" ]]; then
      echo "$PF_HOME doesn't exist."
      exit 0
    fi
    if lsof -nP -iTCP:"$SPRING_PORT" -sTCP:LISTEN >/dev/null 2>&1; then
      echo "Something is listening on :$SPRING_PORT — stop the instance first." >&2
      exit 1
    fi
    read -r -p "Delete $PF_HOME (database, index, logs)? [y/N] " answer
    [[ "$answer" == [yY] ]] && rm -rf "$PF_HOME" && echo "Deleted."
    ;;
  dev)
    # An own -p/--port replaces the slot's port: pfDev.sh dev -p 3005 → :3005 against :8080.
    port_args=(-p "$NEXT_PORT")
    for a in "$@"; do [[ "$a" == -p || "$a" == --port ]] && port_args=(); done
    cd "$NEXT" && PF_BACKEND_URL="http://localhost:$SPRING_PORT" exec npm run dev -- ${port_args[@]+"${port_args[@]}"} "$@"
    ;;
  build)
    cd "$NEXT" && exec npm run build "$@"
    ;;
  e2e)
    export PROJECTFORGE_HOME="$PF_HOME"
    # A slot's dev server: --dev means its port, not :3000.
    args=()
    for arg in "$@"; do
      if [[ "$arg" == "--dev" ]] && ((SLOT > 0)); then args+=(--port "$NEXT_PORT"); else args+=("$arg"); fi
    done
    set -- ${args[@]+"${args[@]}"}
    # Spring serves the export from build/resources, so bring it up to date first. A dev server
    # compiles the working tree itself and needs nothing.
    case " $* " in
      *" --dev "* | *" --port "* | *" --port="*) ;;
      *)
        "$GRADLEW" -p "$ROOT" :projectforge-application:processResources
        ((SLOT == 0)) || export E2E_BASE_URL="http://localhost:$SPRING_PORT"
        ;;
    esac
    cd "$NEXT" && exec npm run e2e -- "$@"
    ;;
  e2e:ui)
    export PROJECTFORGE_HOME="$PF_HOME"
    ((SLOT == 0)) || export E2E_BASE_URL="http://localhost:$SPRING_PORT"
    cd "$NEXT" && exec npm run e2e:ui -- "$@"
    ;;
  release)
    release "$@"
    ;;
  publish)
    publish "$@"
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
