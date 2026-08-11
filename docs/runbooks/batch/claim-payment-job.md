# Claim Payment Batch (`claimPaymentJob`) — WO-213

**Alert:** `BatchJobFailed`, `BatchWindowBreached`  
**Legacy:** CLM006B  
**Schedule:** ASSUMPTION `30 2 * * *` (see `ops/scheduler-map.yaml`)

## Exit codes

| Code | Meaning |
|------|---------|
| 0 | Success |
| 1 | Skip/error threshold exceeded |
| 4 | Outbox write failure |
| 5 | Configuration failure |

## Common failure modes

- **Authority check failure:** verify authz-svc and batch OAuth2 client credentials.
- **Reserve drawdown mismatch:** compare against golden fixture `CLM006B` outputs.
- **Outbox lag:** see `observability/runbooks/audit-outbox-lag-high.md`.

## Restart procedure

1. Inspect Spring Batch `BATCH_JOB_EXECUTION` for last completed step.
2. Re-run CronJob with same parameters; chunk-oriented steps resume from last commit.
3. Confirm `pcis_batch_job_exit_code{job_name="claimPaymentJob"} == 0`.

## Tunables

`pcis.batch.skip-threshold`, payment authority limits via config tunables.

## Escalation

Platform on-call → Claims domain lead if financial totals diverge from golden outputs.
