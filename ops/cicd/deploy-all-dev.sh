#!/usr/bin/env bash
# Fastest path: parallel amd64 builds → ECR push → Helm deploy all PCIS microservices to pcis-dev.
#
# Prerequisites:
#   - Platform bootstrap healthy (postgresql, redis, keycloak in pcis-dev)
#   - AWS profile with ECR push to 792373136340 (default: opsera-dev)
#   - docker, aws, kubectl, helm, git
#
# Usage:
#   AWS_PROFILE=opsera-dev ./ops/cicd/deploy-all-dev.sh
#   SKIP=api-gateway,authz-svc ./ops/cicd/deploy-all-dev.sh   # resume partial deploy
#   BUILD_PARALLEL=3 ./ops/cicd/deploy-all-dev.sh             # limit concurrent builds
#
# Typical runtime: ~25–40 min (parallel builds) + ~15 min rollouts.
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "${ROOT}"

AWS_PROFILE="${AWS_PROFILE:-opsera-dev}"
AWS_REGION="${AWS_REGION:-us-west-2}"
ECR_REGISTRY="${ECR_REGISTRY:-792373136340.dkr.ecr.us-west-2.amazonaws.com}"
NAMESPACE="${DEPLOY_NAMESPACE:-pcis-dev}"
DOCKER_PLATFORM="${DOCKER_PLATFORM:-linux/amd64}"
BUILD_PARALLEL="${BUILD_PARALLEL:-4}"
SHARED_VALUES="${ROOT}/helm/values-dev-shared.yaml"
COMMIT_SHA="$(git rev-parse --short HEAD)"
IMAGE_TAG="dev-${COMMIT_SHA}-$(date -u +%Y%m%d%H%M%S)-amd64"

# service:dockerfile:ecr-repo-name
SERVICES=(
  "authz-svc:services/authz-svc/Dockerfile:opsera/pcis-authz-svc"
  "config-svc:services/config-svc/Dockerfile:opsera/pcis-config-svc"
  "customer-svc:services/customer-svc/Dockerfile:opsera/pcis-customer-svc"
  "policy-svc:services/policy-svc/Dockerfile:opsera/pcis-policy-svc"
  "claims-svc:services/claims-svc/Dockerfile:opsera/pcis-claims-svc"
  "billing-svc:services/billing-svc/Dockerfile:opsera/pcis-billing-svc"
  "premium-svc:services/premium-svc/Dockerfile:opsera/pcis-premium-svc"
  "audit-svc:services/audit-svc/Dockerfile:opsera/pcis-audit-svc"
  "reporting-svc:services/reporting-svc/Dockerfile:opsera/pcis-reporting-svc"
  "sync-agent:services/sync-agent/Dockerfile:opsera/pcis-sync-agent"
  "api-gateway:services/api-gateway/Dockerfile:opsera/pcis-api-gateway"
)

SKIP_LIST=()
if [[ -n "${SKIP:-}" ]]; then
  IFS=',' read -ra SKIP_LIST <<< "${SKIP}"
fi

should_skip() {
  local name="$1"
  if ((${#SKIP_LIST[@]} == 0)); then
    return 1
  fi
  for s in "${SKIP_LIST[@]}"; do
    [[ -z "${s}" ]] && continue
    [[ "${s}" == "${name}" ]] && return 0
  done
  return 1
}

log() { echo "[deploy-all] $*"; }

for tool in docker aws kubectl helm git; do
  command -v "$tool" >/dev/null || { echo "Missing: $tool"; exit 1; }
done

export AWS_PROFILE AWS_REGION
aws sts get-caller-identity >/dev/null
bash ops/bootstrap/preflight-platform.sh "${NAMESPACE}"

log "Image tag: ${IMAGE_TAG}  platform: ${DOCKER_PLATFORM}  parallel: ${BUILD_PARALLEL}"

AWS_PROFILE="${AWS_PROFILE}" aws ecr get-login-password --region "${AWS_REGION}" \
  | docker login --username AWS --password-stdin "${ECR_REGISTRY}"

build_one() {
  local name="$1" dockerfile="$2" ecr_repo="$3"
  local full="${ECR_REGISTRY}/${ecr_repo}:${IMAGE_TAG}"
  log "BUILD ${name} → ${full}"
  docker build --platform "${DOCKER_PLATFORM}" -f "${dockerfile}" -t "${full}" .
  echo "${name}|${full}|${ecr_repo}" >> /tmp/pcis-deploy-images.$$
}

: > /tmp/pcis-deploy-images.$$
trap 'rm -f /tmp/pcis-deploy-images.$$' EXIT

active=0
for entry in "${SERVICES[@]}"; do
  IFS=':' read -r name dockerfile ecr_repo <<< "${entry}"
  should_skip "${name}" && { log "SKIP ${name}"; continue; }

  while (( active >= BUILD_PARALLEL )); do
    wait -n 2>/dev/null || wait
    ((active--)) || true
  done

  build_one "${name}" "${dockerfile}" "${ecr_repo}" &
  ((active++)) || true
done
while (( active > 0 )); do
  wait -n 2>/dev/null || wait
  ((active--)) || true
done

log "Pushing images..."
while IFS='|' read -r name full ecr_repo; do
  [[ -z "${name}" ]] && continue
  AWS_PROFILE="${AWS_PROFILE}" aws ecr describe-repositories --repository-names "${ecr_repo}" --region "${AWS_REGION}" >/dev/null 2>&1 || \
    AWS_PROFILE="${AWS_PROFILE}" aws ecr create-repository --repository-name "${ecr_repo}" \
      --image-scanning-configuration scanOnPush=true --region "${AWS_REGION}" >/dev/null
  docker push "${full}"
done < /tmp/pcis-deploy-images.$$

deploy_one() {
  local name="$1" full="$2" ecr_repo="$3"
  local chart="${ROOT}/helm/charts/${name}"
  log "HELM ${name}"
  helm dependency build "${chart}" >/dev/null
  helm upgrade --install "${name}" "${chart}" \
    --namespace "${NAMESPACE}" \
    -f "${chart}/values.yaml" \
    -f "${chart}/values-dev.yaml" \
    -f "${SHARED_VALUES}" \
    --set "image.repository=${ECR_REGISTRY}/${ecr_repo}" \
    --set "image.tag=${IMAGE_TAG}" \
    --set "image.pullPolicy=Always" \
    --timeout 10m --wait=false
}

# Wave 1: foundation
for wave1 in authz-svc config-svc; do
  line="$(grep "^${wave1}|" /tmp/pcis-deploy-images.$$ || true)"
  [[ -z "${line}" ]] && continue
  IFS='|' read -r name full ecr_repo <<< "${line}"
  deploy_one "${name}" "${full}" "${ecr_repo}"
done

# Wave 2: domain + sync (parallel helm ok)
while IFS='|' read -r name full ecr_repo; do
  [[ -z "${name}" ]] && continue
  [[ "${name}" == "authz-svc" || "${name}" == "config-svc" || "${name}" == "api-gateway" ]] && continue
  deploy_one "${name}" "${full}" "${ecr_repo}" &
done < /tmp/pcis-deploy-images.$$
wait

# Wave 3: api-gateway last
line="$(grep "^api-gateway|" /tmp/pcis-deploy-images.$$ || true)"
if [[ -n "${line}" ]]; then
  IFS='|' read -r name full ecr_repo <<< "${line}"
  deploy_one "${name}" "${full}" "${ecr_repo}"
fi

log "Waiting for rollouts..."
failed=0
while IFS='|' read -r name _ _; do
  [[ -z "${name}" ]] && continue
  if kubectl rollout status "deployment/${name}" -n "${NAMESPACE}" --timeout=600s; then
    log "OK ${name}"
  else
    log "FAILED ${name}"
    kubectl logs -n "${NAMESPACE}" -l "app.kubernetes.io/name=${name}" --tail=30 || true
    failed=1
  fi
done < /tmp/pcis-deploy-images.$$

kubectl get pods -n "${NAMESPACE}" -o wide
if [[ "${failed}" -eq 0 ]]; then
  log "Done. Gateway: https://pcis-api-gateway-dev.agent.opsera.dev/actuator/health"
else
  log "Some rollouts failed — check logs above"
  exit 1
fi
