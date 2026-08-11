variable "environment_name" {
  description = "Environment identifier (dev, tst, or prd)."
  type        = string

  validation {
    condition     = contains(["dev", "tst", "prd"], var.environment_name)
    error_message = "environment_name must be one of: dev, tst, prd."
  }
}

variable "oidc_provider_arn" {
  description = "EKS OIDC provider ARN for audit-svc IRSA trust."
  type        = string
}

variable "oidc_provider_url" {
  description = "EKS OIDC issuer URL without https:// prefix."
  type        = string
}

variable "service_namespace" {
  description = "Kubernetes namespace for the audit-svc ServiceAccount trust."
  type        = string
  default     = null
}

variable "audit_svc_name" {
  description = "ServiceAccount / microservice name allowed to PutObject."
  type        = string
  default     = "audit-svc"
}

variable "object_lock_retention_days" {
  description = "Default Object Lock retention period in days (compliance mode)."
  type        = number
  default     = 365

  validation {
    condition     = var.object_lock_retention_days >= 1
    error_message = "object_lock_retention_days must be at least 1."
  }
}

variable "glacier_transition_days" {
  description = "Days before transitioning audit objects to Glacier."
  type        = number
  default     = 90
}

variable "deep_archive_transition_days" {
  description = "Days before transitioning audit objects to Glacier Deep Archive."
  type        = number
  default     = 365
}

variable "noncurrent_version_expiration_days" {
  description = "Days before expiring non-current object versions."
  type        = number
  default     = 30
}

variable "flow_logs_expiration_days" {
  description = "Days before expiring VPC flow log objects."
  type        = number
  default     = 90
}

variable "tags" {
  description = "Common tags applied to AWS resources."
  type        = map(string)
  default     = {}
}
