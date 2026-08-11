locals {
  base_tags = merge(
    {
      Project   = "PCIS"
      ManagedBy = "Terraform"
      Component = "registry"
    },
    var.tags,
    {
      Environment = var.environment_name
    }
  )

  # Map short name -> full ECR repository name (<= 256 chars).
  repository_map = {
    for name in var.repository_names :
    name => "pcis-${name}-${var.environment_name}"
  }
}

data "aws_caller_identity" "current" {}

data "aws_region" "current" {}

# ---------------------------------------------------------------------------
# ECR repositories — scan on push, immutable tags
# ---------------------------------------------------------------------------
resource "aws_ecr_repository" "service" {
  for_each = local.repository_map

  name                 = each.value
  image_tag_mutability = "IMMUTABLE"
  force_delete         = var.environment_name != "prd"

  image_scanning_configuration {
    scan_on_push = true
  }

  encryption_configuration {
    encryption_type = "AES256"
  }

  tags = merge(local.base_tags, {
    Name        = each.value
    ServiceName = each.key
  })
}

# ---------------------------------------------------------------------------
# Lifecycle — retain last N tagged images; expire untagged after 7 days
# ---------------------------------------------------------------------------
resource "aws_ecr_lifecycle_policy" "service" {
  for_each   = aws_ecr_repository.service
  repository = each.value.name

  policy = jsonencode({
    rules = [
      {
        rulePriority = 1
        description  = "Expire untagged images after ${var.untagged_image_expiration_days} days"
        selection = {
          tagStatus   = "untagged"
          countType   = "sinceImagePushed"
          countUnit   = "days"
          countNumber = var.untagged_image_expiration_days
        }
        action = {
          type = "expire"
        }
      },
      {
        rulePriority = 2
        description  = "Retain only the last ${var.tagged_image_retention_count} tagged images"
        selection = {
          tagStatus   = "tagged"
          countType   = "imageCountMoreThan"
          countNumber = var.tagged_image_retention_count
        }
        action = {
          type = "expire"
        }
      },
    ]
  })
}

# ---------------------------------------------------------------------------
# Cross-account pull from production (specific account ID — no wildcards)
# ---------------------------------------------------------------------------
data "aws_iam_policy_document" "cross_account_pull" {
  count = var.cross_account_pull_enabled ? 1 : 0

  statement {
    sid    = "AllowProductionAccountPull"
    effect = "Allow"
    principals {
      type        = "AWS"
      identifiers = ["arn:aws:iam::${var.production_account_id}:root"]
    }
    actions = [
      "ecr:GetDownloadUrlForLayer",
      "ecr:BatchGetImage",
      "ecr:BatchCheckLayerAvailability",
      "ecr:DescribeImages",
      "ecr:DescribeRepositories",
      "ecr:ListImages",
    ]
  }
}

resource "aws_ecr_repository_policy" "cross_account_pull" {
  for_each = var.cross_account_pull_enabled ? aws_ecr_repository.service : {}

  repository = each.value.name
  policy     = data.aws_iam_policy_document.cross_account_pull[0].json
}

# ---------------------------------------------------------------------------
# EventBridge — route ECR image scan findings (aws.ecr / ECR Image Scan)
# ---------------------------------------------------------------------------
resource "aws_cloudwatch_event_rule" "ecr_scan_findings" {
  count = var.enable_scan_event_rule ? 1 : 0

  name        = "pcis-ecr-image-scan-${var.environment_name}"
  description = "Capture ECR image scan findings for PCIS ${var.environment_name} repositories"

  event_pattern = jsonencode({
    source      = ["aws.ecr"]
    detail-type = ["ECR Image Scan"]
    detail = {
      "repository-name" = values(local.repository_map)
    }
  })

  tags = local.base_tags
}

resource "aws_cloudwatch_log_group" "ecr_scan_findings" {
  count = var.enable_scan_event_rule ? 1 : 0

  name              = "/pcis/ecr-scan-findings/${var.environment_name}"
  retention_in_days = var.environment_name == "prd" ? 90 : 30

  tags = local.base_tags
}

resource "aws_iam_role" "ecr_scan_eventbridge" {
  count = var.enable_scan_event_rule ? 1 : 0

  name = "pcis-ecr-scan-events-${var.environment_name}"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Effect = "Allow"
      Principal = {
        Service = "events.amazonaws.com"
      }
      Action = "sts:AssumeRole"
    }]
  })

  tags = local.base_tags
}

resource "aws_iam_role_policy" "ecr_scan_eventbridge_logs" {
  count = var.enable_scan_event_rule ? 1 : 0

  name = "cloudwatch-logs"
  role = aws_iam_role.ecr_scan_eventbridge[0].id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Effect = "Allow"
      Action = [
        "logs:CreateLogStream",
        "logs:PutLogEvents",
      ]
      Resource = "${aws_cloudwatch_log_group.ecr_scan_findings[0].arn}:*"
    }]
  })
}

resource "aws_cloudwatch_event_target" "ecr_scan_findings_log" {
  count = var.enable_scan_event_rule ? 1 : 0

  rule      = aws_cloudwatch_event_rule.ecr_scan_findings[0].name
  target_id = "ecr-scan-findings-log"
  arn       = aws_cloudwatch_log_group.ecr_scan_findings[0].arn
  role_arn  = aws_iam_role.ecr_scan_eventbridge[0].arn
}
