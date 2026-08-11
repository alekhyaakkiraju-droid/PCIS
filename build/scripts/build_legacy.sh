#!/usr/bin/env bash
# PCIS legacy IBM i build entrypoint (WO-005)
# Usage: build/scripts/build_legacy.sh <dev|tst|prd> [--executor stub|real]
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"
ENV_NAME="${1:-}"
shift || true

if [[ -z "${ENV_NAME}" ]]; then
  echo "ERROR: environment name required. Available: dev, tst, prd" >&2
  exit 2
fi

case "${ENV_NAME}" in
  dev|tst|prd) ;;
  *)
    echo "ERROR: invalid environment '${ENV_NAME}'. Available: dev, tst, prd" >&2
    exit 2
    ;;
esac

EXECUTOR="stub"
EXTRA_ARGS=()
while [[ $# -gt 0 ]]; do
  case "$1" in
    --executor)
      EXECUTOR="${2:-}"
      shift 2
      ;;
    *)
      EXTRA_ARGS+=("$1")
      shift
      ;;
  esac
done

cd "${REPO_ROOT}"
exec python3 "${SCRIPT_DIR}/build_orchestrator.py" \
  --env "${ENV_NAME}" \
  --repo-root "${REPO_ROOT}" \
  --executor "${EXECUTOR}" \
  "${EXTRA_ARGS[@]+"${EXTRA_ARGS[@]}"}"
