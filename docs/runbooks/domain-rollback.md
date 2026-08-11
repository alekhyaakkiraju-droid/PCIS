# Domain Rollback Runbook (WO-217)

**Owner:** Coexistence squad / Platform on-call  
**Trigger:** Cutover gate FAIL, post-cutover error-rate breach, or manual rollback decision

## Purpose

Remove non-authoritative shadow data written to PostgreSQL during `SHADOW_WRITE` or failed parallel-run phases, without touching Db2 for i (system of record during coexistence).

## Prerequisites

- Domain cutover state confirmed with coexistence control plane (target domain not in `TARGET_ONLY`).
- Rollback domain name (`billing`, `claims`, `customer`, …) identified.
- `reconciliation-batch` image deployed with rollback SQL scripts under `classpath:rollback/`.

## Manual rollback job

Trigger the Spring Batch rollback job (profile `rollback-manual`):

```bash
kubectl create job --from=cronjob/reconciliation-job reconciliation-rollback-$(date +%s) \
  --dry-run=client -o yaml | \
  sed 's/reconciliationJob/domainRollbackJob/' | \
  kubectl apply -f -

kubectl set env job/reconciliation-rollback-... \
  SPRING_PROFILES_ACTIVE=rollback-manual \
  RECON_ROLLBACK_DOMAIN=billing
```

Or run locally:

```bash
java -jar reconciliation-batch-exec.jar \
  --spring.profiles.active=rollback-manual \
  --spring.batch.job.name=domainRollbackJob \
  --domain=billing
```

## Scripts executed

| Domain | Script |
|--------|--------|
| billing | `rollback/billing_shadow_cleanup.sql` |
| claims | `rollback/claims_shadow_cleanup.sql` |
| customer | `rollback/customer_shadow_cleanup.sql` |

Each script deletes rows tagged with `crt_user = 'SHADOW_SYNC'` only.

## Verification

1. Confirm job exit code `0` and `rollback.statementsExecuted > 0` in batch execution context.
2. Query target tables — no rows with `crt_user = 'SHADOW_SYNC'` for the domain.
3. Re-run nightly `reconciliationJob` and confirm unexplained break count trends down.
4. Record rollback in cutover audit log with actor, domain, reason, and timestamp.

## Rollback of the rollback

Shadow cleanup is destructive for non-authoritative copies only. If authoritative rows were incorrectly tagged, restore from PostgreSQL PITR — do **not** re-run cleanup scripts.

## Escalation

| Severity | Contact |
|----------|---------|
| P1 financial divergence after rollback | Coexistence squad lead + Finance owner |
| P2 incomplete cleanup | Platform on-call |

## Related

- Parallel-run break triage: `docs/runbooks/batch/batch-operations-overview.md`
- Reconciliation Grafana dashboard: `observability/grafana/dashboards/reconciliation.json`
- Sync lag (missing legacy rows): `observability/alerts/sync-lag.yaml`
