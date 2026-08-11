module "cache" {
  source = "../../modules/cache"

  environment_name         = var.environment_name
  vpc_id                   = module.network.vpc_id
  private_app_subnet_ids   = module.network.private_app_subnet_ids
  private_app_subnet_cidrs = module.network.private_app_subnet_cidrs
  node_type                = var.redis_node_type
  cluster_mode_enabled     = var.redis_cluster_mode_enabled
  num_node_groups          = var.redis_num_node_groups
  replicas_per_node_group  = var.redis_replicas_per_node_group

  tags = {
    Project   = "PCIS"
    ManagedBy = "Terraform"
  }

  depends_on = [module.network]
}

variable "redis_node_type" {
  type    = string
  default = "cache.t3.medium"
}

variable "redis_cluster_mode_enabled" {
  type    = bool
  default = false
}

variable "redis_num_node_groups" {
  type    = number
  default = 2
}

variable "redis_replicas_per_node_group" {
  type    = number
  default = 2
}

output "redis_primary_endpoint" {
  value = module.cache.primary_endpoint_address
}

output "redis_auth_secret_arn" {
  value = module.cache.auth_secret_arn
}
