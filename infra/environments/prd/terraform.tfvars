environment_name        = "prd"
vpc_cidr                = "10.2.0.0/16"
az_count                = 2
enable_ha_nat           = true
flow_log_retention_days = 90
aws_region              = "us-east-1"

kubernetes_version         = "1.29"
application_instance_types = ["m5.xlarge"]
application_scaling        = { min_size = 2, max_size = 6, desired_size = 2 }
batch_instance_types       = ["m5.2xlarge"]
batch_scaling              = { min_size = 0, max_size = 4, desired_size = 0 }
enable_kubernetes_addons   = false
argocd_automated_sync      = false

# Aurora PostgreSQL (WO-131) — Multi-AZ writer/reader isolation in prd
aurora_instance_class          = "db.r6g.xlarge"
aurora_multi_az                = true
aurora_engine_version          = "17.4"
aurora_deletion_protection     = true
aurora_backup_retention_period = 35

# Secrets Manager + ESO (WO-132) — enable ESO after the EKS control plane exists
enable_external_secrets        = false
create_secret_rotation_lambda  = true
secret_rotation_days           = 90
eso_chart_version              = "0.9.20"

# S3 Object Lock audit archives (WO-133)
object_lock_retention_days    = 365
glacier_transition_days       = 90
deep_archive_transition_days  = 365

# Keycloak OIDC IdP (WO-145) — production; realm import off by default
enable_keycloak                 = false
keycloak_replica_count          = 3
keycloak_certificate_arn        = "arn:aws:acm:us-east-1:000000000000:certificate/REPLACE_PRD_CERT"
keycloak_ingress_host           = "auth.pcis.example.com"
keycloak_db_host                = "REPLACE_KEYCLOAK_DB_HOST"
keycloak_enable_realm_import    = false
keycloak_create_secret_shells   = true

# ECR registry (WO-134) — prd is the production account; cross-account pull disabled
production_account_id              = "222233334444"
ecr_repository_names = [
  "customer-svc", "claims-svc", "policy-svc", "premium-svc",
  "billing-svc", "reporting-svc", "authz-svc", "audit-svc",
  "pcis-base-java21",
]
ecr_cross_account_pull_enabled     = false
ecr_tagged_image_retention_count   = 30
ecr_untagged_image_expiration_days = 7

# MSK Kafka + ElastiCache Redis (WO-138)
msk_broker_count               = 3
msk_broker_instance_type       = "kafka.m5.large"
msk_default_replication_factor = 3
msk_min_insync_replicas        = 2
redis_node_type                = "cache.r6g.large"
redis_cluster_mode_enabled     = true
redis_num_node_groups          = 2
redis_replicas_per_node_group  = 2

