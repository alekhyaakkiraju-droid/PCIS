#!/usr/bin/env bash
# WO-221 integration test for openapi-diff-gate.sh
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
FIXTURES="${ROOT}/api/contracts/test-fixtures"
GATE="${ROOT}/scripts/openapi-diff-gate.sh"

OASDIFF="${OASDIFF:-oasdiff}"
if ! command -v "${OASDIFF}" >/dev/null 2>&1; then
  echo "SKIP: oasdiff not installed — install to run WO-221 gate tests"
  exit 0
fi

run_pair() {
  local baseline="$1"
  local current="$2"
  local expect="$3"
  if "${OASDIFF}" breaking "${baseline}" "${current}" --fail-on ERR >/dev/null 2>&1; then
    code=0
  else
    code=$?
  fi
  if [[ "${expect}" == "pass" && "${code}" -ne 0 ]]; then
    echo "FAIL: expected pass for ${baseline} vs ${current}" >&2
    exit 1
  fi
  if [[ "${expect}" == "fail" && "${code}" -eq 0 ]]; then
    echo "FAIL: expected breaking change for ${baseline} vs ${current}" >&2
    exit 1
  fi
  echo "OK: ${baseline##*/} vs ${current##*/} => ${expect}"
}

run_pair "${FIXTURES}/breaking-change-baseline.yaml" "${FIXTURES}/breaking-change-current.yaml" fail
run_pair "${FIXTURES}/non-breaking-change-baseline.yaml" "${FIXTURES}/non-breaking-change-current.yaml" pass

BASELINE="${ROOT}/api/contracts/baseline/pcis-shared-v1.yaml" \
CURRENT="${ROOT}/api/contracts/pcis-shared-v1.yaml" \
  bash "${GATE}"

echo "All OpenAPI gate tests passed"
