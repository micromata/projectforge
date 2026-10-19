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
                   (:projectforge-application:developmentMainForRelease).
                   Run it after a merge or rebase: generated files are merged
                   without conflicts (own side kept, see git-setup)
  git-setup        Registers the git merge driver pf-generated of .gitattributes
                   (keeps the own side of generated files, no conflicts); done by
                   every pfDev.sh call, once for all worktrees
  build            next build (projectforge-next)
  check            Next quality gates: typecheck → lint → format:check
  changelog-fold [id]
                   Moves the entries of changelog/unreleased/ (one file per change,
                   English and German) into the release id of changelog/changelog{,.de}.json,
                   by default the newest one, and regenerates the changelog. Done by
                   release itself, by hand only for a snapshot entry
  release <X.Y.Z> [--skip-tests]
                   Release X.Y.Z: checks changelog/changelog.json (the release on top,
                   tagged X.Y.Z-RELEASE, its sections may come from changelog/unreleased/
                   only; its summary and overview, en+de, are in the release entry or in
                   changelog/unreleased/release.json). A missing release entry is added
                   (default titles, today, published or a mini release, asked first).
                   Runs gen first: generated files out of date are committed after
                   asking (otherwise the tests would fail on them late). Runs the tests (unless --skip-tests), before anything is changed;
                   then folds changelog/unreleased/ into the entry, sets the version,
                   runs gen and a clean build, commits and tags, then commits the next
                   X.Y.(Z+1)-SNAPSHOT. Nothing is pushed.
  publish <X.Y.Z>  Pushes the release and its tag and creates its GitHub release (notes
                   generated from the changelog, the jar attached if wanted, e.g. not for
                   minor releases); asks before each step. The docker
                   images follow with docker/push-release.sh (see its --help), the
                   upload of the jar to SourceForge by hand (rsync command printed at
                   the end, then set as default download, i.e. latest). A release
                   with "published": false in the changelog is only pushed (mini release,
                   no GitHub release, jars or docker images); its changes go into the
                   notes of the next published release
  help             Show this help

Instances (slots):
Slots 1–9 run independent instances side by side, e.g. one per worktree;
slot n uses :8080+n (Spring), :3000+n (next dev), the home ~/ProjectForge-n
with its own HSQLDB and the session cookie JSESSIONID_n. Without a slot
everything is as before: :8080/:3000, ~/ProjectForge and its configured
database.
  setup <slot> [pw] Create the home ~/ProjectForge-n from
                   ~/ProjectForge/projectforge.properties: own HSQLDB, mails and
                   gateway push off, number ranges 4/5/1 (as in the tests). Once per slot. Its first run fills the
                   database with test data (admin password pw, random if omitted)
  reset <slot>     Delete the home dir of a slot instance (asks first)
  run [slot]       Build (skip tests), then run the app in dev mode (Spring on
                   :8080+slot); the first run after setup initializes the
                   database with test data and prints the admin login
  dev [slot] […]   next dev (projectforge-next) on :3000+slot against :8080+slot;
                   -p <n> picks another port (pfDev.sh dev -p 3005)
  e2e [slot] […]   Playwright e2e tests against :8080+slot (or :3000+slot with
                   --dev), credentials from the slot's testAccounts.txt; args are
                   forwarded (e.g. pfDev.sh e2e 1 book-edit --headed). Against
                   Spring, the Next export is refreshed first (processResources, a
                   no-op if current); skipped with --dev / --port <n> (dev server)
  e2e:ui [slot] …  Playwright e2e tests in UI mode
  ps               List the running servers of all slots (:8080–:8089 and
                   :3000–:3009) with PID and working dir (i.e. worktree)
  stop [slot]      Stop the Spring (:8080+slot) and the next dev server
                   (:3000+slot) of a slot

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
# Number ranges of customers and projects (as in application-test.properties): the test data's customers
# 300-304 belong to range 5, so their projects get Kost2. Without ranges a fresh database has free numbers
# and no Kost2 (see NumberRangeService).
projectforge.fibu.number-ranges.allow-free-numbers=true
projectforge.fibu.number-ranges.ranges[0].prefix=4
projectforge.fibu.number-ranges.ranges[0].name=Intern
projectforge.fibu.number-ranges.ranges[0].project-digits=5
projectforge.fibu.number-ranges.ranges[1].prefix=5
projectforge.fibu.number-ranges.ranges[1].customer-min=0
projectforge.fibu.number-ranges.ranges[1].customer-max=999
projectforge.fibu.number-ranges.ranges[1].project-digits=2
projectforge.fibu.number-ranges.ranges[2].prefix=1
projectforge.fibu.number-ranges.ranges[2].customer-min=10000
projectforge.fibu.number-ranges.ranges[2].customer-max=19999
projectforge.fibu.number-ranges.ranges[2].project-digits=1
projectforge.fibu.number-ranges.ranges[2].segments=1,4,1,2
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

# PID of the process listening on TCP port $1, empty if none.
listener_pid() {
  lsof -nP -t -iTCP:"$1" -sTCP:LISTEN 2>/dev/null | head -1 || true
}

# Working dir of process $1 (shows the worktree a server runs from), ~ for $HOME.
process_cwd() {
  local dir
  dir="$(lsof -a -p "$1" -d cwd -Fn 2>/dev/null | sed -n 's/^n//p' || true)"
  echo "${dir/#$HOME/~}"
}

list_servers() {
  local slot port pid kind listeners lines=()
  # One lsof call for all ports ("port pid" per line): each call takes a noticeable fraction of a second.
  listeners="$(lsof -nP -iTCP -sTCP:LISTEN -Fpn 2>/dev/null |
    awk '/^p/ { pid = substr($0, 2) } /^n/ { sub(/.*:/, ""); print $0, pid }' || true)"
  for slot in 0 1 2 3 4 5 6 7 8 9; do
    for port in $((8080 + slot)) $((3000 + slot)); do
      pid="$(awk -v port="$port" '$1 == port { print $2; exit }' <<<"$listeners")"
      [[ -n "$pid" ]] || continue
      ((port >= 8080)) && kind=spring || kind=next
      lines+=("$(printf '%-5s %-7s %-7s %-7s %s' "$slot" ":$port" "$kind" "$pid" "$(process_cwd "$pid")")")
    done
  done
  if ((${#lines[@]} == 0)); then
    echo "No servers running."
    return
  fi
  printf '%-5s %-7s %-7s %-7s %s\n' SLOT PORT KIND PID DIR
  printf '%s\n' "${lines[@]}"
}

# Stops the server listening on port $1: TERM first, KILL if it still listens after 30 s.
stop_port() {
  local port="$1" pid parent pids=()
  pid="$(listener_pid "$port")"
  if [[ -z "$pid" ]]; then
    echo "Nothing listens on :$port."
    return
  fi
  pids=("$pid")
  # next dev: the listening server is a child of `next dev` and npm (node processes), which would outlive it.
  # The forked JVM of bootRun has the Gradle daemon as parent, which keeps running.
  parent="$(ps -o ppid= -p "$pid" | tr -d ' ')"
  while [[ -n "$parent" ]] && ((parent > 1)) && [[ "$(ps -o comm= -p "$parent")" =~ (^|/)(node|npm)$ ]]; do
    pids+=("$parent")
    parent="$(ps -o ppid= -p "$parent" | tr -d ' ')"
  done
  echo "Stopping :$port (PID ${pids[*]})…"
  kill "${pids[@]}" 2>/dev/null || true
  for _ in $(seq 1 60); do
    [[ -z "$(listener_pid "$port")" ]] && { echo "Stopped :$port."; return; }
    sleep 0.5
  done
  echo "Still listening on :$port, killing it."
  kill -9 "${pids[@]}" 2>/dev/null || true
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
  # The generated files must be up to date, otherwise the tests fail on them, late in the release. Changes are
  # committed on their own (before the release commit), e.g. a "Not yet released" entry dated today.
  "$GRADLEW" -p "$ROOT" :projectforge-application:developmentMainForRelease --rerun
  if [[ -n "$(git status --porcelain)" ]]; then
    git status --short
    if ! confirm "gen changed the files above. Commit them (chore: regenerate generated files) and go on?"; then
      echo "Commit or revert them, then start the release again." >&2
      exit 1
    fi
    git add -A
    git commit -q -m "chore: regenerate generated files"
  fi
  # A missing release entry is added after the confirmation below (default titles, dated today), only the
  # kind of release is asked here. An existing entry (e.g. with a title of its own) is taken as it is.
  local published="" has_entry
  has_entry="$(node -e '
    const releases = require(process.argv[1]).releases;
    console.log(releases.some((r) => r.version === process.argv[2]) ? "true" : "false");
  ' "$ROOT/changelog/changelog.json" "$version")"
  if [[ "$has_entry" == false ]]; then
    if confirm "changelog.json has no entry of $version yet, it will be added. A full release, to be published later with \"pfDev.sh publish\" (GitHub release, jar, docker images)? Otherwise a mini release (only pushed)"; then
      published=-PreleasePublished=true
    else
      published=-PreleasePublished=false
    fi
  fi
  # Fails with every problem of the changelog, before anything is changed.
  "$GRADLEW" -p "$ROOT" :projectforge-application:checkReleaseChangelog -PreleaseVersion="$version" ${published:+"$published"}
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
  # The tests run first, on the unchanged tree: a failure stops the release before anything is changed.
  if ! $skip_tests; then "$GRADLEW" -p "$ROOT" clean test; fi
  start="$(git rev-parse --short HEAD)"
  trap 'echo "pfDev.sh release failed. To start over: git reset --hard $start && git tag -d $tag (if created)." >&2' ERR
  # The entries of changelog/unreleased/ go into the release (checked folded already), its entry is added if missing.
  if [[ -n "$published" ]]; then
    "$GRADLEW" -p "$ROOT" :projectforge-application:foldChangelog --rerun -PreleaseVersion="$version" "$published"
  else
    "$GRADLEW" -p "$ROOT" :projectforge-application:foldChangelog --rerun
  fi
  set_version "$version"
  "$GRADLEW" -p "$ROOT" :projectforge-application:developmentMainForRelease --rerun
  git add -A
  git commit -q -m "release: $version"
  # Built from the release commit, so the jar's build.properties show it (and not a dirty tree). A clean build,
  # so no leftovers of earlier builds end up in the release jar. Tagged only after a successful build. The tests
  # ran before the release commit, which only changes the changelog, the version and generated files.
  "$GRADLEW" -p "$ROOT" clean build -x test
  git tag -a "$tag" -m "ProjectForge $version"
  set_version "$next"
  # The jar contains the changelog, so the checksum of a published release's jar follows after the tag.
  local changelog_files=()
  if [[ "$(release_published "$version")" == true ]]; then
    "$GRADLEW" -p "$ROOT" :projectforge-application:releaseSha256Changelog --rerun -PreleaseVersion="$version"
    changelog_files=(changelog/changelog.json site/_changelogs projectforge-next/lib/generated/changelog.json
      projectforge-next/lib/generated/changelog.de.json)
  fi
  git commit -q -m "chore: next development version $next" -- gradle.properties ${changelog_files[@]+"${changelog_files[@]}"}
  trap - ERR
  # Files touched by the build itself (e.g. an npm lock file) are not part of the release.
  if [[ -n "$(git status --porcelain)" ]]; then
    echo "Note: the build changed files that weren't committed:"
    git status --short
  fi
  echo "Released $version (tag $tag), now on $next. Nothing is pushed yet: pfDev.sh publish $version"
}

# Whether the release <version> of changelog.json is published (GitHub release, jar, docker images): true or false.
release_published() {
  node -e '
    const release = require(process.argv[1]).releases.find((r) => r.version === process.argv[2]);
    console.log(release?.published === false ? "false" : "true");
  ' "$ROOT/changelog/changelog.json" "$1"
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
  # A mini release ("published": false) is only pushed, its notes are part of the next published release.
  if [[ "$(release_published "$version")" == false ]]; then
    confirm "Push $(git branch --show-current) and the tag $tag (unpublished: no GitHub release, jar or docker images)?" || exit 1
    git push --follow-tags
    echo "Pushed $tag. Its changes are part of the release notes of the next published release."
    return
  fi
  if [[ ! -f "$notes" ]]; then
    echo "$notes is missing, it's written by pfDev.sh release $version." >&2
    exit 1
  fi
  # The checksum is in the changelog since pfDev.sh release. A local jar must be the one of the release.
  local sha256="" expected notes_with_sha
  expected="$(node -e '
    const release = require(process.argv[1]).releases.find((r) => r.version === process.argv[2]);
    console.log(release?.sha256 ?? "");
  ' "$ROOT/changelog/changelog.json" "$version")"
  if [[ -f "$jar" ]]; then
    sha256="$(shasum -a 256 "$jar" | cut -d' ' -f1)"
    if [[ -z "$expected" ]]; then
      echo "Note: the changelog has no SHA-256 of $version yet, add \"sha256\": \"$sha256\" to its entry."
    elif [[ "$sha256" != "$expected" ]]; then
      echo "The SHA-256 $sha256 of $jar isn't the one of the changelog ($expected), it was rebuilt after the release." >&2
      exit 1
    fi
  else
    echo "Note: $jar is missing (built by pfDev.sh release $version), it can't be attached to the GitHub release."
    sha256="$expected"
  fi
  if [[ -n "$sha256" ]]; then
    notes_with_sha="$ROOT/build/release-notes-$version-sha256.md"
    { cat "$notes"; echo; echo "SHA-256 of $(basename "$jar"): \`$sha256\`"; } >"$notes_with_sha"
    notes="$notes_with_sha"
  fi
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
  if gh release view "$tag" >/dev/null 2>&1; then
    echo "The GitHub release $tag exists already."
  elif confirm "Create the GitHub release $tag (notes from the changelog)?"; then
    # Jars of minor releases may stay off GitHub (space), SourceForge has them. Without a jar, the release has no
    # Jar (GitHub) pill on the website.
    local assets=()
    if [[ -f "$jar" ]] && confirm "Attach $(basename "$jar") to the GitHub release?"; then
      assets=("$jar")
    fi
    # Only the newest release is the latest one, e.g. not 9.0.2 published after 9.0.3.
    local latest=--latest=false
    [[ "$(git tag -l '[0-9]*-RELEASE' --sort=-v:refname | head -1)" == "$tag" ]] && latest=--latest
    gh release create "$tag" ${assets[@]+"${assets[@]}"} --title "ProjectForge $version" --notes-file "$notes" "$latest"
  fi
  echo "Next, the docker images: docker/push-release.sh $version arch (on an arm64 and an amd64 machine)"
  # The upload to SourceForge is done by hand; afterwards the jar is set as default download there (files/latest).
  echo "SourceForge: upload with: rsync -e ssh --progress $jar <user>@frs.sourceforge.net:/home/frs/project/pforge/ProjectForge/$version/"
  echo "  and make it the default download (info icon of the file, \"Default Download For\": all platforms)."
}

# The merge driver of .gitattributes for generated files: keeps the own side (%A) instead of a conflict, the file is
# regenerated by gen afterwards. Without it git merges them as text (with conflicts). .git/config is shared by all
# worktrees.
ensure_git_merge_driver() {
  git -C "$ROOT" config --get merge.pf-generated.driver >/dev/null 2>&1 && return
  git -C "$ROOT" config merge.pf-generated.name "generated file, regenerate with bin/pfDev.sh gen" || return 0
  git -C "$ROOT" config merge.pf-generated.driver true || return 0
}

ensure_git_merge_driver

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
  ps)
    list_servers
    ;;
  stop)
    stop_port "$SPRING_PORT"
    stop_port "$NEXT_PORT"
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
  changelog-fold)
    exec "$GRADLEW" -p "$ROOT" :projectforge-application:foldChangelog ${1:+-PreleaseId="$1"}
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
  git-setup)
    echo "Merge driver pf-generated: $(git -C "$ROOT" config --get merge.pf-generated.driver)"
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
