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

  account_id = data.aws_caller_identity.current.account_id

  audit_bucket_name     = "pcis-audit-archive-${var.environment_name}-${local.account_id}"
  flow_logs_bucket_name = "pcis-flow-logs-${var.environment_name}-${local.account_id}"
  tf_state_bucket_name  = "pcis-terraform-state-${var.environment_name}-${local.account_id}"
}

# ---------------------------------------------------------------------------
# Audit archive — Object Lock compliance mode (SOX/SOC 2 immutability)
# ---------------------------------------------------------------------------
resource "aws_s3_bucket" "audit" {
  bucket              = local.audit_bucket_name
  object_lock_enabled = true
  force_destroy       = false

  tags = merge(local.base_tags, {
    Name    = local.audit_bucket_name
    Purpose = "audit-archive"
    Zone    = "Data"
  })
}

resource "aws_s3_bucket_versioning" "audit" {
  bucket = aws_s3_bucket.audit.id

  versioning_configuration {
    status = "Enabled"
  }
}

resource "aws_s3_bucket_object_lock_configuration" "audit" {
  bucket = aws_s3_bucket.audit.id

  rule {
    default_retention {
      mode = "COMPLIANCE"
      days = var.object_lock_retention_days
    }
  }

  depends_on = [aws_s3_bucket_versioning.audit]
}

resource "aws_s3_bucket_server_side_encryption_configuration" "audit" {
  bucket = aws_s3_bucket.audit.id

  rule {
    apply_server_side_encryption_by_default {
      sse_algorithm     = "aws:kms"
      kms_master_key_id = aws_kms_key.s3_audit.arn
    }
    bucket_key_enabled = true
  }
}

resource "aws_s3_bucket_public_access_block" "audit" {
  bucket = aws_s3_bucket.audit.id

  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}

resource "aws_s3_bucket_lifecycle_configuration" "audit" {
  bucket = aws_s3_bucket.audit.id

  rule {
    id     = "tiered-archive"
    status = "Enabled"

    filter {
      prefix = ""
    }

    transition {
      days          = var.glacier_transition_days
      storage_class = "GLACIER"
    }

    transition {
      days          = var.deep_archive_transition_days
      storage_class = "DEEP_ARCHIVE"
    }

    noncurrent_version_expiration {
      noncurrent_days = var.noncurrent_version_expiration_days
    }
  }

  depends_on = [aws_s3_bucket_versioning.audit]
}

data "aws_iam_policy_document" "audit_bucket" {
  statement {
    sid    = "DenyInsecureTransport"
    effect = "Deny"
    principals {
      type        = "*"
      identifiers = ["*"]
    }
    actions = ["s3:*"]
    resources = [
      aws_s3_bucket.audit.arn,
      "${aws_s3_bucket.audit.arn}/*",
    ]
    condition {
      test     = "Bool"
      variable = "aws:SecureTransport"
      values   = ["false"]
    }
  }

  statement {
    sid    = "DenyPublicAccess"
    effect = "Deny"
    principals {
      type        = "*"
      identifiers = ["*"]
    }
    actions = ["s3:*"]
    resources = [
      aws_s3_bucket.audit.arn,
      "${aws_s3_bucket.audit.arn}/*",
    ]
    condition {
      test     = "StringEquals"
      variable = "s3:ExistingObjectTag/Public"
      values   = ["true"]
    }
  }

  # Only audit-svc IRSA may PutObject. Edge case: delete before retention → AccessDenied via Object Lock.
  statement {
    sid    = "DenyPutObjectExceptAuditSvc"
    effect = "Deny"
    principals {
      type        = "*"
      identifiers = ["*"]
    }
    actions = ["s3:PutObject", "s3:PutObjectAcl"]
    resources = [
      "${aws_s3_bucket.audit.arn}/*",
    ]
    condition {
      test     = "ArnNotEquals"
      variable = "aws:PrincipalArn"
      values   = [aws_iam_role.audit_svc.arn]
    }
  }

  statement {
    sid    = "AllowAuditSvcWrite"
    effect = "Allow"
    principals {
      type        = "AWS"
      identifiers = [aws_iam_role.audit_svc.arn]
    }
    actions = [
      "s3:PutObject",
      "s3:AbortMultipartUpload",
      "s3:ListBucket",
      "s3:GetBucketLocation",
    ]
    resources = [
      aws_s3_bucket.audit.arn,
      "${aws_s3_bucket.audit.arn}/*",
    ]
  }
}

resource "aws_s3_bucket_policy" "audit" {
  bucket = aws_s3_bucket.audit.id
  policy = data.aws_iam_policy_document.audit_bucket.json

  depends_on = [aws_s3_bucket_public_access_block.audit]
}

# ---------------------------------------------------------------------------
# VPC flow logs (no Object Lock) — 90-day expiration
# ---------------------------------------------------------------------------
resource "aws_s3_bucket" "flow_logs" {
  bucket        = local.flow_logs_bucket_name
  force_destroy = var.environment_name != "prd"

  tags = merge(local.base_tags, {
    Name    = local.flow_logs_bucket_name
    Purpose = "vpc-flow-logs"
  })
}

resource "aws_s3_bucket_versioning" "flow_logs" {
  bucket = aws_s3_bucket.flow_logs.id
  versioning_configuration {
    status = "Enabled"
  }
}

resource "aws_s3_bucket_server_side_encryption_configuration" "flow_logs" {
  bucket = aws_s3_bucket.flow_logs.id
  rule {
    apply_server_side_encryption_by_default {
      sse_algorithm     = "aws:kms"
      kms_master_key_id = aws_kms_key.s3_audit.arn
    }
    bucket_key_enabled = true
  }
}

resource "aws_s3_bucket_public_access_block" "flow_logs" {
  bucket = aws_s3_bucket.flow_logs.id

  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}

resource "aws_s3_bucket_lifecycle_configuration" "flow_logs" {
  bucket = aws_s3_bucket.flow_logs.id

  rule {
    id     = "expire-flow-logs"
    status = "Enabled"
    filter {
      prefix = ""
    }
    expiration {
      days = var.flow_logs_expiration_days
    }
  }
}

# ---------------------------------------------------------------------------
# Terraform state (no Object Lock) — versioned
# ---------------------------------------------------------------------------
resource "aws_s3_bucket" "terraform_state" {
  bucket        = local.tf_state_bucket_name
  force_destroy = false

  tags = merge(local.base_tags, {
    Name    = local.tf_state_bucket_name
    Purpose = "terraform-state"
  })
}

resource "aws_s3_bucket_versioning" "terraform_state" {
  bucket = aws_s3_bucket.terraform_state.id
  versioning_configuration {
    status = "Enabled"
  }
}

resource "aws_s3_bucket_server_side_encryption_configuration" "terraform_state" {
  bucket = aws_s3_bucket.terraform_state.id
  rule {
    apply_server_side_encryption_by_default {
      sse_algorithm     = "aws:kms"
      kms_master_key_id = aws_kms_key.s3_audit.arn
    }
    bucket_key_enabled = true
  }
}

resource "aws_s3_bucket_public_access_block" "terraform_state" {
  bucket = aws_s3_bucket.terraform_state.id

  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}

resource "aws_s3_bucket_lifecycle_configuration" "terraform_state" {
  bucket = aws_s3_bucket.terraform_state.id

  rule {
    id     = "expire-old-state-versions"
    status = "Enabled"
    filter {
      prefix = ""
    }
    noncurrent_version_expiration {
      noncurrent_days = 90
    }
  }

  depends_on = [aws_s3_bucket_versioning.terraform_state]
}
