#!/usr/bin/env bash
# WO-221: OpenAPI contract diff gate — fail when generated specs drift from snapshots.
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
PYTHON="${PYTHON:-python3}"

usage() {
  cat <<'EOF'
Usage: scripts/openapi-contract-gate.sh [--dry-run] [--update-snapshots] [--json]

Compares committed OpenAPI snapshots (contracts/snapshots/) against generated specs
declared in contracts/snapshots/manifest.yaml.

Options:
  --dry-run            Report drift without failing (for local verification)
  --update-snapshots   Refresh committed snapshots from generated specs
  --json               Emit machine-readable results
  -h, --help           Show this help
EOF
}

ARGS=()
while [[ $# -gt 0 ]]; do
  case "$1" in
    --dry-run|--update-snapshots|--json)
      ARGS+=("$1")
      shift
      ;;
    -h|--help)
      usage
      exit 0
      ;;
    *)
      echo "ERROR: unknown option: $1" >&2
      usage
      exit 2
      ;;
  esac
done

echo "==> WO-221 OpenAPI contract diff gate"
if ((${#ARGS[@]})); then
  exec "${PYTHON}" "${ROOT}/scripts/openapi_contract_gate.py" "${ARGS[@]}"
else
  exec "${PYTHON}" "${ROOT}/scripts/openapi_contract_gate.py"
fi
