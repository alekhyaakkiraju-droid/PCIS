#!/usr/bin/env bash
# Validate Alertmanager configuration (WO-143).
# Uses amtool when available; falls back to static YAML structure checks.
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
CONFIG="${ROOT}/observability/alertmanager/alertmanager.yaml"
TEMPLATES_DIR="${ROOT}/observability/alertmanager/templates"

if [[ ! -f "${CONFIG}" ]]; then
  echo "ERROR: missing ${CONFIG}"
  exit 1
fi

if ! command -v amtool >/dev/null 2>&1; then
  echo "WARN: amtool not found — running static Alertmanager YAML validation"
  python3 "${ROOT}/observability/alertmanager/validate-alertmanager.py" "${CONFIG}" "${TEMPLATES_DIR}"
  exit $?
fi

echo "==> WO-143 amtool check-config"
amtool check-config "${CONFIG}"

if [[ -d "${TEMPLATES_DIR}" ]]; then
  for tmpl in "${TEMPLATES_DIR}"/*.tmpl; do
    [[ -f "${tmpl}" ]] || continue
    echo "Checking template ${tmpl}"
    amtool template render --template.glob="${tmpl}" --template.text='{{ template "pcis.slack.title" . }}' \
      --template.data="${ROOT}/observability/alertmanager/test-fixtures/sample-alert.json" >/dev/null
  done
fi

echo "OK: Alertmanager configuration passed amtool validation"
