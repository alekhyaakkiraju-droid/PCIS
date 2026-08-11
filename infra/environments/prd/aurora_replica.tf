module "aurora_replica" {
  source = "../../modules/aurora-replica"

  environment_name    = var.environment_name
  cluster_identifier  = module.database.cluster_identifier
  cluster_resource_id = module.database.cluster_resource_id
  engine              = module.database.cluster_engine
  engine_version      = module.database.cluster_engine_version

  availability_zones = module.network.availability_zones
  multi_az           = var.aurora_multi_az
  instance_class     = var.aurora_instance_class

  db_subnet_group_name = module.database.db_subnet_group_name
  kms_key_arn          = module.database.kms_key_arn

  enhanced_monitoring_interval   = 60
  monitoring_role_arn            = module.database.enhanced_monitoring_role_arn
  performance_insights_enabled   = true

  oidc_provider_arn = module.kubernetes.oidc_provider_arn
  oidc_provider_url = module.kubernetes.oidc_provider_url

  tags = {
    Project   = "PCIS"
    ManagedBy = "Terraform"
  }

  depends_on = [module.database, module.kubernetes]
}

output "aurora_reader_instance_id" {
  value = module.aurora_replica.reader_instance_id
}

output "reporting_irsa_role_arn" {
  value = module.aurora_replica.reporting_irsa_role_arn
}

output "aurora_replica_lag_warning_alarm" {
  value = module.aurora_replica.replica_lag_warning_alarm_name
}

output "aurora_replica_lag_critical_alarm" {
  value = module.aurora_replica.replica_lag_critical_alarm_name
}
