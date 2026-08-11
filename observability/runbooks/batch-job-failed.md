# Batch Job Failed (WO-144)

**Alert:** `BatchJobFailed`  
**Runbook key:** `batch-job-failed`  
**Owner:** Platform / Batch Operations on-call

## Trigger and Alert Reference

Fires when `pcis:batch_job_exit_code != 0` or `kube_job_status_failed{namespace=~"pcis-.*"} > 0`
for 0 minutes. Labels include `job_name`, `namespace`, and `pod`. Rule:
`observability/prometheus/alerting-rules.yaml` (`pcis-batch-alerts` group).

Exit codes 1–5 follow the WO-137 contract: 1=item threshold, 2=archive mismatch,
3=cursor failure, 4=audit write failure, 5=config failure.

## Severity and First Responder

| Field | Value |
|-------|-------|
| Severity | critical |
| First responder | Platform on-call (PagerDuty page) |
| Target MTTR | 30 minutes to restart or escalate |

## Prerequisites

- kubectl access to the affected namespace (`pcis-dev`, `pcis-test`, or `pcis-prd`)
- Argo CD read access for batch CronJob/Job definitions
- Grafana PCIS Batch dashboard
- Change ticket if manual re-run in production

## Diagnostic Queries and Log Filters

**Prometheus / Grafana:**

```promql
pcis:batch_job_exit_code{job_name="<JOB>"}
kube_job_status_failed{namespace="<NS>", job_name=~".+"}
pcis:batch_job_duration:p95{job_name="<JOB>"}
```

**Kubernetes:**

```bash
kubectl -n <NS> get jobs -l batch.kubernetes.io/job-name=<JOB>
kubectl -n <NS> logs job/<JOB-POD-NAME> --tail=200
kubectl -n <NS> describe job <JOB-NAME>
```

**Structured logs (JSON):**

```text
service="<batch-service>" AND job_name="<JOB>" AND level=ERROR
MDC: correlationId, reasonCode, run_id
```

## Step-by-Step Recovery

1. Identify the failed job and exit code from alert labels and `pcis_batch_job_exit_code`.
2. Pull pod logs; locate the terminal `reasonCode` and stack trace (no PII in tickets).
3. **Exit code 4 (audit write failure):** run unrecorded-mutation reconciliation — compare
   committed financial rows against audit outbox depth before restart.
4. **Exit code 2 (archive mismatch):** quarantine the chunk per purge runbook; do not
   delete source rows until verification passes.
5. For transient infra failures (DB timeout, OOM): fix underlying issue, then restart from
   last committed chunk:

   ```bash
   kubectl -n <NS> delete job <FAILED-JOB-NAME>
   argocd app sync pcis-batch-<ENV>
   # Or trigger CronJob manually after confirming restart parameters
   ```

6. Confirm Spring Batch restart metadata resumed from the last committed chunk (one item per
   commit for PRM005B, CLM006B, POL006B, BIL003B, CMM001B; ≤1000 rows for AUD002B).

## Verification, Escalation, and Post-Incident

**Verification:**

- [ ] Job completes with exit code 0
- [ ] `pcis:batch_job_exit_code` returns to 0
- [ ] RPT-equivalent counters match expected processed/skipped counts
- [ ] No duplicate financial records (spot-check reconciliation query)
- [ ] Alert clears in Alertmanager within one evaluation cycle

**Escalation:** Escalate to Batch Operations lead if exit code 2 or 4 persists after one
restart attempt, or if financial reconciliation shows unexplained deltas.

**Rollback:** See `ops/runbooks/rollback.md` if a bad release caused the failure.

**Post-incident:** Open problem ticket with job name, exit code, correlation IDs, and root
cause. Update scheduler annotations if window headroom was insufficient.
