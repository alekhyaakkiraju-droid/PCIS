variable "environment_name" {
  description = "Environment identifier (dev, tst, or prd)."
  type        = string

  validation {
    condition     = contains(["dev", "tst", "prd"], var.environment_name)
    error_message = "environment_name must be one of: dev, tst, prd."
  }
}

variable "oidc_provider_arn" {
  description = "EKS OIDC provider ARN for IRSA trust policies."
  type        = string
}

variable "oidc_provider_url" {
  description = "EKS OIDC issuer URL without https:// prefix."
  type        = string
}

variable "service_namespace" {
  description = "Kubernetes namespace used in IRSA subject trusts."
  type        = string
  default     = null
}

variable "microservice_secret_access" {
  description = "Map of microservice → secret keys it may read (least privilege)."
  type        = map(list(string))
  default = {
    "claims-svc"    = ["aurora-writer", "kafka-sasl"]
    "policy-svc"    = ["aurora-writer", "kafka-sasl"]
    "billing-svc"   = ["aurora-writer", "kafka-sasl"]
    "reporting-svc" = ["aurora-reader", "kafka-sasl"]
    "authz-svc"     = ["aurora-writer", "keycloak-client"]
  }
}

variable "rotation_days" {
  description = "Maximum automatic rotation cycle for database credentials (days)."
  type        = number
  default     = 90

  validation {
    condition     = var.rotation_days > 0 && var.rotation_days <= 90
    error_message = "rotation_days must be between 1 and 90."
  }
}

variable "create_rotation_lambda" {
  description = "When true, deploy the AWS Secrets Manager RDS PostgreSQL single-user rotation Lambda via SAR."
  type        = bool
  default     = true
}

variable "rotation_lambda_arn" {
  description = "Optional existing rotation Lambda ARN. Used when create_rotation_lambda=false."
  type        = string
  default     = null
}

variable "enable_eso" {
  description = "Install External Secrets Operator via Helm and create ClusterSecretStore. Requires live EKS + Helm/k8s providers."
  type        = bool
  default     = false
}

variable "eso_chart_version" {
  description = "Pinned External Secrets Operator Helm chart version."
  type        = string
  default     = "0.9.20"
}

variable "external_secret_refresh_interval" {
  description = "Default ExternalSecret refresh interval shown in examples and ClusterSecretStore docs."
  type        = string
  default     = "1h"
}

variable "tags" {
  description = "Common tags applied to AWS resources."
  type        = map(string)
  default     = {}
}
