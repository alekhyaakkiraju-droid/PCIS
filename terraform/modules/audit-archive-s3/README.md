# Audit archive S3 (WO-171)

This module wraps the production Object Lock bucket defined in `infra/modules/object-storage`.

## Usage

```hcl
module "audit_archive_s3" {
  source = "../../terraform/modules/audit-archive-s3"

  environment_name            = var.environment_name
  object_lock_retention_days  = 2555
  tags                        = var.tags
}
```

## Compliance

- Object Lock **COMPLIANCE** mode (root cannot bypass retention)
- Versioning enabled
- Lifecycle: transition to Glacier after 90 days; expiry matches tier retention
- SSE-KMS with customer-managed key (`audit-kms` module)

See also: `docs/retention-schedule.md`
