# Per-microservice IRSA roles with least-privilege GetSecretValue on assigned secrets only.

data "aws_iam_policy_document" "irsa_assume" {
  for_each = var.microservice_secret_access

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

resource "aws_iam_role" "service_secrets" {
  for_each = var.microservice_secret_access

  name_prefix        = "pcis-${var.environment_name}-sec-${each.key}-"
  assume_role_policy = data.aws_iam_policy_document.irsa_assume[each.key].json
  tags = merge(local.base_tags, {
    Name    = "pcis-${var.environment_name}-sec-${each.key}"
    Service = each.key
    Purpose = "secretsmanager-irsa"
  })
}

data "aws_iam_policy_document" "service_secrets" {
  for_each = var.microservice_secret_access

  statement {
    sid    = "ReadOwnSecretsOnly"
    effect = "Allow"
    actions = [
      "secretsmanager:GetSecretValue",
      "secretsmanager:DescribeSecret",
    ]
    resources = [
      for key in each.value : aws_secretsmanager_secret.this[key].arn
    ]
  }

  statement {
    sid       = "DecryptSecretsCMK"
    effect    = "Allow"
    actions   = ["kms:Decrypt", "kms:DescribeKey"]
    resources = [aws_kms_key.secrets.arn]
  }
}

resource "aws_iam_role_policy" "service_secrets" {
  for_each = var.microservice_secret_access

  name   = "secrets-least-privilege"
  role   = aws_iam_role.service_secrets[each.key].id
  policy = data.aws_iam_policy_document.service_secrets[each.key].json
}

# IRSA role used by External Secrets Operator to sync all PCIS secrets into the cluster.
data "aws_iam_policy_document" "eso_assume" {
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
      values   = ["system:serviceaccount:external-secrets:external-secrets"]
    }
  }
}

resource "aws_iam_role" "eso" {
  name_prefix        = "pcis-${var.environment_name}-eso-"
  assume_role_policy = data.aws_iam_policy_document.eso_assume.json
  tags = merge(local.base_tags, {
    Name    = "pcis-${var.environment_name}-eso"
    Purpose = "external-secrets-operator"
  })
}

data "aws_iam_policy_document" "eso" {
  statement {
    sid    = "ESOReadPCISSecrets"
    effect = "Allow"
    actions = [
      "secretsmanager:GetSecretValue",
      "secretsmanager:DescribeSecret",
      "secretsmanager:ListSecrets",
    ]
    resources = [
      for s in aws_secretsmanager_secret.this : s.arn
    ]
  }

  statement {
    sid       = "ESODecryptSecretsCMK"
    effect    = "Allow"
    actions   = ["kms:Decrypt", "kms:DescribeKey"]
    resources = [aws_kms_key.secrets.arn]
  }
}

resource "aws_iam_role_policy" "eso" {
  name   = "eso-secretsmanager"
  role   = aws_iam_role.eso.id
  policy = data.aws_iam_policy_document.eso.json
}
