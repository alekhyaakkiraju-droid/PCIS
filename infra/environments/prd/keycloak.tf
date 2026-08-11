# Keycloak 26.x OIDC IdP (WO-145) — production wiring

module "keycloak" {
  count  = var.enable_keycloak ? 1 : 0
  source = "../../modules/keycloak"

  environment_name     = var.environment_name
  aws_region           = var.aws_region
  replica_count        = var.keycloak_replica_count
  resources            = var.keycloak_resources
  certificate_arn      = var.keycloak_certificate_arn
  ingress_host         = var.keycloak_ingress_host
  ingress_scheme       = var.keycloak_ingress_scheme
  db_host              = var.keycloak_db_host
  db_port              = var.keycloak_db_port
  db_name              = var.keycloak_db_name
  db_username          = var.keycloak_db_username
  admin_username       = var.keycloak_admin_username
  enable_realm_import  = var.keycloak_enable_realm_import
  create_secret_shells = var.keycloak_create_secret_shells

  tags = {
    Project   = "PCIS"
    ManagedBy = "Terraform"
  }

  depends_on = [
    module.kubernetes,
  ]
}

variable "enable_keycloak" {
  type    = bool
  default = false
}

variable "keycloak_replica_count" {
  type    = number
  default = 3
}

variable "keycloak_resources" {
  type = object({
    requests = object({
      cpu    = string
      memory = string
    })
    limits = object({
      cpu    = string
      memory = string
    })
  })
  default = {
    requests = { cpu = "1", memory = "2Gi" }
    limits   = { cpu = "4", memory = "4Gi" }
  }
}

variable "keycloak_certificate_arn" {
  type    = string
  default = ""
}

variable "keycloak_ingress_host" {
  type    = string
  default = "auth.pcis.example.com"
}

variable "keycloak_ingress_scheme" {
  type    = string
  default = "internal"
}

variable "keycloak_db_host" {
  type    = string
  default = "REPLACE_KEYCLOAK_DB_HOST"
}

variable "keycloak_db_port" {
  type    = number
  default = 5432
}

variable "keycloak_db_name" {
  type    = string
  default = "keycloak"
}

variable "keycloak_db_username" {
  type    = string
  default = "keycloak"
}

variable "keycloak_admin_username" {
  type    = string
  default = "pcis-kc-admin"
}

variable "keycloak_enable_realm_import" {
  type    = bool
  default = false
}

variable "keycloak_create_secret_shells" {
  type    = bool
  default = true
}

output "keycloak_issuer_url" {
  value = try(module.keycloak[0].issuer_url, null)
}

output "keycloak_discovery_url" {
  value = try(module.keycloak[0].discovery_url, null)
}

output "keycloak_jwks_url" {
  value = try(module.keycloak[0].jwks_url, null)
}

output "keycloak_admin_secret_arn" {
  value = try(module.keycloak[0].admin_secret_arn, null)
}

output "keycloak_namespace" {
  value = try(module.keycloak[0].namespace, null)
}
