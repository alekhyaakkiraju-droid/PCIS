#!/usr/bin/env bash
# Stop PCIS local Java processes and optional Docker infra.
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
PID_FILE="${ROOT}/.local/pids"
STOP_DOCKER="${1:-}"

stop_pids() {
  if [[ ! -f "$PID_FILE" ]]; then
    return 0
  fi
  while read -r pid; do
    if [[ -n "$pid" ]] && kill -0 "$pid" 2>/dev/null; then
      kill "$pid" 2>/dev/null || true
    fi
  done <"$PID_FILE"
  rm -f "$PID_FILE"
}

stop_pids

# Stop stray vite on common local port
pkill -f "vite --port 3001" 2>/dev/null || true

if [[ "$STOP_DOCKER" == "--docker" ]]; then
  docker compose -f "${ROOT}/docker-compose.local.yml" down
fi

echo "Stopped local PCIS Java processes."
if [[ "$STOP_DOCKER" != "--docker" ]]; then
  echo "Docker infra still running. Use: ./scripts/stop-local.sh --docker"
fi
