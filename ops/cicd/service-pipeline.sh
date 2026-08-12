#!/usr/bin/env bash
# Opsera C2C reference script — parameterized microservice build/push/deploy.
# Requires pcis-platform-dev-pipeline to have succeeded first.
#
# Required env: APP_NAME, REPO_URL, BRANCH, GIT_PAT, DOCKERFILE_PATH, ECR_REGISTRY,
#   ECR_REPO, AWS_REGION, AWS_ACCESS_KEY_ID, AWS_SECRET_ACCESS_KEY, EKS_CLUSTER,
#   DEPLOY_NAMESPACE, HELM_RELEASE, CHART_PATH
# Optional: INGRESS_HOST, SERVICE_LABEL (defaults to APP_NAME)
set -euo pipefail

trap 'echo "[OPSERA:complete:failed:${BASH_COMMAND} exit $?]"' ERR

SERVICE_LABEL="${SERVICE_LABEL:-${APP_NAME}}"

for tool in docker aws kubectl helm git curl jq; do
  command -v "$tool" >/dev/null 2>&1 || { echo "[OPSERA:complete:failed:Missing tool: $tool]"; exit 1; }
done

unset AWS_SESSION_TOKEN AWS_WEB_IDENTITY_TOKEN_FILE AWS_ROLE_ARN || true

if ! docker info >/dev/null 2>&1; then
  dockerd --storage-driver=vfs --data-root=/var/lib/docker >/var/log/dockerd.log 2>&1 &
  for i in $(seq 1 30); do docker info >/dev/null 2>&1 && break; sleep 2; done
  docker info >/dev/null 2>&1 || { echo "[OPSERA:complete:failed:Docker daemon failed]"; exit 1; }
fi

export AWS_DEFAULT_REGION="${AWS_REGION}"
export AWS_ACCESS_KEY_ID AWS_SECRET_ACCESS_KEY
aws sts get-caller-identity >/dev/null

if [[ -n "${LOCAL_REPO_DIR:-}" && -d "${LOCAL_REPO_DIR}" ]]; then
  cd "${LOCAL_REPO_DIR}"
  COMMIT_SHA="$(git rev-parse HEAD)"
else
  WORK_DIR="/tmp/c2c-${APP_NAME}"
  rm -rf "${WORK_DIR}"
  mkdir -p "${WORK_DIR}"
  cd "${WORK_DIR}"

  echo "[OPSERA:clone:running:Cloning ${REPO_URL} @ ${BRANCH}]"
  REPO_SLUG="${REPO_URL#https://github.com/}"
  REPO_SLUG="${REPO_SLUG%.git}"
  git clone --depth 1 --branch "${BRANCH}" "https://${GIT_PAT}@github.com/${REPO_SLUG}.git" repo
  cd repo
  COMMIT_SHA="$(git rev-parse HEAD)"
fi

IMAGE_TAG="dev-${COMMIT_SHA:0:8}-$(date -u +%Y%m%d%H%M%S)"
export IMAGE_TAG
echo "[OPSERA:clone:success:Using ${COMMIT_SHA:0:12} tag=${IMAGE_TAG}]"

DOCKER_PLATFORM="${DOCKER_PLATFORM:-linux/amd64}"
echo "[OPSERA:build:running:Building ${APP_NAME} image via ${DOCKERFILE_PATH} (${DOCKER_PLATFORM})]"
docker build --platform "${DOCKER_PLATFORM}" -f "${DOCKERFILE_PATH}" -t "${ECR_REPO}:${IMAGE_TAG}" .
echo "[OPSERA:build:success:Built ${ECR_REPO}:${IMAGE_TAG}]"

echo "[OPSERA:push:running:Push to ${ECR_REGISTRY}]"
aws ecr describe-repositories --repository-names "${ECR_REPO}" --region "${AWS_REGION}" >/dev/null 2>&1 || \
  aws ecr create-repository --repository-name "${ECR_REPO}" --image-scanning-configuration scanOnPush=true --region "${AWS_REGION}"
aws ecr get-login-password --region "${AWS_REGION}" | docker login --username AWS --password-stdin "${ECR_REGISTRY}"
docker tag "${ECR_REPO}:${IMAGE_TAG}" "${ECR_REGISTRY}/${ECR_REPO}:${IMAGE_TAG}"
docker push "${ECR_REGISTRY}/${ECR_REPO}:${IMAGE_TAG}"
echo "[OPSERA:push:success:Pushed ${ECR_REGISTRY}/${ECR_REPO}:${IMAGE_TAG}]"

echo "[OPSERA:deploy:running:Helm deploy ${HELM_RELEASE} to ${EKS_CLUSTER}/${DEPLOY_NAMESPACE}]"
aws eks update-kubeconfig --name "${EKS_CLUSTER}" --region "${AWS_REGION}" || true
kubectl create namespace "${DEPLOY_NAMESPACE}" --dry-run=client -o yaml | kubectl apply -f -

bash ops/bootstrap/preflight-platform.sh "${DEPLOY_NAMESPACE}"

echo "[OPSERA:deploy:running:Building Helm dependencies for ${CHART_PATH}]"
helm dependency build "${CHART_PATH}"

helm upgrade --install "${HELM_RELEASE}" "${CHART_PATH}" \
  --namespace "${DEPLOY_NAMESPACE}" \
  -f "${CHART_PATH}/values.yaml" \
  -f "${CHART_PATH}/values-dev.yaml" \
  --set image.repository="${ECR_REGISTRY}/${ECR_REPO}" \
  --set image.tag="${IMAGE_TAG}" \
  --set image.pullPolicy=Always \
  --timeout 10m \
  --wait=false

if ! kubectl rollout status "deployment/${HELM_RELEASE}" -n "${DEPLOY_NAMESPACE}" --timeout=600s; then
  echo "[OPSERA:deploy:failed:${APP_NAME} rollout did not complete]"
  kubectl get pods -n "${DEPLOY_NAMESPACE}" -l "app.kubernetes.io/name=${SERVICE_LABEL}" -o wide || true
  kubectl logs -n "${DEPLOY_NAMESPACE}" -l "app.kubernetes.io/name=${SERVICE_LABEL}" --tail=50 || true
  exit 1
fi

echo "[OPSERA_REPORT:{\"namespace\":\"${DEPLOY_NAMESPACE}\",\"cluster\":\"${EKS_CLUSTER}\",\"imageTag\":\"${IMAGE_TAG}\",\"release\":\"${HELM_RELEASE}\",\"service\":\"${APP_NAME}\"}]"
echo "[OPSERA:deploy:success:Deployed ${APP_NAME} to ${DEPLOY_NAMESPACE}]"
if [[ -n "${INGRESS_HOST:-}" ]]; then
  echo "[OPSERA:complete:success:https://${INGRESS_HOST}]"
else
  echo "[OPSERA:complete:success:${APP_NAME} deployed]"
fi
