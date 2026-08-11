# Phase 0 Production Baseline Report (WO-238) — Synthetic Example

**Generated:** `2026-08-11T06:00:00Z`  
**Environment:** INSPRD / INSPRDDTA  
**Operator:** `baseline-test-fixture`  
**Scripts:** `baseline/scripts/measure_table_volumes.sql`, `baseline/scripts/measure_batch_windows.sql`

> Synthetic populated example for CI and operator training. Values are
> fabricated from `baseline/test-fixtures/run_log_samples/` and representative
> table volumes — not real INSPRDDTA production data.

## Executive Summary

All 55 tables measured successfully. Six batch programs recorded 30-day run-log
entries after WO-237 instrumentation. PRM005B is the longest-running nightly
job (P95 612s); AUD002B is shortest (P95 228s). No batch errors in the sample
window.

| Metric | Value |
|--------|-------|
| Total tables measured | 55 / 55 |
| Total production rows (sum) | 4,872,416 |
| Batch programs with 30-day runs | 6 / 6 |
| Longest measured batch (max duration) | PRM005B — 648s |

## Table Volume Baseline (selected rows)

| # | Table | Domain | Row Count | Min CRT_TIMESTAMP | Max CRT_TIMESTAMP |
|---|-------|--------|-----------|-------------------|-------------------|
| 1 | CUSTOMER_T | CUS | 128,450 | 2019-03-14 08:22:11 | 2026-08-10 17:44:02 |
| 14 | POLICY_T | POL | 312,880 | 2019-04-01 06:00:00 | 2026-08-10 23:59:58 |
| 37 | CLAIM_T | CLM | 89,204 | 2019-06-18 12:01:33 | 2026-08-10 16:30:00 |
| 51 | AUDIT_LOG_T | AUD | 2,841,000 | 2020-01-01 00:00:01 | 2026-08-11 02:18:42 |
| 49 | RPT_RUN_LOG_T | RPT | 1,842 | 2025-01-02 03:00:00 | 2026-08-11 05:09:44 |

Full 55-table inventory: see `phase0_baseline_report.md` template.

## Batch Window Baseline (30-day)

Derived from synthetic run-log fixtures (durations = END − START):

| Program | CronJob | Runs | Avg (s) | Max (s) | Min (s) | P95 (s) | Avg Selected | Avg Updated | Total Errors |
|---------|---------|------|---------|---------|---------|---------|--------------|-------------|--------------|
| AUD002B | audit-archive-job | 30 | 222.4 | 228 | 218 | 227.1 | 2,480 | 2,480 | 0 |
| BIL003B | billing-installment-job | 30 | 738.2 | 752 | 710 | 748.5 | 14,200 | 14,180 | 0 |
| CLM006B | claim-payment-job | 30 | 665.1 | 678 | 640 | 672.0 | 3,850 | 3,820 | 0 |
| CMM001B | commission-calc-job | 30 | 513.6 | 528 | 498 | 521.4 | 6,100 | 6,100 | 0 |
| PRM005B | premium-processing-job | 30 | 584.7 | 648 | 560 | 612.3 | 18,400 | 12,300 | 0 |
| POL006B | policy-renewal-job | 12 | 1,031.0 | 1,080 | 980 | 1,065.2 | 4,200 | 4,200 | 0 |

## KPI Mapping

| KPI / Guardrail | Source Metric | Baseline Value | Target / Threshold | Consumer |
|-----------------|---------------|----------------|--------------------|----------|
| Batch-window headroom ≥ 25% | Max duration vs 4h window | PRM005B 648s / 14400s = 4.5% util | ≤ 75% util | Prometheus alert |
| Scheduler map avg duration | AVG_DURATION_SECONDS | PRM005B 585s (example) | Replace MEASUREMENT_PENDING | ops/scheduler-map.yaml |
| Scheduler map max duration | MAX_DURATION_SECONDS | PRM005B 648s (example) | Replace MEASUREMENT_PENDING | ops/scheduler-map.yaml |
| Largest production table | ROW_COUNT max | AUDIT_LOG_T — 2,841,000 | Bulk-load planning | Migration WO |
| Audit log growth rate | AUDIT_LOG_T rows | 2,841,000 rows | 365-day retention | AUD002B sizing |
| Batch error rate | TOTAL_REC_ERRORS / RUN_COUNT | 0 / 162 = 0% | Zero steady-state | Alertmanager |
| API p95 regression baseline | Interactive latency | Pending separate measurement | No regression | Grafana |
| Golden-output seed sizing | Top table counts | AUDIT_LOG_T 10% subset ≈ 284k | ≤ 10% prod | golden/fixtures |

## Scheduler Map Values (example post-measurement)

These values would replace `MEASUREMENT_PENDING` in `ops/scheduler-map.yaml`:

| Program | measured_avg_duration_seconds | measured_max_duration_seconds |
|---------|------------------------------|------------------------------|
| AUD002B | 222 | 228 |
| BIL003B | 738 | 752 |
| CLM006B | 665 | 678 |
| CMM001B | 514 | 528 |
| PRM005B | 585 | 648 |
| POL006B | 1031 | 1080 |
