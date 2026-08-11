locals {
  base_tags = merge(
    {
      Project   = "PCIS"
      ManagedBy = "Terraform"
      Component = "cache"
    },
    var.tags,
    {
      Environment = var.environment_name
    }
  )

  replication_group_id = "pcis-${var.environment_name}-redis"
  auth_secret_name     = coalesce(var.auth_secret_name, "pcis/${var.environment_name}/redis-auth")
}

resource "aws_security_group" "redis" {
  name_prefix = "pcis-${var.environment_name}-redis-"
  description = "ElastiCache Redis: inbound 6379 from private-app subnets only"
  vpc_id      = var.vpc_id

  ingress {
    description = "Redis from private application subnets"
    from_port   = 6379
    to_port     = 6379
    protocol    = "tcp"
    cidr_blocks = var.private_app_subnet_cidrs
  }

  egress {
    description = "Allow outbound for AWS endpoints"
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = merge(local.base_tags, {
    Name = "pcis-${var.environment_name}-redis-sg"
  })

  lifecycle {
    create_before_destroy = true
  }
}

resource "aws_elasticache_subnet_group" "redis" {
  name       = "pcis-${var.environment_name}-redis"
  subnet_ids = var.private_app_subnet_ids

  tags = local.base_tags
}

resource "aws_secretsmanager_secret" "auth" {
  name                    = local.auth_secret_name
  description             = "ElastiCache Redis AUTH token for ${var.environment_name} (value set out-of-band; never in Terraform state)"
  recovery_window_in_days = var.environment_name == "prd" ? 30 : 7

  tags = merge(local.base_tags, {
    Name       = local.auth_secret_name
    NoTfValues = "true"
  })
}

# AUTH token is referenced from Secrets Manager at deploy time; Terraform never writes the value.
resource "aws_elasticache_replication_group" "redis" {
  replication_group_id = local.replication_group_id
  description          = "PCIS Redis for rate limiting and reference cache (${var.environment_name})"

  engine               = "redis"
  engine_version       = var.engine_version
  node_type            = var.node_type
  port                 = 6379
  parameter_group_name = var.cluster_mode_enabled ? aws_elasticache_parameter_group.cluster[0].name : aws_elasticache_parameter_group.standard[0].name

  subnet_group_name  = aws_elasticache_subnet_group.redis.name
  security_group_ids = [aws_security_group.redis.id]

  at_rest_encryption_enabled = true
  transit_encryption_enabled = true
  auth_token                 = "REPLACE_FROM_SECRETS_MANAGER"
  auth_token_update_strategy = "ROTATE"

  automatic_failover_enabled = var.cluster_mode_enabled || var.environment_name == "prd"
  multi_az_enabled           = var.environment_name == "prd"

  num_node_groups         = var.cluster_mode_enabled ? var.num_node_groups : null
  replicas_per_node_group = var.cluster_mode_enabled ? var.replicas_per_node_group : null
  num_cache_clusters      = var.cluster_mode_enabled ? null : 1

  lifecycle {
    ignore_changes = [auth_token]
  }

  tags = merge(local.base_tags, {
    Name = local.replication_group_id
  })
}

resource "aws_elasticache_parameter_group" "standard" {
  count = var.cluster_mode_enabled ? 0 : 1

  name        = "pcis-${var.environment_name}-redis-std"
  family      = "redis7"
  description = "PCIS Redis standard mode (${var.environment_name})"

  tags = local.base_tags
}

resource "aws_elasticache_parameter_group" "cluster" {
  count = var.cluster_mode_enabled ? 1 : 0

  name        = "pcis-${var.environment_name}-redis-cluster"
  family      = "redis7"
  description = "PCIS Redis cluster mode (${var.environment_name})"

  parameter {
    name  = "cluster-enabled"
    value = "yes"
  }

  tags = local.base_tags
}
