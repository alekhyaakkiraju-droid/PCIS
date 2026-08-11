#!/usr/bin/env python3
"""Static validation for Alertmanager YAML when amtool is unavailable (WO-143)."""
from __future__ import annotations

import sys
from pathlib import Path

try:
    import yaml
except ImportError:  # pragma: no cover - stdlib fallback path
    yaml = None


REQUIRED_TOP_LEVEL = {"route", "receivers", "inhibit_rules"}
REQUIRED_RECEIVERS = {"pagerduty-critical", "slack-warning"}
REQUIRED_INHIBITIONS = {
    ("BatchJobFailed", "BatchWindowBreached", ("job_name",)),
    ("AuditOutboxLagHigh", "AuditOutboxBacklog", ("service",)),
}


def load_yaml(path: Path) -> dict:
    text = path.read_text()
    if yaml is not None:
        data = yaml.safe_load(text)
    else:
        # Minimal structural check without PyYAML.
        for key in REQUIRED_TOP_LEVEL:
            if f"{key}:" not in text:
                raise ValueError(f"missing top-level key: {key}")
        return {"_static_only": True}
    if not isinstance(data, dict):
        raise ValueError(f"{path}: root must be a mapping")
    return data


def validate_config(config_path: Path, templates_dir: Path) -> list[str]:
    errors: list[str] = []
    data = load_yaml(config_path)

    if data.get("_static_only"):
        if not (templates_dir / "pcis.tmpl").is_file():
            errors.append("missing templates/pcis.tmpl")
        return errors

    missing = REQUIRED_TOP_LEVEL - set(data)
    if missing:
        errors.append(f"missing keys: {sorted(missing)}")

    receivers = {r.get("name") for r in data.get("receivers", []) if isinstance(r, dict)}
    missing_receivers = REQUIRED_RECEIVERS - receivers
    if missing_receivers:
        errors.append(f"missing receivers: {sorted(missing_receivers)}")

    route = data.get("route", {})
    if route.get("repeat_interval") != "4h":
        errors.append("route.repeat_interval must be 4h to limit alert fatigue")

    group_by = route.get("group_by") or []
    for label in ("alertname", "service", "job_name"):
        if label not in group_by:
            errors.append(f"route.group_by must include {label}")

    found_inhibitions = set()
    for rule in data.get("inhibit_rules", []):
        if not isinstance(rule, dict):
            continue
        src = rule.get("source_matchers") or []
        tgt = rule.get("target_matchers") or []
        equal = tuple(rule.get("equal") or [])
        src_name = next((m.split("=", 1)[1].strip('"') for m in src if m.startswith("alertname=")), None)
        tgt_name = next((m.split("=", 1)[1].strip('"') for m in tgt if m.startswith("alertname=")), None)
        if src_name and tgt_name:
            found_inhibitions.add((src_name, tgt_name, equal))

    for expected in REQUIRED_INHIBITIONS:
        if expected not in found_inhibitions:
            errors.append(f"missing inhibition rule: {expected[0]} inhibits {expected[1]}")

    critical_route = False
    for sub in route.get("routes") or []:
        if not isinstance(sub, dict):
            continue
        if sub.get("receiver") == "pagerduty-critical":
            matchers = sub.get("matchers") or []
            if any('severity="critical"' in m for m in matchers):
                critical_route = True
    if not critical_route:
        errors.append("missing route to pagerduty-critical for severity=critical")

    tmpl = templates_dir / "pcis.tmpl"
    if not tmpl.is_file():
        errors.append("missing templates/pcis.tmpl")
    else:
        content = tmpl.read_text()
        for fragment in ("pcis.slack.title", "pcis.pagerduty.description", "runbook_url"):
            if fragment not in content:
                errors.append(f"pcis.tmpl missing fragment: {fragment}")

    return errors


def main() -> int:
    if len(sys.argv) != 3:
        print("usage: validate-alertmanager.py <alertmanager.yaml> <templates-dir>")
        return 2

    config_path = Path(sys.argv[1])
    templates_dir = Path(sys.argv[2])
    errors = validate_config(config_path, templates_dir)
    if errors:
        print("FAILED:")
        for err in errors:
            print(f" - {err}")
        return 1

    print("OK: Alertmanager YAML passed static validation (amtool not installed)")
    return 0


if __name__ == "__main__":
    sys.exit(main())
