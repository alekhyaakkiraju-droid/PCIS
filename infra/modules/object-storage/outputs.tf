output "audit_bucket_arn" {
  description = "ARN of the Object Lock audit archive bucket."
  value       = aws_s3_bucket.audit.arn
}

output "audit_bucket_name" {
  description = "Name of the Object Lock audit archive bucket."
  value       = aws_s3_bucket.audit.id
}

output "flow_logs_bucket_arn" {
  description = "ARN of the VPC flow logs bucket (no Object Lock)."
  value       = aws_s3_bucket.flow_logs.arn
}

output "flow_logs_bucket_name" {
  description = "Name of the VPC flow logs bucket."
  value       = aws_s3_bucket.flow_logs.id
}

output "terraform_state_bucket_arn" {
  description = "ARN of the Terraform state bucket (versioned, no Object Lock)."
  value       = aws_s3_bucket.terraform_state.arn
}

output "terraform_state_bucket_name" {
  description = "Name of the Terraform state bucket."
  value       = aws_s3_bucket.terraform_state.id
}

output "kms_key_arn" {
  description = "Customer-managed KMS key ARN for S3 audit encryption."
  value       = aws_kms_key.s3_audit.arn
}

output "kms_key_alias" {
  description = "KMS alias for the S3 audit encryption key."
  value       = aws_kms_alias.s3_audit.name
}

output "object_lock_retention_days" {
  description = "Configured compliance-mode default retention days."
  value       = var.object_lock_retention_days
}

output "audit_svc_role_arn" {
  description = "IRSA role ARN for audit-svc PutObject access (Helm annotation)."
  value       = aws_iam_role.audit_svc.arn
}
