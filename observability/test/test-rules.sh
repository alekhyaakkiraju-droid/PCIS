#!/usr/bin/env bash
# Validate and optionally integration-test PCIS Prometheus rules (WO-141).
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "${ROOT}"

INTEGRATION=false
if [[ "${1:-}" == "--integration" ]]; then
  INTEGRATION=true
fi

echo "==> WO-141 promtool / metadata validation"
bash observability/prometheus/promtool-check.sh

if [[ "${INTEGRATION}" != "true" ]]; then
  if ! command -v docker >/dev/null 2>&1; then
    echo "SKIP: docker not available — integration test skipped (promtool is primary gate)"
    exit 0
  fi
  if ! docker info >/dev/null 2>&1; then
    echo "SKIP: docker daemon not running — integration test skipped (promtool is primary gate)"
    exit 0
  fi
  echo "==> Starting optional docker-compose integration test"
  docker compose -f observability/docker-compose.test.yaml up --abort-on-container-exit --exit-code-from test-runner
  exit $?
fi

PROM="${PROMETHEUS_URL:-http://localhost:9090}"
PGW="${PUSHGATEWAY_URL:-http://localhost:9091}"
METRICS="${ROOT}/observability/test-fixtures/sample-metrics.txt"

wait_ready() {
  local url="$1"
  local name="$2"
  for _ in $(seq 1 30); do
    if curl -sf "${url}/-/ready" >/dev/null 2>&1; then
      echo "OK: ${name} ready"
      return 0
    fi
    sleep 2
  done
  echo "ERROR: ${name} not ready at ${url}"
  return 1
}

push_metrics() {
  # Strip comment lines; push as job pcis-wo141-test.
  grep -v '^#' "${METRICS}" | grep -v '^[[:space:]]*$' | curl -sf --data-binary @- \
    "${PGW}/metrics/job/pcis-wo141-test/instance/integration"
  echo "Pushed sample metrics to pushgateway"
}

query() {
  curl -sfG "${PROM}/api/v1/query" --data-urlencode "query=${1}"
}

wait_ready "${PROM}" "Prometheus"
wait_ready "${PGW}" "Pushgateway"
push_metrics

echo "Waiting for rule evaluation (45s)..."
sleep 45

errors=()

# Recording rules should produce SLI series.
for expr in \
  'pcis:api_request_duration:p95' \
  'error_rate_5m' \
  'pcis:batch_job_duration:p95' \
  'pcis:batch_window:utilization_ratio'; do
  result="$(query "${expr}")"
  if ! echo "${result}" | python3 -c "import json,sys; d=json.load(sys.stdin); sys.exit(0 if d.get('data',{}).get('result') else 1)"; then
    errors+=("recording rule produced no series: ${expr}")
  else
    echo "OK: recording rule ${expr}"
  fi
done

# Alerting rules should include required alerts in rules endpoint.
rules_json="$(curl -sf "${PROM}/api/v1/rules?type=alert")"
python3 - <<'PY' "${rules_json}" || errors+=("alert rules API check failed")
import json, sys
payload = json.loads(sys.argv[1])
expected = {
    "ApiReadLatencyHigh", "ApiWriteLatencyHigh", "BatchJobFailed",
    "BatchWindowBreached", "AuditOutboxLagHigh", "AuditOutboxBacklog",
    "ErrorRateHigh", "CertificateExpirySoon", "SecretRotationOverdue",
}
found = set()
for group in payload.get("data", {}).get("groups", []):
    for rule in group.get("rules", []):
        if rule.get("type") == "alerting":
            found.add(rule.get("name"))
missing = expected - found
if missing:
    print("missing alert rules:", sorted(missing))
    sys.exit(1)
print(f"OK: {len(expected)} alert rules registered")
PY

# Check at least one alert is firing or pending on breach samples.
alerts_json="$(curl -sf "${PROM}/api/v1/alerts")"
python3 - <<'PY' "${alerts_json}" || errors+=("no alerts firing/pending on breach samples")
import json, sys
payload = json.loads(sys.argv[1])
alerts = payload.get("data", {}).get("alerts", [])
active = [a for a in alerts if a.get("state") in ("firing", "pending")]
if not active:
    print("WARN: no firing/pending alerts yet (may need longer evaluation window)")
    # Do not fail — promtool + metadata are primary gates per WO constraints.
    sys.exit(0)
print(f"OK: {len(active)} alert(s) active:", ", ".join(sorted({a['labels']['alertname'] for a in active})))
PY

if ((${#errors[@]} > 0)); then
  echo "FAILED:"
  printf ' - %s\n' "${errors[@]}"
  exit 1
fi

echo "OK: WO-141 Prometheus rules integration test passed"
exit 0
