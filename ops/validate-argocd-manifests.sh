#!/usr/bin/env bash
# WO-139 — validate Argo CD ApplicationSet manifests against helm/charts/
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
exec python3 "${ROOT}/ops/validate-argocd-manifests.py" --repo-root "${ROOT}"
