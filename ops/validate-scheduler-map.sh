#!/usr/bin/env bash
# Validate consistency between ops/scheduler-map.yaml and helm/charts/pcis-batch/values.yaml (WO-137).
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "${ROOT}"

echo "==> WO-003 scheduler map validation"
python3 ops/validate-scheduler-map.py

echo "==> WO-137 scheduler map ↔ Helm values consistency"
python3 - <<'PY'
import re
import sys
from pathlib import Path

root = Path(".")
sched = (root / "ops/scheduler-map.yaml").read_text()
values = (root / "helm/charts/pcis-batch/values.yaml").read_text()

# Extract kubernetes_cronjobs keys from scheduler-map
cron_section = sched.split("kubernetes_cronjobs:", 1)
if len(cron_section) < 2:
    sys.exit("scheduler-map.yaml missing kubernetes_cronjobs section")
block = cron_section[1].split("\nexit_code_contract:", 1)[0]
map_names = set(re.findall(r"^  ([a-z0-9-]+):\n", block, re.M))

# Extract job names from Helm values
helm_names = set(re.findall(r'^\s+- name: ([a-z0-9-]+)\s*$', values, re.M))

expected = {
    "audit-archive-job",
    "billing-installment-job",
    "claim-payment-job",
    "commission-calc-job",
    "premium-processing-job",
    "policy-renewal-job",
}

errors = []
if map_names != expected:
    errors.append(f"kubernetes_cronjobs names {sorted(map_names)} != expected {sorted(expected)}")
if helm_names != expected:
    errors.append(f"Helm jobs names {sorted(helm_names)} != expected {sorted(expected)}")
if map_names != helm_names:
    errors.append(f"Mismatch map {sorted(map_names)} vs helm {sorted(helm_names)}")

# Schedule parity: each map schedule appears in helm values for same job
for name in expected:
    m = re.search(rf"  {name}:\n(?:    .+\n)*?    schedule: \"([^\"]+)\"", block)
    h = re.search(rf"- name: {name}\n(?:    .+\n)*?    schedule: \"([^\"]+)\"", values)
    if not m or not h:
        errors.append(f"Could not parse schedule for {name}")
        continue
    if m.group(1) != h.group(1):
        errors.append(f"Schedule mismatch for {name}: map={m.group(1)} helm={h.group(1)}")

if errors:
    print("FAILED:")
    for e in errors:
        print(" -", e)
    sys.exit(1)

print(f"OK: {len(expected)} CronJobs consistent between scheduler-map and Helm values")
PY

echo "==> All scheduler validations passed"
