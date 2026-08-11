#!/usr/bin/env bash
# Validate PCIS Helm charts: prefer helm template; fall back to file-content checks.
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
CHARTS_DIR="${ROOT}/helm/charts"
COMMON="${CHARTS_DIR}/pcis-common"
SERVICES=(customer-svc claims-svc policy-svc premium-svc billing-svc reporting-svc authz-svc audit-svc)
REQUIRED_STRINGS=(
  "runAsNonRoot"
  "readOnlyRootFilesystem"
  "allowPrivilegeEscalation: false"
  "drop:"
  "- ALL"
  "/actuator/health/liveness"
  "/actuator/health/readiness"
  "/actuator/health/startup"
  "failureThreshold"
  "sidecar.istio.io/inject"
  "api-gateway"
  "enableHpa"
  "enablePdb"
  "minAvailable"
)

PASS=0
FAIL=0

pass() { echo "PASS: $*"; PASS=$((PASS + 1)); }
fail() { echo "FAIL: $*"; FAIL=$((FAIL + 1)); }

echo "==> Checking library chart structure"
for f in _helpers.tpl _deployment.tpl _service.tpl _serviceaccount.tpl _pdb.tpl _hpa.tpl _networkpolicy.tpl _configmap.tpl; do
  if [[ -f "${COMMON}/templates/${f}" ]]; then
    pass "library has ${f}"
  else
    fail "missing ${COMMON}/templates/${f}"
  fi
done

if grep -q 'type: library' "${COMMON}/Chart.yaml"; then
  pass "pcis-common is type library"
else
  fail "pcis-common Chart.yaml missing type: library"
fi

echo "==> Checking required security / probe strings in library templates"
for s in "${REQUIRED_STRINGS[@]}"; do
  if grep -RFq -- "$s" "${COMMON}/templates"; then
    pass "library contains '${s}'"
  else
    fail "library missing '${s}'"
  fi
done

echo "==> Checking service charts"
for svc in "${SERVICES[@]}"; do
  dir="${CHARTS_DIR}/${svc}"
  for f in Chart.yaml values.yaml values-dev.yaml values-prd.yaml; do
    [[ -f "${dir}/${f}" ]] && pass "${svc}/${f}" || fail "missing ${svc}/${f}"
  done
  if grep -q 'file://../pcis-common' "${dir}/Chart.yaml"; then
    pass "${svc} depends on pcis-common"
  else
    fail "${svc} missing pcis-common dependency"
  fi
  for tpl in deployment service serviceaccount pdb hpa networkpolicy configmap; do
    if [[ -f "${dir}/templates/${tpl}.yaml" ]] && grep -q "pcis-common.${tpl}" "${dir}/templates/${tpl}.yaml"; then
      pass "${svc}/templates/${tpl}.yaml includes library"
    else
      fail "${svc}/templates/${tpl}.yaml missing include"
    fi
  done
  if grep -q 'minAvailable: 1' "${dir}/values-dev.yaml"; then
    pass "${svc} values-dev minAvailable=1"
  else
    fail "${svc} values-dev missing minAvailable: 1"
  fi
  if grep -q 'minAvailable: 2' "${dir}/values-prd.yaml"; then
    pass "${svc} values-prd minAvailable=2"
  else
    fail "${svc} values-prd missing minAvailable: 2"
  fi
done

if command -v helm >/dev/null 2>&1; then
  echo "==> helm is installed — running helm dependency update + template"
  for svc in "${SERVICES[@]}"; do
    dir="${CHARTS_DIR}/${svc}"
    if helm dependency update "${dir}" >/tmp/helm-dep-${svc}.log 2>&1; then
      pass "helm dependency update ${svc}"
    else
      fail "helm dependency update ${svc}"
      cat /tmp/helm-dep-${svc}.log
      continue
    fi
    out="/tmp/helm-render-${svc}.yaml"
    if helm template "test-${svc}" "${dir}" -f "${dir}/values.yaml" -f "${dir}/values-prd.yaml" > "${out}" 2>/tmp/helm-tpl-${svc}.err; then
      pass "helm template ${svc}"
      for needle in runAsNonRoot readOnlyRootFilesystem allowPrivilegeEscalation "/actuator/health/liveness" "/actuator/health/startup" "sidecar.istio.io/inject" "api-gateway" "minAvailable: 2"; do
        if grep -Fq -- "$needle" "${out}"; then
          pass "render ${svc} contains ${needle}"
        else
          fail "render ${svc} missing ${needle}"
        fi
      done
      # batch flags: disable HPA/PDB
      batch_out="/tmp/helm-render-${svc}-batch.yaml"
      if helm template "test-${svc}-batch" "${dir}" \
        --set autoscaling.enableHpa=false \
        --set podDisruptionBudget.enablePdb=false \
        > "${batch_out}" 2>/tmp/helm-tpl-${svc}-batch.err; then
        if grep -q 'kind: HorizontalPodAutoscaler' "${batch_out}"; then
          fail "batch render ${svc} still has HPA"
        else
          pass "batch render ${svc} omits HPA"
        fi
        if grep -q 'kind: PodDisruptionBudget' "${batch_out}"; then
          fail "batch render ${svc} still has PDB"
        else
          pass "batch render ${svc} omits PDB"
        fi
      else
        fail "helm template batch ${svc}"
        cat /tmp/helm-tpl-${svc}-batch.err
      fi
    else
      fail "helm template ${svc}"
      cat /tmp/helm-tpl-${svc}.err
    fi
  done
else
  echo "==> helm not installed — skipped render tests (file checks only)"
fi

echo
echo "Results: ${PASS} passed, ${FAIL} failed"
if [[ "${FAIL}" -gt 0 ]]; then
  exit 1
fi
exit 0
