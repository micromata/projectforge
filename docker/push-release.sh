#!/bin/bash
#
# Pushes the docker images of a release to the registry, each architecture built natively.
#
# Run `arch` on an arm64 machine (e. g. the Mac) and on an amd64 machine (e. g. a Linux server), then
# `manifest` once on either of them to combine both into the multi-arch tags X.Y.Z and latest.
#
# Usage:
#   docker/push-release.sh <X.Y.Z> arch|manifest [options]
#
# Commands:
#   arch                  Builds the image for this machine's architecture and pushes it as
#                         <repo>:X.Y.Z-<arch> (amd64 or arm64)
#   manifest              Pushes <repo>:X.Y.Z and <repo>:latest, combining X.Y.Z-amd64 and X.Y.Z-arm64
#
# Options:
#   -r, --repo REPO       Image repository (default: docker.io/micromata/projectforge)
#   -j, --jar FILE        Boot jar (default: projectforge-application/build/libs/projectforge-application-X.Y.Z.jar,
#                         downloaded from the GitHub release if it isn't there)
#   -h, --help            Show this help
#
# The Dockerfile and docker/*.sh are taken from the tag X.Y.Z-RELEASE, not from the working tree, so any
# branch works as long as the tag is fetched. Log in first: `podman login docker.io` or `docker login`.
#
# Environment:
#   CONTAINER_TOOL        Force "docker" or "podman" (default: docker if available, else podman)
#

set -euo pipefail

BASE_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
GITHUB_DOWNLOAD="https://github.com/micromata/projectforge/releases/download"

REPO="docker.io/micromata/projectforge"
JAR_PATH=""

usage() {
  sed -n '3,27p' "${BASH_SOURCE[0]}" | sed 's/^# \{0,1\}//'
}

confirm() {
  local answer
  read -r -p "$1 [y/N] " answer
  [[ "$answer" == [yY] ]]
}

VERSION="${1:-}"
COMMAND="${2:-}"
if [[ "$VERSION" == -h || "$VERSION" == --help ]]; then
  usage
  exit 0
fi
if [[ ! "$VERSION" =~ ^[0-9]+\.[0-9]+\.[0-9]+$ || ! "$COMMAND" =~ ^(arch|manifest)$ ]]; then
  usage >&2
  exit 1
fi
shift 2
while [ $# -gt 0 ]; do
  case "$1" in
    -r | --repo)
      REPO="$2"
      shift 2
      ;;
    -j | --jar)
      JAR_PATH="$2"
      shift 2
      ;;
    -h | --help)
      usage
      exit 0
      ;;
    *)
      echo "Error: unknown option '$1'" >&2
      usage >&2
      exit 1
      ;;
  esac
done
TAG="$VERSION-RELEASE"

# Detect the container tool:
TOOL="${CONTAINER_TOOL:-}"
if [ -z "$TOOL" ]; then
  if command -v docker &>/dev/null; then
    TOOL=docker
  elif command -v podman &>/dev/null; then
    TOOL=podman
  else
    echo "Error: neither docker nor podman found in PATH." >&2
    exit 1
  fi
fi
echo "Using container tool: $TOOL"

build_and_push_arch() {
  local arch
  case "$(uname -m)" in
    x86_64 | amd64) arch=amd64 ;;
    arm64 | aarch64) arch=arm64 ;;
    *)
      echo "Error: unsupported architecture $(uname -m)." >&2
      exit 1
      ;;
  esac
  if ! git -C "$BASE_DIR" rev-parse -q --verify "refs/tags/$TAG" >/dev/null; then
    echo "Error: there is no tag $TAG, fetch it first: git fetch --tags" >&2
    exit 1
  fi
  local jar_file="projectforge-application-$VERSION.jar" image="$REPO:$VERSION-$arch"
  # Global, the EXIT trap runs after this function has returned.
  CONTEXT="$(mktemp -d)"
  trap 'rm -rf "$CONTEXT"' EXIT
  # Build context of the release: its Dockerfile and docker scripts plus the jar, nothing else.
  mkdir -p "$CONTEXT/docker"
  for file in Dockerfile docker/entrypoint.sh docker/environment.sh; do
    git -C "$BASE_DIR" show "$TAG:$file" >"$CONTEXT/$file"
  done
  [ -n "$JAR_PATH" ] || JAR_PATH="$BASE_DIR/projectforge-application/build/libs/$jar_file"
  if [ -f "$JAR_PATH" ]; then
    echo "Using boot jar: $JAR_PATH"
    cp "$JAR_PATH" "$CONTEXT/$jar_file"
  else
    echo "Downloading $jar_file from the GitHub release $TAG..."
    curl -fL --progress-bar -o "$CONTEXT/$jar_file" "$GITHUB_DOWNLOAD/$TAG/$jar_file"
  fi
  echo "Building $image..."
  "$TOOL" build --platform "linux/$arch" --build-arg "JAR_FILE=$jar_file" -t "$image" "$CONTEXT"
  confirm "Push $image?" || exit 1
  "$TOOL" push "$image"
  local other=amd64 machine="the amd64 machine (e. g. the Linux server)"
  if [ "$arch" = amd64 ]; then
    other=arm64 machine="the arm64 machine (e. g. the Mac)"
  fi
  echo
  echo "------------------------------------------------------------------------------"
  if remote_tag_exists "$VERSION-$other"; then
    echo "Pushed $image, $REPO:$VERSION-$other is there already. Next, on either machine:"
  else
    # The script itself may be newer than the tag, so the other machine updates its branch, not to the tag.
    cat <<EOF
Pushed $image. Next, on $machine, in its ProjectForge checkout:
  git fetch --tags
  git checkout $(git -C "$BASE_DIR" branch --show-current) && git pull
  docker/push-release.sh $VERSION arch

Then, on either machine:
EOF
  fi
  echo "  docker/push-release.sh $VERSION manifest"
  echo "------------------------------------------------------------------------------"
}

# Whether the registry has the tag, e. g. pushed by `arch` on the other machine.
remote_tag_exists() {
  if [ "$TOOL" = podman ]; then
    podman search --list-tags --limit 10000 --format '{{.Tag}}' "$REPO" 2>/dev/null | grep -qx -- "$1"
  else
    docker manifest inspect "$REPO:$1" >/dev/null 2>&1
  fi
}

push_manifests() {
  local amd64="$REPO:$VERSION-amd64" arm64="$REPO:$VERSION-arm64" tag
  for tag in "$VERSION-amd64" "$VERSION-arm64"; do
    if ! remote_tag_exists "$tag"; then
      echo "Error: $REPO:$tag isn't in the registry, run docker/push-release.sh $VERSION arch on that machine first." >&2
      exit 1
    fi
  done
  confirm "Push $REPO:$VERSION and $REPO:latest from $amd64 and $arm64?" || exit 1
  for tag in "$VERSION" latest; do
    if [ "$TOOL" = podman ]; then
      podman manifest exists "$REPO:$tag" && podman manifest rm "$REPO:$tag" >/dev/null
      # docker:// reads the images from the registry, both must have been pushed by `arch`.
      podman manifest create "$REPO:$tag" "docker://$amd64" "docker://$arm64"
      podman manifest push --all "$REPO:$tag" "docker://$REPO:$tag"
    else
      docker buildx imagetools create -t "$REPO:$tag" "$amd64" "$arm64"
    fi
  done
  echo "Pushed $REPO:$VERSION and $REPO:latest. Check: https://hub.docker.com/r/micromata/projectforge/tags"
}

case "$COMMAND" in
  arch) build_and_push_arch ;;
  manifest) push_manifests ;;
esac
