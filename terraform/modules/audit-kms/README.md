# Audit archive KMS (WO-171)

Customer-managed KMS key for audit archive encryption and cryptographic erasure.

Production implementation lives in `infra/modules/object-storage/kms.tf` (alias `pcis-audit-archive-{env}`).

## Cryptographic erasure

After tier retention expires, `auditPurgeJob` calls `ScheduleKeyDeletion` with a configurable waiting period (default 7 days). Destroying the CMK renders S3 archive objects permanently unreadable (GDPR erasure after SOX minimum retention).

See `docs/retention-schedule.md` for GDPR/SOX reconciliation.
