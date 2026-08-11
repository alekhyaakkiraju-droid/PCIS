output "cluster_name" {
  description = "EKS cluster name."
  value       = aws_eks_cluster.this.name
}

output "cluster_endpoint" {
  description = "EKS API server endpoint."
  value       = aws_eks_cluster.this.endpoint
}

output "cluster_ca_certificate" {
  description = "Base64-encoded cluster CA certificate."
  value       = aws_eks_cluster.this.certificate_authority[0].data
}

output "cluster_security_group_id" {
  description = "Cluster security group ID."
  value       = aws_security_group.cluster.id
}

output "oidc_provider_arn" {
  description = "IAM OIDC provider ARN for IRSA."
  value       = aws_iam_openid_connect_provider.eks.arn
}

output "oidc_provider_url" {
  description = "OIDC issuer URL without https:// prefix."
  value       = local.oidc_issuer
}

output "node_role_arn" {
  description = "IAM role ARN used by managed node groups."
  value       = aws_iam_role.node.arn
}

output "application_node_group_name" {
  description = "Application managed node group name."
  value       = aws_eks_node_group.application.node_group_name
}

output "batch_node_group_name" {
  description = "Batch managed node group name (min=0)."
  value       = aws_eks_node_group.batch.node_group_name
}

output "kubernetes_version" {
  description = "Provisioned Kubernetes version."
  value       = aws_eks_cluster.this.version
}
