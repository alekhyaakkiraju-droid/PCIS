output "repository_urls" {
  description = "Map of service short name to ECR repository URL."
  value = {
    for name, repo in aws_ecr_repository.service :
    name => repo.repository_url
  }
}

output "repository_arns" {
  description = "Map of service short name to ECR repository ARN."
  value = {
    for name, repo in aws_ecr_repository.service :
    name => repo.arn
  }
}

output "repository_names" {
  description = "Map of service short name to full ECR repository name."
  value       = local.repository_map
}

output "registry_id" {
  description = "AWS account registry ID hosting the repositories."
  value       = data.aws_caller_identity.current.account_id
}

output "scan_findings_event_rule_arn" {
  description = "EventBridge rule ARN for ECR image scan findings (null when disabled)."
  value       = var.enable_scan_event_rule ? aws_cloudwatch_event_rule.ecr_scan_findings[0].arn : null
}

output "scan_findings_log_group_name" {
  description = "CloudWatch log group receiving ECR scan finding events."
  value       = var.enable_scan_event_rule ? aws_cloudwatch_log_group.ecr_scan_findings[0].name : null
}
