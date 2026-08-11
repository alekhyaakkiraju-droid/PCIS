# Keycloak Operations Runbook (WO-145)

Operational guide for the PCIS Keycloak 26.x OIDC identity provider.

## Overview

| Item | Value |
|------|-------|
| Realm | `pcis` |
| Chart | Bitnami `keycloak` (image tag `26.x`) |
| Module | `infra/modules/keycloak` |
| Realm export | `infra/keycloak/realm-export.json` |
| Local compose | `infra/keycloak/docker-compose.yml` |
| Secrets | `pcis/{env}/keycloak-admin`, `pcis/{env}/keycloak-db` (values out-of-band) |

OIDC discovery: `https://<ingress_host>/realms/pcis/.well-known/openid-configuration`

## Environments

| Env | Terraform dir | Module tfvars | Replicas | Realm import |
|-----|---------------|---------------|----------|--------------|
| dev | `infra/environments/dev` | `infra/keycloak/environments/dev.tfvars` | 1 | on |
| staging (tst) | `infra/environments/tst` | `infra/keycloak/environments/staging.tfvars` | 2 | on |
| prod (prd) | `infra/environments/prd` | `infra/keycloak/environments/prod.tfvars` | 3 | off |

Set `enable_keycloak = true` in the environment `terraform.tfvars` only after EKS is reachable and Secrets Manager values exist.

## Deploy

1. Create the dedicated PostgreSQL database `keycloak` (separate from PCIS app DBs).
2. Populate Secrets Manager (never commit values):
   - `pcis/{env}/keycloak-admin` JSON: `{"admin-password":"..."}`
   - `pcis/{env}/keycloak-db` JSON: `{"password":"..."}`
3. Sync into the namespace secrets `keycloak-admin` / `keycloak-db` (ESO or kubectl).
4. Apply Terraform with `enable_keycloak = true`.
5. Run smoke tests: `./infra/keycloak/tests/smoke_test.sh` against the ingress URL.

## Local development

```bash
docker compose -f infra/keycloak/docker-compose.yml up -d
curl -s http://localhost:8080/realms/pcis/.well-known/openid-configuration | jq .
./infra/keycloak/tests/smoke_test.sh
```

Admin console (local only): `http://localhost:8080` — user `admin` / password `admin`.

Seed persona passwords in the realm export are temporary placeholders (`TempChangeMe!*`) and must be rotated before any shared environment use.

## Clients and roles

**Clients**

| Client | Type | Notes |
|--------|------|-------|
| `pcis-spa` | public | Auth Code + PKCE S256 |
| `pcis-gateway` | confidential | Introspection / BFF |
| `pcis-batch` | confidential | Client credentials; SA role `BATCH_SVC` |

**Realm roles:** `CLAIMS_ADJUSTER`, `CLAIMS_SUPERVISOR`, `CSR`, `UNDERWRITER`, `FINANCE`, `COMPLIANCE`, `BATCH_SVC`

**Custom claim:** `authority_limit` (user attribute → access token via `oidc-usermodel-attribute-mapper`). Absent when the attribute is unset (not `null`/`0`).

**TTLs:** access `900s`, refresh/SSO idle `28800s`, client-credentials ~`3600s`.

## Realm export / import

- **Export (running instance):** Admin Console → Realm settings → Partial export, or `kcadm.sh` / Admin API.
- **Import (cluster):** ConfigMap `keycloak-realm-export` + `--import-realm` (dev/tst). Re-import of the same realm is idempotent for clients/roles when using Keycloak’s import strategies; prefer partial updates in prd.
- **Import (local):** volume-mount `realm-export.json` in docker-compose (already configured).

Committed client secrets are placeholders (`CHANGE_ME_*`). Rotate via Admin Console or Admin API and store real values in Secrets Manager (`pcis/{env}/keycloak-client` from the secrets module).

## Secret rotation

| Secret | Rotation |
|--------|----------|
| Keycloak admin password | Manual; update SM → sync K8s secret → rolling restart |
| Keycloak DB password | Manual aligned with DB credential rotation; update SM + Keycloak pods |
| `pcis-gateway` / `pcis-batch` client secrets | Rotate in Keycloak, update consumers via SM `keycloak-client` |

Terraform never writes secret **values** — only shell ARNs/names and placeholder K8s secrets with `lifecycle.ignore_changes`.

## Backup / restore

1. **Database:** Keycloak state lives in PostgreSQL. Include the `keycloak` database in RDS/Aurora backup/PITR (or the dedicated instance’s backup plan).
2. **Realm config:** Keep `realm-export.json` in git as the baseline; after production drift, re-export and open a PR.
3. **Restore:** Restore DB from snapshot, redeploy Helm release, verify `/health/ready` and discovery document, then run `smoke_test.sh`.

## Health and probes

| Probe | Path | Purpose |
|-------|------|---------|
| Startup | `/health/started` | Allow slow DB connect without crash-loop |
| Readiness | `/health/ready` | Gate Service/ALB traffic until DB reachable |
| Liveness | `/health/live` | Restart only on hard failure |

Downstream services must **fail closed** (503) when JWKS is unreachable — never allow unauthenticated access.

## Troubleshooting

| Symptom | Likely cause | Action |
|---------|--------------|--------|
| Pods not Ready | DB DNS/credentials/TLS | Check `externalDatabase` host, SM sync, Postgres connectivity |
| CrashLoopBackOff on boot | Bad admin/DB secret | Confirm K8s secrets replaced placeholders |
| 401 on client-credentials | Placeholder client secret | Update Keycloak client secret + caller config |
| Missing `authority_limit` | Attribute unset (expected for non-adjusters) | Set user attribute only for adjusters |
| Multi-role user missing a role | Role not assigned | Assign all realm roles; JWT `realm_access.roles` is an array |
| Revoked token still accepted by resource server | Local JWT validation within access TTL | Use introspection for privileged paths or keep 15m access TTL |

## Smoke test

```bash
KEYCLOAK_BASE_URL=https://auth.dev.pcis.example.com \
BATCH_CLIENT_SECRET='<from-secrets-manager>' \
GATEWAY_CLIENT_SECRET='<from-secrets-manager>' \
  ./infra/keycloak/tests/smoke_test.sh
```

The script skips (exit 0) when Keycloak is not reachable — safe for CI without a live IdP.
