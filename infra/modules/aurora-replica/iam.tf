data "aws_iam_policy_document" "reporting_irsa_assume" {
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
      values   = ["system:serviceaccount:${local.service_namespace}:reporting-svc"]
    }
  }
}

resource "aws_iam_role" "reporting_reader_irsa" {
  name_prefix        = "pcis-${var.environment_name}-db-reporting-svc-"
  assume_role_policy = data.aws_iam_policy_document.reporting_irsa_assume.json

  tags = merge(local.base_tags, {
    Name       = "pcis-${var.environment_name}-db-reporting-svc"
    Service    = "reporting-svc"
    Purpose    = "aurora-reader-iam-auth"
    ConsumedBy = "helm"
  })
}

data "aws_iam_policy_document" "reporting_reader_connect" {
  statement {
    sid     = "RDSIAMDatabaseConnectReader"
    effect  = "Allow"
    actions = ["rds-db:connect"]
    resources = [
      "arn:${data.aws_partition.current.partition}:rds-db:${data.aws_region.current.name}:${data.aws_caller_identity.current.account_id}:dbuser:${var.cluster_resource_id}/reporting-svc",
    ]
  }
}

resource "aws_iam_role_policy" "reporting_reader_connect" {
  name   = "rds-db-connect-reader"
  role   = aws_iam_role.reporting_reader_irsa.id
  policy = data.aws_iam_policy_document.reporting_reader_connect.json
}
