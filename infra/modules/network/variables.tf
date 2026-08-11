variable "environment_name" {
  description = "Environment identifier (dev, tst, or prd)."
  type        = string

  validation {
    condition     = contains(["dev", "tst", "prd"], var.environment_name)
    error_message = "environment_name must be one of: dev, tst, prd."
  }
}

variable "vpc_cidr" {
  description = "CIDR block for the VPC. Must not overlap other environments."
  type        = string

  validation {
    condition     = can(cidrnetmask(var.vpc_cidr))
    error_message = "vpc_cidr must be a valid IPv4 CIDR block."
  }
}

variable "az_count" {
  description = "Number of availability zones to use (handles regions with fewer than 3 AZs)."
  type        = number
  default     = 2

  validation {
    condition     = var.az_count >= 2 && var.az_count <= 3
    error_message = "az_count must be 2 or 3."
  }
}

variable "enable_ha_nat" {
  description = "When true, create one NAT gateway per AZ (prd). When false, create a single shared NAT (dev/tst)."
  type        = bool
  default     = false
}

variable "flow_log_retention_days" {
  description = "S3 lifecycle expiration (days) for VPC flow logs. Default 90."
  type        = number
  default     = 90

  validation {
    condition     = var.flow_log_retention_days >= 1
    error_message = "flow_log_retention_days must be at least 1."
  }
}

variable "aws_region" {
  description = "AWS region for resource naming and data lookups. Prefer data sources over hardcoding account IDs."
  type        = string
  default     = null
}

variable "tags" {
  description = "Common tags applied to all resources (must include Environment, Project, ManagedBy at composition layer)."
  type        = map(string)
  default     = {}
}
