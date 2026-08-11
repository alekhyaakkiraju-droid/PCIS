# PCIS Data Retention Schedule (WO-171)

## Purpose

This document reconciles GDPR/CCPA erasure requirements with SOX/SOC 2 immutability requirements for audit data.

## Tier retention periods

| Tier | Retention (days) | Source |
|------|------------------|--------|
| PUBLIC | 365 | `pcis.audit.retention.public-days` |
| INTERNAL | 365 | `pcis.audit.retention.internal-days` |
| CONFIDENTIAL | 730 | `pcis.audit.retention.confidential-days` |
| RESTRICTED | 2555 | `pcis.audit.retention.restricted-days` |

## Absolute floor

**365 days (1 year)** — no tier or tunable may configure retention below this value. Enforced in `RetentionConfigService` and `DetachedPartitionPurgeService`.

## Reconciliation decision

| Requirement | Mechanism |
|-------------|-----------|
| **SOX immutability** | Audit logs retained for minimum 1 year; S3 Object Lock COMPLIANCE mode prevents premature deletion |
| **GDPR erasure (right to be forgotten)** | Satisfied by **cryptographic erasure** after SOX minimum period — KMS key destruction renders cold-archive data permanently unreadable |
| **Precedence** | SOX minimum retention takes precedence during the retention window; GDPR erasure applies only after expiry via key destruction |

## Purge mechanisms

1. **PostgreSQL partition drop** — detached `audit_log_t_y*` partitions past tier retention are `DROP TABLE` permanently
2. **Cryptographic erasure** — S3 archive exports past retention have their KMS CMK scheduled for deletion (7-day cancellation window)

## Evidence

All purge operations are recorded in `purge_evidence` (INSERT/SELECT only — no UPDATE/DELETE grants).

## Jobs

| Job | Schedule | Program |
|-----|----------|---------|
| `auditArchiveJob` | Daily 02:00 UTC | AUD002B |
| `auditPurgeJob` | Daily 03:00 UTC | AUDPURGE |

## References

- WO-133: S3 Object Lock infrastructure (`infra/modules/object-storage`)
- WO-170: Audit archive batch job
- WO-171: Retention purge with cryptographic erasure
