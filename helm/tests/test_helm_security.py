#!/usr/bin/env python3
"""File-content checks for PCIS Helm security defaults."""
from pathlib import Path
import sys

ROOT = Path(__file__).resolve().parents[2]
COMMON = ROOT / "helm" / "charts" / "pcis-common" / "templates"
SERVICES = [
    "customer-svc", "claims-svc", "policy-svc", "premium-svc",
    "billing-svc", "reporting-svc", "authz-svc", "audit-svc",
]
REQUIRED = [
    "runAsNonRoot",
    "readOnlyRootFilesystem",
    "allowPrivilegeEscalation: false",
    "/actuator/health/liveness",
    "/actuator/health/readiness",
    "/actuator/health/startup",
    "sidecar.istio.io/inject",
    "api-gateway",
]


def main() -> int:
    errors = []
    text = "\n".join(p.read_text() for p in COMMON.glob("*.tpl"))
    for s in REQUIRED:
        if s not in text:
            errors.append(f"library missing {s}")
    for svc in SERVICES:
        chart = ROOT / "helm" / "charts" / svc
        if "file://../pcis-common" not in (chart / "Chart.yaml").read_text():
            errors.append(f"{svc} missing dependency")
        if "minAvailable: 1" not in (chart / "values-dev.yaml").read_text():
            errors.append(f"{svc} values-dev minAvailable")
        if "minAvailable: 2" not in (chart / "values-prd.yaml").read_text():
            errors.append(f"{svc} values-prd minAvailable")
    if errors:
        print("FAILED:")
        for e in errors:
            print(" -", e)
        return 1
    print(f"OK: {len(REQUIRED)} security strings + {len(SERVICES)} service charts")
    return 0


if __name__ == "__main__":
    sys.exit(main())
