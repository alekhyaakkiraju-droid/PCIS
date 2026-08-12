#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
exec bash "${ROOT}/helm/scripts/deploy-platform.sh" \
  "${ROOT}/helm/charts/pcis-platform" \
  "${PCIS_NAMESPACE:-pcis-dev}" \
  "${PCIS_PLATFORM_RELEASE:-pcis-platform}"
