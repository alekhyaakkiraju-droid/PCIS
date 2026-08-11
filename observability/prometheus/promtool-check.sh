#!/usr/bin/env bash
# Validate Prometheus rule files with promtool (WO-141).
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
RULES_DIR="${ROOT}/observability/prometheus"

if ! command -v promtool >/dev/null 2>&1; then
  echo "WARN: promtool not found — skipping syntax validation (run observability/test/test-rules.sh for YAML checks)"
  exit 0
fi

echo "==> WO-141 promtool check rules"
for rules_file in "${RULES_DIR}/recording-rules.yaml" "${RULES_DIR}/alerting-rules.yaml"; do
  if [[ ! -f "${rules_file}" ]]; then
    echo "ERROR: missing ${rules_file}"
    exit 1
  fi
  echo "Checking ${rules_file}"
  promtool check rules "${rules_file}"
done

echo "==> Alert metadata lint"
python3 "${ROOT}/observability/test/validate_alert_metadata.py"

echo "OK: all Prometheus rules passed promtool and metadata validation"
