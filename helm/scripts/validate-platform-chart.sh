#!/usr/bin/env bash
# Validate pcis-platform chart: template render + Deployment/StatefulSet selector parity.
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
CHART="${ROOT}/helm/charts/pcis-platform"

helm template pcis-platform "${CHART}" \
  -f "${CHART}/values.yaml" \
  -f "${CHART}/values-dev.yaml" \
  > /tmp/pcis-platform-render.yaml

python3 <<'PY'
import sys

try:
    import yaml
except ImportError:
    print("SKIP: PyYAML not installed; helm template succeeded")
    sys.exit(0)

docs = list(yaml.safe_load_all(open("/tmp/pcis-platform-render.yaml")))
errors = []
for d in docs:
    if not d or d.get("kind") not in ("Deployment", "StatefulSet"):
        continue
    name = d["metadata"]["name"]
    sel = d["spec"]["selector"]["matchLabels"]
    tmpl = d["spec"]["template"]["metadata"]["labels"]
    for key, val in sel.items():
        if tmpl.get(key) != val:
            errors.append(f"{d['kind']}/{name}: selector {key}={val!r} != template {tmpl.get(key)!r}")
if errors:
    print("VALIDATION FAILED:")
    print("\n".join(errors))
    sys.exit(1)
print("pcis-platform chart validation passed")
PY
