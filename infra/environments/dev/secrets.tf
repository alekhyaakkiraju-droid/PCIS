module "secrets" {
  source = "../../modules/secrets"

  environment_name       = var.environment_name
  oidc_provider_arn      = module.kubernetes.oidc_provider_arn
  oidc_provider_url      = module.kubernetes.oidc_provider_url
  enable_eso             = var.enable_external_secrets
  create_rotation_lambda = var.create_secret_rotation_lambda
  rotation_days          = var.secret_rotation_days
  eso_chart_version      = var.eso_chart_version

  tags = {
    Project   = "PCIS"
    ManagedBy = "Terraform"
  }

  depends_on = [
    module.kubernetes,
  ]
}

variable "enable_external_secrets" {
  type    = bool
  default = false
}

variable "create_secret_rotation_lambda" {
  type    = bool
  default = true
}

variable "secret_rotation_days" {
  type    = number
  default = 90
}

variable "eso_chart_version" {
  type    = string
  default = "0.9.20"
}

output "secret_arns" {
  value = module.secrets.secret_arns
}

output "eso_service_account_role_arn" {
  value = module.secrets.eso_service_account_role_arn
}

output "secrets_service_irsa_role_arns" {
  value = module.secrets.service_irsa_role_arns
}

output "cluster_secret_store_name" {
  value = module.secrets.cluster_secret_store_name
}
