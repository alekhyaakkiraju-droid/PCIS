# PCIS Keycloak Module (WO-145)

Provisions **Keycloak 26.x** via the Bitnami Helm chart with an external PostgreSQL backend, TLS/ingress hooks (ACM cert ARN), Kubernetes health probes, and **Secrets Manager shells** for admin/DB credentials (values never written by Terraform).

## Layout

| File | Purpose |
|------|---------|
| `main.tf` | Namespace, SM shells, placeholder K8s secrets, realm ConfigMap, Helm release |
| `variables.tf` / `outputs.tf` | Module contract |
| `versions.tf` | Terraform + provider constraints |
| `../../keycloak/realm-export.json` | PCIS realm (clients, roles, mappers, seed personas) |

## Interface

| Input | Description |
|-------|-------------|
| `environment_name` | `dev`, `tst`, or `prd` |
| `replica_count` / `resources` | Sizing per environment |
| `certificate_arn` | ACM cert for ALB HTTPS ingress |
| `ingress_host` | Public/internal hostname |
| `db_host` / `db_port` / `db_name` / `db_username` | External Postgres connection |
| `admin_secret_name` / `db_secret_name` | Secrets Manager refs (defaults `pcis/{env}/keycloak-*`) |
| `enable_realm_import` | Mount realm-export and `--import-realm` (typical for dev/tst) |

| Output | Description |
|--------|-------------|
| `issuer_url` / `discovery_url` / `jwks_url` | OIDC endpoints for the `pcis` realm |
| `admin_secret_arn` / `db_secret_arn` | SM shells for operators |
| `helm_release_name` / `namespace` | Runtime identifiers |

## Security posture

- **No real secrets in Terraform or git** — SM shells + K8s placeholder secrets with `lifecycle.ignore_changes` on data.
- Admin password and DB password populated out-of-band (console/CLI/ESO).
- Readiness probe hits `/health/ready` so pods do not receive traffic until the DB is reachable.
- Ingress TLS via ACM certificate ARN annotation when `certificate_arn` is set.

## Environments

Wired under `infra/environments/{dev,tst,prd}/keycloak.tf` (gated by `enable_keycloak`).  
Module-level tfvars also live at `infra/keycloak/environments/{dev,staging,prod}.tfvars` (`staging`→tst, `prod`→prd).

## Local parity

```bash
cd infra/keycloak && docker compose up -d
# OIDC discovery: http://localhost:8080/realms/pcis/.well-known/openid-configuration
```

## Validation

```bash
# Static realm + module file checks
python3 -m unittest discover -s infra/keycloak/tests -v

# Terraform fmt/validate (wrapper configures stub providers)
./infra/keycloak/tests/validate_terraform.sh
```
