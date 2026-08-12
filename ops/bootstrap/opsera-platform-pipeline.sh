#!/usr/bin/env bash
# Opsera C2C workflow script — pcis-platform-dev-pipeline
# Stages: clone → deploy (no image build). Import via Opsera portal after pushing to main.
set -euo pipefail

trap 'echo "[OPSERA:complete:failed:${BASH_COMMAND} exit $?]"' ERR

for tool in aws kubectl helm git; do
  command -v "$tool" >/dev/null 2>&1 || { echo "[OPSERA:complete:failed:Missing tool: $tool]"; exit 1; }
done

unset AWS_SESSION_TOKEN AWS_WEB_IDENTITY_TOKEN_FILE AWS_ROLE_ARN || true

export AWS_DEFAULT_REGION="${AWS_REGION}"
export AWS_ACCESS_KEY_ID AWS_SECRET_ACCESS_KEY
aws sts get-caller-identity >/dev/null

WORK_DIR="/tmp/c2c-${APP_NAME}-platform"
rm -rf "${WORK_DIR}"
mkdir -p "${WORK_DIR}"
cd "${WORK_DIR}"

# ═══ JOB: clone ═══
echo "[OPSERA:clone:running:Cloning ${REPO_URL} @ ${BRANCH}]"
REPO_SLUG="${REPO_URL#https://github.com/}"
REPO_SLUG="${REPO_SLUG%.git}"
git clone --depth 1 --branch "${BRANCH}" "https://${GIT_PAT}@github.com/${REPO_SLUG}.git" repo
cd repo
COMMIT_SHA="$(git rev-parse HEAD)"
echo "[OPSERA:clone:success:Cloned ${COMMIT_SHA:0:12}]"

# ═══ JOB: deploy ═══
echo "[OPSERA:deploy:running:Platform bootstrap ${HELM_RELEASE} → ${EKS_CLUSTER}/${DEPLOY_NAMESPACE}]"
aws eks update-kubeconfig --name "${EKS_CLUSTER}" --region "${AWS_REGION}"
kubectl create namespace "${DEPLOY_NAMESPACE}" --dry-run=client -o yaml | kubectl apply -f -

helm upgrade --install "${HELM_RELEASE}" "${CHART_PATH}" \
  --namespace "${DEPLOY_NAMESPACE}" \
  --create-namespace \
  -f "${CHART_PATH}/values.yaml" \
  -f "${CHART_PATH}/values-dev.yaml" \
  --wait --timeout 20m

kubectl rollout status statefulset/postgresql -n "${DEPLOY_NAMESPACE}" --timeout=600s || true
kubectl rollout status deployment/redis -n "${DEPLOY_NAMESPACE}" --timeout=300s || true
kubectl rollout status deployment/keycloak -n "${DEPLOY_NAMESPACE}" --timeout=600s || true
kubectl rollout status deployment/kafka -n "${DEPLOY_NAMESPACE}" --timeout=300s || true

echo "[OPSERA_REPORT:{\"namespace\":\"${DEPLOY_NAMESPACE}\",\"cluster\":\"${EKS_CLUSTER}\",\"release\":\"${HELM_RELEASE}\",\"commit\":\"${COMMIT_SHA:0:12}\",\"service\":\"pcis-platform\"}]"
echo "[OPSERA:deploy:success:Platform bootstrap ready in ${DEPLOY_NAMESPACE}]"
echo "[OPSERA:complete:success:pcis-platform deployed]"
