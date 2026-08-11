output "cluster_endpoint" {
  description = "Writer endpoint for the Aurora cluster."
  value       = aws_rds_cluster.aurora.endpoint
}

output "reader_endpoint" {
  description = "Reader endpoint for reporting / read-only workloads."
  value       = aws_rds_cluster.aurora.reader_endpoint
}

output "cluster_identifier" {
  description = "Aurora cluster identifier."
  value       = aws_rds_cluster.aurora.cluster_identifier
}

output "cluster_resource_id" {
  description = "Cluster resource ID used in rds-db:connect ARNs."
  value       = aws_rds_cluster.aurora.cluster_resource_id
}

output "cluster_arn" {
  description = "Aurora cluster ARN."
  value       = aws_rds_cluster.aurora.arn
}

output "kms_key_arn" {
  description = "Customer-managed KMS key ARN used for encryption at rest."
  value       = aws_kms_key.aurora.arn
}

output "kms_key_alias" {
  description = "KMS alias for the Aurora encryption key."
  value       = aws_kms_alias.aurora.name
}

output "security_group_id" {
  description = "Security group ID attached to the Aurora cluster."
  value       = aws_security_group.aurora.id
}

output "db_subnet_group_name" {
  description = "DB subnet group name (private data subnets)."
  value       = aws_db_subnet_group.aurora.name
}

output "cluster_parameter_group_name" {
  description = "Custom cluster parameter group name (TLS + UTC)."
  value       = aws_rds_cluster_parameter_group.aurora.name
}

output "writer_instance_id" {
  description = "Writer instance identifier."
  value       = aws_rds_cluster_instance.writer.id
}

output "cluster_engine" {
  description = "Aurora engine identifier."
  value       = aws_rds_cluster.aurora.engine
}

output "cluster_engine_version" {
  description = "Aurora PostgreSQL engine version."
  value       = aws_rds_cluster.aurora.engine_version
}

output "enhanced_monitoring_role_arn" {
  description = "IAM role ARN for RDS Enhanced Monitoring (null when disabled)."
  value       = var.enhanced_monitoring_interval > 0 ? aws_iam_role.rds_enhanced_monitoring[0].arn : null
}

output "irsa_role_arns" {
  description = "Map of microservice name → IRSA role ARN for Helm chart consumption."
  value       = { for name, role in aws_iam_role.db_irsa : name => role.arn }
}

output "master_user_secret_arn" {
  description = "ARN of the AWS-managed Secrets Manager secret for the master user."
  value       = try(aws_rds_cluster.aurora.master_user_secret[0].secret_arn, null)
  sensitive   = true
}
