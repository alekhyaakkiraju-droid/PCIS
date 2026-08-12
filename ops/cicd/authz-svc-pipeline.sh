#!/usr/bin/env bash
# Opsera C2C reference script — pcis-authz-svc-dev-pipeline
# Requires pcis-platform-dev-pipeline to have succeeded first.
set -euo pipefail

export APP_NAME="${APP_NAME:-authz-svc}"
export DOCKERFILE_PATH="${DOCKERFILE_PATH:-services/authz-svc/Dockerfile}"
export ECR_REPO="${ECR_REPO:-opsera/pcis-authz-svc}"
export HELM_RELEASE="${HELM_RELEASE:-authz-svc}"
export CHART_PATH="${CHART_PATH:-helm/charts/authz-svc}"
export SERVICE_LABEL="${SERVICE_LABEL:-authz-svc}"

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
exec "${SCRIPT_DIR}/service-pipeline.sh"
