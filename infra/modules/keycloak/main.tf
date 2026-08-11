locals {
  base_tags = merge(
    {
      Project   = "PCIS"
      ManagedBy = "Terraform"
      Component = "keycloak"
    },
    var.tags,
    {
      Environment = var.environment_name
    }
  )

  namespace = coalesce(var.namespace, "pcis-keycloak-${var.environment_name}")

  admin_secret_name = coalesce(var.admin_secret_name, "pcis/${var.environment_name}/keycloak-admin")
  db_secret_name    = coalesce(var.db_secret_name, "pcis/${var.environment_name}/keycloak-db")

  realm_export_path = coalesce(
    var.realm_export_path,
    "${path.module}/../../keycloak/realm-export.json"
  )

  # Kubernetes secret names synced from Secrets Manager (values never in Terraform).
  k8s_admin_secret = "keycloak-admin"
  k8s_db_secret    = "keycloak-db"
}

resource "kubernetes_namespace" "keycloak" {
  metadata {
    name = local.namespace
    labels = {
      "app.kubernetes.io/part-of"          = "pcis"
      "app.kubernetes.io/name"             = "keycloak"
      "environment"                        = var.environment_name
      "pod-security.kubernetes.io/enforce" = "restricted"
      "pod-security.kubernetes.io/audit"   = "restricted"
      "pod-security.kubernetes.io/warn"    = "restricted"
    }
  }
}

# Secret shells only — values are never written by Terraform (WO-145 constraint).
resource "aws_secretsmanager_secret" "admin" {
  count = var.create_secret_shells ? 1 : 0

  name                    = local.admin_secret_name
  description             = "PCIS Keycloak admin credentials for ${var.environment_name} (value set out-of-band; never in Terraform)"
  recovery_window_in_days = var.environment_name == "prd" ? 30 : 7

  tags = merge(local.base_tags, {
    Name       = local.admin_secret_name
    SecretKey  = "keycloak-admin"
    NoTfValues = "true"
  })
}

resource "aws_secretsmanager_secret" "db" {
  count = var.create_secret_shells ? 1 : 0

  name                    = local.db_secret_name
  description             = "PCIS Keycloak PostgreSQL password for ${var.environment_name} (value set out-of-band; never in Terraform)"
  recovery_window_in_days = var.environment_name == "prd" ? 30 : 7

  tags = merge(local.base_tags, {
    Name       = local.db_secret_name
    SecretKey  = "keycloak-db"
    NoTfValues = "true"
  })
}

# Placeholder Opaque secrets so Helm can reference existingSecret before ESO syncs real values.
# Operators replace these via External Secrets / kubectl from Secrets Manager — never commit real passwords.
resource "kubernetes_secret" "admin_placeholder" {
  metadata {
    name      = local.k8s_admin_secret
    namespace = kubernetes_namespace.keycloak.metadata[0].name
    labels = {
      "app.kubernetes.io/part-of" = "pcis"
      "pcis.secret/source"        = "secrets-manager-placeholder"
    }
    annotations = {
      "pcis.secret/sm-name" = local.admin_secret_name
      "pcis.secret/note"    = "Replace with Secrets Manager value before production traffic; Terraform never writes the password."
    }
  }

  data = {
    admin-password = "REPLACE_FROM_SECRETS_MANAGER"
  }

  type = "Opaque"

  lifecycle {
    ignore_changes = [data, binary_data]
  }
}

resource "kubernetes_secret" "db_placeholder" {
  metadata {
    name      = local.k8s_db_secret
    namespace = kubernetes_namespace.keycloak.metadata[0].name
    labels = {
      "app.kubernetes.io/part-of" = "pcis"
      "pcis.secret/source"        = "secrets-manager-placeholder"
    }
    annotations = {
      "pcis.secret/sm-name" = local.db_secret_name
      "pcis.secret/note"    = "Replace with Secrets Manager value before production traffic; Terraform never writes the password."
    }
  }

  data = {
    password = "REPLACE_FROM_SECRETS_MANAGER"
  }

  type = "Opaque"

  lifecycle {
    ignore_changes = [data, binary_data]
  }
}

resource "kubernetes_config_map" "realm_export" {
  count = var.enable_realm_import ? 1 : 0

  metadata {
    name      = "keycloak-realm-export"
    namespace = kubernetes_namespace.keycloak.metadata[0].name
    labels = {
      "app.kubernetes.io/part-of" = "pcis"
      "app.kubernetes.io/name"    = "keycloak"
    }
  }

  data = {
    "pcis-realm.json" = file(local.realm_export_path)
  }
}

resource "helm_release" "keycloak" {
  name       = var.release_name
  repository = "https://charts.bitnami.com/bitnami"
  chart      = "keycloak"
  version    = var.chart_version
  namespace  = kubernetes_namespace.keycloak.metadata[0].name

  values = [
    yamlencode({
      image = {
        tag = var.keycloak_image_tag
      }
      replicaCount = var.replica_count
      production   = var.environment_name == "prd"
      # Edge case: readiness must fail until PostgreSQL is reachable — gates traffic, avoids crash-loop serving.
      startupProbe = {
        httpGet = {
          path = "/health/started"
          port = "http-management"
        }
        initialDelaySeconds = 30
        periodSeconds       = 10
        failureThreshold    = 30
      }
      livenessProbe = {
        httpGet = {
          path = "/health/live"
          port = "http-management"
        }
        initialDelaySeconds = 0
        periodSeconds       = 20
        failureThreshold    = 3
      }
      readinessProbe = {
        httpGet = {
          path = "/health/ready"
          port = "http-management"
        }
        initialDelaySeconds = 0
        periodSeconds       = 10
        failureThreshold    = 3
      }
      resources = var.resources
      auth = {
        adminUser              = var.admin_username
        existingSecret         = local.k8s_admin_secret
        passwordSecretKey      = "admin-password"
        adminPasswordSecretKey = "admin-password"
      }
      postgresql = {
        enabled = false
      }
      externalDatabase = {
        host                      = var.db_host
        port                      = var.db_port
        user                      = var.db_username
        database                  = var.db_name
        existingSecret            = local.k8s_db_secret
        existingSecretPasswordKey = "password"
      }
      ingress = {
        enabled          = var.enable_ingress
        ingressClassName = var.ingress_class_name
        hostname         = var.ingress_host
        tls              = var.certificate_arn != ""
        annotations = merge(
          {
            "alb.ingress.kubernetes.io/scheme"           = var.ingress_scheme
            "alb.ingress.kubernetes.io/target-type"      = "ip"
            "alb.ingress.kubernetes.io/listen-ports"     = var.certificate_arn != "" ? "[{\"HTTPS\":443}]" : "[{\"HTTP\":80}]"
            "alb.ingress.kubernetes.io/healthcheck-path" = "/health/ready"
          },
          var.certificate_arn != "" ? {
            "alb.ingress.kubernetes.io/certificate-arn" = var.certificate_arn
            "alb.ingress.kubernetes.io/ssl-redirect"    = "443"
          } : {}
        )
      }
      extraEnvVars = [
        {
          name  = "KC_HEALTH_ENABLED"
          value = "true"
        },
        {
          name  = "KC_METRICS_ENABLED"
          value = "true"
        },
        {
          name  = "KC_HTTP_RELATIVE_PATH"
          value = "/"
        },
      ]
      extraStartupArgs = var.enable_realm_import ? "--import-realm" : ""
      extraVolumes = var.enable_realm_import ? [
        {
          name = "realm-import"
          configMap = {
            name = kubernetes_config_map.realm_export[0].metadata[0].name
          }
        }
      ] : []
      extraVolumeMounts = var.enable_realm_import ? [
        {
          name      = "realm-import"
          mountPath = "/opt/bitnami/keycloak/data/import"
          readOnly  = true
        }
      ] : []
      podLabels = {
        "pcis.environment" = var.environment_name
      }
    }),
  ]

  depends_on = [
    kubernetes_secret.admin_placeholder,
    kubernetes_secret.db_placeholder,
  ]

  wait    = true
  atomic  = true
  timeout = 900
}
