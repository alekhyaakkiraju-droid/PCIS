output "secret_arns" {
  description = "Map of secret key → Secrets Manager ARN (no secret values)."
  value       = { for k, s in aws_secretsmanager_secret.this : k => s.arn }
}

output "secret_names" {
  description = "Map of secret key → Secrets Manager name."
  value       = { for k, s in aws_secretsmanager_secret.this : k => s.name }
}

output "rotation_enabled_secrets" {
  description = "Secret keys with 90-day automatic rotation configured."
  value       = keys(aws_secretsmanager_secret_rotation.aurora)
}

output "rotation_lambda_arn" {
  description = "Lambda ARN used for Aurora credential rotation."
  value       = local.rotation_lambda_arn
}

output "service_irsa_role_arns" {
  description = "Map of microservice → IRSA role ARN for Helm ServiceAccount annotations."
  value       = { for name, role in aws_iam_role.service_secrets : name => role.arn }
}

output "eso_service_account_role_arn" {
  description = "IRSA role ARN for the External Secrets Operator service account."
  value       = aws_iam_role.eso.arn
}

output "cluster_secret_store_name" {
  description = "ClusterSecretStore name when ESO is enabled."
  value       = var.enable_eso ? "aws-secrets-manager" : null
}
