locals {
  pcis_namespaces = ["pcis-dev", "pcis-tst", "pcis-prd"]
}

resource "kubernetes_namespace" "pcis" {
  for_each = toset(local.pcis_namespaces)

  metadata {
    name = each.key
    labels = {
      "istio-injection"                    = "enabled"
      "pod-security.kubernetes.io/enforce" = "restricted"
      "pod-security.kubernetes.io/audit"   = "restricted"
      "pod-security.kubernetes.io/warn"    = "restricted"
      "app.kubernetes.io/part-of"          = "pcis"
      "environment"                        = var.environment_name
    }
  }
}

resource "kubernetes_network_policy" "default_deny" {
  for_each = toset(local.pcis_namespaces)

  metadata {
    name      = "default-deny-all"
    namespace = kubernetes_namespace.pcis[each.key].metadata[0].name
  }

  spec {
    pod_selector {}
    policy_types = ["Ingress", "Egress"]
  }
}
