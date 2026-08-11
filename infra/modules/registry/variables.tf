variable "environment_name" {
  description = "Environment identifier (dev, tst, or prd)."
  type        = string

  validation {
    condition     = contains(["dev", "tst", "prd"], var.environment_name)
    error_message = "environment_name must be one of: dev, tst, prd."
  }
}

variable "repository_names" {
  description = "Microservice and base image repository short names (without pcis- prefix)."
  type        = list(string)
  default = [
    "customer-svc",
    "claims-svc",
    "policy-svc",
    "premium-svc",
    "billing-svc",
    "reporting-svc",
    "authz-svc",
    "audit-svc",
    "pcis-base-java21",
  ]

  validation {
    condition = alltrue([
      for name in var.repository_names : length(name) <= 200
    ])
    error_message = "Each repository name must be <= 200 characters (ECR limit is 256 including prefix)."
  }
}

variable "production_account_id" {
  description = "AWS account ID allowed to pull images cross-account (typically the prd account)."
  type        = string

  validation {
    condition     = can(regex("^[0-9]{12}$", var.production_account_id))
    error_message = "production_account_id must be a 12-digit AWS account ID."
  }
}

variable "cross_account_pull_enabled" {
  description = "When false, skip cross-account repository policies (e.g. in isolated dev sandboxes)."
  type        = bool
  default     = true
}

variable "tagged_image_retention_count" {
  description = "Maximum number of tagged images to retain per repository."
  type        = number
  default     = 30

  validation {
    condition     = var.tagged_image_retention_count >= 1
    error_message = "tagged_image_retention_count must be at least 1."
  }
}

variable "untagged_image_expiration_days" {
  description = "Days after which untagged images are expired."
  type        = number
  default     = 7

  validation {
    condition     = var.untagged_image_expiration_days >= 1
    error_message = "untagged_image_expiration_days must be at least 1."
  }
}

variable "enable_scan_event_rule" {
  description = "Create an EventBridge rule matching ECR Image Scan events for PCIS repositories."
  type        = bool
  default     = true
}

variable "tags" {
  description = "Additional tags applied to all registry resources."
  type        = map(string)
  default     = {}
}
