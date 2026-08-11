module "registry" {
  source = "../../modules/registry"

  environment_name              = var.environment_name
  production_account_id         = var.production_account_id
  repository_names              = var.ecr_repository_names
  cross_account_pull_enabled    = var.ecr_cross_account_pull_enabled
  tagged_image_retention_count  = var.ecr_tagged_image_retention_count
  untagged_image_expiration_days = var.ecr_untagged_image_expiration_days

  tags = {
    Project   = "PCIS"
    ManagedBy = "Terraform"
  }
}

variable "production_account_id" {
  description = "Production AWS account ID for cross-account ECR pull policies."
  type        = string
}

variable "ecr_repository_names" {
  description = "ECR repository short names (WO-134)."
  type        = list(string)
}

variable "ecr_cross_account_pull_enabled" {
  description = "Enable cross-account pull policies on ECR repositories."
  type        = bool
  default     = true
}

variable "ecr_tagged_image_retention_count" {
  description = "Tagged images retained per ECR repository."
  type        = number
  default     = 30
}

variable "ecr_untagged_image_expiration_days" {
  description = "Days before untagged ECR images expire."
  type        = number
  default     = 7
}

output "ecr_repository_urls" {
  value = module.registry.repository_urls
}

output "ecr_scan_findings_event_rule_arn" {
  value = module.registry.scan_findings_event_rule_arn
}
