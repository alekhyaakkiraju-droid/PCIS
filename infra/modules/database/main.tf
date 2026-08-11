locals {
  base_tags = merge(
    {
      Project   = "PCIS"
      ManagedBy = "Terraform"
    },
    var.tags,
    {
      Environment = var.environment_name
    }
  )

  cluster_identifier = "pcis-${var.environment_name}-aurora"
  service_namespace  = coalesce(var.service_namespace, "pcis-${var.environment_name}")

  writer_az = var.availability_zones[0]
}

resource "aws_db_subnet_group" "aurora" {
  name_prefix = "pcis-${var.environment_name}-aurora-"
  description = "Private data subnets for PCIS Aurora (${var.environment_name})"
  subnet_ids  = var.private_data_subnet_ids

  tags = merge(local.base_tags, {
    Name = "pcis-${var.environment_name}-aurora-subnets"
    Zone = "Data"
  })
}

# checkov:skip=CKV_AWS_382:Aurora Enhanced Monitoring and AWS APIs require unrestricted egress from the ENI; inbound remains app-CIDR-only on 5432.
resource "aws_security_group" "aurora" {
  name_prefix = "pcis-${var.environment_name}-aurora-"
  description = "Aurora PostgreSQL: inbound 5432 from private-app subnets only"
  vpc_id      = var.vpc_id

  ingress {
    description = "PostgreSQL from private application subnets"
    from_port   = 5432
    to_port     = 5432
    protocol    = "tcp"
    cidr_blocks = var.private_app_subnet_cidrs
  }

  # No other inbound rules. Egress allows AWS API / monitoring.
  egress {
    description = "Allow outbound for AWS service endpoints"
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = merge(local.base_tags, {
    Name = "pcis-${var.environment_name}-aurora-sg"
    Zone = "Data"
  })

  lifecycle {
    create_before_destroy = true
  }
}

resource "aws_iam_role" "rds_enhanced_monitoring" {
  count = var.enhanced_monitoring_interval > 0 ? 1 : 0

  name_prefix = "pcis-${var.environment_name}-aurora-mon-"
  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Effect = "Allow"
      Principal = {
        Service = "monitoring.rds.amazonaws.com"
      }
      Action = "sts:AssumeRole"
    }]
  })

  tags = local.base_tags
}

resource "aws_iam_role_policy_attachment" "rds_enhanced_monitoring" {
  count = var.enhanced_monitoring_interval > 0 ? 1 : 0

  role       = aws_iam_role.rds_enhanced_monitoring[0].name
  policy_arn = "arn:${data.aws_partition.current.partition}:iam::aws:policy/service-role/AmazonRDSEnhancedMonitoringRole"
}

# checkov:skip=CKV_AWS_139:Deletion protection is forced on in prd via aurora_deletion_protection=true; left off in non-prod for safe teardown.
resource "aws_rds_cluster" "aurora" {
  cluster_identifier = local.cluster_identifier
  engine             = "aurora-postgresql"
  engine_version     = var.engine_version
  engine_mode        = "provisioned"

  database_name   = var.database_name
  master_username = var.master_username

  # AWS-managed master secret — avoids committing credentials (WO-132 owns rotation).
  manage_master_user_password = true

  db_subnet_group_name            = aws_db_subnet_group.aurora.name
  vpc_security_group_ids          = [aws_security_group.aurora.id]
  db_cluster_parameter_group_name = aws_rds_cluster_parameter_group.aurora.name

  storage_encrypted                   = true
  kms_key_id                          = aws_kms_key.aurora.arn
  iam_database_authentication_enabled = true

  backup_retention_period      = var.backup_retention_period
  preferred_backup_window      = var.preferred_backup_window
  preferred_maintenance_window = var.preferred_maintenance_window
  copy_tags_to_snapshot        = true

  # PITR is available whenever automated backups are retained (> 0).
  deletion_protection       = var.deletion_protection
  skip_final_snapshot       = var.environment_name != "prd"
  final_snapshot_identifier = var.environment_name == "prd" ? "${local.cluster_identifier}-final" : null

  enabled_cloudwatch_logs_exports = ["postgresql"]

  apply_immediately = var.environment_name != "prd"

  tags = merge(local.base_tags, {
    Name = local.cluster_identifier
    Zone = "Data"
  })

  depends_on = [
    aws_kms_alias.aurora,
  ]
}

resource "aws_rds_cluster_instance" "writer" {
  identifier         = "${local.cluster_identifier}-writer"
  cluster_identifier = aws_rds_cluster.aurora.id
  instance_class     = var.instance_class
  engine             = aws_rds_cluster.aurora.engine
  engine_version     = aws_rds_cluster.aurora.engine_version

  # promotion_tier 0 = writer / highest failover priority
  promotion_tier = 0

  availability_zone = local.writer_az

  db_subnet_group_name = aws_db_subnet_group.aurora.name
  publicly_accessible  = false

  performance_insights_enabled          = var.performance_insights_enabled
  performance_insights_kms_key_id       = var.performance_insights_enabled ? aws_kms_key.aurora.arn : null
  performance_insights_retention_period = var.performance_insights_enabled ? 7 : null

  monitoring_interval = var.enhanced_monitoring_interval
  monitoring_role_arn = var.enhanced_monitoring_interval > 0 ? aws_iam_role.rds_enhanced_monitoring[0].arn : null

  auto_minor_version_upgrade = true
  # Parameter changes requiring reboot use the maintenance window in prd.
  apply_immediately = var.environment_name != "prd"

  tags = merge(local.base_tags, {
    Name = "${local.cluster_identifier}-writer"
    Role = "writer"
  })
}
