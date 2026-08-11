output "replication_group_id" {
  description = "ElastiCache replication group ID."
  value       = aws_elasticache_replication_group.redis.id
}

output "primary_endpoint_address" {
  description = "Redis primary endpoint."
  value       = aws_elasticache_replication_group.redis.primary_endpoint_address
}

output "reader_endpoint_address" {
  description = "Redis reader endpoint (cluster mode)."
  value       = aws_elasticache_replication_group.redis.reader_endpoint_address
}

output "configuration_endpoint_address" {
  description = "Redis configuration endpoint (cluster mode)."
  value       = aws_elasticache_replication_group.redis.configuration_endpoint_address
}

output "security_group_id" {
  description = "Redis security group ID."
  value       = aws_security_group.redis.id
}

output "auth_secret_arn" {
  description = "Secrets Manager ARN for Redis AUTH token."
  value       = aws_secretsmanager_secret.auth.arn
}

output "port" {
  description = "Redis port."
  value       = aws_elasticache_replication_group.redis.port
}
