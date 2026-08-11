variable "vpc_cidr" {
  description = "CIDR block for the VPC. Must be a /16 to accommodate the fixed subnet layout."
  type        = string
  default     = "10.0.0.0/16"

  validation {
    condition     = can(cidrnetmask(var.vpc_cidr))
    error_message = "vpc_cidr must be a valid CIDR block."
  }
}

variable "environment_name" {
  description = "Name of the deployment environment. Must be one of: dev, tst, prd."
  type        = string

  validation {
    condition     = contains(["dev", "tst", "prd"], var.environment_name)
    error_message = "environment_name must be one of: dev, tst, prd."
  }
}

variable "az_count" {
  description = <<-EOT
    Number of Availability Zones to use (2 or 3).
    Production uses 3 for maximum resilience; dev/tst use 2 for cost optimisation.
  EOT
  type        = number
  default     = 2

  validation {
    condition     = var.az_count >= 2 && var.az_count <= 3
    error_message = "az_count must be 2 or 3."
  }
}

variable "enable_ha_nat" {
  description = <<-EOT
    When true, a NAT Gateway is provisioned in every AZ for high availability.
    When false (dev/tst default), a single NAT Gateway in AZ-1 is used and all
    private subnets across every AZ route egress traffic through it.
  EOT
  type        = bool
  default     = false
}

variable "flow_log_retention_days" {
  description = "Number of days to retain VPC Flow Log data in the S3 bucket before expiry."
  type        = number
  default     = 90

  validation {
    condition     = var.flow_log_retention_days >= 1
    error_message = "flow_log_retention_days must be at least 1."
  }
}

variable "common_tags" {
  description = <<-EOT
    Additional tags to merge onto every resource.  The module always adds:
      Environment = var.environment_name
      Project     = "PCIS"
      ManagedBy   = "Terraform"
    Providing the same keys here will override the module defaults.
  EOT
  type        = map(string)
  default     = {}
}
