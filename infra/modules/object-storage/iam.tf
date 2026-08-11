locals {
  service_namespace = coalesce(var.service_namespace, "pcis-${var.environment_name}")
}

data "aws_iam_policy_document" "audit_svc_assume" {
  statement {
    effect  = "Allow"
    actions = ["sts:AssumeRoleWithWebIdentity"]

    principals {
      type        = "Federated"
      identifiers = [var.oidc_provider_arn]
    }

    condition {
      test     = "StringEquals"
      variable = "${var.oidc_provider_url}:aud"
      values   = ["sts.amazonaws.com"]
    }

    condition {
      test     = "StringEquals"
      variable = "${var.oidc_provider_url}:sub"
      values   = ["system:serviceaccount:${local.service_namespace}:${var.audit_svc_name}"]
    }
  }
}

resource "aws_iam_role" "audit_svc" {
  name_prefix        = "pcis-${var.environment_name}-audit-s3-"
  assume_role_policy = data.aws_iam_policy_document.audit_svc_assume.json
  tags = merge(local.base_tags, {
    Name    = "pcis-${var.environment_name}-${var.audit_svc_name}-s3"
    Service = var.audit_svc_name
    Purpose = "audit-archive-write"
  })
}

data "aws_iam_policy_document" "audit_svc" {
  statement {
    sid    = "WriteAuditArchive"
    effect = "Allow"
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

  statement {
    sid       = "EncryptWithAuditCMK"
    effect    = "Allow"
    actions   = ["kms:Encrypt", "kms:GenerateDataKey", "kms:DescribeKey"]
    resources = [aws_kms_key.s3_audit.arn]
  }
}

resource "aws_iam_role_policy" "audit_svc" {
  name   = "audit-archive-write"
  role   = aws_iam_role.audit_svc.id
  policy = data.aws_iam_policy_document.audit_svc.json
}
