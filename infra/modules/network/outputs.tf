output "vpc_id" {
  description = "ID of the provisioned VPC."
  value       = aws_vpc.main.id
}

output "vpc_cidr_block" {
  description = "CIDR block of the provisioned VPC."
  value       = aws_vpc.main.cidr_block
}

output "public_subnet_ids" {
  description = "List of public subnet IDs (one per AZ). These subnets host the ALB and NAT Gateways."
  value       = aws_subnet.public[*].id
}

output "private_app_subnet_ids" {
  description = "List of private application subnet IDs (one per AZ). These host EKS nodes and the service mesh."
  value       = aws_subnet.private_app[*].id
}

output "private_data_subnet_ids" {
  description = "List of private data subnet IDs (one per AZ). These host Aurora, ElastiCache, and MSK."
  value       = aws_subnet.private_data[*].id
}

output "nat_gateway_ids" {
  description = "List of NAT Gateway IDs. Single element in dev/tst; one per AZ in prd (HA)."
  value       = aws_nat_gateway.main[*].id
}

output "internet_gateway_id" {
  description = "ID of the Internet Gateway attached to the VPC."
  value       = aws_internet_gateway.main.id
}

output "alb_security_group_id" {
  description = "ID of the ALB security group (allows HTTPS/443 inbound from internet)."
  value       = aws_security_group.alb.id
}

output "service_mesh_security_group_id" {
  description = "ID of the service mesh security group (Istio mTLS inter-service traffic)."
  value       = aws_security_group.service_mesh.id
}

output "database_security_group_id" {
  description = "ID of the database security group (Aurora/ElastiCache/MSK inbound from app subnets)."
  value       = aws_security_group.database.id
}

output "flow_log_bucket_arn" {
  description = "ARN of the S3 bucket receiving VPC flow logs."
  value       = aws_s3_bucket.flow_logs.arn
}

output "availability_zones" {
  description = "List of Availability Zone names used by this VPC."
  value       = local.azs
}
