module "object_storage" {
  source = "../../modules/object-storage"

  environment_name             = var.environment_name
  oidc_provider_arn            = module.kubernetes.oidc_provider_arn
  oidc_provider_url            = module.kubernetes.oidc_provider_url
  object_lock_retention_days   = var.object_lock_retention_days
  glacier_transition_days      = var.glacier_transition_days
  deep_archive_transition_days = var.deep_archive_transition_days

  tags = {
    Project   = "PCIS"
    ManagedBy = "Terraform"
  }

  depends_on = [module.kubernetes]
}

variable "object_lock_retention_days" {
  type    = number
  default = 365
}

variable "glacier_transition_days" {
  type    = number
  default = 90
}

variable "deep_archive_transition_days" {
  type    = number
  default = 365
}

output "audit_bucket_arn" {
  value = module.object_storage.audit_bucket_arn
}

output "audit_bucket_name" {
  value = module.object_storage.audit_bucket_name
}

output "flow_logs_bucket_arn" {
  value = module.object_storage.flow_logs_bucket_arn
}

output "s3_audit_kms_key_arn" {
  value = module.object_storage.kms_key_arn
}

output "audit_svc_s3_role_arn" {
  value = module.object_storage.audit_svc_role_arn
}
