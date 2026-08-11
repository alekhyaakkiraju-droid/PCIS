resource "kubernetes_namespace" "istio_system" {
  metadata {
    name = "istio-system"
    labels = {
      "app.kubernetes.io/part-of" = "istio"
    }
  }
}

resource "helm_release" "istio_base" {
  name       = "istio-base"
  repository = "https://istio-release.storage.googleapis.com/charts"
  chart      = "base"
  version    = var.istio_version
  namespace  = kubernetes_namespace.istio_system.metadata[0].name

  create_namespace = false
  wait             = true
  atomic           = true
}

resource "helm_release" "istiod" {
  name       = "istiod"
  repository = "https://istio-release.storage.googleapis.com/charts"
  chart      = "istiod"
  version    = var.istio_version
  namespace  = kubernetes_namespace.istio_system.metadata[0].name

  values = [
    file("${path.module}/helm-values/istio-values.yaml"),
  ]

  depends_on = [helm_release.istio_base]
  wait       = true
  atomic     = true
}

resource "helm_release" "istio_ingress" {
  name       = "istio-ingress"
  repository = "https://istio-release.storage.googleapis.com/charts"
  chart      = "gateway"
  version    = var.istio_version
  namespace  = kubernetes_namespace.istio_system.metadata[0].name

  depends_on = [helm_release.istiod]
  wait       = true
  atomic     = true
}

resource "kubernetes_manifest" "peer_authentication_strict" {
  manifest = {
    apiVersion = "security.istio.io/v1beta1"
    kind       = "PeerAuthentication"
    metadata = {
      name      = "default"
      namespace = "istio-system"
    }
    spec = {
      mtls = {
        mode = "STRICT"
      }
    }
  }

  depends_on = [helm_release.istiod]
}
