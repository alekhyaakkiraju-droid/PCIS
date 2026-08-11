# External Secrets Operator — gated so day-0 validate/apply works before the cluster exists.
# Edge case: ESO pod restarts reconcile ExternalSecrets on the next refresh cycle (1h default).

resource "kubernetes_namespace" "external_secrets" {
  count = var.enable_eso ? 1 : 0

  metadata {
    name = "external-secrets"
    labels = {
      "app.kubernetes.io/part-of"          = "external-secrets"
      "pod-security.kubernetes.io/enforce" = "restricted"
      "pod-security.kubernetes.io/audit"   = "restricted"
      "pod-security.kubernetes.io/warn"    = "restricted"
    }
  }
}

resource "helm_release" "external_secrets" {
  count = var.enable_eso ? 1 : 0

  name       = "external-secrets"
  repository = "https://charts.external-secrets.io"
  chart      = "external-secrets"
  version    = var.eso_chart_version
  namespace  = kubernetes_namespace.external_secrets[0].metadata[0].name

  values = [
    yamlencode({
      installCRDs = true
      serviceAccount = {
        create = true
        name   = "external-secrets"
        annotations = {
          "eks.amazonaws.com/role-arn" = aws_iam_role.eso.arn
        }
      }
      webhook = {
        create = true
      }
      # ClusterSecretStore is applied with the chart so CRDs exist in the same release lifecycle.
      extraObjects = [
        {
          apiVersion = "external-secrets.io/v1beta1"
          kind       = "ClusterSecretStore"
          metadata = {
            name = "aws-secrets-manager"
            labels = {
              "app.kubernetes.io/part-of" = "pcis"
              environment                 = var.environment_name
            }
          }
          spec = {
            provider = {
              aws = {
                service = "SecretsManager"
                region  = data.aws_region.current.name
                auth = {
                  jwt = {
                    serviceAccountRef = {
                      name      = "external-secrets"
                      namespace = "external-secrets"
                    }
                  }
                }
              }
            }
          }
        }
      ]
    }),
  ]

  wait    = true
  atomic  = true
  timeout = 600
}
