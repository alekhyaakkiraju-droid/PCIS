#!/usr/bin/env bash
# Deploy pcis-platform without blocking on a single helm --wait (staged rollouts + diagnostics).
set -euo pipefail

CHART_PATH="${1:?Usage: deploy-platform.sh <chart-path>}"
NAMESPACE="${2:?Usage: deploy-platform.sh <chart-path> <namespace>}"
RELEASE="${3:-pcis-platform}"

log() { echo "[deploy-platform] $*"; }
fail() {
  log "FAILED: $*"
  log "Platform pods in ${NAMESPACE}:"
  kubectl get pods -n "${NAMESPACE}" -l 'app.kubernetes.io/part-of=pcis,app.kubernetes.io/component=platform' -o wide 2>/dev/null \
    || kubectl get pods -n "${NAMESPACE}" -o wide || true
  for pod in $(kubectl get pods -n "${NAMESPACE}" -o jsonpath='{.items[*].metadata.name}' 2>/dev/null \
    | tr ' ' '\n' | grep -E '^(postgresql|redis|keycloak)' || true); do
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

recover_stuck_helm_release() {
  if ! helm status "${RELEASE}" -n "${NAMESPACE}" >/dev/null 2>&1; then
    return 0
  fi
  local status=""
  status=$(helm status "${RELEASE}" -n "${NAMESPACE}" -o json 2>/dev/null \
    | python3 -c "import sys,json; print(json.load(sys.stdin)['info']['status'])" 2>/dev/null \
    || true)
  log "Helm release ${RELEASE} status: ${status:-unknown}"
  case "${status}" in
    pending-install|pending-upgrade|pending-rollback|failed)
      log "Clearing stuck Helm release lock (${status})"
      local latest_secret
      latest_secret=$(kubectl get secrets -n "${NAMESPACE}" -l "owner=helm,name=${RELEASE}" \
        -o jsonpath='{.items[-1].metadata.name}' 2>/dev/null || true)
      if [ -n "${latest_secret}" ]; then
        kubectl delete secret "${latest_secret}" -n "${NAMESPACE}" --ignore-not-found=true || true
      fi
      if helm status "${RELEASE}" -n "${NAMESPACE}" >/dev/null 2>&1; then
        helm uninstall "${RELEASE}" -n "${NAMESPACE}" --no-hooks --wait --timeout 5m 2>/dev/null \
          || kubectl delete secrets -n "${NAMESPACE}" -l "owner=helm,name=${RELEASE}" --ignore-not-found=true
      fi
      ;;
  esac
}

recover_stuck_helm_release

if ! helm upgrade --install "${RELEASE}" "${CHART_PATH}" \
  --namespace "${NAMESPACE}" \
  --create-namespace \
  -f "${CHART_PATH}/values.yaml" \
  -f "${CHART_PATH}/values-dev.yaml" \
  --timeout 10m \
  --wait=false; then
  log "Helm upgrade failed — attempting stuck-release recovery and retry"
  recover_stuck_helm_release
  helm upgrade --install "${RELEASE}" "${CHART_PATH}" \
    --namespace "${NAMESPACE}" \
    --create-namespace \
    -f "${CHART_PATH}/values.yaml" \
    -f "${CHART_PATH}/values-dev.yaml" \
    --timeout 10m \
    --wait=false
fi

wait_rollout statefulset keycloak-db 300
wait_rollout statefulset postgresql 300
wait_rollout deployment redis 180
wait_rollout deployment keycloak 600

if kubectl get deployment kafka -n "${NAMESPACE}" >/dev/null 2>&1; then
  wait_rollout deployment kafka 180
fi

log "Platform bootstrap ready in ${NAMESPACE}"
