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
APP="$ROOT/projectforge-app"
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
  bootjar [<plugin jar>[,<plugin jar>…]]
                   Test build of the boot jar (tests skipped), prints its path. Without
                   an argument the plugins pinned in plugins-bin/ are downloaded; a jar
                   of a plugin given here (e.g. one built in the repository of an
                   external plugin: bin/pf.sh bootjar there) replaces its pin and needs no
                   GitHub release (also PF_EXTRA_PLUGINS)
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
                   X.Y.(Z+1)-SNAPSHOT. Finally asks whether to run publish (push +
                   GitHub release); declined, nothing is pushed.
                   External plugins (checkouts in PF_EXTERNAL_PLUGINS, comma separated,
                   or one per line in ~/.config/projectforge/external-plugins; needed
                   for a published release): tested, built into the jar, pinned in
                   plugins-bin/ (url + sha256, in the release commit; the jar is attached by
                   publish to the GitHub release plugin-<name>-X.Y.Z, so a build from the
                   sources downloads it), their submodule pinned and tagged X.Y.Z-RELEASE
                   as well; publish pushes them after this repository
  publish <X.Y.Z>  Pushes the release and its tag and creates its GitHub release (notes
                   generated from the changelog, the jar attached if wanted, e.g. not for
                   minor releases); asks before each step. The docker
                   images follow with docker/push-release.sh (see its --help), the
                   upload of the jar to SourceForge by hand (rsync command printed at
                   the end, then set as default download, i.e. latest). A release
                   with "published": false in the changelog (mini release) gets a GitHub
                   release with its own notes, but no jar or docker images, never the
                   latest one; its changes also go into the notes of the next published
                   release
  github-release <X.Y.Z>...
                   Creates the missing GitHub releases of releases tagged and pushed
                   already (e.g. older mini releases), oldest first; nothing is pushed
  help             Show this help

Instances (slots):
Slots 1–9 run independent instances side by side, e.g. one per worktree;
slot n uses :8080+n (Spring), :3000+n (next dev), the home ~/ProjectForge-n
with its own HSQLDB and the session cookie JSESSIONID_n. Without a slot
everything is as before: :8080/:3000, ~/ProjectForge and its configured
database.
  setup <slot> [pw] Create the home ~/ProjectForge-n from
                   ~/ProjectForge/projectforge.properties: own HSQLDB, mails,
                   gateway push and Sipgate off, number ranges 4/5/1 (as in the tests). Once per slot. Its first run fills the
                   database with test data (admin password pw, random if omitted)
  reset <slot>     Delete the home dir of a slot instance (asks first)
  renew <slot> [-y] [pw]
                   Stop the Spring of a slot, delete its home, set it up again
                   and start it (stop + reset + setup + run); asks first, -y
                   skips the question. The next dev server keeps running
  run [slot]       Build (skip tests), then run the app in dev mode (Spring on
                   :8080+slot); the first run after setup initializes the
                   database with test data and prints the admin login. With
                   PF_HOTSWAP_AGENT set: hot code replacement (see Env)
  reload [slot]    Hot code replacement (PF_HOTSWAP_AGENT): compiles the
                   worktree (classes only) and reloads the classes changed since
                   the last reload into the running instance (jdb, debug port
                   :5005+slot; not while IntelliJ is attached)
  dev [slot] […]   next dev (projectforge-next) on :3000+slot against :8080+slot;
                   -p <n> picks another port (pfDev.sh dev -p 3005)
  e2e [slot] […]   Playwright e2e tests against :8080+slot (or :3000+slot with
                   --dev), credentials from the slot's testAccounts.txt; args are
                   forwarded (e.g. pfDev.sh e2e 1 book-edit --headed). Against
                   Spring, the Next export is refreshed first (processResources, a
                   no-op if current); skipped with --dev / --port <n> (dev server)
  e2e:ui [slot] …  Playwright e2e tests in UI mode
  app [slot] […]   The app (projectforge-app, React Native) against :8080+slot, same as
                   projectforge-app/bin/pfApp.sh run [slot] […] — that script has the
                   other app helpers (sim, metro, proxy, build-ios, check, …)
  app-android [slot] […]
                   The app in the Android emulator (booted if none runs) against
                   :8080+slot, reached there as 10.0.2.2: pfApp.sh run [slot] --android
  app-check        App quality gates (pfApp.sh check)
  ps               List the running servers of all slots (:8080–:8089 and
                   :3000–:3009) with PID and working dir (i.e. worktree)
  stop [slot]      Stop the Spring (:8080+slot) and the next dev server
                   (:3000+slot) of a slot

Env:
  PROJECTFORGE_HOME  Base dir of the main instance (default: ~/ProjectForge)
  PF_HOTSWAP_AGENT   HotswapAgent jar: `run` starts the app with it on a JetBrains
                     Runtime and a debug port :5005+slot; reload changed classes
                     with `pfDev.sh reload`, or attach IntelliJ (Remote JVM Debug)
  PF_HOTSWAP_JDK     Home of that JetBrains Runtime (default: IntelliJ's own,
                     /Applications/IntelliJ IDEA.app/Contents/jbr/Contents/Home)
  PF_HOTSWAP_AGENT_OPTS
                     Options of the agent (default disablePlugin=Spring: its bean
                     proxies break the start; "" turns it on)
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
# The database and search index of the main instance are not copied — they belong to its DB.
init_slot_home() {
  local src="$MAIN_HOME"
  if [[ ! -f "$src/projectforge.properties" ]]; then
    echo "No $src/projectforge.properties to derive the slot instance from." >&2
    exit 1
  fi
  echo "Creating $PF_HOME from $src (own HSQLDB, mails, gateway push and Sipgate off)."
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
# Sipgate off: the copied token would sync every address change with the real Sipgate account, importing its
# contacts into this test database (blank token = not configured, see SipgateConfiguration.isConfigured).
projectforge.sipgate.token=
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

# Like confirm, but Enter means yes.
confirm_yes() {
  local answer
  read -r -p "$1 [Y/n] " answer
  [[ -z "$answer" || "$answer" == [yY] ]]
}

# External plugins (dev-docs/external-plugins.md): checkouts of the repositories of plugins built outside of this one,
# whose jars go into the release jar. Comma separated in PF_EXTERNAL_PLUGINS, otherwise one per line in
# EXTERNAL_PLUGINS_FILE. Each one builds with "./gradlew -PpfCoreDir=<this checkout> :plugin:<task>" into
# plugin/build/libs/<name>-<version>.jar and has this repository as submodule projectforge/ (pinned on release).
EXTERNAL_PLUGINS_FILE="$HOME/.config/projectforge/external-plugins"

external_plugins() {
  local list="${PF_EXTERNAL_PLUGINS:-}" dir
  if [[ -z "$list" && -f "$EXTERNAL_PLUGINS_FILE" ]]; then
    list="$(grep -Ev '^[[:space:]]*(#|$)' "$EXTERNAL_PLUGINS_FILE" | paste -sd, -)"
  fi
  local dirs=()
  IFS=',' read -r -a dirs <<< "$list"
  for dir in ${dirs[@]+"${dirs[@]}"}; do
    dir="$(echo "$dir" | xargs)"
    [[ -n "$dir" ]] && echo "${dir/#\~/$HOME}"
  done
  return 0
}

# Fails, if a checkout of an external plugin isn't ready for the release tag <tag>.
check_external_plugins() {
  local tag="$1" dir
  while read -r dir; do
    if [[ ! -x "$dir/gradlew" ]]; then
      echo "External plugin: no gradle project at $dir." >&2
      exit 1
    fi
    if [[ -n "$(git -C "$dir" status --porcelain)" ]]; then
      echo "External plugin $dir: the working tree isn't clean." >&2
      exit 1
    fi
    if git -C "$dir" rev-parse -q --verify "refs/tags/$tag" >/dev/null; then
      echo "External plugin $dir: the tag $tag exists already." >&2
      exit 1
    fi
    git -C "$dir" fetch --quiet
    if git -C "$dir" rev-parse -q --verify '@{upstream}' >/dev/null 2>&1 &&
      (($(git -C "$dir" rev-list --count 'HEAD..@{upstream}') > 0)); then
      echo "External plugin $dir: the branch is behind its upstream, pull first." >&2
      exit 1
    fi
  done < <(external_plugins)
}

# Runs ":plugin:<task>" of every external plugin against this checkout.
run_external_plugins() {
  local task="$1" dir
  while read -r dir; do
    (cd "$dir" && ./gradlew -PpfCoreDir="$ROOT" ":plugin:$task")
  done < <(external_plugins)
}

# Builds the jars of the external plugins against this checkout; prints them comma separated (pf.extraPlugins).
build_external_plugins() {
  local version="$1" dir jars=() found
  while read -r dir; do
    (cd "$dir" && ./gradlew -q -PpfCoreDir="$ROOT" :plugin:jar) >&2
    found=("$dir"/plugin/build/libs/*-"$version".jar)
    if [[ ${#found[@]} -ne 1 || ! -f "${found[0]}" ]]; then
      echo "External plugin $dir: expected one jar plugin/build/libs/*-$version.jar." >&2
      exit 1
    fi
    jars+=("${found[0]}")
  done < <(external_plugins)
  (IFS=,; echo "${jars[*]+"${jars[*]}"}")
}

# Fails, if the release jar lacks one of the jars <jars> (comma separated).
check_release_jar_plugins() {
  local version="$1" jars="$2" jar name
  local app="$ROOT/projectforge-application/build/libs/projectforge-application-$version.jar"
  IFS=',' read -r -a list <<< "$jars"
  for jar in ${list[@]+"${list[@]}"}; do
    name="$(basename "$jar")"
    if ! unzip -l "$app" "BOOT-INF/lib/$name" >/dev/null 2>&1; then
      echo "The release jar lacks the external plugin BOOT-INF/lib/$name." >&2
      exit 1
    fi
  done
}

# Name of the plugin of the jar <jar> built for <version>, e.g. cveradar (projectforge-plugin-cveradar-X.Y.Z.jar).
plugin_name_of_jar() {
  local name
  name="$(basename "$1" .jar)"
  name="${name%-$2}"
  echo "${name#projectforge-plugin-}"
}

# Writes plugins-bin/<name>.properties (version, url of the GitHub release asset, sha256) for every jar <jars>
# (comma separated), see downloadPinnedPlugins in projectforge-application/build.gradle.kts. publish uploads the jars.
pin_external_plugins() {
  local version="$1" jars="$2" jar name sha256 repo_url
  [[ -z "$jars" ]] && return 0
  repo_url="$(gh repo view --json url --jq .url)"
  mkdir -p "$ROOT/plugins-bin"
  IFS=',' read -r -a list <<< "$jars"
  for jar in ${list[@]+"${list[@]}"}; do
    name="$(plugin_name_of_jar "$jar" "$version")"
    sha256="$(shasum -a 256 "$jar" | cut -d' ' -f1)"
    printf 'version=%s\nurl=%s/releases/download/plugin-%s-%s/%s\nsha256=%s\n' \
      "$version" "$repo_url" "$name" "$version" "$(basename "$jar")" "$sha256" >"$ROOT/plugins-bin/$name.properties"
  done
}

# Creates the GitHub releases plugin-<name>-<version> with the jars pinned for <version> in plugins-bin/ as assets
# (not the latest release), after the push of the tag. Skipped if the release exists; the jar must be the pinned one.
publish_external_plugin_assets() {
  local version="$1" tag="$1-RELEASE" pin name jar pinned sha256 asset_tag dir found
  for pin in "$ROOT"/plugins-bin/*.properties; do
    [[ -f "$pin" ]] || continue
    [[ "$(sed -n 's/^version=//p' "$pin")" == "$version" ]] || continue
    name="$(basename "$pin" .properties)"
    asset_tag="plugin-$name-$version"
    if gh release view "$asset_tag" >/dev/null 2>&1; then
      echo "The GitHub release $asset_tag exists already."
      continue
    fi
    jar=""
    while read -r dir; do
      found=("$dir"/plugin/build/libs/projectforge-plugin-"$name"-"$version".jar)
      [[ -f "${found[0]}" ]] && jar="${found[0]}"
    done < <(external_plugins)
    if [[ -z "$jar" ]]; then
      echo "The jar of the plugin $name $version isn't built here (external plugin checkout missing), upload it to $asset_tag by hand." >&2
      exit 1
    fi
    pinned="$(sed -n 's/^sha256=//p' "$pin")"
    sha256="$(shasum -a 256 "$jar" | cut -d' ' -f1)"
    if [[ "$sha256" != "$pinned" ]]; then
      echo "The SHA-256 $sha256 of $jar isn't the pinned one ($pinned), it was rebuilt after the release." >&2
      exit 1
    fi
    confirm_yes "Create the GitHub release $asset_tag with $(basename "$jar") (the pinned plugin of $version)?" || continue
    gh release create "$asset_tag" "$jar" --target "$(git rev-parse "$tag^{commit}")" --latest=false \
      --title "Plugin $name $version" \
      --notes "Binary of the plugin $name for ProjectForge $version, pinned in plugins-bin/$name.properties. Do not delete."
  done
}

# Pins the submodule projectforge/ of every external plugin to the release tag (commit "release: X.Y.Z") and tags
# the plugin with the same tag.
tag_external_plugins() {
  local version="$1" tag="$1-RELEASE" dir
  while read -r dir; do
    if [[ -e "$dir/projectforge/.git" ]]; then
      git -C "$dir/projectforge" fetch -q "$ROOT" "refs/tags/$tag:refs/tags/$tag"
      git -C "$dir/projectforge" checkout -q "$tag"
      git -C "$dir" add projectforge
      git -C "$dir" diff --cached --quiet || git -C "$dir" commit -q -m "release: $version"
    fi
    git -C "$dir" tag -a "$tag" -m "ProjectForge $version"
  done < <(external_plugins)
}

# Pushes the external plugins with the release tag, after this repository (their submodule points to its tag).
push_external_plugins() {
  local tag="$1-RELEASE" dir
  while read -r dir; do
    if ! git -C "$dir" rev-parse -q --verify "refs/tags/$tag" >/dev/null; then
      echo "External plugin $dir: no tag $tag, not pushed." >&2
      continue
    fi
    if confirm_yes "Push the external plugin $dir and its tag $tag?"; then
      git -C "$dir" push --follow-tags
    fi
  done < <(external_plugins)
}

RELEASE_USAGE="Usage: pfDev.sh release X.Y.Z [--skip-tests] / pfDev.sh publish X.Y.Z / pfDev.sh github-release X.Y.Z..."

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
  check_external_plugins "$tag"
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
  # A published release ships the jar, which must contain the external plugins (e.g. the CVE-Clinic).
  local is_published
  if [[ -n "$published" ]]; then
    [[ "$published" == *=true ]] && is_published=true || is_published=false
  else
    is_published="$(release_published "$version")"
  fi
  if [[ "$is_published" == true && -z "$(external_plugins)" ]]; then
    echo "No external plugins configured (PF_EXTERNAL_PLUGINS or $EXTERNAL_PLUGINS_FILE), the release jar would lack them." >&2
    exit 1
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
  if ! $skip_tests; then
    "$GRADLEW" -p "$ROOT" clean test
    run_external_plugins test
  fi
  start="$(git rev-parse --short HEAD)"
  trap 'echo "pfDev.sh release failed. To start over: git reset --hard $start && git tag -d $tag (if created; also in the external plugins)." >&2' ERR
  # The entries of changelog/unreleased/ go into the release (checked folded already), its entry is added if missing.
  if [[ -n "$published" ]]; then
    "$GRADLEW" -p "$ROOT" :projectforge-application:foldChangelog --rerun -PreleaseVersion="$version" "$published"
  else
    "$GRADLEW" -p "$ROOT" :projectforge-application:foldChangelog --rerun
  fi
  set_version "$version"
  "$GRADLEW" -p "$ROOT" :projectforge-application:developmentMainForRelease --rerun
  # The external plugins are built before the release commit (the version is set already; they only need the sources
  # of this checkout) to pin their jars in plugins-bin/, which belongs to the commit the tag points to.
  local extra_jars
  extra_jars="$(build_external_plugins "$version")"
  [[ "$is_published" == true ]] && pin_external_plugins "$version" "$extra_jars"
  git add -A
  git commit -q -m "release: $version"
  # Built from the release commit, so the jar's build.properties show it (and not a dirty tree). A clean build,
  # so no leftovers of earlier builds end up in the release jar. Tagged only after a successful build. The tests
  # ran before the release commit, which only changes the changelog, the version and generated files.
  # The external plugins go into the jar as built above.
  "$GRADLEW" -p "$ROOT" clean build -x test ${extra_jars:+"-Ppf.extraPlugins=$extra_jars"}
  check_release_jar_plugins "$version" "$extra_jars"
  git tag -a "$tag" -m "ProjectForge $version"
  tag_external_plugins "$version"
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
  echo "Released $version (tag $tag), now on $next."
  # Pushes the branch and the tag and creates the GitHub release (publish asks before each step), so a release is
  # not forgotten on GitHub. Declined: nothing is pushed, publish can follow by hand.
  if confirm_yes "Publish $version now (push, GitHub release)?"; then
    publish "$version"
  else
    echo "Nothing is pushed yet: pfDev.sh publish $version"
  fi
}

# Whether the release <version> of changelog.json is published (jar, docker images; a mini release isn't): true or false.
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
  local tag="$version-RELEASE" jar="$ROOT/projectforge-application/build/libs/projectforge-application-$version.jar"
  cd "$ROOT"
  if ! git rev-parse -q --verify "refs/tags/$tag" >/dev/null; then
    echo "There is no tag $tag, run pfDev.sh release $version first." >&2
    exit 1
  fi
  # A mini release ("published": false) gets a GitHub release with its own notes, but no jar or docker images.
  if [[ "$(release_published "$version")" == false ]]; then
    confirm_yes "Push $(git branch --show-current) and the tag $tag (mini release: no jar or docker images)?" || exit 1
    git push --follow-tags
    push_external_plugins "$version"
    github_release "$version"
    echo "Its changes are also part of the release notes of the next published release."
    return
  fi
  confirm_yes "Push $(git branch --show-current) and the tag $tag?" || exit 1
  git push --follow-tags
  push_external_plugins "$version"
  publish_external_plugin_assets "$version"
  github_release "$version"
  echo "Next, the docker images: docker/push-release.sh $version arch (on an arm64 and an amd64 machine)"
  # The upload to SourceForge is done by hand; afterwards the jar is set as default download there (files/latest).
  echo "SourceForge: upload with: rsync -e ssh --progress $jar <user>@frs.sourceforge.net:/home/frs/project/pforge/ProjectForge/$version/"
  echo "  and make it the default download (info icon of the file, \"Default Download For\": all platforms)."
}

# Creates the GitHub release of the pushed tag of <version>, notes from the changelog (also used for releases tagged
# and pushed earlier, pfDev.sh github-release). A mini release gets no jar and is never the latest one.
github_release() {
  local version="$1"
  local tag="$version-RELEASE" notes="$ROOT/build/release-notes-$version.md"
  local jar="$ROOT/projectforge-application/build/libs/projectforge-application-$version.jar"
  cd "$ROOT"
  # gh would create a missing tag on the default branch, so the pushed tag is checked first.
  if ! git ls-remote --exit-code --tags origin "refs/tags/$tag" >/dev/null; then
    echo "The tag $tag isn't on origin." >&2
    exit 1
  fi
  if gh release view "$tag" >/dev/null 2>&1; then
    echo "The GitHub release $tag exists already."
    return
  fi
  # Written by pfDev.sh release; missing for a release tagged earlier (or on another machine).
  if [[ ! -f "$notes" ]]; then
    "$GRADLEW" -p "$ROOT" -q :projectforge-application:releaseNotesChangelog --rerun -PreleaseVersion="$version"
  fi
  local assets=() latest=--latest=false
  if [[ "$(release_published "$version")" == true ]]; then
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
  fi
  echo "----- $notes -----"
  cat "$notes"
  echo "-----"
  confirm_yes "Create the GitHub release $tag (notes from the changelog)?" || return 0
  if [[ "$(release_published "$version")" == true ]]; then
    # Jars of minor releases may stay off GitHub (space), SourceForge has them. Without a jar, the release has no
    # Jar (GitHub) pill on the website.
    if [[ -f "$jar" ]] && confirm "Attach $(basename "$jar") to the GitHub release?"; then
      assets=("$jar")
    fi
    # Only the newest published release is the latest one, e.g. not 9.0.2 published after 9.0.3. The latest one
    # on GitHub is the download of the website, so a mini release (without jar) never is.
    local newest
    newest="$(node -e '
      const release = require(process.argv[1]).releases.find((r) => r.tag && r.published !== false);
      console.log(release?.tag ?? "");
    ' "$ROOT/changelog/changelog.json")"
    [[ "$newest" == "$tag" ]] && latest=--latest
  fi
  gh release create "$tag" ${assets[@]+"${assets[@]}"} --title "ProjectForge $version" --notes-file "$notes" "$latest"
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

# `setup`: creates the home of a slot instance; $1 = admin password for the initial database (random if omitted).
slot_setup() {
  if [[ -d "$PF_HOME" ]]; then
    echo "$PF_HOME already exists; \`pfDev.sh reset $SLOT\` first to start over." >&2
    exit 1
  fi
  init_slot_home
  # Read by the first `run`, which initializes the database with it (the setup needs a running instance).
  (umask 077 && echo "${1:-Pf-$(openssl rand -hex 8)-1x}" >"$PF_HOME/$SETUP_PENDING")
  echo "Done. Start it with \`pfDev.sh run $SLOT\` (:$SPRING_PORT); the first start fills the database with test data."
}

# `reset`: deletes the home of a slot instance; asks first unless $1 is "yes".
slot_reset() {
  if [[ ! -d "$PF_HOME" ]]; then
    echo "$PF_HOME doesn't exist."
    return 0
  fi
  if lsof -nP -iTCP:"$SPRING_PORT" -sTCP:LISTEN >/dev/null 2>&1; then
    echo "Something is listening on :$SPRING_PORT — stop the instance first." >&2
    exit 1
  fi
  local answer="${1:-}"
  [[ "$answer" == yes ]] || read -r -p "Delete $PF_HOME (database, index, logs)? [y/N] " answer
  [[ "$answer" == [yY] || "$answer" == yes ]] && rm -rf "$PF_HOME" && echo "Deleted."
  return 0
}

INTELLIJ_JBR="/Applications/IntelliJ IDEA.app/Contents/jbr/Contents/Home"

# Hot code replacement for `run`, only if $PF_HOTSWAP_AGENT (the HotswapAgent jar) is set: the app runs on a JetBrains
# Runtime ($PF_HOTSWAP_JDK, default IntelliJ's own) with enhanced class redefinition, the agent and a debug port
# (5005+slot) for IntelliJ, which reloads changed classes. Fills HOTSWAP_GRADLE_ARGS with the bootRun properties; the
# options don't go into JAVA_TOOL_OPTIONS, which Gradle's own JVMs read as well.
HOTSWAP_GRADLE_ARGS=()
# Debug port of the slot, and the stamp: changed classes are those newer than it (set by `run` and every `reload`).
hotswap_port() { echo $((5005 + SLOT)); }
hotswap_stamp() { echo "$PF_HOME/.hotswap-reload"; }

# The JetBrains Runtime of hotswap ($PF_HOTSWAP_JDK or IntelliJ's own), checked.
hotswap_jdk() {
  local jdk="${PF_HOTSWAP_JDK:-$INTELLIJ_JBR}"
  if [[ ! -x "$jdk/bin/java" ]]; then
    echo "No java in $jdk; set PF_HOTSWAP_JDK to the home of a JetBrains Runtime (e.g. $INTELLIJ_JBR)." >&2
    exit 1
  fi
  echo "$jdk"
}

hotswap_setup() {
  [[ -n "${PF_HOTSWAP_AGENT:-}" ]] || return 0
  local jdk jar="$PF_HOTSWAP_AGENT" port
  port="$(hotswap_port)"
  jdk="$(hotswap_jdk)" || exit 1
  local java="$jdk/bin/java" agent
  if [[ ! -f "$jar" ]]; then
    echo "PF_HOTSWAP_AGENT: no HotswapAgent jar $jar." >&2
    exit 1
  fi
  if ! "$java" -XX:+AllowEnhancedClassRedefinition -version >/dev/null 2>&1; then
    echo "$jdk is no JetBrains Runtime with enhanced class redefinition (PF_HOTSWAP_JDK)." >&2
    exit 1
  fi
  # A JVM just stopped (stop, renew) may still hold the port for a few seconds after its http port is closed.
  for _ in $(seq 1 30); do
    [[ -z "$(listener_pid "$port")" ]] && break
    sleep 0.5
  done
  if [[ -n "$(listener_pid "$port")" ]]; then
    echo "The debug port :$port is already in use." >&2
    exit 1
  fi
  # The Spring plugin is off by default: it wraps beans in proxies (no constructor run), whose final Kotlin methods
  # then see null fields, and the start fails. Classes are reloaded without it, only the Spring wiring isn't
  # refreshed (new beans or injected fields need a restart). PF_HOTSWAP_AGENT_OPTS="" turns it on.
  local opts="${PF_HOTSWAP_AGENT_OPTS-disablePlugin=Spring}"
  agent="-javaagent:$jar${opts:+=$opts}"
  local jvm_args=(
    -XX:+AllowEnhancedClassRedefinition -XX:HotswapAgent=external "$agent"
    "-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=127.0.0.1:$port"
    # Reflective access of HotswapAgent's plugins (as documented by HotswapAgent for JDK 17+).
    --add-opens=java.base/java.lang=ALL-UNNAMED --add-opens=java.base/java.lang.reflect=ALL-UNNAMED
    --add-opens=java.base/java.io=ALL-UNNAMED --add-opens=java.base/jdk.internal.loader=ALL-UNNAMED
    --add-opens=java.base/sun.nio.ch=ALL-UNNAMED --add-opens=java.base/sun.security.action=ALL-UNNAMED
    --add-opens=java.desktop/java.beans=ALL-UNNAMED --add-opens=java.desktop/com.sun.beans=ALL-UNNAMED
    --add-opens=java.desktop/com.sun.beans.introspect=ALL-UNNAMED --add-opens=java.desktop/com.sun.beans.util=ALL-UNNAMED
  )
  # bootRun takes the JVM as a toolchain (see projectforge-application/build.gradle.kts): its version and vendor,
  # found only in $jdk (no auto-detection, which might pick another JDK of the same version). One JVM arg per line,
  # paths may contain spaces.
  local settings version vendor
  settings="$("$java" -XshowSettings:properties -version 2>&1)"
  version="$(sed -n 's/^ *java\.specification\.version = //p' <<<"$settings")"
  vendor="$(sed -n 's/^ *java\.vendor = //p' <<<"$settings")"
  HOTSWAP_GRADLE_ARGS=(
    "-Porg.gradle.java.installations.paths=$jdk" -Porg.gradle.java.installations.auto-detect=false
    "-PpfBootRunJavaVersion=$version" "-PpfBootRunJavaVendor=$vendor"
    "-PpfBootRunJvmArgs=$(printf '%s\n' "${jvm_args[@]}")"
    # The other modules from their class dirs instead of their jars: new classes and those not loaded yet come from
    # the latest compile.
    -PpfBootRunProjectClasses=true
  )
  echo "Hotswap: $jdk, agent $(basename "$jar"), debugger on :$port (\`pfDev.sh reload\` or IntelliJ: Remote JVM Debug)."
}

# `reload`: compiles the worktree (classes only: no jars, no next build) and redefines the classes changed since
# the last reload in the running instance via jdb over the debug port of hotswap.
slot_reload() {
  if [[ -z "${PF_HOTSWAP_AGENT:-}" ]]; then
    echo "reload needs hot code replacement: set PF_HOTSWAP_AGENT and start with \`pfDev.sh run\`." >&2
    exit 1
  fi
  local port stamp_file
  port="$(hotswap_port)"
  stamp_file="$(hotswap_stamp)"
  if [[ -z "$(listener_pid "$port")" ]]; then
    if [[ -n "$(listener_pid "$SPRING_PORT")" ]]; then
      # The JVM doesn't listen on its debug port while a debugger is attached.
      echo "Nothing listens on the debug port :$port: a debugger is attached (IntelliJ? only one at a time), or the" \
        "instance on :$SPRING_PORT was started without PF_HOTSWAP_AGENT." >&2
    else
      echo "No instance on :$SPRING_PORT; start it with \`pfDev.sh run\` (PF_HOTSWAP_AGENT set)." >&2
    fi
    exit 1
  fi
  local jdk
  jdk="$(hotswap_jdk)" || exit 1
  [[ -f "$stamp_file" ]] || touch "$stamp_file"
  "$GRADLEW" -p "$ROOT" compileKotlin compileJava -q || exit 1
  # The next stamp: after this compile, before the redefine (classes compiled from now on are changed next time).
  local stamp="$stamp_file.new"
  touch "$stamp"
  local commands=() dir file count=0
  while IFS= read -r dir; do
    while IFS= read -r file; do
      local name="${file#"$dir"/}"
      name="${name%.class}"
      [[ "$name" == module-info || "$name" == */package-info ]] && continue
      commands+=("redefine ${name//\//.} $file")
      count=$((count + 1))
    done < <(find "$dir" -name '*.class' -newer "$stamp_file")
  done < <(find "$ROOT" -maxdepth 5 \( -name node_modules -o -name .git \) -prune -o -type d -path '*/build/classes/*/main' -print)
  if ((count == 0)); then
    mv "$stamp" "$stamp_file"
    echo "Nothing to reload."
    return 0
  fi
  # A redefined class prints nothing; one not loaded yet can't be redefined, it is loaded from the class dirs when
  # needed. Anything else is an error. English messages, whatever the locale.
  local output messages skipped errors
  output="$(printf '%s\n' "${commands[@]}" exit \
    | "$jdk/bin/jdb" -J-Duser.language=en -J-Duser.country=US -attach "127.0.0.1:$port" 2>&1)"
  if grep -q "Unable to attach" <<<"$output"; then
    echo "jdb can't attach to :$port; only one debugger at a time (IntelliJ attached?)." >&2
    rm -f "$stamp"
    exit 1
  fi
  messages="$(sed -E 's/^(> ?)+//' <<<"$output" | grep -v "^ *$\|^Set uncaught\|^Set deferred\|^Initializing jdb" || true)"
  skipped="$(grep -c "^No class named" <<<"$messages" || true)"
  errors="$(grep -v "^No class named" <<<"$messages" || true)"
  mv "$stamp" "$stamp_file"
  echo "Reloaded $((count - skipped)) classes ($skipped not loaded yet, they come fresh from the class dirs)."
  if [[ -n "$errors" ]]; then
    echo "$errors" >&2
    exit 1
  fi
}

# `run`: builds, then runs the app in dev mode in the foreground (replaces this shell); args go to bootRun.
slot_run() {
  local opts="-XX:ReservedCodeCacheSize=256m -Dprojectforge.base.dir=$PF_HOME"
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
  hotswap_setup
  "$GRADLEW" -p "$ROOT" build -x test
  [[ -n "${PF_HOTSWAP_AGENT:-}" ]] && touch "$(hotswap_stamp)"
  # First start after `setup`: fill the fresh database as soon as the instance answers.
  [[ -f "$PF_HOME/$SETUP_PENDING" ]] && init_database &
  JAVA_TOOL_OPTIONS="$opts" \
    exec "$GRADLEW" -p "$ROOT" :projectforge-application:bootRun ${HOTSWAP_GRADLE_ARGS[@]+"${HOTSWAP_GRADLE_ARGS[@]}"} "$@"
}

# `renew`: stops the Spring of a slot, deletes its home, sets it up again and starts it. The question comes
# first, so answering no changes nothing; the next dev server keeps running (it proxies, and reconnects).
slot_renew() {
  local assume_yes="" password=""
  for arg in "$@"; do
    if [[ "$arg" == -y ]]; then assume_yes=yes; else password="$arg"; fi
  done
  if [[ -d "$PF_HOME" && -z "$assume_yes" ]]; then
    local answer
    read -r -p "Delete $PF_HOME (database, index, logs) and restart slot $SLOT? [y/N] " answer
    [[ "$answer" == [yY] ]] || { echo "Nothing changed."; return 0; }
  fi
  stop_port "$SPRING_PORT"
  slot_reset yes
  slot_setup "$password"
  slot_run
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
    slot_run "$@"
    ;;
  reload)
    slot_reload
    ;;
  setup)
    require_slot setup
    slot_setup "${1:-}"
    ;;
  reset)
    require_slot reset
    slot_reset
    ;;
  renew)
    require_slot renew
    slot_renew "$@"
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
  bootjar)
    extra="${1:-${PF_EXTRA_PLUGINS:-}}"
    if [[ -n "$extra" ]]; then
      abs=()
      IFS=',' read -r -a parts <<< "$extra"
      for part in "${parts[@]}"; do
        part="${part/#\~/$HOME}"
        [[ -e "$part" ]] || { echo "Plugin jar $part doesn't exist." >&2; exit 1; }
        abs+=("$(cd "$(dirname "$part")" && pwd)/$(basename "$part")")
      done
      extra="$(IFS=,; echo "${abs[*]}")"
    elif ! compgen -G "$ROOT/plugins-bin/*.properties" >/dev/null; then
      echo "Note: no plugin jar given and nothing pinned in plugins-bin/, the boot jar gets no external plugin."
    fi
    "$GRADLEW" -p "$ROOT" :projectforge-application:bootJar -x test ${extra:+"-Ppf.extraPlugins=$extra"}
    version="$(sed -n 's/^version=//p' "$ROOT/gradle.properties")"
    echo "Boot jar: $ROOT/projectforge-application/build/libs/projectforge-application-$version.jar"
    ;;
  app)
    ((SLOT == 0)) || set -- "$SLOT" "$@"
    exec "$APP/bin/pfApp.sh" run "$@"
    ;;
  app-android)
    ((SLOT == 0)) || set -- "$SLOT" "$@"
    exec "$APP/bin/pfApp.sh" run "$@" --android
    ;;
  app-check)
    exec "$APP/bin/pfApp.sh" check
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
  github-release)
    if (($# == 0)); then
      echo "$RELEASE_USAGE" >&2
      exit 1
    fi
    for version in "$@"; do
      github_release "$version"
    done
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
