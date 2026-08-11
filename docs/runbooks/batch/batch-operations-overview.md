# Batch Operations Overview (WO-213)

**Owner:** Batch Operations / Platform on-call

## Scheduler map and dependency order

See `ops/scheduler-map.yaml` for authoritative legacy-to-target mapping. Nightly order (ASSUMPTION until Phase 0 baselines):

1. `billingInstallmentJob` (BIL003B)
2. `commissionCalculationJob` (CMM001B)
3. `claimPaymentJob` (CLM006B)
4. `premiumProcessingJob` (PRM005B)
5. `policyRenewalJob` (POL006B)
6. `auditArchiveJob` (AUD002B)

## Dashboards

- Grafana: `k8s/monitoring/dashboards/batch-jobs.json` (also `observability/grafana/dashboards/batch-jobs.json`)
- Key metrics: `spring_batch_job_duration_seconds`, `pcis_batch_job_exit_code`, `pcis_batch_items_skipped_total`, `pcis_batch_outbox_lag_seconds`

## Parallel-run reconciliation breaks

When reconciliation alerts fire during coexistence:

| Classification | Meaning | Action |
|----------------|---------|--------|
| MISSING_IN_TARGET | Row in Db2 not in PostgreSQL | Check sync-agent watermark lag (WO-214) |
| MISSING_IN_LEGACY | Row in PostgreSQL not in Db2 | Verify no write-back for unmigrated domain |
| VALUE_MISMATCH | Field differs cent-level | Compare golden fixtures; check approved parity register |

## Escalation

| Severity | Contact |
|----------|---------|
| P1 batch failure (exit ≠ 0) | Platform on-call → Batch Operations lead |
| Window breach >75% | Batch Operations → Engineering manager |
| Reconciliation break spike | Coexistence squad + Compliance |

Per-job runbooks: `docs/runbooks/batch/*.md`
