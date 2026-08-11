# Keycloak module inputs — staging (maps to environment_name = tst)
environment_name   = "tst"
aws_region         = "us-east-1"
replica_count      = 2
keycloak_image_tag = "26.0.2"
chart_version      = "24.4.13"
ingress_host       = "auth.tst.pcis.example.com"
ingress_scheme     = "internal"
certificate_arn    = "arn:aws:acm:us-east-1:000000000000:certificate/REPLACE_TST_CERT"
db_host            = "pcis-tst-keycloak.cluster-xxxxxxxxxxxx.us-east-1.rds.amazonaws.com"
db_port            = 5432
db_name            = "keycloak"
db_username        = "keycloak"
admin_username     = "pcis-kc-admin"
enable_realm_import = true
enable_ingress      = true
create_secret_shells = true

resources = {
  requests = {
    cpu    = "500m"
    memory = "1536Mi"
  }
  limits = {
    cpu    = "2"
    memory = "3Gi"
  }
}
