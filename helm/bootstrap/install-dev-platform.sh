#!/usr/bin/env bash
# Install PCIS platform bootstrap (PostgreSQL, Redis, Keycloak, Kafka) into pcis-dev.
# Run once before microservice Helm releases or Argo CD ApplicationSet sync.
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
CHART="${ROOT}/helm/charts/pcis-platform"
NAMESPACE="${PCIS_NAMESPACE:-pcis-dev}"
RELEASE="${PCIS_PLATFORM_RELEASE:-pcis-platform}"
VALUES="${CHART}/values-dev.yaml"

kubectl create namespace "${NAMESPACE}" --dry-run=client -o yaml | kubectl apply -f -

helm upgrade --install "${RELEASE}" "${CHART}" \
  --namespace "${NAMESPACE}" \
  --create-namespace \
  -f "${CHART}/values.yaml" \
  -f "${VALUES}" \
  --wait \
  --timeout 20m

echo "Platform bootstrap complete. See helm template notes:"
helm get notes "${RELEASE}" -n "${NAMESPACE}" 2>/dev/null || true
