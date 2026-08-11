# ─────────────────────────────────────────────────────────────────────────────
# Variables
# ─────────────────────────────────────────────────────────────────────────────

variable "aws_region" {
  description = "AWS region to deploy to."
  type        = string
}

variable "vpc_cidr" {
  description = "VPC CIDR block for the tst environment."
  type        = string
  default     = "10.1.0.0/16"
}

variable "az_count" {
  description = "Number of Availability Zones to use."
  type        = number
  default     = 2
}

variable "enable_ha_nat" {
  description = "Enable one NAT Gateway per AZ."
  type        = bool
  default     = false
}

variable "flow_log_retention_days" {
  description = "Retention period in days for VPC flow logs."
  type        = number
  default     = 60
}

# ─────────────────────────────────────────────────────────────────────────────
# Network Module
# ─────────────────────────────────────────────────────────────────────────────

module "network" {
  source = "../../modules/network"

  environment_name        = "tst"
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
  value = module.network.nat_gateway_ids
}

output "alb_security_group_id" {
  value = module.network.alb_security_group_id
}

output "database_security_group_id" {
  value = module.network.database_security_group_id
}
