data "aws_caller_identity" "current" {}
data "aws_partition" "current" {}
data "aws_region" "current" {}

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

  service_namespace = coalesce(var.service_namespace, "pcis-${var.environment_name}")
  secret_prefix     = "pcis/${var.environment_name}"

  # Secret shells only — values are never written by Terraform.
  secret_keys = toset([
    "aurora-writer",
    "aurora-reader",
    "keycloak-client",
    "kafka-sasl",
  ])

  rotatable_keys = toset(["aurora-writer", "aurora-reader"])

  rotation_lambda_arn = var.rotation_lambda_arn != null ? var.rotation_lambda_arn : try(
    aws_serverlessapplicationrepository_cloudformation_stack.rds_rotation[0].outputs["RotationLambdaARN"],
    null
  )
}

resource "aws_secretsmanager_secret" "this" {
  for_each = local.secret_keys

  name                    = "${local.secret_prefix}/${each.key}"
  description             = "PCIS ${each.key} secret for ${var.environment_name} (value set out-of-band; never in Terraform)"
  kms_key_id              = aws_kms_key.secrets.arn
  recovery_window_in_days = var.environment_name == "prd" ? 30 : 7

  tags = merge(local.base_tags, {
    Name       = "${local.secret_prefix}/${each.key}"
    SecretKey  = each.key
    Rotatable  = contains(local.rotatable_keys, each.key) ? "true" : "false"
    NoTfValues = "true"
  })
}

# AWS-provided RDS PostgreSQL single-user rotation Lambda (Secrets Manager SAR app).
resource "aws_serverlessapplicationrepository_cloudformation_stack" "rds_rotation" {
  count = var.create_rotation_lambda && var.rotation_lambda_arn == null ? 1 : 0

  name             = "pcis-${var.environment_name}-rds-secret-rotation"
  application_id   = "arn:aws:serverlessrepo:${data.aws_region.current.name}:297356227824:applications/SecretsManagerRDSPostgreSQLRotationSingleUser"
  semantic_version = "1.1.607"
  capabilities     = ["CAPABILITY_IAM", "CAPABILITY_RESOURCE_POLICY"]

  parameters = {
    functionName = "pcis-${var.environment_name}-rds-rotation"
    endpoint     = "https://secretsmanager.${data.aws_region.current.name}.amazonaws.com"
  }

  tags = local.base_tags
}

# checkov:skip=CKV_AWS_304:Aurora credentials rotate every 90 days (PCIS maximum); Checkov cannot resolve Lambda ARN from SAR stack output at static-analysis time.
resource "aws_secretsmanager_secret_rotation" "aurora" {
  for_each = var.create_rotation_lambda || var.rotation_lambda_arn != null ? local.rotatable_keys : toset([])

  secret_id           = aws_secretsmanager_secret.this[each.key].id
  rotation_lambda_arn = local.rotation_lambda_arn

  rotation_rules {
    automatically_after_days = 90
  }

  depends_on = [
    aws_serverlessapplicationrepository_cloudformation_stack.rds_rotation,
  ]
}

# Edge case: alert when rotation fails so operators intervene while AWSCURRENT remains usable.
resource "aws_cloudwatch_metric_alarm" "rotation_failure" {
  for_each = aws_secretsmanager_secret_rotation.aurora

  alarm_name          = "pcis-${var.environment_name}-secret-rotation-${each.key}"
  alarm_description   = "Secrets Manager rotation failed for ${aws_secretsmanager_secret.this[each.key].name}; previous version remains AWSCURRENT until fixed."
  comparison_operator = "GreaterThanOrEqualToThreshold"
  evaluation_periods  = 1
  metric_name         = "RotationFailed"
  namespace           = "AWS/SecretsManager"
  period              = 300
  statistic           = "Sum"
  threshold           = 1
  treat_missing_data  = "notBreaching"

  dimensions = {
    SecretId = aws_secretsmanager_secret.this[each.key].id
  }

  tags = local.base_tags
}
