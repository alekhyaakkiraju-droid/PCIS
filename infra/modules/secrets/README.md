# PCIS Secrets Module (WO-132)

Provisions **AWS Secrets Manager** shells (no values in Terraform), **90-day rotation** for Aurora credentials via the AWS RDS PostgreSQL rotation Lambda, **least-privilege IRSA** policies per microservice, and optional **External Secrets Operator** install with a `ClusterSecretStore`.

## Layout

| File | Purpose |
|------|---------|
| `main.tf` | Secrets, SAR rotation Lambda, rotation schedules, CloudWatch failure alarms |
| `iam.tf` | Per-service IRSA + ESO IRSA |
| `eso.tf` | Helm ESO + `ClusterSecretStore` (gated by `enable_eso`) |
| `examples/` | Sample `ExternalSecret` manifests (1h refresh) |

## Secrets (names only)

| Name | Rotation |
|------|----------|
| `pcis/{env}/aurora-writer` | 90 days |
| `pcis/{env}/aurora-reader` | 90 days |
| `pcis/{env}/keycloak-client` | manual / app-owned |
| `pcis/{env}/kafka-sasl` | manual / app-owned |

**Values are never written by Terraform.** Populate via console/CLI/rotation; state holds ARNs only.

## Access pattern

1. Set secret value out-of-band (or let rotation seed after first version).
2. Annotate ServiceAccount with `service_irsa_role_arns[<svc>]` **or** rely on ESO sync into a K8s Secret.
3. With `enable_eso=true`, pods consume namespaced Secrets synced every `1h`.

## Two-phase apply

1. `enable_eso = false` — create Secrets Manager + IAM  
2. `enable_eso = true` — install ESO + ClusterSecretStore (requires live EKS)

## Edge cases

- **Rotation failure:** CloudWatch alarm `pcis-{env}-secret-rotation-*`; AWSCURRENT stays usable.
- **ESO restart:** ExternalSecrets reconcile on next refresh interval.
- **Shared Aurora, different schemas:** IAM still per-service secret keys, not a shared cluster credential.
