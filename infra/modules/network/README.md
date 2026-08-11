# PCIS Network Module

Terraform module that provisions the foundational AWS VPC network layer for the
PCIS modernisation programme. All subsequent infrastructure modules (EKS,
Aurora, MSK, ElastiCache) deploy into subnets created by this module.

## Architecture

```
VPC (10.{env}.0.0/16)
│
├── Public subnets (/20, one per AZ)          — Security zone: Public (ALB/WAF)
│     10.{env}.0.0/20   AZ-1
│     10.{env}.16.0/20  AZ-2
│     10.{env}.32.0/20  AZ-3 (prd only)
│
├── Private-app subnets (/19, one per AZ)     — Security zones: DMZ + Internal
│     10.{env}.96.0/19   AZ-1                   (API Gateway, EKS, service mesh)
│     10.{env}.128.0/19  AZ-2
│     10.{env}.160.0/19  AZ-3 (prd only)
│
└── Private-data subnets (/20, one per AZ)    — Security zone: Data
      10.{env}.192.0/20  AZ-1                   (Aurora, ElastiCache, MSK)
      10.{env}.208.0/20  AZ-2
      10.{env}.224.0/20  AZ-3 (prd only)
```

CIDR allocation per environment:

| Environment | VPC CIDR      |
|-------------|---------------|
| dev         | 10.0.0.0/16   |
| tst         | 10.1.0.0/16   |
| prd         | 10.2.0.0/16   |

## NAT Gateway strategy

| Mode            | Config                  | Environments |
|-----------------|-------------------------|--------------|
| Single NAT      | `enable_ha_nat = false` | dev, tst     |
| HA NAT (per AZ) | `enable_ha_nat = true`  | prd          |

In single-NAT mode, **all** private subnets (across every AZ) route egress
traffic through the one NAT gateway placed in public subnet AZ-1.  This
provides egress for all private subnets at reduced cost, with the trade-off
that an AZ-1 failure interrupts egress for private subnets in all AZs.

## Security posture

* The VPC default security group is overridden to deny **all** inbound and
  outbound traffic.
* Three explicit security groups are created:
  * **ALB SG** — allows HTTPS (443) and HTTP (80) inbound from `0.0.0.0/0`.
  * **Service-mesh SG** — allows Istio Envoy ports (15000-15090) between mesh
    members and application traffic (8080) from private-app subnets.
  * **Database SG** — allows PostgreSQL (5432), Redis (6379), and Kafka
    (9092-9096) **from private-app subnets only**.

## VPC Flow Logs

Flow logs are delivered to an S3 bucket named
`pcis-{env}-vpc-flow-logs-{account_id}`.  The bucket has:

* AES-256 server-side encryption
* Public access fully blocked
* Versioning enabled
* Lifecycle rule that expires objects after `flow_log_retention_days` (default 90)

## Usage

```hcl
module "network" {
  source = "../../modules/network"

  environment_name        = "dev"
  vpc_cidr                = "10.0.0.0/16"
  az_count                = 2
  enable_ha_nat           = false
  flow_log_retention_days = 30

  common_tags = {
    CostCenter = "PCIS-Engineering"
  }
}

# Consume outputs in downstream modules:
module "eks" {
  # ...
  vpc_id             = module.network.vpc_id
  subnet_ids         = module.network.private_app_subnet_ids
  security_group_ids = [module.network.service_mesh_security_group_id]
}
```

## Requirements

| Name      | Version   |
|-----------|-----------|
| terraform | >= 1.6.0  |
| aws       | ~> 5.0    |

## Inputs

| Name                    | Type        | Default       | Description |
|-------------------------|-------------|---------------|-------------|
| vpc_cidr                | string      | 10.0.0.0/16   | VPC CIDR block |
| environment_name        | string      | —             | dev, tst, or prd |
| az_count                | number      | 2             | Number of AZs (2 or 3) |
| enable_ha_nat           | bool        | false         | One NAT per AZ if true |
| flow_log_retention_days | number      | 90            | Flow log S3 retention days |
| common_tags             | map(string) | {}            | Extra tags for all resources |

## Outputs

| Name                          | Description |
|-------------------------------|-------------|
| vpc_id                        | VPC ID |
| vpc_cidr_block                | VPC CIDR block |
| public_subnet_ids             | Public subnet IDs (list) |
| private_app_subnet_ids        | Private-app subnet IDs (list) |
| private_data_subnet_ids       | Private-data subnet IDs (list) |
| nat_gateway_ids               | NAT Gateway IDs (list) |
| internet_gateway_id           | Internet Gateway ID |
| alb_security_group_id         | ALB security group ID |
| service_mesh_security_group_id| Service-mesh security group ID |
| database_security_group_id    | Database security group ID |
| flow_log_bucket_arn           | Flow-log S3 bucket ARN |
| availability_zones            | AZ names used |

## State isolation

Each environment maintains its own Terraform state file in S3:

```
s3://pcis-terraform-state-{account_id}/
  dev/network/terraform.tfstate
  tst/network/terraform.tfstate
  prd/network/terraform.tfstate
```

State locking uses the shared `pcis-terraform-locks` DynamoDB table with
environment-specific `LockID` values, preventing concurrent plan/apply
operations from corrupting state.

## Initialising a new environment

```bash
cd infra/environments/dev
terraform init \
  -backend-config="bucket=pcis-terraform-state-$(aws sts get-caller-identity --query Account --output text)" \
  -backend-config="region=${AWS_DEFAULT_REGION}"
terraform plan
terraform apply
```
