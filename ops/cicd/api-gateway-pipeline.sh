#!/usr/bin/env bash
# Opsera C2C reference script — pcis-api-gateway-dev-pipeline
# Requires pcis-platform-dev-pipeline to have succeeded first.
set -euo pipefail

export APP_NAME="${APP_NAME:-api-gateway}"
export DOCKERFILE_PATH="${DOCKERFILE_PATH:-services/api-gateway/Dockerfile}"
export ECR_REPO="${ECR_REPO:-opsera/pcis-api-gateway}"
export HELM_RELEASE="${HELM_RELEASE:-api-gateway}"
export CHART_PATH="${CHART_PATH:-helm/charts/api-gateway}"
export SERVICE_LABEL="${SERVICE_LABEL:-api-gateway}"
export INGRESS_HOST="${INGRESS_HOST:-pcis-api-gateway-dev.agent.opsera.dev}"

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
exec "${SCRIPT_DIR}/service-pipeline.sh"
