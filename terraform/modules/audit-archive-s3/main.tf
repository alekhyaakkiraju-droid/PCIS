variable "environment_name" {
  type        = string
  description = "Deployment environment (dev, tst, prd)"
}

variable "object_lock_retention_days" {
  type        = number
  description = "Default Object Lock retention in compliance mode"
  default     = 2555
}

variable "tags" {
  type        = map(string)
  description = "Additional resource tags"
  default     = {}
}

module "object_storage" {
  source = "../../../infra/modules/object-storage"

  environment_name           = var.environment_name
  object_lock_retention_days = var.object_lock_retention_days
  tags                       = var.tags
}

output "audit_bucket_name" {
  value = module.object_storage.audit_bucket_name
}

output "audit_kms_key_arn" {
  value = module.object_storage.kms_key_arn
}
