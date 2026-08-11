output "cluster_arn" {
  description = "MSK cluster ARN."
  value       = aws_msk_cluster.kafka.arn
}

output "cluster_name" {
  description = "MSK cluster name."
  value       = aws_msk_cluster.kafka.cluster_name
}

output "bootstrap_brokers_sasl_scram" {
  description = "TLS SASL/SCRAM bootstrap broker string."
  value       = aws_msk_cluster.kafka.bootstrap_brokers_sasl_scram
  sensitive   = true
}

output "zookeeper_connect_string" {
  description = "Zookeeper connect string."
  value       = aws_msk_cluster.kafka.zookeeper_connect_string
  sensitive   = true
}

output "security_group_id" {
  description = "MSK security group ID."
  value       = aws_security_group.msk.id
}

output "scram_secret_arn" {
  description = "Secrets Manager ARN for SASL/SCRAM credentials."
  value       = aws_secretsmanager_secret.scram.arn
}

output "configuration_arn" {
  description = "MSK configuration ARN."
  value       = aws_msk_configuration.kafka.arn
}
