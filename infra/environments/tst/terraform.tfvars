environment_name        = "tst"
vpc_cidr                = "10.1.0.0/16"
az_count                = 2
enable_ha_nat           = false
flow_log_retention_days = 90
aws_region              = "us-east-1"

kubernetes_version         = "1.29"
application_instance_types = ["m5.xlarge"]
application_scaling        = { min_size = 1, max_size = 3, desired_size = 1 }
batch_instance_types       = ["m5.2xlarge"]
batch_scaling              = { min_size = 0, max_size = 4, desired_size = 0 }
enable_kubernetes_addons   = false
argocd_automated_sync      = true

# Aurora PostgreSQL (WO-131) — single-AZ for cost in non-prod
aurora_instance_class          = "db.r6g.large"
aurora_multi_az                = false
aurora_engine_version          = "17.4"
aurora_deletion_protection     = false
aurora_backup_retention_period = 35

# Secrets Manager + ESO (WO-132) — enable ESO after the EKS control plane exists
enable_external_secrets        = false
create_secret_rotation_lambda  = true
secret_rotation_days           = 90
eso_chart_version              = "0.9.20"

# S3 Object Lock audit archives (WO-133)
object_lock_retention_days    = 180
glacier_transition_days       = 90
deep_archive_transition_days  = 365

# Keycloak OIDC IdP (WO-145) — staging/tst; enable after EKS is live
enable_keycloak                 = false
keycloak_replica_count          = 2
keycloak_certificate_arn        = "arn:aws:acm:us-east-1:000000000000:certificate/REPLACE_TST_CERT"
keycloak_ingress_host           = "auth.tst.pcis.example.com"
keycloak_db_host                = "REPLACE_KEYCLOAK_DB_HOST"
keycloak_enable_realm_import    = true
keycloak_create_secret_shells   = true

