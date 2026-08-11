data "aws_caller_identity" "current" {}
data "aws_partition" "current" {}
data "aws_region" "current" {}

resource "aws_kms_key" "aurora" {
  description             = "PCIS Aurora PostgreSQL encryption key (${var.environment_name})"
  deletion_window_in_days = 30
  enable_key_rotation     = true
  multi_region            = false

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Sid    = "EnableRootAccountAdmin"
        Effect = "Allow"
        Principal = {
          AWS = "arn:${data.aws_partition.current.partition}:iam::${data.aws_caller_identity.current.account_id}:root"
        }
        Action   = "kms:*"
        Resource = "*"
      },
      {
        Sid    = "AllowRDSUseOfTheKey"
        Effect = "Allow"
        Principal = {
          Service = "rds.amazonaws.com"
        }
        Action = [
          "kms:Encrypt",
          "kms:Decrypt",
          "kms:ReEncrypt*",
          "kms:GenerateDataKey*",
          "kms:CreateGrant",
          "kms:DescribeKey",
        ]
        Resource = "*"
        Condition = {
          StringEquals = {
            "kms:ViaService" = "rds.${data.aws_region.current.name}.amazonaws.com"
          }
        }
      },
    ]
  })

  tags = merge(local.base_tags, {
    Name = "pcis-aurora-${var.environment_name}"
  })

  lifecycle {
    prevent_destroy = false
  }
}

resource "aws_kms_alias" "aurora" {
  name          = "alias/pcis-aurora-${var.environment_name}"
  target_key_id = aws_kms_key.aurora.key_id
}
