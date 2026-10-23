#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"
RUNTIME="$ROOT/.local/mcp"
mkdir -p "$RUNTIME/home"
chmod 700 "$RUNTIME" "$RUNTIME/home"
export PROJECTFORGE_HOME="$RUNTIME/home"
export GRADLE_USER_HOME="$ROOT/.local/gradle"
if [[ -d /opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home ]]; then
  export JAVA_HOME=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home
fi
JAVA="${JAVA_HOME:+$JAVA_HOME/bin/}java"
for port in 18080; do
  if lsof -tiTCP:"$port" -sTCP:LISTEN >/dev/null 2>&1; then
    echo "Port $port is in use. Stop the existing demo with local-mcp/stop.sh first." >&2
    exit 1
  fi
done
if [[ "${1:-}" != "--no-build" ]]; then
  ./gradlew :projectforge-application:bootJar --no-daemon --max-workers=2
fi
JAR="$(find projectforge-application/build/libs -name '*.jar' ! -name '*-plain.jar' | head -1)"
[[ -f "$JAR" ]] || { echo "Build the application first." >&2; exit 1; }
# Run a snapshot so later Gradle builds cannot replace the running JVM's JAR.
cp "$JAR" "$RUNTIME/projectforge-application.jar"
cp local-mcp/projectforge.properties "$PROJECTFORGE_HOME/projectforge.properties"
"$JAVA" -Xmx2g -jar "$RUNTIME/projectforge-application.jar" > "$RUNTIME/projectforge.log" 2>&1 &
PF_PID=$!
echo "$PF_PID" > "$RUNTIME/projectforge.pid"
cleanup() { kill "$PF_PID" 2>/dev/null || true; }
trap cleanup EXIT INT TERM
node local-mcp/setup.mjs
echo 'ProjectForge + session OAuth: http://localhost:18080'
echo 'In this directory run: claude, then /mcp → projectforge-local → Authenticate'
echo 'Existing ProjectForge browser login is reused. Local web login: admin / Local-Demo-2026!'
echo 'OAuth consent contains scopes only; no separate password or OTP.'
echo "Logs: $RUNTIME"
wait "$PF_PID"
