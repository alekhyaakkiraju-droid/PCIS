# PCIS MSK Messaging Module (WO-138)

Provisions Amazon MSK with SASL/SCRAM, TLS in transit, and topic auto-creation disabled.

## Features

- **SASL/SCRAM** authentication with credentials in Secrets Manager (90-day rotation cycle)
- **TLS** encryption in transit (`client_broker = TLS`)
- **Logging** to CloudWatch (30-day) and S3 (90-day) for audit
- **Security group** restricts port 9096 to private application subnet CIDRs
- **Configuration** sets `auto.create.topics.enable=false`

## Usage

```hcl
module "messaging" {
  source = "../../modules/messaging"

  environment_name           = var.environment_name
  vpc_id                     = module.network.vpc_id
  private_app_subnet_ids     = module.network.private_app_subnet_ids
  private_app_subnet_cidrs   = module.network.private_app_subnet_cidrs
  broker_count               = var.msk_broker_count
  broker_instance_type       = var.msk_broker_instance_type
  default_replication_factor = var.msk_default_replication_factor
  min_insync_replicas        = var.msk_min_insync_replicas
}
```

## Edge cases

- **Broker failure** — replication factor 3 in prd with min.insync.replicas=2
- **Version upgrade** — rolling upgrade; test canary topic before production
- **SCRAM rotation** — update secret value out-of-band; MSK association picks up new version
