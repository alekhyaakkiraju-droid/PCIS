data "aws_caller_identity" "current" {}
data "aws_partition" "current" {}
data "aws_region" "current" {}

# Dedicated S3 audit CMK — separate from Aurora (WO-131) and Secrets (WO-132).
# Edge case: accidental key deletion makes objects unreadable — long deletion window + no schedule by default.
resource "aws_kms_key" "s3_audit" {
  description             = "PCIS S3 audit archive encryption key (${var.environment_name})"
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
        Sid    = "AllowS3UseOfTheKey"
        Effect = "Allow"
        Principal = {
          Service = "s3.amazonaws.com"
        }
        Action = [
          "kms:Encrypt",
          "kms:Decrypt",
          "kms:ReEncrypt*",
          "kms:GenerateDataKey*",
          "kms:DescribeKey",
        ]
        Resource = "*"
        Condition = {
          StringEquals = {
            "kms:ViaService" = "s3.${data.aws_region.current.name}.amazonaws.com"
          }
        }
      },
    ]
  })

  tags = merge(local.base_tags, {
    Name = "pcis-s3-audit-${var.environment_name}"
  })
}

resource "aws_kms_alias" "s3_audit" {
  name          = "alias/pcis-s3-audit-${var.environment_name}"
  target_key_id = aws_kms_key.s3_audit.key_id
}
