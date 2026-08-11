output "vpc_id" {
  description = "ID of the provisioned VPC."
  value       = aws_vpc.this.id
}

output "vpc_cidr_block" {
  description = "CIDR block of the VPC."
  value       = aws_vpc.this.cidr_block
}

output "public_subnet_ids" {
  description = "IDs of public subnets (ALB / WAF zone)."
  value       = aws_subnet.public[*].id
}

output "private_app_subnet_ids" {
  description = "IDs of private application subnets (Internal / DMZ services)."
  value       = aws_subnet.private_app[*].id
}

output "private_data_subnet_ids" {
  description = "IDs of private data subnets (Aurora, ElastiCache, MSK)."
  value       = aws_subnet.private_data[*].id
}

output "nat_gateway_ids" {
  description = "IDs of NAT gateways used for private subnet egress."
  value       = aws_nat_gateway.this[*].id
}

output "alb_security_group_id" {
  description = "Security group ID for ALB HTTPS ingress."
  value       = aws_security_group.alb.id
}

output "mesh_security_group_id" {
  description = "Security group ID for inter-service mesh traffic."
  value       = aws_security_group.mesh.id
}

output "database_security_group_id" {
  description = "Security group ID for database access from app subnets."
  value       = aws_security_group.database.id
}

output "flow_log_bucket_arn" {
  description = "ARN of the S3 bucket receiving VPC flow logs."
  value       = aws_s3_bucket.flow_logs.arn
}

output "availability_zones" {
  description = "Availability zones used by this network."
  value       = local.azs
}
