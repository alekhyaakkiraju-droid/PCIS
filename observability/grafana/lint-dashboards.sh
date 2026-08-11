#!/usr/bin/env bash
# Validate Grafana dashboard JSON for WO-142 dashboards-as-code.
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
DASHBOARD_DIR="${SCRIPT_DIR}/dashboards"

if ! command -v jq >/dev/null 2>&1; then
  echo "ERROR: jq is required but not installed." >&2
  exit 1
fi

failures=0
checked=0

check_dashboard() {
  local file="$1"
  local base
  base="$(basename "${file}")"
  checked=$((checked + 1))

  echo "==> validating ${base}"

  if ! jq empty "${file}" >/dev/null 2>&1; then
    echo "  FAIL: invalid JSON" >&2
    failures=$((failures + 1))
    return
  fi

  local inputs_count
  inputs_count="$(jq '.__inputs | length' "${file}")"
  if [[ "${inputs_count}" -lt 1 ]]; then
    echo "  FAIL: missing __inputs datasource declaration" >&2
    failures=$((failures + 1))
  fi

  local has_prometheus_input
  has_prometheus_input="$(jq '[.__inputs[]? | select(.pluginId == "prometheus")] | length' "${file}")"
  if [[ "${has_prometheus_input}" -lt 1 ]]; then
    echo "  FAIL: __inputs must include a Prometheus datasource" >&2
    failures=$((failures + 1))
  fi

  local hardcoded_uid
  hardcoded_uid="$(jq '[.. | strings | select(test("^[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}$"))] | length' "${file}")"
  if [[ "${hardcoded_uid}" -gt 0 ]]; then
    echo "  FAIL: hardcoded datasource UID detected (use \${DS_PROMETHEUS})" >&2
    failures=$((failures + 1))
  fi

  local panel_count
  panel_count="$(jq '[.panels[]? | select(.type != "row")] | length' "${file}")"
  if [[ "${panel_count}" -lt 1 ]]; then
    echo "  FAIL: dashboard has no panels" >&2
    failures=$((failures + 1))
  fi

  local empty_titles
  empty_titles="$(jq '[.panels[]? | select(.title == "" or .title == null)] | length' "${file}")"
  if [[ "${empty_titles}" -gt 0 ]]; then
    echo "  FAIL: panel(s) with empty title" >&2
    failures=$((failures + 1))
  fi

  local title uid schema
  title="$(jq -r '.title // empty' "${file}")"
  uid="$(jq -r '.uid // empty' "${file}")"
  schema="$(jq -r '.schemaVersion // empty' "${file}")"

  if [[ -z "${title}" || -z "${uid}" || -z "${schema}" ]]; then
    echo "  FAIL: missing title, uid, or schemaVersion" >&2
    failures=$((failures + 1))
  else
    echo "  OK: title=${title}, uid=${uid}, schemaVersion=${schema}, panels=${panel_count}"
  fi
}

shopt -s nullglob
dashboard_files=("${DASHBOARD_DIR}"/*.json)
if [[ ${#dashboard_files[@]} -eq 0 ]]; then
  echo "ERROR: no dashboard JSON files found in ${DASHBOARD_DIR}" >&2
  exit 1
fi

for dashboard in "${dashboard_files[@]}"; do
  check_dashboard "${dashboard}"
done

if command -v grafana-dashboard-linter >/dev/null 2>&1; then
  echo "==> running grafana-dashboard-linter (optional)"
  for dashboard in "${dashboard_files[@]}"; do
    grafana-dashboard-linter "${dashboard}" || failures=$((failures + 1))
  done
else
  echo "==> grafana-dashboard-linter not installed; skipping optional lint"
fi

echo
echo "Checked ${checked} dashboard(s); failures=${failures}"
if [[ "${failures}" -gt 0 ]]; then
  exit 1
fi

echo "All dashboard validations passed."
