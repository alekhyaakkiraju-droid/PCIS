#!/usr/bin/env bash
# Start PCIS local stack: Docker infra + domain services + frontend dev server.
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

LOCAL_DIR="${ROOT}/.local"
PID_FILE="${LOCAL_DIR}/pids"
LOG_DIR="${LOCAL_DIR}/logs"
FRONTEND_PORT="${FRONTEND_PORT:-3001}"
GATEWAY_PORT="${GATEWAY_PORT:-8081}"

mkdir -p "$LOCAL_DIR" "$LOG_DIR"
: > "$PID_FILE"

log() { printf '==> %s\n' "$*"; }

require_cmd() {
  if ! command -v "$1" >/dev/null 2>&1; then
    echo "ERROR: required command not found: $1" >&2
    exit 1
  fi
}

wait_http() {
  local url=$1
  local label=$2
  local attempts=${3:-60}
  for ((i = 1; i <= attempts; i++)); do
    if curl -sf "$url" >/dev/null 2>&1; then
      log "$label is ready"
      return 0
    fi
    sleep 2
  done
  echo "ERROR: timed out waiting for $label ($url)" >&2
  return 1
}

start_jar() {
  local name=$1
  local jar=$2
  if [[ ! -f "$jar" ]]; then
    echo "ERROR: missing jar for $name: $jar" >&2
    exit 1
  fi
  log "Starting $name"
  nohup java -jar "$jar" --spring.profiles.active=local >>"${LOG_DIR}/${name}.log" 2>&1 &
  echo $! >>"$PID_FILE"
}

require_cmd docker
require_cmd java
require_cmd mvn
require_cmd curl
require_cmd npm

if ! docker info >/dev/null 2>&1; then
  echo "ERROR: Docker daemon is not running" >&2
  exit 1
fi

log "Starting Docker infrastructure (docker-compose.local.yml)"
docker compose -f docker-compose.local.yml up -d

log "Waiting for Postgres on localhost:5434"
for _ in $(seq 1 30); do
  if docker compose -f docker-compose.local.yml exec -T postgres pg_isready -U postgres >/dev/null 2>&1; then
    break
  fi
  sleep 2
done

if [[ -x "${ROOT}/scripts/seed-cross-service-fixtures.sh" ]]; then
  log "Seeding cross-service fixtures"
  PCIS_DB_URL=jdbc:postgresql://localhost:5434/pcis \
    PCIS_DB_USER=pcis \
    PCIS_DB_PASSWORD=pcis \
    "${ROOT}/scripts/seed-cross-service-fixtures.sh" || true
fi

wait_http "http://localhost:8180/realms/pcis/.well-known/openid-configuration" "Keycloak" 90 || true

log "Building shared libs and domain services (skip tests)"
mvn -q -pl shared-libs/pcis-observability-starter,shared-libs/pcis-schema,shared-libs/pcis-classification,shared-libs/pcis-outbox \
  clean install -DskipTests
mvn -q -pl services/customer-svc,services/claims-svc,services/policy-svc,services/billing-svc,services/api-gateway \
  clean package -DskipTests

start_jar "customer-svc" "${ROOT}/services/customer-svc/target/customer-svc-0.1.0-SNAPSHOT.jar"
start_jar "claims-svc" "${ROOT}/services/claims-svc/target/claims-svc-0.1.0-SNAPSHOT.jar"
start_jar "policy-svc" "${ROOT}/services/policy-svc/target/policy-svc-0.1.0-SNAPSHOT.jar"
start_jar "billing-svc" "${ROOT}/services/billing-svc/target/billing-svc-0.1.0-SNAPSHOT.jar"
start_jar "api-gateway" "${ROOT}/services/api-gateway/target/api-gateway-0.1.0-SNAPSHOT.jar"

wait_http "http://127.0.0.1:8082/actuator/health" "customer-svc" 90
wait_http "http://127.0.0.1:8084/actuator/health" "policy-svc" 90
wait_http "http://127.0.0.1:8085/actuator/health" "billing-svc" 90
wait_http "http://127.0.0.1:8086/actuator/health" "claims-svc" 90
wait_http "http://127.0.0.1:8081/actuator/health" "api-gateway" 90

log "Starting frontend dev server on http://127.0.0.1:${FRONTEND_PORT}"
cd "${ROOT}/frontend"
if [[ ! -d node_modules ]]; then
  npm install --silent
fi

log ""
log "PCIS local stack is up:"
log "  UI:        http://127.0.0.1:${FRONTEND_PORT}"
log "  Gateway:   http://127.0.0.1:${GATEWAY_PORT}"
log "  Keycloak:  http://localhost:8180 (admin/admin)"
log "  Logs:      ${LOG_DIR}/"
log "  Stop with: ./scripts/stop-local.sh"
log ""

exec npm run dev -- --host 127.0.0.1 --port "${FRONTEND_PORT}"
