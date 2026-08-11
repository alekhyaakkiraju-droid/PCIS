module "database" {
  source = "../../modules/database"

  environment_name         = var.environment_name
  vpc_id                   = module.network.vpc_id
  private_data_subnet_ids  = module.network.private_data_subnet_ids
  private_app_subnet_cidrs = module.network.private_app_subnet_cidrs
  availability_zones       = module.network.availability_zones

  multi_az                = var.aurora_multi_az
  instance_class          = var.aurora_instance_class
  engine_version          = var.aurora_engine_version
  deletion_protection     = var.aurora_deletion_protection
  backup_retention_period = var.aurora_backup_retention_period
  microservice_names      = var.aurora_microservice_names

  oidc_provider_arn = module.kubernetes.oidc_provider_arn
  oidc_provider_url = module.kubernetes.oidc_provider_url

  tags = {
    Project   = "PCIS"
    ManagedBy = "Terraform"
  }

  depends_on = [
    module.network,
    module.kubernetes,
  ]
}

variable "aurora_multi_az" {
  type    = bool
  default = false
}

variable "aurora_instance_class" {
  type    = string
  default = "db.r6g.large"
}

variable "aurora_engine_version" {
  type    = string
  default = "17.4"
}

variable "aurora_deletion_protection" {
  type    = bool
  default = false
}

variable "aurora_backup_retention_period" {
  type    = number
  default = 35
}

variable "aurora_microservice_names" {
  type = list(string)
  default = [
    "claims-svc",
    "policy-svc",
    "billing-svc",
    "reporting-svc",
    "authz-svc",
  ]
}

output "aurora_cluster_endpoint" {
  value = module.database.cluster_endpoint
}

output "aurora_reader_endpoint" {
  value = module.database.reader_endpoint
}

output "aurora_cluster_identifier" {
  value = module.database.cluster_identifier
}

output "aurora_kms_key_arn" {
  value = module.database.kms_key_arn
}

output "aurora_security_group_id" {
  value = module.database.security_group_id
}

output "aurora_irsa_role_arns" {
  value = module.database.irsa_role_arns
}
