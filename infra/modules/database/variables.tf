variable "environment_name" {
  description = "Environment identifier (dev, tst, or prd)."
  type        = string

  validation {
    condition     = contains(["dev", "tst", "prd"], var.environment_name)
    error_message = "environment_name must be one of: dev, tst, prd."
  }
}

variable "vpc_id" {
  description = "VPC ID from the network module (WO-129)."
  type        = string
}

variable "private_data_subnet_ids" {
  description = "Private data subnet IDs for the Aurora DB subnet group."
  type        = list(string)

  validation {
    condition     = length(var.private_data_subnet_ids) >= 2
    error_message = "At least two private data subnet IDs are required for Aurora."
  }
}

variable "private_app_subnet_cidrs" {
  description = "CIDRs of private application subnets allowed to reach PostgreSQL (5432)."
  type        = list(string)

  validation {
    condition     = length(var.private_app_subnet_cidrs) >= 1
    error_message = "At least one private application subnet CIDR is required."
  }
}

variable "availability_zones" {
  description = "AZs from the network module. Writer/reader placement uses these."
  type        = list(string)

  validation {
    condition     = length(var.availability_zones) >= 1
    error_message = "At least one availability zone is required."
  }
}

variable "multi_az" {
  description = "When true (prd), place the reader in a different AZ from the writer. When false (dev/tst), colocate for cost."
  type        = bool
  default     = false
}

variable "instance_class" {
  description = "Aurora instance class for writer and reader."
  type        = string
  default     = "db.r6g.large"
}

variable "engine_version" {
  description = "Aurora PostgreSQL engine version (17.x)."
  type        = string
  default     = "17.4"

  validation {
    condition     = startswith(var.engine_version, "17.")
    error_message = "engine_version must be Aurora PostgreSQL 17.x."
  }
}

variable "database_name" {
  description = "Initial database name created in the cluster."
  type        = string
  default     = "pcis"
}

variable "master_username" {
  description = "Master username (password managed by AWS Secrets Manager)."
  type        = string
  default     = "pcis_admin"
}

variable "backup_retention_period" {
  description = "Automated backup retention in days (PITR enabled while retention > 0)."
  type        = number
  default     = 35

  validation {
    condition     = var.backup_retention_period >= 35
    error_message = "backup_retention_period must be at least 35 days for PCIS RPO targets."
  }
}

variable "preferred_backup_window" {
  description = "Daily backup window (UTC)."
  type        = string
  default     = "02:00-03:00"
}

variable "preferred_maintenance_window" {
  description = "Weekly maintenance window (UTC)."
  type        = string
  default     = "sun:03:00-sun:04:00"
}

variable "deletion_protection" {
  description = "Protect the cluster from accidental deletion (recommended for prd)."
  type        = bool
  default     = false
}

variable "microservice_names" {
  description = "Microservice names that receive IRSA roles for IAM database authentication."
  type        = list(string)
  default = [
    "claims-svc",
    "policy-svc",
    "billing-svc",
    "reporting-svc",
    "authz-svc",
  ]
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

variable "enhanced_monitoring_interval" {
  description = "Enhanced Monitoring interval in seconds (0 disables)."
  type        = number
  default     = 60
}

variable "performance_insights_enabled" {
  description = "Enable Performance Insights on cluster instances."
  type        = bool
  default     = true
}

variable "tags" {
  description = "Common tags applied to AWS resources."
  type        = map(string)
  default     = {}
}
