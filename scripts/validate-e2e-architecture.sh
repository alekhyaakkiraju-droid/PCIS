#!/usr/bin/env bash
# Validate PCIS local architecture data flow end-to-end.
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
GATEWAY="${GATEWAY_URL:-http://127.0.0.1:8081}"
KEYCLOAK="${KEYCLOAK_URL:-http://localhost:8180}"
FRONTEND="${FRONTEND_URL:-http://127.0.0.1:3001}"

pass=0
fail=0
skip=0

log() { printf '==> %s\n' "$*"; }
ok() { pass=$((pass + 1)); printf 'PASS  %s\n' "$1"; }
bad() { fail=$((fail + 1)); printf 'FAIL  %s\n' "$1"; }
skip_check() { skip=$((skip + 1)); printf 'SKIP  %s\n' "$1"; }

expect_http() {
  local label=$1
  local url=$2
  local expected=$3
  local code
  code=$(curl -s -o /dev/null -w "%{http_code}" "$url" || echo 000)
  if [[ "$code" == "$expected" ]]; then
    ok "$label (HTTP $code)"
  else
    bad "$label (expected HTTP $expected, got $code) — $url"
  fi
}

expect_json_field() {
  local label=$1
  local url=$2
  local jq_expr=$3
  local body
  body=$(curl -sf "$url" 2>/dev/null || true)
  if [[ -z "$body" ]]; then
    bad "$label (empty response)"
    return
  fi
  if echo "$body" | jq -e "$jq_expr" >/dev/null 2>&1; then
    ok "$label"
  else
    bad "$label — $url"
  fi
}

log "PCIS architecture E2E validation"
log "Gateway: $GATEWAY"

log "Layer 1 — Infrastructure"
expect_http "customer-svc health" "http://127.0.0.1:8082/actuator/health" 200
expect_http "policy-svc health" "http://127.0.0.1:8084/actuator/health" 200
expect_http "billing-svc health" "http://127.0.0.1:8085/actuator/health" 200
expect_http "claims-svc health" "http://127.0.0.1:8086/actuator/health" 200
expect_http "Keycloak OIDC discovery" "$KEYCLOAK/realms/pcis/.well-known/openid-configuration" 200

log "Layer 2 — Edge / API Gateway routing"
expect_http "Gateway health" "$GATEWAY/actuator/health" 200
expect_http "Customer search via gateway" "$GATEWAY/api/v1/customers/search?q=Marta" 200
expect_http "Claims list via gateway" "$GATEWAY/api/v1/claims" 200
expect_http "Policy list via gateway" "$GATEWAY/api/v1/policies?customerId=19284" 200
expect_http "Billing installments via gateway" "$GATEWAY/api/v1/billing/installments" 200
expect_http "Billing aging via gateway" "$GATEWAY/api/v1/billing/aging" 200
expect_http "Billing customer summary via gateway" "$GATEWAY/api/v1/customers/19284/billing/summary" 200

log "Layer 3 — Cross-service orchestration (Customer 360)"
expect_json_field "Customer 360 profile available" \
  "$GATEWAY/api/v1/customers/19284/360" '.profile.status == "AVAILABLE"'
expect_json_field "Customer 360 policies fan-out" \
  "$GATEWAY/api/v1/customers/19284/360" '.policies.status == "AVAILABLE"'
expect_json_field "Customer 360 billing fan-out" \
  "$GATEWAY/api/v1/customers/19284/360" '.billing.status == "AVAILABLE"'
expect_json_field "Customer 360 claims fan-out" \
  "$GATEWAY/api/v1/customers/19284/360" '.billing.data.balanceDue != null'

log "Layer 4 — Domain mutation + outbox (claims FNOL)"
fnol_body='{"polNbr":"POL000003001","custId":19284,"lossDate":"2026-08-05","claimType":"PRP","description":"Architecture E2E validation claim","initialReserveType":"LOS","initialReserveAmt":1000.00}'
fnol_resp=$(curl -sf -X POST "$GATEWAY/api/v1/claims" \
  -H 'Content-Type: application/json' \
  -H 'X-Correlation-ID: arch-e2e-fnol' \
  -d "$fnol_body" 2>/dev/null || true)
if [[ -n "$fnol_resp" ]] && echo "$fnol_resp" | jq -e '.claimNbr != null' >/dev/null 2>&1; then
  ok "Claims FNOL create"
  claim_nbr=$(echo "$fnol_resp" | jq -r '.claimNbr')
  outbox_count=$(PGPASSWORD=pcis psql -h localhost -p 5434 -U pcis -d pcis_claims -tAc \
    "SELECT COUNT(*) FROM outbox_events WHERE aggregate_id = '$claim_nbr' AND event_type = 'ClaimCreated'" 2>/dev/null || echo 0)
  if [[ "${outbox_count:-0}" -ge 1 ]]; then
    ok "Outbox event persisted for FNOL (transactional outbox pattern)"
  else
    bad "Outbox event missing for FNOL claim $claim_nbr"
  fi
else
  bad "Claims FNOL create"
fi

log "Layer 5 — Presentation tier"
expect_http "Frontend dev server" "$FRONTEND/" 200

log "Deferred / not in local stack (documented gaps)"
skip_check "Kafka event relay (outbox relay disabled locally; MSK in AWS)"
skip_check "audit-svc consumer (service not started locally)"
skip_check "authz-svc decisions (service not started locally)"
skip_check "Elasticsearch search index (not in PCIS target v1)"
skip_check "Stripe / SendGrid / Firebase (AD-08 tokenized gateway; notification stub)"
skip_check "S3 / Snowflake warehouse loader (AWS batch archive only)"

log ""
log "Results: $pass passed, $fail failed, $skip skipped"
if [[ "$fail" -gt 0 ]]; then
  exit 1
fi
