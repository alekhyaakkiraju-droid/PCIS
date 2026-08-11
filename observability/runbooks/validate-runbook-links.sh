#!/usr/bin/env bash
# Validate runbook_url paths in alerting-rules.yaml exist and carry required H2 sections (WO-144).
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
ALERTING="${ROOT}/observability/prometheus/alerting-rules.yaml"

REQUIRED_HEADERS=(
  "Trigger and Alert Reference"
  "Severity and First Responder"
  "Prerequisites"
  "Diagnostic Queries and Log Filters"
  "Step-by-Step Recovery"
  "Verification, Escalation, and Post-Incident"
)

errors=()

if [[ ! -f "${ALERTING}" ]]; then
  echo "ERROR: missing ${ALERTING}"
  exit 1
fi

check_runbook_file() {
  local path="$1"
  local full_path="${ROOT}/${path}"

  if [[ ! -f "${full_path}" ]]; then
    errors+=("missing runbook file: ${path}")
    return
  fi

  local content
  content="$(<"${full_path}")"
  local header
  for header in "${REQUIRED_HEADERS[@]}"; do
    if ! grep -q "^## ${header}" <<<"${content}"; then
      errors+=("${path}: missing required H2 header '## ${header}'")
    fi
  done
}

while IFS= read -r line; do
  if [[ "${line}" =~ runbook_url:[[:space:]]*\"([^\"]+)\" ]]; then
    url="${BASH_REMATCH[1]}"
    path="${url%%#*}"

    if [[ "${path}" != observability/runbooks/* ]]; then
      errors+=("runbook_url must use observability/runbooks/ prefix: ${url}")
      continue
    fi

    check_runbook_file "${path}"
  fi
done < "${ALERTING}"

expected_runbooks=(
  observability/runbooks/api-read-latency-high.md
  observability/runbooks/api-write-latency-high.md
  observability/runbooks/error-rate-high.md
  observability/runbooks/batch-job-failed.md
  observability/runbooks/batch-window-breached.md
  observability/runbooks/audit-outbox-lag-high.md
  observability/runbooks/audit-outbox-backlog.md
  observability/runbooks/certificate-expiry-soon.md
  observability/runbooks/secret-rotation-overdue.md
)

for rb in "${expected_runbooks[@]}"; do
  if [[ ! -f "${ROOT}/${rb}" ]]; then
    errors+=("expected runbook missing: ${rb}")
  fi
done

if ((${#errors[@]} > 0)); then
  echo "FAILED: runbook link validation"
  printf ' - %s\n' "${errors[@]}"
  exit 1
fi

count="$(grep -c 'runbook_url:' "${ALERTING}" || true)"
echo "OK: ${count} runbook_url entries validated; ${#expected_runbooks[@]} SLO runbooks present with required H2 sections"
exit 0
