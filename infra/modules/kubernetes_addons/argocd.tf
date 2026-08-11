resource "kubernetes_namespace" "argocd" {
  metadata {
    name = "argocd"
    labels = {
      "app.kubernetes.io/part-of" = "argocd"
    }
  }
}

resource "helm_release" "argocd" {
  name       = "argocd"
  repository = "https://argoproj.github.io/argo-helm"
  chart      = "argo-cd"
  version    = var.argocd_version
  namespace  = kubernetes_namespace.argocd.metadata[0].name

  values = [
    templatefile("${path.module}/helm-values/argocd-values.yaml", {
      environment_name       = var.environment_name
      automated_sync_enabled = var.argocd_automated_sync && var.environment_name != "prd"
      revision_history_limit = 5
    }),
  ]

  depends_on = [helm_release.istiod]
  wait       = true
  atomic     = true
}

resource "kubernetes_secret" "argocd_repo_placeholder" {
  metadata {
    name      = "repo-pcis-placeholder"
    namespace = kubernetes_namespace.argocd.metadata[0].name
    labels = {
      "argocd.argoproj.io/secret-type" = "repository"
    }
  }

  data = {
    type = "git"
    url  = "https://github.com/alekhyaakkiraju-droid/PCIS.git"
  }

  type = "Opaque"

  depends_on = [helm_release.argocd]
}
