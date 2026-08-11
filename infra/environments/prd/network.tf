# ─────────────────────────────────────────────────────────────────────────────
# Variables
# ─────────────────────────────────────────────────────────────────────────────

variable "aws_region" {
  description = "AWS region to deploy to."
  type        = string
}

variable "vpc_cidr" {
  description = "VPC CIDR block for the prd environment."
  type        = string
  default     = "10.2.0.0/16"
}

variable "az_count" {
  description = "Number of Availability Zones to use."
  type        = number
  default     = 3
}

variable "enable_ha_nat" {
  description = "Enable one NAT Gateway per AZ (true for prd HA)."
  type        = bool
  default     = true
}

variable "flow_log_retention_days" {
  description = "Retention period in days for VPC flow logs."
  type        = number
  default     = 90
}

# ─────────────────────────────────────────────────────────────────────────────
# Network Module
# ─────────────────────────────────────────────────────────────────────────────

module "network" {
  source = "../../modules/network"

  environment_name        = "prd"
  vpc_cidr                = var.vpc_cidr
  az_count                = var.az_count
  enable_ha_nat           = var.enable_ha_nat
  flow_log_retention_days = var.flow_log_retention_days

  common_tags = {
    CostCenter   = "PCIS-Production"
    Owner        = "platform-team"
    Criticality  = "high"
  }
}

# ─────────────────────────────────────────────────────────────────────────────
# Outputs
# ─────────────────────────────────────────────────────────────────────────────

output "vpc_id" {
  value = module.network.vpc_id
}

output "vpc_cidr_block" {
  value = module.network.vpc_cidr_block
}

output "public_subnet_ids" {
  value = module.network.public_subnet_ids
}

output "private_app_subnet_ids" {
  value = module.network.private_app_subnet_ids
}

output "private_data_subnet_ids" {
  value = module.network.private_data_subnet_ids
}

output "nat_gateway_ids" {
  description = "One NAT Gateway ID per AZ (HA mode)."
  value       = module.network.nat_gateway_ids
}

output "alb_security_group_id" {
  value = module.network.alb_security_group_id
}

output "database_security_group_id" {
  value = module.network.database_security_group_id
}

output "availability_zones" {
  value = module.network.availability_zones
}
