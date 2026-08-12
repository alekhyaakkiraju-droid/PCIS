#!/usr/bin/env bash
# Verify pcis-platform services exist before deploying microservices.
set -euo pipefail

NAMESPACE="${1:-pcis-dev}"

check_svc() {
  local name="$1"
  if ! kubectl get svc "${name}" -n "${NAMESPACE}" >/dev/null 2>&1; then
    echo "[preflight] MISSING service/${name} in ${NAMESPACE}"
    echo "[preflight] Run pcis-platform-dev-pipeline first."
    exit 1
  fi
  echo "[preflight] OK service/${name}"
}

for svc in postgresql redis keycloak; do
  check_svc "${svc}"
done

echo "[preflight] Platform services present in ${NAMESPACE}"
