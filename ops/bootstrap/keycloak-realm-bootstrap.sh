#!/usr/bin/env bash
# Fix PCIS Keycloak realm OIDC scopes after a broken --import-realm (dev only).
# Usage: AWS_PROFILE=opsera-dev ./ops/bootstrap/keycloak-realm-bootstrap.sh
set -euo pipefail

NAMESPACE="${NAMESPACE:-pcis-dev}"
REALM="${REALM:-pcis}"
APP_URL="${APP_URL:-https://pcis-dev.agent.opsera.dev}"
REALM_FILE="${REALM_FILE:-helm/charts/pcis-platform/files/realm-export.json}"

KC_POD="$(kubectl get pod -n "${NAMESPACE}" -l app.kubernetes.io/name=keycloak -o jsonpath='{.items[0].metadata.name}')"
KCADM=(kubectl exec -n "${NAMESPACE}" "${KC_POD}" -- /opt/keycloak/bin/kcadm.sh)

"${KCADM[@]}" config credentials --server http://localhost:8080 --realm master --user admin --password admin

if "${KCADM[@]}" get "realms/${REALM}" -r master >/dev/null 2>&1; then
  echo "Deleting broken realm ${REALM}..."
  "${KCADM[@]}" delete "realms/${REALM}" -r master
fi

echo "Creating realm ${REALM} with standard OIDC scopes..."
"${KCADM[@]}" create realms \
  -s "realm=${REALM}" \
  -s enabled=true \
  -s displayName=PCIS \
  -s sslRequired=external \
  -s registrationAllowed=false \
  -s loginWithEmailAllowed=true \
  -s bruteForceProtected=true

kubectl create configmap keycloak-realm -n "${NAMESPACE}" \
  --from-file=pcis-realm.json="${REALM_FILE}" \
  --dry-run=client -o yaml | kubectl apply -f -

kubectl rollout restart deployment/keycloak -n "${NAMESPACE}"
kubectl rollout status deployment/keycloak -n "${NAMESPACE}" --timeout=180s

KC_POD="$(kubectl get pod -n "${NAMESPACE}" -l app.kubernetes.io/name=keycloak -o jsonpath='{.items[0].metadata.name}')"
KCADM=(kubectl exec -n "${NAMESPACE}" "${KC_POD}" -- /opt/keycloak/bin/kcadm.sh)
"${KCADM[@]}" config credentials --server http://localhost:8080 --realm master --user admin --password admin

echo "Partial-importing clients, roles, users..."
"${KCADM[@]}" create partialImport -r "${REALM}" -s ifResourceExists=OVERWRITE \
  -f /opt/keycloak/data/import/pcis-realm.json

CLIENT_ID="$("${KCADM[@]}" get clients -r "${REALM}" -q clientId=pcis-spa --fields id --format csv --noquotes | tail -1)"
"${KCADM[@]}" update "clients/${CLIENT_ID}" -r "${REALM}" \
  -s "redirectUris=[\"http://localhost:3000/*\",\"${APP_URL}/*\",\"https://app.dev.pcis.example.com/*\",\"https://app.tst.pcis.example.com/*\",\"https://app.pcis.example.com/*\"]" \
  -s "webOrigins=[\"+\",\"http://localhost:3000\",\"${APP_URL}\"]" \
  -s "rootUrl=${APP_URL}"

for SCOPE_NAME in authority_limit configuration-admin; do
  SCOPE_ID="$("${KCADM[@]}" get client-scopes -r "${REALM}" -q "name=${SCOPE_NAME}" --fields id --format csv --noquotes | tail -1 || true)"
  if [[ -n "${SCOPE_ID}" ]]; then
    "${KCADM[@]}" create "clients/${CLIENT_ID}/default-client-scopes/${SCOPE_ID}" -r "${REALM}" 2>/dev/null || true
  fi
done

ROLES_SCOPE_ID="$("${KCADM[@]}" get client-scopes -r "${REALM}" -q name=roles --fields id --format csv --noquotes | tail -1 || true)"
if [[ -n "${ROLES_SCOPE_ID}" ]]; then
  REALM_ROLES_MAPPER_ID="$("${KCADM[@]}" get "client-scopes/${ROLES_SCOPE_ID}/protocol-mappers/models" -r "${REALM}" --fields id,name 2>/dev/null \
    | python3 -c "import sys,json; m=json.load(sys.stdin); print(next((x['id'] for x in m if x.get('name')=='realm roles'), ''))" || true)"
  if [[ -n "${REALM_ROLES_MAPPER_ID}" ]]; then
    "${KCADM[@]}" update "client-scopes/${ROLES_SCOPE_ID}/protocol-mappers/models/${REALM_ROLES_MAPPER_ID}" -r "${REALM}" \
      -s 'config."multivalued"=true' \
      -s 'config."userinfo.token.claim"=true' \
      -s 'config."id.token.claim"=true' \
      -s 'config."access.token.claim"=true' \
      -s 'config."claim.name"=roles' \
      -s 'config."jsonType.label"=String'
  fi
fi

echo "Keycloak realm bootstrap complete for ${APP_URL}"
