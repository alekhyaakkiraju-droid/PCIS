# Keycloak module inputs — prod (maps to environment_name = prd)
environment_name   = "prd"
aws_region         = "us-east-1"
replica_count      = 3
keycloak_image_tag = "26.0.2"
chart_version      = "24.4.13"
ingress_host       = "auth.pcis.example.com"
ingress_scheme     = "internal"
certificate_arn    = "arn:aws:acm:us-east-1:000000000000:certificate/REPLACE_PRD_CERT"
db_host            = "pcis-prd-keycloak.cluster-xxxxxxxxxxxx.us-east-1.rds.amazonaws.com"
db_port            = 5432
db_name            = "keycloak"
db_username        = "keycloak"
admin_username     = "pcis-kc-admin"
# Prefer GitOps / controlled import in prd; realm import off by default.
enable_realm_import = false
enable_ingress      = true
create_secret_shells = true

resources = {
  requests = {
    cpu    = "1"
    memory = "2Gi"
  }
  limits = {
    cpu    = "4"
    memory = "4Gi"
  }
}
