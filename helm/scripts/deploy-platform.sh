#!/usr/bin/env bash
# Deploy pcis-platform without blocking on a single helm --wait (staged rollouts + diagnostics).
set -euo pipefail

CHART_PATH="${1:?Usage: deploy-platform.sh <chart-path>}"
NAMESPACE="${2:?Usage: deploy-platform.sh <chart-path> <namespace>}"
RELEASE="${3:-pcis-platform}"

log() { echo "[deploy-platform] $*"; }
fail() {
  log "FAILED: $*"
  log "Pods in ${NAMESPACE}:"
  kubectl get pods -n "${NAMESPACE}" -o wide || true
  for pod in $(kubectl get pods -n "${NAMESPACE}" -o jsonpath='{.items[*].metadata.name}' 2>/dev/null); do
    log "--- describe ${pod} ---"
    kubectl describe pod "${pod}" -n "${NAMESPACE}" | tail -30 || true
    log "--- logs ${pod} (last 40 lines) ---"
    kubectl logs "${pod}" -n "${NAMESPACE}" --tail=40 2>/dev/null || true
  done
  exit 1
}

wait_rollout() {
  local kind="$1" name="$2" timeout="$3"
  log "Waiting for ${kind}/${name} (timeout ${timeout}s)..."
  if ! kubectl rollout status "${kind}/${name}" -n "${NAMESPACE}" --timeout="${timeout}s"; then
    fail "${kind}/${name} did not become ready"
  fi
}

helm upgrade --install "${RELEASE}" "${CHART_PATH}" \
  --namespace "${NAMESPACE}" \
  --create-namespace \
  -f "${CHART_PATH}/values.yaml" \
  -f "${CHART_PATH}/values-dev.yaml" \
  --timeout 10m \
  --wait=false

wait_rollout statefulset keycloak-db 300
wait_rollout statefulset postgresql 300
wait_rollout deployment redis 180
wait_rollout deployment keycloak 600

if kubectl get deployment kafka -n "${NAMESPACE}" >/dev/null 2>&1; then
  wait_rollout deployment kafka 180
fi

log "Platform bootstrap ready in ${NAMESPACE}"
