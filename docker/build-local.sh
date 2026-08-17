#!/bin/bash
#
# Builds a local ProjectForge docker image from the boot jar produced by Gradle.
#
# Useful for testing and for handing a self-contained instance (embedded HSQLDB) to
# somebody else, e. g. for pentesting: no PostgreSQL stack, no Gradle, no registry needed.
#
# Usage:
#   docker/build-local.sh [options]
#
# Options:
#   -t, --tag TAG         Image tag (default: projectforge:pentest)
#   -j, --jar FILE        Boot jar to use (default: auto-detected in projectforge-application/build/libs)
#   -p, --platform PLAT   Target platform, e. g. linux/amd64. Uses buildx (default: native)
#   -b, --base-image IMG  Base image (default: the Dockerfile's BASE_IMAGE default)
#   -s, --save            Save the image to build/docker/<name>.tar for handing it over
#   -r, --run             Run the image interactively after building
#   -n, --name NAME       Container name for --run (default: projectforge-pentest)
#   -d, --dir DIR         Host directory mounted as /ProjectForge for --run
#                         (default: $HOME/ProjectForgePentest)
#   -h, --help            Show this help
#
# Environment:
#   CONTAINER_TOOL        Force "docker" or "podman" (default: docker if available, else podman)
#

set -euo pipefail

BASE_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

IMAGE_TAG="projectforge:pentest"
JAR_PATH=""
PLATFORM=""
BASE_IMAGE=""
DO_SAVE=false
DO_RUN=false
CONTAINER_NAME="projectforge-pentest"
HOST_DIR="${HOME}/ProjectForgePentest"

usage() {
  sed -n '3,24p' "${BASH_SOURCE[0]}" | sed 's/^# \{0,1\}//'
}

while [ $# -gt 0 ]; do
  case "$1" in
    -t | --tag)
      IMAGE_TAG="$2"
      shift 2
      ;;
    -j | --jar)
      JAR_PATH="$2"
      shift 2
      ;;
    -p | --platform)
      PLATFORM="$2"
      shift 2
      ;;
    -b | --base-image)
      BASE_IMAGE="$2"
      shift 2
      ;;
    -s | --save)
      DO_SAVE=true
      shift
      ;;
    -r | --run)
      DO_RUN=true
      shift
      ;;
    -n | --name)
      CONTAINER_NAME="$2"
      shift 2
      ;;
    -d | --dir)
      HOST_DIR="$2"
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

# Find the boot jar (the '-plain' jar produced by the Kotlin/Java plugin isn't runnable):
if [ -z "$JAR_PATH" ]; then
  LIBS_DIR="${BASE_DIR}/projectforge-application/build/libs"
  # shellcheck disable=SC2012
  JAR_PATH=$(ls -t "${LIBS_DIR}"/projectforge-application-*.jar 2>/dev/null | grep -v -- '-plain\.jar$' | head -1 || true)
  if [ -z "$JAR_PATH" ]; then
    echo "Error: no boot jar found in ${LIBS_DIR}." >&2
    echo "Please build it first: ./gradlew :projectforge-application:bootJar" >&2
    exit 1
  fi
fi
if [ ! -f "$JAR_PATH" ]; then
  echo "Error: jar not found: $JAR_PATH" >&2
  exit 1
fi
JAR_FILE=$(basename "$JAR_PATH")
echo "Using boot jar: $JAR_PATH"

# The Dockerfile copies ${JAR_FILE} from the build context, but .dockerignore excludes */build,
# therefore the jar has to be copied to the base directory temporarily:
CONTEXT_JAR="${BASE_DIR}/${JAR_FILE}"
CLEANUP_JAR=false
if [ ! -f "$CONTEXT_JAR" ]; then
  echo "Copying jar to build context: $CONTEXT_JAR"
  cp "$JAR_PATH" "$CONTEXT_JAR"
  CLEANUP_JAR=true
fi

cleanup() {
  if [ "$CLEANUP_JAR" = true ] && [ -f "$CONTEXT_JAR" ]; then
    echo "Removing jar from build context..."
    rm -f "$CONTEXT_JAR"
  fi
}
trap cleanup EXIT

BUILD_ARGS=(--build-arg "JAR_FILE=${JAR_FILE}")
if [ -n "$BASE_IMAGE" ]; then
  BUILD_ARGS+=(--build-arg "BASE_IMAGE=${BASE_IMAGE}")
fi

echo "Building image $IMAGE_TAG..."
if [ -n "$PLATFORM" ]; then
  "$TOOL" buildx build --platform "$PLATFORM" "${BUILD_ARGS[@]}" -t "$IMAGE_TAG" --load "$BASE_DIR"
else
  "$TOOL" build "${BUILD_ARGS[@]}" -t "$IMAGE_TAG" "$BASE_DIR"
fi

cleanup
CLEANUP_JAR=false

TAR_FILE=""
if [ "$DO_SAVE" = true ]; then
  TAR_DIR="${BASE_DIR}/build/docker"
  mkdir -p "$TAR_DIR"
  TAR_FILE="${TAR_DIR}/$(echo "$IMAGE_TAG" | tr ':/' '--').tar"
  echo "Saving image to $TAR_FILE..."
  "$TOOL" save -o "$TAR_FILE" "$IMAGE_TAG"
  echo "Saved ($(du -h "$TAR_FILE" | cut -f1))."
fi

RUN_CMD="$TOOL run -t -i -p 127.0.0.1:8080:8080 -v ${HOST_DIR}:/ProjectForge --name ${CONTAINER_NAME} ${IMAGE_TAG}"

cat <<EOF

------------------------------------------------------------------------------
Image built: $IMAGE_TAG ($("$TOOL" image inspect "$IMAGE_TAG" --format '{{.Architecture}}' 2>/dev/null || echo 'unknown arch'))

First start (interactive, the console setup wizard needs a terminal):
  $RUN_CMD

Then open http://localhost:8080 and run the setup page.
Choose target "Test system" for a database filled with test data.

Restart afterwards (recommended for full plugin functionality):
  $TOOL stop ${CONTAINER_NAME} && $TOOL start -ai ${CONTAINER_NAME}

Reset everything:
  $TOOL rm -f ${CONTAINER_NAME} && rm -rf ${HOST_DIR}
EOF

if [ -n "$TAR_FILE" ]; then
  cat <<EOF

Handing the image over (recipient side):
  docker load -i $(basename "$TAR_FILE")
  $RUN_CMD
EOF
fi
echo "------------------------------------------------------------------------------"

if [ "$DO_RUN" = true ]; then
  echo ""
  echo "Starting container..."
  mkdir -p "$HOST_DIR"
  exec $RUN_CMD
fi
