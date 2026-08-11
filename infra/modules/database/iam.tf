# IRSA roles for IAM database authentication (one role per microservice).
# Downstream Helm charts bind ServiceAccounts to these role ARNs.

data "aws_iam_policy_document" "irsa_assume" {
  for_each = toset(var.microservice_names)

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
      values   = ["system:serviceaccount:${local.service_namespace}:${each.key}"]
    }
  }
}

resource "aws_iam_role" "db_irsa" {
  for_each = toset(var.microservice_names)

  name_prefix        = "pcis-${var.environment_name}-db-${each.key}-"
  assume_role_policy = data.aws_iam_policy_document.irsa_assume[each.key].json
  tags = merge(local.base_tags, {
    Name       = "pcis-${var.environment_name}-db-${each.key}"
    Service    = each.key
    Purpose    = "aurora-iam-auth"
    ConsumedBy = "helm"
  })
}

data "aws_iam_policy_document" "db_connect" {
  for_each = toset(var.microservice_names)

  statement {
    sid     = "RDSIAMDatabaseConnect"
    effect  = "Allow"
    actions = ["rds-db:connect"]
    resources = [
      # DB username matches the microservice name for least privilege.
      "arn:${data.aws_partition.current.partition}:rds-db:${data.aws_region.current.name}:${data.aws_caller_identity.current.account_id}:dbuser:${aws_rds_cluster.aurora.cluster_resource_id}/${each.key}",
    ]
  }
}

resource "aws_iam_role_policy" "db_connect" {
  for_each = toset(var.microservice_names)

  name   = "rds-db-connect"
  role   = aws_iam_role.db_irsa[each.key].id
  policy = data.aws_iam_policy_document.db_connect[each.key].json
}
