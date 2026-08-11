# Phase 0 Production Baseline Report (WO-238)

**Generated:** `{{MEASUREMENT_TIMESTAMP}}`  
**Environment:** INSPRD / INSPRDDTA  
**Operator:** `{{OPERATOR_NAME}}`  
**Scripts:** `baseline/scripts/measure_table_volumes.sql`, `baseline/scripts/measure_batch_windows.sql`

## Purpose

This report captures production data volumes and batch-window timings required to
replace ASSUMPTION scheduler placeholders in `ops/scheduler-map.yaml` and to
establish guardrail thresholds for observability (25% batch-window headroom, API
p95 regression baselines).

## Executive Summary

{{EXECUTIVE_SUMMARY}}

| Metric | Value |
|--------|-------|
| Total tables measured | {{TABLE_COUNT}} / 55 |
| Total production rows (sum) | {{TOTAL_ROW_COUNT}} |
| Batch programs with 30-day runs | {{BATCH_PROGRAMS_WITH_DATA}} / 6 |
| Longest measured batch (max duration) | {{LONGEST_BATCH_PROGRAM}} — {{LONGEST_BATCH_MAX_SECONDS}}s |

## Table Volume Baseline

Source: `baseline/scripts/measure_table_volumes.sql` against `INSPRDDTA.*`

| # | Table | Domain | Row Count | Min CRT_TIMESTAMP | Max CRT_TIMESTAMP |
|---|-------|--------|-----------|-------------------|-------------------|
| 1 | CUSTOMER_T | CUS | {{CUSTOMER_T_ROW_COUNT}} | {{CUSTOMER_T_MIN_CRT}} | {{CUSTOMER_T_MAX_CRT}} |
| 2 | CUSTOMER_ADDRESS_T | CUS | {{CUSTOMER_ADDRESS_T_ROW_COUNT}} | {{CUSTOMER_ADDRESS_T_MIN_CRT}} | {{CUSTOMER_ADDRESS_T_MAX_CRT}} |
| 3 | CUSTOMER_CONTACT_T | CUS | {{CUSTOMER_CONTACT_T_ROW_COUNT}} | {{CUSTOMER_CONTACT_T_MIN_CRT}} | {{CUSTOMER_CONTACT_T_MAX_CRT}} |
| 4 | AGENT_T | AGT | {{AGENT_T_ROW_COUNT}} | {{AGENT_T_MIN_CRT}} | {{AGENT_T_MAX_CRT}} |
| 5 | AGENT_LICENSE_T | AGT | {{AGENT_LICENSE_T_ROW_COUNT}} | {{AGENT_LICENSE_T_MIN_CRT}} | {{AGENT_LICENSE_T_MAX_CRT}} |
| 6 | AGENT_COMMISSION_T | AGT | {{AGENT_COMMISSION_T_ROW_COUNT}} | {{AGENT_COMMISSION_T_MIN_CRT}} | {{AGENT_COMMISSION_T_MAX_CRT}} |
| 7 | COMMISSION_T | AGT | {{COMMISSION_T_ROW_COUNT}} | {{COMMISSION_T_MIN_CRT}} | {{COMMISSION_T_MAX_CRT}} |
| 8 | COMMISSION_RATE_T | AGT | {{COMMISSION_RATE_T_ROW_COUNT}} | {{COMMISSION_RATE_T_MIN_CRT}} | {{COMMISSION_RATE_T_MAX_CRT}} |
| 9 | QUOTE_T | QTE | {{QUOTE_T_ROW_COUNT}} | {{QUOTE_T_MIN_CRT}} | {{QUOTE_T_MAX_CRT}} |
| 10 | QUOTE_COVERAGE_T | QTE | {{QUOTE_COVERAGE_T_ROW_COUNT}} | {{QUOTE_COVERAGE_T_MIN_CRT}} | {{QUOTE_COVERAGE_T_MAX_CRT}} |
| 11 | UW_RULE_T | UND | {{UW_RULE_T_ROW_COUNT}} | {{UW_RULE_T_MIN_CRT}} | {{UW_RULE_T_MAX_CRT}} |
| 12 | UW_REFERRAL_T | UND | {{UW_REFERRAL_T_ROW_COUNT}} | {{UW_REFERRAL_T_MIN_CRT}} | {{UW_REFERRAL_T_MAX_CRT}} |
| 13 | UW_DECISION_T | UND | {{UW_DECISION_T_ROW_COUNT}} | {{UW_DECISION_T_MIN_CRT}} | {{UW_DECISION_T_MAX_CRT}} |
| 14 | POLICY_T | POL | {{POLICY_T_ROW_COUNT}} | {{POLICY_T_MIN_CRT}} | {{POLICY_T_MAX_CRT}} |
| 15 | COVERAGE_T | POL | {{COVERAGE_T_ROW_COUNT}} | {{COVERAGE_T_MIN_CRT}} | {{COVERAGE_T_MAX_CRT}} |
| 16 | COVERAGE_TYPE_T | POL | {{COVERAGE_TYPE_T_ROW_COUNT}} | {{COVERAGE_TYPE_T_MIN_CRT}} | {{COVERAGE_TYPE_T_MAX_CRT}} |
| 17 | DEDUCTIBLE_T | POL | {{DEDUCTIBLE_T_ROW_COUNT}} | {{DEDUCTIBLE_T_MIN_CRT}} | {{DEDUCTIBLE_T_MAX_CRT}} |
| 18 | POLICY_HISTORY_T | POL | {{POLICY_HISTORY_T_ROW_COUNT}} | {{POLICY_HISTORY_T_MIN_CRT}} | {{POLICY_HISTORY_T_MAX_CRT}} |
| 19 | POLICY_VEHICLE_T | POL | {{POLICY_VEHICLE_T_ROW_COUNT}} | {{POLICY_VEHICLE_T_MIN_CRT}} | {{POLICY_VEHICLE_T_MAX_CRT}} |
| 20 | POLICY_PROPERTY_T | POL | {{POLICY_PROPERTY_T_ROW_COUNT}} | {{POLICY_PROPERTY_T_MIN_CRT}} | {{POLICY_PROPERTY_T_MAX_CRT}} |
| 21 | ENDORSEMENT_T | POL | {{ENDORSEMENT_T_ROW_COUNT}} | {{ENDORSEMENT_T_MIN_CRT}} | {{ENDORSEMENT_T_MAX_CRT}} |
| 22 | CANCELLATION_REASON_T | POL | {{CANCELLATION_REASON_T_ROW_COUNT}} | {{CANCELLATION_REASON_T_MIN_CRT}} | {{CANCELLATION_REASON_T_MAX_CRT}} |
| 23 | RATE_TABLE_T | PRM | {{RATE_TABLE_T_ROW_COUNT}} | {{RATE_TABLE_T_MIN_CRT}} | {{RATE_TABLE_T_MAX_CRT}} |
| 24 | RATE_FACTOR_T | PRM | {{RATE_FACTOR_T_ROW_COUNT}} | {{RATE_FACTOR_T_MIN_CRT}} | {{RATE_FACTOR_T_MAX_CRT}} |
| 25 | PREMIUM_CALC_T | PRM | {{PREMIUM_CALC_T_ROW_COUNT}} | {{PREMIUM_CALC_T_MIN_CRT}} | {{PREMIUM_CALC_T_MAX_CRT}} |
| 26 | PREMIUM_CALC_DETAIL_T | PRM | {{PREMIUM_CALC_DETAIL_T_ROW_COUNT}} | {{PREMIUM_CALC_DETAIL_T_MIN_CRT}} | {{PREMIUM_CALC_DETAIL_T_MAX_CRT}} |
| 27 | DISCOUNT_RULE_T | PRM | {{DISCOUNT_RULE_T_ROW_COUNT}} | {{DISCOUNT_RULE_T_MIN_CRT}} | {{DISCOUNT_RULE_T_MAX_CRT}} |
| 28 | SURCHARGE_RULE_T | PRM | {{SURCHARGE_RULE_T_ROW_COUNT}} | {{SURCHARGE_RULE_T_MIN_CRT}} | {{SURCHARGE_RULE_T_MAX_CRT}} |
| 29 | TAX_TABLE_T | PRM | {{TAX_TABLE_T_ROW_COUNT}} | {{TAX_TABLE_T_MIN_CRT}} | {{TAX_TABLE_T_MAX_CRT}} |
| 30 | RISK_SCORE_FACTOR_T | PRM | {{RISK_SCORE_FACTOR_T_ROW_COUNT}} | {{RISK_SCORE_FACTOR_T_MIN_CRT}} | {{RISK_SCORE_FACTOR_T_MAX_CRT}} |
| 31 | BILLING_PLAN_T | BIL | {{BILLING_PLAN_T_ROW_COUNT}} | {{BILLING_PLAN_T_MIN_CRT}} | {{BILLING_PLAN_T_MAX_CRT}} |
| 32 | BILLING_SCHEDULE_T | BIL | {{BILLING_SCHEDULE_T_ROW_COUNT}} | {{BILLING_SCHEDULE_T_MIN_CRT}} | {{BILLING_SCHEDULE_T_MAX_CRT}} |
| 33 | BILLING_NOTICE_T | BIL | {{BILLING_NOTICE_T_ROW_COUNT}} | {{BILLING_NOTICE_T_MIN_CRT}} | {{BILLING_NOTICE_T_MAX_CRT}} |
| 34 | INVOICE_T | BIL | {{INVOICE_T_ROW_COUNT}} | {{INVOICE_T_MIN_CRT}} | {{INVOICE_T_MAX_CRT}} |
| 35 | PAYMENT_T | PAY | {{PAYMENT_T_ROW_COUNT}} | {{PAYMENT_T_MIN_CRT}} | {{PAYMENT_T_MAX_CRT}} |
| 36 | REFUND_T | PAY | {{REFUND_T_ROW_COUNT}} | {{REFUND_T_MIN_CRT}} | {{REFUND_T_MAX_CRT}} |
| 37 | CLAIM_T | CLM | {{CLAIM_T_ROW_COUNT}} | {{CLAIM_T_MIN_CRT}} | {{CLAIM_T_MAX_CRT}} |
| 38 | CLAIM_RESERVE_T | CLM | {{CLAIM_RESERVE_T_ROW_COUNT}} | {{CLAIM_RESERVE_T_MIN_CRT}} | {{CLAIM_RESERVE_T_MAX_CRT}} |
| 39 | CLAIM_RESERVE_HISTORY_T | CLM | {{CLAIM_RESERVE_HISTORY_T_ROW_COUNT}} | {{CLAIM_RESERVE_HISTORY_T_MIN_CRT}} | {{CLAIM_RESERVE_HISTORY_T_MAX_CRT}} |
| 40 | CLAIM_PAYMENT_T | CLM | {{CLAIM_PAYMENT_T_ROW_COUNT}} | {{CLAIM_PAYMENT_T_MIN_CRT}} | {{CLAIM_PAYMENT_T_MAX_CRT}} |
| 41 | CLAIM_ADJUSTER_T | CLM | {{CLAIM_ADJUSTER_T_ROW_COUNT}} | {{CLAIM_ADJUSTER_T_MIN_CRT}} | {{CLAIM_ADJUSTER_T_MAX_CRT}} |
| 42 | CLAIM_NOTE_T | CLM | {{CLAIM_NOTE_T_ROW_COUNT}} | {{CLAIM_NOTE_T_MIN_CRT}} | {{CLAIM_NOTE_T_MAX_CRT}} |
| 43 | CLAIM_DOCUMENT_T | CLM | {{CLAIM_DOCUMENT_T_ROW_COUNT}} | {{CLAIM_DOCUMENT_T_MIN_CRT}} | {{CLAIM_DOCUMENT_T_MAX_CRT}} |
| 44 | APPROVAL_T | CLM | {{APPROVAL_T_ROW_COUNT}} | {{APPROVAL_T_MIN_CRT}} | {{APPROVAL_T_MAX_CRT}} |
| 45 | RECOVERY_T | CLM | {{RECOVERY_T_ROW_COUNT}} | {{RECOVERY_T_MIN_CRT}} | {{RECOVERY_T_MAX_CRT}} |
| 46 | REINSURANCE_TREATY_T | REI | {{REINSURANCE_TREATY_T_ROW_COUNT}} | {{REINSURANCE_TREATY_T_MIN_CRT}} | {{REINSURANCE_TREATY_T_MAX_CRT}} |
| 47 | REINSURANCE_CESSION_T | REI | {{REINSURANCE_CESSION_T_ROW_COUNT}} | {{REINSURANCE_CESSION_T_MIN_CRT}} | {{REINSURANCE_CESSION_T_MAX_CRT}} |
| 48 | DOCUMENT_T | DOC | {{DOCUMENT_T_ROW_COUNT}} | {{DOCUMENT_T_MIN_CRT}} | {{DOCUMENT_T_MAX_CRT}} |
| 49 | RPT_RUN_LOG_T | RPT | {{RPT_RUN_LOG_T_ROW_COUNT}} | {{RPT_RUN_LOG_T_MIN_CRT}} | {{RPT_RUN_LOG_T_MAX_CRT}} |
| 50 | RPT_PARM_T | RPT | {{RPT_PARM_T_ROW_COUNT}} | {{RPT_PARM_T_MIN_CRT}} | {{RPT_PARM_T_MAX_CRT}} |
| 51 | AUDIT_LOG_T | AUD | {{AUDIT_LOG_T_ROW_COUNT}} | {{AUDIT_LOG_T_MIN_CRT}} | {{AUDIT_LOG_T_MAX_CRT}} |
| 52 | AUDIT_LOG_ARCHIVE_T | AUD | {{AUDIT_LOG_ARCHIVE_T_ROW_COUNT}} | {{AUDIT_LOG_ARCHIVE_T_MIN_CRT}} | {{AUDIT_LOG_ARCHIVE_T_MAX_CRT}} |
| 53 | SEC_USER_T | SEC | {{SEC_USER_T_ROW_COUNT}} | {{SEC_USER_T_MIN_CRT}} | {{SEC_USER_T_MAX_CRT}} |
| 54 | ROLE_MENU_T | SEC | {{ROLE_MENU_T_ROW_COUNT}} | {{ROLE_MENU_T_MIN_CRT}} | {{ROLE_MENU_T_MAX_CRT}} |
| 55 | CODE_TABLE_T | SEC | {{CODE_TABLE_T_ROW_COUNT}} | {{CODE_TABLE_T_MIN_CRT}} | {{CODE_TABLE_T_MAX_CRT}} |

## Batch Window Baseline (30-day)

Source: `baseline/scripts/measure_batch_windows.sql` — `RPT_RUN_LOG_T` with
`START_TIMESTAMP` / `END_TIMESTAMP` (WO-237).

| Program | CronJob | Runs | Avg (s) | Max (s) | Min (s) | P95 (s) | Avg Selected | Avg Updated | Total Errors |
|---------|---------|------|---------|---------|---------|---------|--------------|-------------|--------------|
| AUD002B | audit-archive-job | {{AUD002B_RUN_COUNT}} | {{AUD002B_AVG_DURATION}} | {{AUD002B_MAX_DURATION}} | {{AUD002B_MIN_DURATION}} | {{AUD002B_P95_DURATION}} | {{AUD002B_AVG_SELECTED}} | {{AUD002B_AVG_UPDATED}} | {{AUD002B_TOTAL_ERRORS}} |
| BIL003B | billing-installment-job | {{BIL003B_RUN_COUNT}} | {{BIL003B_AVG_DURATION}} | {{BIL003B_MAX_DURATION}} | {{BIL003B_MIN_DURATION}} | {{BIL003B_P95_DURATION}} | {{BIL003B_AVG_SELECTED}} | {{BIL003B_AVG_UPDATED}} | {{BIL003B_TOTAL_ERRORS}} |
| CLM006B | claim-payment-job | {{CLM006B_RUN_COUNT}} | {{CLM006B_AVG_DURATION}} | {{CLM006B_MAX_DURATION}} | {{CLM006B_MIN_DURATION}} | {{CLM006B_P95_DURATION}} | {{CLM006B_AVG_SELECTED}} | {{CLM006B_AVG_UPDATED}} | {{CLM006B_TOTAL_ERRORS}} |
| CMM001B | commission-calc-job | {{CMM001B_RUN_COUNT}} | {{CMM001B_AVG_DURATION}} | {{CMM001B_MAX_DURATION}} | {{CMM001B_MIN_DURATION}} | {{CMM001B_P95_DURATION}} | {{CMM001B_AVG_SELECTED}} | {{CMM001B_AVG_UPDATED}} | {{CMM001B_TOTAL_ERRORS}} |
| PRM005B | premium-processing-job | {{PRM005B_RUN_COUNT}} | {{PRM005B_AVG_DURATION}} | {{PRM005B_MAX_DURATION}} | {{PRM005B_MIN_DURATION}} | {{PRM005B_P95_DURATION}} | {{PRM005B_AVG_SELECTED}} | {{PRM005B_AVG_UPDATED}} | {{PRM005B_TOTAL_ERRORS}} |
| POL006B | policy-renewal-job | {{POL006B_RUN_COUNT}} | {{POL006B_AVG_DURATION}} | {{POL006B_MAX_DURATION}} | {{POL006B_MIN_DURATION}} | {{POL006B_P95_DURATION}} | {{POL006B_AVG_SELECTED}} | {{POL006B_AVG_UPDATED}} | {{POL006B_TOTAL_ERRORS}} |

## KPI Mapping

Maps measured baselines to programme guardrails and downstream consumers.

| KPI / Guardrail | Source Metric | Baseline Value | Target / Threshold | Consumer |
|-----------------|---------------|----------------|--------------------|----------|
| Batch-window headroom ≥ 25% | `MAX_DURATION_SECONDS` vs declared window | {{HEADROOM_BASELINE_NOTE}} | `measured_max / window ≤ 0.75` | Prometheus `batch_window_headroom` alert |
| Scheduler map avg duration | `AVG_DURATION_SECONDS` per program | See batch table | Replace `MEASUREMENT_PENDING` in `ops/scheduler-map.yaml` | WO-137 CronJob resource sizing |
| Scheduler map max duration | `MAX_DURATION_SECONDS` per program | See batch table | Replace `MEASUREMENT_PENDING` in `ops/scheduler-map.yaml` | `startingDeadlineSeconds` tuning |
| Largest production table | `ROW_COUNT` max across 55 tables | {{LARGEST_TABLE_NAME}} — {{LARGEST_TABLE_ROWS}} | Migration bulk-load planning | Data migration WO |
| Audit log growth rate | `AUDIT_LOG_T` row count + CRT range | {{AUDIT_LOG_T_ROW_COUNT}} rows | Retention / archive capacity | AUD002B archive sizing |
| Batch error rate | `TOTAL_REC_ERRORS / RUN_COUNT` | {{BATCH_ERROR_RATE_NOTE}} | Zero errors in steady state | Alertmanager P1 rules |
| API p95 regression baseline | Interactive latency (out of scope here) | {{API_P95_PLACEHOLDER}} | No worse than measured p95 | Grafana API dashboard |
| Golden-output seed sizing | Top-N table row counts | {{GOLDEN_SIZING_NOTE}} | Representative subset ≤ 10% prod | `golden/fixtures/` |

## Scheduler Map Update Checklist

After production measurement completes:

1. Run `measure_batch_windows.sql` and copy `AVG_DURATION_SECONDS` / `MAX_DURATION_SECONDS` into each `batch_programs.*` entry in `ops/scheduler-map.yaml`.
2. Re-run `python3 ops/validate-scheduler-map.py` and `./ops/validate-scheduler-map.sh`.
3. Replace ASSUMPTION CronJob schedules only when IBM i JOBSCDE recovery confirms cadence.
4. Commit updated `phase0_baseline_report.md` (this file) with placeholders replaced by measured values.

## Validation

```bash
python3 ops/validate-scheduler-map.py
./ops/validate-scheduler-map.sh
./baseline/scripts/validate_sql_syntax.sh
```
