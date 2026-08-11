output "namespace" {
  description = "Kubernetes namespace hosting Keycloak."
  value       = kubernetes_namespace.keycloak.metadata[0].name
}

output "helm_release_name" {
  description = "Helm release name for Keycloak."
  value       = helm_release.keycloak.name
}

output "helm_release_version" {
  description = "Installed Helm chart version."
  value       = helm_release.keycloak.version
}

output "ingress_host" {
  description = "Configured Keycloak ingress hostname."
  value       = var.ingress_host
}

output "issuer_url" {
  description = "OIDC issuer URL for the pcis realm."
  value       = "https://${var.ingress_host}/realms/pcis"
}

output "discovery_url" {
  description = "OIDC discovery document URL."
  value       = "https://${var.ingress_host}/realms/pcis/.well-known/openid-configuration"
}

output "jwks_url" {
  description = "JWKS endpoint for RS256 JWT validation."
  value       = "https://${var.ingress_host}/realms/pcis/protocol/openid-connect/certs"
}

output "admin_secret_arn" {
  description = "Secrets Manager ARN for Keycloak admin credentials (value set out-of-band)."
  value       = try(aws_secretsmanager_secret.admin[0].arn, null)
}

output "admin_secret_name" {
  description = "Secrets Manager name for Keycloak admin credentials."
  value       = local.admin_secret_name
}

output "db_secret_arn" {
  description = "Secrets Manager ARN for Keycloak DB password (value set out-of-band)."
  value       = try(aws_secretsmanager_secret.db[0].arn, null)
}

output "db_secret_name" {
  description = "Secrets Manager name for Keycloak DB password."
  value       = local.db_secret_name
}

output "replica_count" {
  description = "Configured Keycloak replica count."
  value       = var.replica_count
}
