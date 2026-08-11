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

