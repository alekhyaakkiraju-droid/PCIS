# PCIS Network Module (WO-129)

Terraform module that provisions the foundational AWS VPC for PCIS across **dev**, **tst**, and **prd**.

## What it creates

| Resource | Purpose |
|----------|---------|
| VPC | Multi-AZ network with DNS support |
| Public subnets | ALB / WAF (Public zone) |
| Private app subnets | Service mesh / API workloads (Internal / DMZ) |
| Private data subnets | Aurora, ElastiCache, MSK (Data zone) |
| Internet Gateway | Public subnet egress / ingress |
| NAT Gateway(s) | Private subnet egress (single or HA) |
| VPC Flow Logs | All traffic → S3 with lifecycle retention |
| Default SG | Deny-all inbound |
| ALB / Mesh / DB SGs | Explicit allow rules |

## Usage

```hcl
module "network" {
  source = "../../modules/network"

  environment_name        = "dev"
  vpc_cidr                = "10.0.0.0/16"
  az_count                = 2
  enable_ha_nat           = false
  flow_log_retention_days = 90

  tags = {
    Project   = "PCIS"
    ManagedBy = "Terraform"
  }
}
```

## Inputs

| Name | Type | Default | Description |
|------|------|---------|-------------|
| `environment_name` | string | — | `dev`, `tst`, or `prd` |
| `vpc_cidr` | string | — | Non-overlapping CIDR per env |
| `az_count` | number | `2` | 2–3 AZs (handles regions with fewer AZs) |
| `enable_ha_nat` | bool | `false` | `true` = one NAT per AZ; `false` = shared NAT for all private subnets |
| `flow_log_retention_days` | number | `90` | S3 lifecycle expiration |
| `tags` | map(string) | `{}` | Merged with Project/ManagedBy/Environment |

## Outputs

`vpc_id`, `vpc_cidr_block`, `public_subnet_ids`, `private_app_subnet_ids`, `private_data_subnet_ids`, `nat_gateway_ids`, security group IDs, `flow_log_bucket_arn`, `availability_zones`.

## Environments

| Env | CIDR | HA NAT |
|-----|------|--------|
| dev | `10.0.0.0/16` | no |
| tst | `10.1.0.0/16` | no |
| prd | `10.2.0.0/16` | yes |

Compositions live under `infra/environments/{dev,tst,prd}/`.

## Backend

Environments default to a **local** backend so `plan`/`validate` work before bootstrap.

Remote state (S3 + DynamoDB locking) is defined in:
- `infra/backend.s3.tf.example` — canonical S3 backend block
- `infra/environments/*/backend.hcl` — per-env state keys (`dev|tst|prd/network/terraform.tfstate`)

Adopt remote state with `terraform init -migrate-state -backend-config=backend.hcl` after swapping in the S3 backend block.

## Validate locally

```bash
cd infra/environments/dev
terraform init -backend=false
terraform validate
terraform plan -var-file=terraform.tfvars
```

## Terratest

```bash
cd infra/test
go test -v -timeout 30m -run TestNetworkDevModule
```

Requires AWS credentials with permission to create VPC resources in a sandbox account.
