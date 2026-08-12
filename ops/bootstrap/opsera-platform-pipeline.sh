#!/usr/bin/env bash
# Opsera C2C workflow script — pcis-platform-dev-pipeline
# Stages: clone → deploy (staged rollouts; no blocking helm --wait).
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

echo "[OPSERA:clone:running:Cloning ${REPO_URL} @ ${BRANCH}]"
REPO_SLUG="${REPO_URL#https://github.com/}"
REPO_SLUG="${REPO_SLUG%.git}"
git clone --depth 1 --branch "${BRANCH}" "https://${GIT_PAT}@github.com/${REPO_SLUG}.git" repo
cd repo
COMMIT_SHA="$(git rev-parse HEAD)"
echo "[OPSERA:clone:success:Cloned ${COMMIT_SHA:0:12}]"

echo "[OPSERA:deploy:running:Platform bootstrap ${HELM_RELEASE} → ${EKS_CLUSTER}/${DEPLOY_NAMESPACE}]"
aws eks update-kubeconfig --name "${EKS_CLUSTER}" --region "${AWS_REGION}"
kubectl create namespace "${DEPLOY_NAMESPACE}" --dry-run=client -o yaml | kubectl apply -f -

bash helm/scripts/validate-platform-chart.sh
bash helm/scripts/deploy-platform.sh "${CHART_PATH}" "${DEPLOY_NAMESPACE}" "${HELM_RELEASE}"

echo "[OPSERA_REPORT:{\"namespace\":\"${DEPLOY_NAMESPACE}\",\"cluster\":\"${EKS_CLUSTER}\",\"release\":\"${HELM_RELEASE}\",\"commit\":\"${COMMIT_SHA:0:12}\",\"service\":\"pcis-platform\"}]"
echo "[OPSERA:deploy:success:Platform bootstrap ready in ${DEPLOY_NAMESPACE}]"
echo "[OPSERA:complete:success:pcis-platform deployed]"
