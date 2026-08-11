# ─────────────────────────────────────────────────────────────────────────────
# Variables (used by the provider and module below)
# ─────────────────────────────────────────────────────────────────────────────

variable "aws_region" {
  description = "AWS region to deploy to. No default — must be set in terraform.tfvars or via TF_VAR_aws_region."
  type        = string
}

variable "vpc_cidr" {
  description = "VPC CIDR block for the dev environment."
  type        = string
  default     = "10.0.0.0/16"
}

variable "az_count" {
  description = "Number of Availability Zones to use."
  type        = number
  default     = 2
}

variable "enable_ha_nat" {
  description = "Enable one NAT Gateway per AZ (false for dev to reduce cost)."
  type        = bool
  default     = false
}

variable "flow_log_retention_days" {
  description = "Retention period in days for VPC flow logs."
  type        = number
  default     = 30
}

# ─────────────────────────────────────────────────────────────────────────────
# Network Module
# ─────────────────────────────────────────────────────────────────────────────

module "network" {
  source = "../../modules/network"

  environment_name        = "dev"
  vpc_cidr                = var.vpc_cidr
  az_count                = var.az_count
  enable_ha_nat           = var.enable_ha_nat
  flow_log_retention_days = var.flow_log_retention_days

  common_tags = {
    CostCenter = "PCIS-Engineering"
    Owner      = "platform-team"
  }
}

# ─────────────────────────────────────────────────────────────────────────────
# Outputs (forwarded from module for downstream consumption by EKS, Aurora, etc.)
# ─────────────────────────────────────────────────────────────────────────────

output "vpc_id" {
  description = "VPC ID."
  value       = module.network.vpc_id
}

output "vpc_cidr_block" {
  description = "VPC CIDR block."
  value       = module.network.vpc_cidr_block
}

output "public_subnet_ids" {
  description = "Public subnet IDs."
  value       = module.network.public_subnet_ids
}

output "private_app_subnet_ids" {
  description = "Private application subnet IDs."
  value       = module.network.private_app_subnet_ids
}

output "private_data_subnet_ids" {
  description = "Private data subnet IDs."
  value       = module.network.private_data_subnet_ids
}

output "nat_gateway_ids" {
  description = "NAT Gateway IDs."
  value       = module.network.nat_gateway_ids
}

output "alb_security_group_id" {
  description = "ALB security group ID."
  value       = module.network.alb_security_group_id
}

output "database_security_group_id" {
  description = "Database security group ID."
  value       = module.network.database_security_group_id
}
