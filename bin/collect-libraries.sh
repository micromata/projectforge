#!/usr/bin/env bash
#
# collect-libraries.sh — collects the libraries of ProjectForge's components for the CVE-Clinic.
#
# One zip per component (upload it with "Upload libraries" of the component in the inventory):
#   backend       the runtime classpath of projectforge-application with all plugins (`gradle dependencies`)
#   next, app     the npm packages needed at runtime (CycloneDX via `npm sbom`, else the package-lock.json)
#   image-<name>  the packages of a Docker image (CycloneDX via syft, else dpkg/apk/rpm lists); its jars are
#                 left out, they are the ones of backend
# The web site (site/) isn't one: its npm packages only build the CSS (development dependencies, which the
# CVE-Clinic leaves out), nothing of them is delivered.
# CycloneDX is preferred wherever possible: it has the full package urls (with the distribution of an OS
# package) and is what Dependency-Track reads too. Nothing is uploaded by this script.
#
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
OUT="$ROOT/build/cve-clinic-libraries"
COMPOSE="$ROOT/docker/compose/docker-compose.yml"
SYFT_IMAGE="${SYFT_IMAGE:-docker.io/anchore/syft:latest}"
ALL_COMPONENTS="backend,next,app,images"
# The limits of the upload (LibraryListParser, LibraryArchive).
MAX_FILE_BYTES=$((5 * 1024 * 1024))
MAX_ZIP_BYTES=$((10 * 1024 * 1024))

usage() {
  cat <<'EOF'
collect-libraries.sh [options] — the libraries of ProjectForge's components as zips for the CVE-Clinic

Options:
  --only <list>    components, comma separated: backend,next,app,images (default: all)
  --image <ref>    an image to collect (repeatable); replaces the images of docker/compose/docker-compose.yml.
                   Give the tags actually deployed.
  --no-syft        no syft, the package lists of the images only (without the distribution)
  --out <dir>      output directory (default: build/cve-clinic-libraries)
  -h, --help       this help

The images are read with docker or podman (override with CONTAINER_ENGINE=…). syft is taken from the PATH,
otherwise run as container (docker.io/anchore/syft:latest, override with SYFT_IMAGE=…).
EOF
}

only="$ALL_COMPONENTS"
images=()
use_syft=true
while [[ $# -gt 0 ]]; do
  case "$1" in
    --only) only="$2"; shift 2 ;;
    --image) images+=("$2"); shift 2 ;;
    --no-syft) use_syft=false; shift ;;
    --out) OUT="$2"; shift 2 ;;
    -h | --help) usage; exit 0 ;;
    *) echo "Unknown option: $1" >&2; usage >&2; exit 2 ;;
  esac
done

wanted() { [[ ",$only," == *",$1,"* ]]; }
warn() { echo "WARNING: $*" >&2; }
fail() { echo "ERROR: $*" >&2; exit 1; }
size_of() { wc -c <"$1" | tr -d ' '; }

mkdir -p "$OUT"
summary=()

# A fresh directory of a component; the others of an earlier run (--only) stay.
prepare() {
  rm -rf "${OUT:?}/$1" "$OUT/$1.zip"
  mkdir -p "$OUT/$1"
}

# Zips the directory of a component after checking the limits; an empty one is an error.
pack() {
  local component="$1" dir="$OUT/$1" file count=0
  for file in "$dir"/*; do
    [[ -s "$file" ]] || fail "$component: empty list $(basename "$file")"
    (($(size_of "$file") <= MAX_FILE_BYTES)) || fail "$component: $(basename "$file") is larger than 5 MB"
    count=$((count + 1))
  done
  ((count > 0)) || fail "$component: nothing collected"
  (cd "$dir" && zip -q -r "$OUT/$component.zip" .)
  local size
  size="$(size_of "$OUT/$component.zip")"
  ((size <= MAX_ZIP_BYTES)) || fail "$component.zip is larger than 10 MB"
  summary+=("$(printf '%-24s %3d file(s) %8d KB  %s' "$component.zip" "$count" $((size / 1024)) \
    "$(cd "$dir" && ls | tr '\n' ' ')")")
}

collect_backend() {
  echo "backend: runtime classpath of projectforge-application …"
  prepare backend
  # Only resolves the dependencies, builds nothing.
  "$ROOT/gradlew" -q -p "$ROOT" :projectforge-application:dependencies --configuration runtimeClasspath \
    >"$OUT/backend/backend-gradle.txt"
  pack backend
}

# The npm packages of a project: CycloneDX without the development dependencies, else the lockfile itself.
collect_npm() {
  local component="$1" project="$ROOT/$2"
  echo "$component: npm packages of $2 …"
  [[ -f "$project/package-lock.json" ]] || fail "$component: $2/package-lock.json not found"
  prepare "$component"
  local error
  if command -v npm >/dev/null &&
    error="$(cd "$project" && npm sbom --package-lock-only --omit dev --sbom-format cyclonedx 2>&1 \
      >"$OUT/$component/$component.cdx.json")"; then
    :
  else
    warn "$component: npm sbom failed ($(grep -m1 -v -e '^npm warn' -e 'error code' <<<"${error:-}" | sed 's/^npm error //')), taking the package-lock.json"
    rm -f "$OUT/$component/$component.cdx.json"
    cp "$project/package-lock.json" "$OUT/$component/"
  fi
  pack "$component"
}

# syft from the PATH, or as container. It reads the image from an archive (`save`), so it needs no socket of the
# engine and works with docker and podman alike.
syft_image() {
  local image="$1" archive_dir="$OUT/.archive"
  mkdir -p "$archive_dir"
  trap 'rm -rf "$OUT/.archive"' RETURN
  "$ENGINE" save -o "$archive_dir/image.tar" "$image" >/dev/null || return 1
  # The jars of the image are the libraries of backend: not collected twice.
  local args=(-q --select-catalogers "-java-archive-cataloger" -o cyclonedx-json)
  if command -v syft >/dev/null; then
    syft "docker-archive:$archive_dir/image.tar" "${args[@]}"
  else
    "$ENGINE" run --rm -v "$archive_dir:/scan:ro" "$SYFT_IMAGE" "docker-archive:/scan/image.tar" "${args[@]}"
  fi
}

# The package list of the image's operating system: dpkg (Debian, Ubuntu), apk (Alpine) or rpm (Red Hat).
package_list() {
  local image="$1"
  "$ENGINE" run --rm --entrypoint dpkg-query "$image" -W -f '${Package} ${Version}\n' 2>/dev/null ||
    "$ENGINE" run --rm --entrypoint apk "$image" info -v 2>/dev/null ||
    "$ENGINE" run --rm --entrypoint rpm "$image" -qa 2>/dev/null
}

# With the registry: podman doesn't guess one for a short name (`postgres:13`), docker takes the same.
qualified() {
  local image="$1" first="${1%%/*}"
  if [[ "$image" != */* ]]; then
    echo "docker.io/library/$image"
  elif [[ "$first" != *.* && "$first" != *:* && "$first" != localhost ]]; then
    echo "docker.io/$image"
  else
    echo "$image"
  fi
}

collect_image() {
  local image name
  image="$(qualified "$1")"
  name="$(basename "${image%%[:@]*}")"
  local component="image-$name"
  echo "$component: packages of $image …"
  [[ "${image##*/}" == *[:@]* && "$image" != *:latest ]] || warn "$image: no fixed tag, give the deployed one with --image"
  "$ENGINE" image inspect "$image" >/dev/null 2>&1 || "$ENGINE" pull -q "$image" >/dev/null
  prepare "$component"
  if $use_syft && syft_image "$image" >"$OUT/$component/$component.cdx.json"; then
    :
  else
    $use_syft && warn "$component: syft failed, taking the package list"
    rm -f "$OUT/$component/$component.cdx.json"
    package_list "$image" >"$OUT/$component/$component-packages.txt" ||
      fail "$component: neither dpkg, apk nor rpm found in $image"
    warn "$component: a package list has no distribution; the matches of OS packages are only hints"
  fi
  pack "$component"
}

wanted backend && collect_backend
wanted next && collect_npm next projectforge-next
wanted app && collect_npm app projectforge-app
if wanted images; then
  ENGINE="${CONTAINER_ENGINE:-$(command -v docker || command -v podman || true)}"
  if [[ -z "$ENGINE" ]]; then
    warn "images: neither docker nor podman found, the images are skipped"
  else
    if [[ ${#images[@]} -eq 0 ]]; then
      while read -r image; do images+=("$image"); done < <(sed -n 's/^[[:space:]]*image:[[:space:]]*//p' "$COMPOSE")
    fi
    for image in "${images[@]}"; do collect_image "$image"; done
  fi
fi

echo
echo "In $OUT (one upload per component):"
if ((${#summary[@]} > 0)); then printf "  %s\n" "${summary[@]}"; fi
