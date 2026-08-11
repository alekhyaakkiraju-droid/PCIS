variable "environment_name" {
  description = "Environment identifier (dev, tst, or prd)."
  type        = string

  validation {
    condition     = contains(["dev", "tst", "prd"], var.environment_name)
    error_message = "environment_name must be one of: dev, tst, prd."
  }
}

variable "cluster_identifier" {
  description = "Aurora cluster identifier from the database module."
  type        = string
}

variable "cluster_resource_id" {
  description = "Cluster resource ID used in rds-db:connect ARNs."
  type        = string
}

variable "engine" {
  description = "Aurora engine identifier."
  type        = string
}

variable "engine_version" {
  description = "Aurora PostgreSQL engine version (17.x)."
  type        = string

  validation {
    condition     = startswith(var.engine_version, "17.")
    error_message = "engine_version must be Aurora PostgreSQL 17.x."
  }
}

variable "instance_class" {
  description = "Aurora instance class for the reporting read replica."
  type        = string
  default     = "db.r6g.large"
}

variable "availability_zones" {
  description = "AZs from the network module. Reader placement uses these."
  type        = list(string)

  validation {
    condition     = length(var.availability_zones) >= 1
    error_message = "At least one availability zone is required."
  }
}

variable "multi_az" {
  description = "When true (prd), place the reader in a different AZ from the writer."
  type        = bool
  default     = false
}

variable "db_subnet_group_name" {
  description = "DB subnet group name from the database module."
  type        = string
}

variable "kms_key_arn" {
  description = "KMS key ARN for Performance Insights encryption."
  type        = string
}

variable "enhanced_monitoring_interval" {
  description = "Enhanced Monitoring interval in seconds (0 disables)."
  type        = number
  default     = 60
}

variable "monitoring_role_arn" {
  description = "IAM role ARN for Enhanced Monitoring (null when disabled)."
  type        = string
  default     = null
}

variable "performance_insights_enabled" {
  description = "Enable Performance Insights on the reader instance."
  type        = bool
  default     = true
}

variable "statement_timeout_ms" {
  description = "statement_timeout for reporting workloads (milliseconds)."
  type        = number
  default     = 300000
}

variable "idle_in_transaction_session_timeout_ms" {
  description = "idle_in_transaction_session_timeout for reporting workloads (milliseconds)."
  type        = number
  default     = 60000
}

variable "lag_warning_seconds" {
  description = "CloudWatch alarm threshold for replica lag warning (seconds)."
  type        = number
  default     = 60
}

variable "lag_critical_seconds" {
  description = "CloudWatch alarm threshold for replica lag critical (seconds)."
  type        = number
  default     = 300
}

variable "oidc_provider_arn" {
  description = "EKS OIDC provider ARN used for IRSA trust policies."
  type        = string
}

variable "oidc_provider_url" {
  description = "EKS OIDC issuer URL without https:// prefix."
  type        = string
}

variable "service_namespace" {
  description = "Kubernetes namespace used in IRSA subject trusts (pcis-<env>)."
  type        = string
  default     = null
}

variable "tags" {
  description = "Common tags applied to AWS resources."
  type        = map(string)
  default     = {}
}
