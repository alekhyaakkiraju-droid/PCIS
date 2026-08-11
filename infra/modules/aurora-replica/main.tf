data "aws_partition" "current" {}
data "aws_region" "current" {}
data "aws_caller_identity" "current" {}

locals {
  base_tags = merge(
    {
      Project   = "PCIS"
      ManagedBy = "Terraform"
      Component = "aurora-replica"
    },
    var.tags,
    {
      Environment = var.environment_name
    }
  )

  cluster_identifier = var.cluster_identifier
  service_namespace  = coalesce(var.service_namespace, "pcis-${var.environment_name}")
  reader_identifier  = "${local.cluster_identifier}-reader"
  reader_az          = var.multi_az && length(var.availability_zones) > 1 ? var.availability_zones[1] : var.availability_zones[0]
}

resource "aws_rds_cluster_instance" "reader" {
  identifier              = local.reader_identifier
  cluster_identifier      = var.cluster_identifier
  instance_class          = var.instance_class
  engine                  = var.engine
  engine_version          = var.engine_version
  promotion_tier          = 15
  availability_zone       = local.reader_az
  db_subnet_group_name    = var.db_subnet_group_name
  db_parameter_group_name = aws_db_parameter_group.reporting.name
  publicly_accessible     = false

  performance_insights_enabled          = var.performance_insights_enabled
  performance_insights_kms_key_id       = var.performance_insights_enabled ? var.kms_key_arn : null
  performance_insights_retention_period = var.performance_insights_enabled ? 7 : null

  monitoring_interval = var.enhanced_monitoring_interval
  monitoring_role_arn = var.enhanced_monitoring_interval > 0 ? var.monitoring_role_arn : null

  auto_minor_version_upgrade = true
  apply_immediately          = var.environment_name != "prd"

  tags = merge(local.base_tags, {
    Name = local.reader_identifier
    Role = "reader"
  })
}

resource "aws_cloudwatch_metric_alarm" "replica_lag_warning" {
  alarm_name          = "pcis-${var.environment_name}-aurora-replica-lag-warning"
  alarm_description   = "Aurora reporting replica lag exceeded ${var.lag_warning_seconds}s (warning)."
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = 2
  metric_name         = "AuroraReplicaLag"
  namespace           = "AWS/RDS"
  period              = 60
  statistic           = "Maximum"
  threshold           = var.lag_warning_seconds
  treat_missing_data  = "notBreaching"

  dimensions = {
    DBClusterIdentifier = var.cluster_identifier
    Role                = "READER"
  }

  tags = local.base_tags
}

resource "aws_cloudwatch_metric_alarm" "replica_lag_critical" {
  alarm_name          = "pcis-${var.environment_name}-aurora-replica-lag-critical"
  alarm_description   = "Aurora reporting replica lag exceeded ${var.lag_critical_seconds}s (critical)."
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = 1
  metric_name         = "AuroraReplicaLag"
  namespace           = "AWS/RDS"
  period              = 60
  statistic           = "Maximum"
  threshold           = var.lag_critical_seconds
  treat_missing_data  = "notBreaching"

  dimensions = {
    DBClusterIdentifier = var.cluster_identifier
    Role                = "READER"
  }

  tags = local.base_tags
}
