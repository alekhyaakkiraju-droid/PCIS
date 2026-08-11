#!/usr/bin/env python3
"""Validate pcis-batch Helm chart rendering (WO-137)."""
from __future__ import annotations

import re
import subprocess
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
CHART = ROOT / "helm" / "charts" / "pcis-batch"
EXPECTED_JOBS = [
    "audit-archive-job",
    "billing-installment-job",
    "claim-payment-job",
    "commission-calc-job",
    "premium-processing-job",
    "policy-renewal-job",
]
REQUIRED_STRINGS = [
    "kind: CronJob",
    "concurrencyPolicy: Forbid",
    "startingDeadlineSeconds: 300",
    "backoffLimit: 0",
    "restartPolicy: Never",
    "runAsNonRoot: true",
    "readOnlyRootFilesystem: true",
    "allowPrivilegeEscalation: false",
    "node-role: batch",
    "key: batch",
    "effect: NoSchedule",
    "mountPath: /tmp",
    "placeholder-pending-phase0-baseline",
    "PcisBatchJobFailed",
    "severity: critical",
]


def main() -> int:
    errors: list[str] = []

    dep = subprocess.run(
        ["helm", "dependency", "update", str(CHART)],
        capture_output=True,
        text=True,
    )
    if dep.returncode != 0:
        print(dep.stderr or dep.stdout)
        return 1

    render = subprocess.run(
        [
            "helm", "template", "test-batch", str(CHART),
            "-f", str(CHART / "values.yaml"),
            "-f", str(CHART / "values-dev.yaml"),
        ],
        capture_output=True,
        text=True,
    )
    if render.returncode != 0:
        print(render.stderr or render.stdout)
        return 1

    out = render.stdout
    for s in REQUIRED_STRINGS:
        if s not in out:
            errors.append(f"render missing: {s}")

    cronjobs = re.findall(r"^  name: ([a-z0-9-]+)$", out, re.M)
    cronjob_names = [n for n in cronjobs if n.endswith("-job")]
    if sorted(set(cronjob_names)) != sorted(EXPECTED_JOBS):
        errors.append(f"CronJob names {cronjob_names} != expected {EXPECTED_JOBS}")

    cron_count = out.count("kind: CronJob")
    if cron_count != 6:
        errors.append(f"expected 6 CronJobs, got {cron_count}")

    sa_count = out.count("kind: ServiceAccount")
    if sa_count != 6:
        errors.append(f"expected 6 ServiceAccounts, got {sa_count}")

    if "kind: PrometheusRule" not in out:
        errors.append("missing PrometheusRule")

    if errors:
        print("FAILED:")
        for e in errors:
            print(" -", e)
        return 1

    print(f"OK: pcis-batch chart — 6 CronJobs, security context, tolerations, alerts")
    return 0


if __name__ == "__main__":
    sys.exit(main())
