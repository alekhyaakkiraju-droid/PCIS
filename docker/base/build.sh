#!/usr/bin/env bash
# Build the PCIS Java 21 base image with reproducible timestamps and optional scans.
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
IMAGE_NAME="${IMAGE_NAME:-pcis-base-java21}"
IMAGE_TAG="${IMAGE_TAG:-local}"
FULL_REF="${IMAGE_NAME}:${IMAGE_TAG}"
SKIP_SCAN="${SKIP_SCAN:-0}"

export DOCKER_BUILDKIT=1

if [[ -z "${SOURCE_DATE_EPOCH:-}" ]]; then
  if git -C "${ROOT}/../.." rev-parse --show-toplevel >/dev/null 2>&1; then
    SOURCE_DATE_EPOCH="$(git -C "${ROOT}/../.." log -1 --format=%ct 2>/dev/null || date +%s)"
  else
    SOURCE_DATE_EPOCH="$(date +%s)"
  fi
fi
export SOURCE_DATE_EPOCH

echo "Building ${FULL_REF} (DOCKER_BUILDKIT=1 SOURCE_DATE_EPOCH=${SOURCE_DATE_EPOCH})"

docker build \
  --build-arg "SOURCE_DATE_EPOCH=${SOURCE_DATE_EPOCH}" \
  -t "${FULL_REF}" \
  -f "${ROOT}/Dockerfile" \
  "${ROOT}"

DIGEST="$(docker image inspect --format='{{index .RepoDigests 0}}' "${FULL_REF}" 2>/dev/null || true)"
if [[ -z "${DIGEST}" || "${DIGEST}" == "<no value>" ]]; then
  DIGEST="$(docker image inspect --format='{{.Id}}' "${FULL_REF}")"
fi
echo "Image digest/id: ${DIGEST}"

if [[ "${SKIP_SCAN}" != "1" ]]; then
  if command -v trivy >/dev/null 2>&1; then
    echo "Running trivy image scan..."
    trivy image --severity CRITICAL,HIGH --exit-code 0 "${FULL_REF}" || true
  elif command -v grype >/dev/null 2>&1; then
    echo "Running grype image scan..."
    grype "${FULL_REF}" || true
  else
    echo "Neither trivy nor grype found; skipping vulnerability scan (set SKIP_SCAN=1 to silence)."
  fi
fi

echo "Build complete: ${FULL_REF}"
echo "${DIGEST}"
