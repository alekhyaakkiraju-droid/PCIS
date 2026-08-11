module "messaging" {
  source = "../../modules/messaging"

  environment_name           = var.environment_name
  vpc_id                     = module.network.vpc_id
  private_app_subnet_ids     = module.network.private_app_subnet_ids
  private_app_subnet_cidrs   = module.network.private_app_subnet_cidrs
  broker_count               = var.msk_broker_count
  broker_instance_type       = var.msk_broker_instance_type
  default_replication_factor = var.msk_default_replication_factor
  min_insync_replicas        = var.msk_min_insync_replicas

  tags = {
    Project   = "PCIS"
    ManagedBy = "Terraform"
  }

  depends_on = [module.network]
}

variable "msk_broker_count" {
  type    = number
  default = 2
}

variable "msk_broker_instance_type" {
  type    = string
  default = "kafka.m5.large"
}

variable "msk_default_replication_factor" {
  type    = number
  default = 2
}

variable "msk_min_insync_replicas" {
  type    = number
  default = 1
}

output "msk_cluster_arn" {
  value = module.messaging.cluster_arn
}

output "msk_bootstrap_brokers_sasl_scram" {
  value     = module.messaging.bootstrap_brokers_sasl_scram
  sensitive = true
}

output "msk_scram_secret_arn" {
  value = module.messaging.scram_secret_arn
}
