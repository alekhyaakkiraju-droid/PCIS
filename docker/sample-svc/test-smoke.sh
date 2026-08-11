#!/usr/bin/env bash
# Smoke: build base + sample-svc when Docker is available; otherwise skip cleanly.
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_DOCKER="$(cd "${ROOT}/.." && pwd)"

if ! command -v docker >/dev/null 2>&1; then
  echo "SKIP: docker CLI not found"
  exit 0
fi

if ! docker info >/dev/null 2>&1; then
  echo "SKIP: docker daemon not available"
  exit 0
fi

export DOCKER_BUILDKIT=1
IMAGE_TAG="${IMAGE_TAG:-smoke}"
BASE_REF="pcis-base-java21:${IMAGE_TAG}"
SAMPLE_REF="pcis-sample-svc:${IMAGE_TAG}"

echo "Building base ${BASE_REF}..."
SKIP_SCAN="${SKIP_SCAN:-1}" IMAGE_TAG="${IMAGE_TAG}" "${REPO_DOCKER}/base/build.sh"

echo "Building sample ${SAMPLE_REF}..."
docker build \
  --build-arg "BASE_IMAGE=${BASE_REF}" \
  -t "${SAMPLE_REF}" \
  -f "${ROOT}/Dockerfile" \
  "${ROOT}"

echo "Smoke build OK: ${SAMPLE_REF}"
echo "Note: placeholder JAR is not a Spring Boot fat JAR; full health-check needs a real fat jar."
