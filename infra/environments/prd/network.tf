module "network" {
  source = "../../modules/network"

  environment_name        = var.environment_name
  vpc_cidr                = var.vpc_cidr
  az_count                = var.az_count
  enable_ha_nat           = var.enable_ha_nat
  flow_log_retention_days = var.flow_log_retention_days

  tags = {
    Project   = "PCIS"
    ManagedBy = "Terraform"
  }
}

variable "environment_name" {
  type = string
}

variable "vpc_cidr" {
  type = string
}

variable "az_count" {
  type = number
}

variable "enable_ha_nat" {
  type = bool
}

variable "flow_log_retention_days" {
  type = number
}

output "vpc_id" {
  value = module.network.vpc_id
}

output "public_subnet_ids" {
  value = module.network.public_subnet_ids
}

output "private_app_subnet_ids" {
  value = module.network.private_app_subnet_ids
}

output "private_app_subnet_cidrs" {
  value = module.network.private_app_subnet_cidrs
}

output "private_data_subnet_ids" {
  value = module.network.private_data_subnet_ids
}

output "nat_gateway_ids" {
  value = module.network.nat_gateway_ids
}
