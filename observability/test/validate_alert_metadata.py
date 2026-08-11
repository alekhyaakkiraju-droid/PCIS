#!/usr/bin/env python3
"""Validate PCIS alert rules carry required severity and runbook_url (WO-141)."""
from __future__ import annotations

import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
ALERTING = ROOT / "observability/prometheus/alerting-rules.yaml"
EXPECTED_ALERTS = {
    "ApiReadLatencyHigh",
    "ApiWriteLatencyHigh",
    "BatchJobFailed",
    "BatchWindowBreached",
    "AuditOutboxLagHigh",
    "AuditOutboxBacklog",
    "ErrorRateHigh",
    "CertificateExpirySoon",
    "SecretRotationOverdue",
}
VALID_SEVERITIES = {"critical", "warning", "info"}


def parse_alerts(text: str) -> dict[str, dict[str, str]]:
    alerts: dict[str, dict[str, str]] = {}
    current: str | None = None
    section: str | None = None

    for raw_line in text.splitlines():
        line = raw_line.strip()
        if line.startswith("- alert:"):
            current = line.split(":", 1)[1].strip()
            alerts[current] = {"severity": "", "runbook_url": "", "summary": ""}
            section = None
            continue
        if current is None:
            continue
        if line.startswith("severity:"):
            alerts[current]["severity"] = line.split(":", 1)[1].strip()
            section = "labels"
        elif line.startswith("runbook_url:"):
            alerts[current]["runbook_url"] = line.split(":", 1)[1].strip().strip('"')
            section = "annotations"
        elif line.startswith("summary:"):
            alerts[current]["summary"] = line.split(":", 1)[1].strip().strip('"')
            section = "annotations"
        elif line.startswith("labels:"):
            section = "labels"
        elif line.startswith("annotations:"):
            section = "annotations"

    return alerts


def main() -> int:
    alerts = parse_alerts(ALERTING.read_text())
    errors: list[str] = []

    missing = EXPECTED_ALERTS - set(alerts)
    extra = set(alerts) - EXPECTED_ALERTS
    if missing:
        errors.append(f"missing alerts: {sorted(missing)}")
    if extra:
        errors.append(f"unexpected alerts: {sorted(extra)}")

    for name, fields in alerts.items():
        if fields["severity"] not in VALID_SEVERITIES:
            errors.append(f"{name}: missing or invalid severity label")
        if not fields["runbook_url"]:
            errors.append(f"{name}: missing runbook_url annotation")
        if not fields["summary"]:
            errors.append(f"{name}: missing summary annotation")

    if errors:
        print("FAILED:")
        for err in errors:
            print(f" - {err}")
        return 1

    print(f"OK: {len(EXPECTED_ALERTS)} alerts validated (severity + runbook_url + summary)")
    return 0


if __name__ == "__main__":
    sys.exit(main())
