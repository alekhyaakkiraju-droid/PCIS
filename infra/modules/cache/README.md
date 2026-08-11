# PCIS ElastiCache Redis Module (WO-138)

Provisions Amazon ElastiCache Redis with encryption, AUTH from Secrets Manager,
and optional cluster mode for production.

## Features

- **Encryption** in transit and at rest
- **AUTH token** stored in Secrets Manager — `lifecycle.ignore_changes` on `auth_token`
- **Cluster mode** enabled in production (2 shards, 2 replicas)
- **Security group** restricts port 6379 to private application subnet CIDRs

## Usage

```hcl
module "cache" {
  source = "../../modules/cache"

  environment_name         = var.environment_name
  vpc_id                   = module.network.vpc_id
  private_app_subnet_ids   = module.network.private_app_subnet_ids
  private_app_subnet_cidrs = module.network.private_app_subnet_cidrs
  node_type                = var.redis_node_type
  cluster_mode_enabled     = var.redis_cluster_mode_enabled
}
```

## Edge cases

- **Failover in cluster mode** — clients must handle MOVED redirections (Lettuce/Jedis cluster)
- **AUTH rotation** — update secret out-of-band; use `auth_token_update_strategy = ROTATE`
