# Claims Service Operational Runbook (WO-200)

**Service:** `claims-svc`  
**Runbook key:** `claims-svc-runbook`  
**Owner:** Platform / Claims Operations on-call

## Trigger and Alert Reference

This runbook covers claims-domain alerts in `observability/alerts/claims-svc-alerts.yaml`
and `observability/alerts/claims-outbox-lag.yaml`:

| Alert | Severity | Condition |
|-------|----------|-----------|
| `ClaimsBatchJobFailed` | P1 | Batch job non-zero exit or `kube_job_status_failed > 0` |
| `ClaimsOutboxLagHigh` | P1 | `claims_outbox_lag_seconds > 30` for 2 minutes |
| `ClaimsApiErrorRateHigh` | P2 | API error rate > 1% over 5 minutes |
| `ClaimsApiReadLatencyHigh` | P2 | Read p95 > 500ms over 5 minutes |
| `ClaimsApiWriteLatencyHigh` | P2 | Write p95 > 1000ms over 5 minutes |

Grafana dashboard: `observability/grafana/dashboards/claims-svc.json`

## Severity and First Responder

| Field | Value |
|-------|-------|
| P1 alerts | Platform on-call (PagerDuty page) |
| P2 alerts | Platform on-call (`#pcis-ops` Slack) |
| Target MTTR | 30 minutes for P1; 60 minutes for P2 |

## Prerequisites

- kubectl access to `pcis-dev`, `pcis-test`, or `pcis-prd`
- Grafana PCIS Claims dashboard
- Argo CD access for `claims-svc` and `claims-batch` applications
- OpenTelemetry/Jaeger trace access
- PostgreSQL read access to `pcis_claims` schema

---

## 1. Batch Job Failure

### Diagnostic Queries

```promql
claims_batch_job_duration_seconds{service="claims-svc"}
claims_batch_items_skipped_total{service="claims-svc"}
claims_batch_items_processed_total{service="claims-svc"}
kube_job_status_failed{namespace="<NS>", job_name=~"claim-payment.*"}
pcis:batch_job_exit_code{job_name=~"claimPaymentJob|claim-payment.*"}
```

```bash
kubectl -n <NS> get jobs -l app=claims-batch
kubectl -n <NS> logs job/<JOB-POD-NAME> --tail=300
kubectl -n <NS> describe job <JOB-NAME>
```

```sql
-- Review skipped payments (Spring Batch skip table if enabled)
SELECT claim_nbr, payment_amt, payment_status
FROM claim_payment
WHERE payment_status = 'S'
ORDER BY crt_timestamp DESC
LIMIT 50;
```

### Recovery Steps

1. Confirm exit code and failure reason from job logs (`reasonCode`, stack trace).
2. Review skip records in the database; classify as data vs authority vs infra failure.
3. For transient failures (DB timeout, OOM): resolve infra issue, delete failed job, re-run:

   ```bash
   kubectl -n <NS> delete job <FAILED-JOB-NAME>
   kubectl -n <NS> create job claim-payment-manual-$(date +%s) \
     --from=cronjob/claim-payment-batch
   ```

4. For data failures: fix source rows, then re-run from last committed chunk.
5. Verify `claims_batch_items_processed_total` incremented and exit code is 0.

---

## 2. Outbox Backlog

### Diagnostic Queries

```promql
claims_outbox_lag_seconds{service="claims-svc"}
claims_outbox_pending_count{service="claims-svc"}
claims_outbox_publish_failures_total{service="claims-svc"}
```

```bash
kubectl -n <NS> get pods -l app=claims-svc
kubectl -n <NS> logs deploy/claims-svc --tail=200 | grep -i outbox
kafka-topics.sh --bootstrap-server <BROKER> --describe --topic claims-events
```

```sql
SELECT COUNT(*) AS pending, MIN(crt_timestamp) AS oldest
FROM outbox_events
WHERE status = 'PENDING';
```

### Recovery Steps

1. Verify Kafka broker health and `claims-events` topic exists with active consumers.
2. Check claims-svc relay logs for publish failures (auth, serialization, broker unreachable).
3. If relay is stalled, restart claims-svc pods after broker recovery:

   ```bash
   kubectl -n <NS> rollout restart deploy/claims-svc
   kubectl -n <NS> rollout status deploy/claims-svc
   ```

4. For poison messages after max retries, inspect DLQ topic `claims-events-dlq`.
5. Manual replay (non-prod or with change ticket):

   ```sql
   UPDATE outbox_events
   SET status = 'PENDING', attempt_count = 0, next_attempt_at = NOW()
   WHERE status = 'FAILED' AND event_type LIKE 'Claim%';
   ```

6. Confirm `claims_outbox_lag_seconds` returns below 30s.

---

## 3. API Latency Degradation

### Diagnostic Queries

```promql
histogram_quantile(0.95, sum by (le, uri) (rate(claims_api_request_duration_seconds_bucket{service="claims-svc"}[5m])))
hikaricp_connections_active{service="claims-svc"}
hikaricp_connections_pending{service="claims-svc"}
rate(claims_api_error_rate{service="claims-svc"}[5m])
```

```bash
kubectl -n <NS> top pods -l app=claims-svc
curl -s https://authz-svc.<ENV>.internal/actuator/health
curl -s https://claims-svc.<ENV>.internal/actuator/metrics/hikaricp.connections.active
```

**Logs:**

```text
service="claims-svc" AND level=WARN
MDC: correlationId, uri, duration_ms
```

### Recovery Steps

1. Identify hot endpoints from p95 breakdown by `uri` label on the Grafana dashboard.
2. Check HikariCP pool saturation — if `connections_pending > 0`, scale or tune pool size.
3. Verify downstream `authz-svc` health; latency spikes often correlate with authz timeouts.
4. Review recent deployments — correlate spike with release timestamp.
5. Scale horizontally if CPU/memory saturated:

   ```bash
   kubectl -n <NS> scale deploy/claims-svc --replicas=<N>
   ```

6. Confirm read p95 ≤ 500ms and write p95 ≤ 1000ms restored.

---

## 4. Rollback Procedure

Use when a deployment introduces sustained P1/P2 alerts or reconciliation failures.

```bash
# List recent revisions
argocd app history claims-svc-<ENV>

# Rollback to last known-good revision
argocd app rollback claims-svc-<ENV> <REVISION>

# Verify rollout
kubectl -n <NS> rollout status deploy/claims-svc
curl -s https://claims-svc.<ENV>.internal/actuator/health
```

For batch job regression, rollback the claims-batch CronJob image via Argo CD:

```bash
argocd app rollback claims-batch-<ENV> <REVISION>
```

---

## Verification Checklist

- [ ] Alert cleared in Alertmanager
- [ ] Grafana panels show healthy metrics (latency, outbox lag, error rate)
- [ ] `/actuator/health` returns UP
- [ ] Reconciliation gate PASS if parallel-run is active
- [ ] Post-incident ticket filed with correlation IDs and timeline
