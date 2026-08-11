# PCIS Database Module (WO-131)

Provisions an **Aurora PostgreSQL 17** cluster with a writer, a reporting read replica, customer-managed KMS encryption, TLS-only connections, 35-day automated backups (PITR), and IRSA roles for IAM database authentication.

## Layout

| File | Purpose |
|------|---------|
| `main.tf` | Cluster, instances, subnet group, security group, monitoring |
| `kms.tf` | CMK with automatic rotation + alias `pcis-aurora-{env}` |
| `parameter_group.tf` | `rds.force_ssl=1`, `timezone=UTC`, env-specific `log_statement` |
| `iam.tf` | Per-microservice IRSA roles (`rds-db:connect`) |
| `variables.tf` / `outputs.tf` | Module contract |

## Interface

| Input | Description |
|-------|-------------|
| `private_data_subnet_ids` | From network module (Data zone) |
| `private_app_subnet_cidrs` | Ingress allow-list for port 5432 |
| `multi_az` | `true` in prd (reader in different AZ); `false` in dev/tst |
| `instance_class` | `db.r6g.large` (dev/tst), `db.r6g.xlarge` (prd) |
| `oidc_provider_arn` / `oidc_provider_url` | From EKS module for IRSA |

| Output | Description |
|--------|-------------|
| `cluster_endpoint` / `reader_endpoint` | Writer vs reporting endpoints |
| `kms_key_arn` / `security_group_id` | Encryption + network controls |
| `irsa_role_arns` | Map for Helm ServiceAccount annotations |

## Security posture

- Encryption at rest via customer-managed KMS (`enable_key_rotation=true`).
- TLS enforced by cluster parameter group (`rds.force_ssl=1`).
- IAM database authentication enabled; master password stored in AWS-managed Secrets Manager.
- Security group: **only** private-app CIDRs on 5432.
- Reader `promotion_tier=15` so failover never auto-promotes the reporting replica.

## Environments

Committed under `infra/environments/{dev,tst,prd}/database.tf` + tfvars fixtures.
