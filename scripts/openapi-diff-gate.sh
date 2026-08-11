#!/usr/bin/env bash
# WO-221: fail-closed OpenAPI breaking-change gate
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
BASELINE="${ROOT}/api/contracts/baseline/pcis-shared-v1.yaml"
CURRENT="${ROOT}/api/contracts/pcis-shared-v1.yaml"

OASDIFF="${OASDIFF:-oasdiff}"
if ! command -v "${OASDIFF}" >/dev/null 2>&1; then
  echo "ERROR: oasdiff not found on PATH. Install from https://github.com/Tufin/oasdiff" >&2
  exit 1
fi

if [[ ! -f "${BASELINE}" ]]; then
  echo "ERROR: baseline contract missing: ${BASELINE}" >&2
  exit 1
fi
if [[ ! -f "${CURRENT}" ]]; then
  echo "ERROR: current contract missing: ${CURRENT}" >&2
  exit 1
fi

echo "Running OpenAPI diff: ${BASELINE} -> ${CURRENT}"
if "${OASDIFF}" breaking "${BASELINE}" "${CURRENT}" --fail-on ERR; then
  echo "OK: no breaking API contract changes detected"
else
  echo "FAILED: breaking OpenAPI contract change detected" >&2
  exit 1
fi
