locals {
  base_tags = merge(
    {
      Project   = "PCIS"
      ManagedBy = "Terraform"
      Component = "messaging"
    },
    var.tags,
    {
      Environment = var.environment_name
    }
  )

  cluster_name     = "pcis-${var.environment_name}-msk"
  scram_secret_name = coalesce(var.scram_secret_name, "pcis/${var.environment_name}/msk-scram")
  account_id       = data.aws_caller_identity.current.account_id
  logs_bucket_name = "pcis-msk-logs-${var.environment_name}-${local.account_id}"
}

data "aws_caller_identity" "current" {}

data "aws_region" "current" {}

resource "aws_msk_configuration" "kafka" {
  name              = "pcis-${var.environment_name}-msk-config"
  kafka_versions    = [var.kafka_version]
  server_properties = <<-PROPS
    auto.create.topics.enable=false
    default.replication.factor=${var.default_replication_factor}
    min.insync.replicas=${var.min_insync_replicas}
    num.partitions=3
    log.retention.hours=168
  PROPS

  description = "PCIS MSK configuration (${var.environment_name}) — topic auto-creation disabled"
}

resource "aws_security_group" "msk" {
  name_prefix = "pcis-${var.environment_name}-msk-"
  description = "MSK SASL/TLS: inbound 9096 from private-app subnets only"
  vpc_id      = var.vpc_id

  ingress {
    description = "SASL/SCRAM TLS from private application subnets"
    from_port   = 9096
    to_port     = 9096
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
    Name = "pcis-${var.environment_name}-msk-sg"
  })

  lifecycle {
    create_before_destroy = true
  }
}

resource "aws_cloudwatch_log_group" "msk" {
  name              = "/pcis/msk/${var.environment_name}/broker"
  retention_in_days = var.log_retention_days

  tags = local.base_tags
}

resource "aws_s3_bucket" "msk_logs" {
  bucket        = local.logs_bucket_name
  force_destroy = var.environment_name != "prd"

  tags = merge(local.base_tags, {
    Name    = local.logs_bucket_name
    Purpose = "msk-broker-logs"
  })
}

resource "aws_s3_bucket_public_access_block" "msk_logs" {
  bucket = aws_s3_bucket.msk_logs.id

  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}

resource "aws_s3_bucket_server_side_encryption_configuration" "msk_logs" {
  bucket = aws_s3_bucket.msk_logs.id
  rule {
    apply_server_side_encryption_by_default {
      sse_algorithm = "AES256"
    }
  }
}

resource "aws_s3_bucket_lifecycle_configuration" "msk_logs" {
  bucket = aws_s3_bucket.msk_logs.id

  rule {
    id     = "expire-msk-logs"
    status = "Enabled"
    filter {
      prefix = ""
    }
    expiration {
      days = var.s3_log_retention_days
    }
  }
}

resource "aws_secretsmanager_secret" "scram" {
  name                    = local.scram_secret_name
  description             = "MSK SASL/SCRAM credentials for ${var.environment_name} (value set out-of-band; rotated every 90 days)"
  recovery_window_in_days = var.environment_name == "prd" ? 30 : 7

  tags = merge(local.base_tags, {
    Name       = local.scram_secret_name
    NoTfValues = "true"
  })
}

resource "aws_msk_cluster" "kafka" {
  cluster_name           = local.cluster_name
  kafka_version          = var.kafka_version
  number_of_broker_nodes = var.broker_count

  broker_node_group_info {
    instance_type   = var.broker_instance_type
    client_subnets  = var.private_app_subnet_ids
    security_groups = [aws_security_group.msk.id]

    storage_info {
      ebs_storage_info {
        volume_size = 100
      }
    }
  }

  configuration_info {
    arn      = aws_msk_configuration.kafka.arn
    revision = aws_msk_configuration.kafka.latest_revision
  }

  client_authentication {
    sasl {
      scram = true
    }
    unauthenticated = false
  }

  encryption_info {
    encryption_in_transit {
      client_broker = "TLS"
      in_cluster    = true
    }
  }

  logging_info {
    broker_logs {
      cloudwatch_logs {
        enabled   = true
        log_group = aws_cloudwatch_log_group.msk.name
      }
      s3 {
        enabled = true
        bucket  = aws_s3_bucket.msk_logs.id
        prefix  = "broker-logs/"
      }
    }
  }

  tags = merge(local.base_tags, {
    Name = local.cluster_name
  })
}

resource "aws_msk_scram_secret_association" "kafka" {
  cluster_arn     = aws_msk_cluster.kafka.arn
  secret_arn_list = [aws_secretsmanager_secret.scram.arn]
}
