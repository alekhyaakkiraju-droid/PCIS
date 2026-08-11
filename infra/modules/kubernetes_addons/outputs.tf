output "argocd_namespace" {
  value = kubernetes_namespace.argocd.metadata[0].name
}

output "istio_namespace" {
  value = kubernetes_namespace.istio_system.metadata[0].name
}

output "pcis_namespaces" {
  value = keys(kubernetes_namespace.pcis)
}
