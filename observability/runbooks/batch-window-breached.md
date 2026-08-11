# Batch Window Breached (WO-144)

**Alert:** `BatchWindowBreached`  
**Runbook key:** `batch-window-breached`  
**Owner:** Platform / Batch Operations on-call

## Trigger and Alert Reference

Fires when `pcis:batch_window:utilization_ratio > 0.75` for 5 minutes. Indicates the batch
job consumed more than 75% of its configured `pcis_batch_window_seconds` gauge (ASSUMPTION
placeholder until Phase 0 baseline completes). Rule: `observability/prometheus/alerting-rules.yaml`
(`pcis-batch-alerts` group). Inhibited when `BatchJobFailed` fires for the same `job_name`.

## Severity and First Responder

| Field | Value |
|-------|-------|
| Severity | warning |
| First responder | Platform on-call (`#pcis-ops` Slack) |
| Target MTTR | Same business day — tune window or optimize job |

## Prerequisites

- Grafana Batch Window Utilization panel
- Access to `pcis_batch_window_seconds` gauge values in Prometheus
- Baseline report reference (Phase 0 measured durations per job)
- Batch scheduler map (`ops/scheduler-map.yaml`)

## Diagnostic Queries and Log Filters

**Prometheus:**

```promql
pcis:batch_window:utilization_ratio{job_name="<JOB>"}
pcis:batch_job_duration:p95{job_name="<JOB>"}
pcis_batch_window_seconds{job_name="<JOB>"}
rate(pcis_db_query_count_total{job_name="<JOB>"}[5m])
```

**Logs:**

```text
job_name="<JOB>" AND message=~"step.*duration|chunk.*complete"
```

Compare current duration against committed baseline report values.

## Step-by-Step Recovery

1. Confirm the job is still running (not failed) — if `BatchJobFailed` is also firing,
   follow `batch-job-failed.md` first.
2. Check for data-volume spikes (row counts vs. baseline) in job completion metrics.
3. Review slow SQL: HikariCP active connections and query count per run.
4. **Short-term:** If job will complete within the hard window, monitor; document overrun.
5. **Medium-term mitigations:**
   - Increase chunk parallelism only where commit semantics allow
   - Add indexes identified by slow-query logs
   - Request baseline remeasurement if `pcis_batch_window_seconds` placeholder is wrong
6. **Schedule adjustment:** Coordinate with Batch Operations to shift CronJob schedule only
   after change-advisory approval in production.

## Verification, Escalation, and Post-Incident

**Verification:**

- [ ] Utilization ratio drops below 0.75 on the next successful run
- [ ] At least 25% headroom restored (ratio ≤ 0.75) for three consecutive runs
- [ ] No downstream SLA impact (downstream jobs started on time)

**Escalation:** Escalate to Batch Operations if utilization exceeds 0.90 or overlaps the
next scheduler slot in `ops/scheduler-map.yaml`.

**Rollback:** Revert recent deployment if regression coincides with window breach —
`ops/runbooks/rollback.md`.

**Post-incident:** Update baseline report and `pcis_batch_window_seconds` gauge when measured
values are available.
