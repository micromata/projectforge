#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
file="$ROOT/.local/mcp/projectforge.pid"
if [[ -f "$file" ]]; then
  pid="$(cat "$file")"
  command="$(ps -p "$pid" -o command= 2>/dev/null || true)"
  if [[ "$command" == *projectforge-application* ]]; then
    kill "$pid"
  fi
  rm -f "$file"
fi
