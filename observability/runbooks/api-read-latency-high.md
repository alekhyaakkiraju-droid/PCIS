# API Read Latency High (WO-144)

**Alert:** `ApiReadLatencyHigh`  
**Runbook key:** `api-read-latency-high`  
**Owner:** Platform / SRE on-call

## Trigger and Alert Reference

Fires when `pcis:api_request_duration:p95{method=~"GET|HEAD"} > 0.5` (500ms) for 5 minutes,
or when the metric is absent during business hours (06:00–22:00). Rule:
`observability/prometheus/alerting-rules.yaml` (`pcis-api-slo-alerts` group).

## Severity and First Responder

| Field | Value |
|-------|-------|
| Severity | warning |
| First responder | Platform on-call (`#pcis-ops` Slack) |
| Target MTTR | 30 minutes to restore p95 ≤ 500ms |

## Prerequisites

- Grafana PCIS API Latency dashboard
- kubectl access to affected service deployment
- Distributed trace access (OpenTelemetry/Jaeger)
- Baseline report p95 reference for the endpoint group

## Diagnostic Queries and Log Filters

**Prometheus:**

```promql
pcis:api_request_duration:p95{service="<SERVICE>", method=~"GET|HEAD"}
pcis:api_request_duration:p99{service="<SERVICE>", method=~"GET|HEAD"}
pcis:hikaricp_connections_active{service="<SERVICE>"}
rate(http_server_requests_seconds_count{service="<SERVICE>", status=~"5.."}[5m])
```

**Traces:** Filter by service and slowest span (>500ms).

**Logs:**

```text
service="<SERVICE>" AND uri="<PATH>" AND level=WARN
MDC: correlationId, uri, duration_ms
```

## Step-by-Step Recovery

1. Identify hot endpoints from p95 breakdown by `uri` label.
2. Check for correlated `ErrorRateHigh` — address errors first if firing.
3. Inspect DB connection pool saturation and slow queries.
4. Review recent deployments — correlate latency spike with release time.
5. Scale horizontally if CPU/memory saturated:

   ```bash
   kubectl -n <NS> scale deploy/<SERVICE> --replicas=<N>
   ```

6. Enable or increase read-cache TTL only after confirming cache invalidation correctness.
7. If absent metric during business hours, verify metrics scrape target and
   `pcis-observability-starter` health.

## Verification, Escalation, and Post-Incident

**Verification:**

- [ ] p95 ≤ 500ms for GET/HEAD for 15 minutes
- [ ] No regression vs. committed baseline report
- [ ] Error rate remains below 1%
- [ ] Alert clears in Alertmanager

**Escalation:** Escalate to service owner if p95 > 1s for 15 minutes or customer-facing
SLA breach is reported.

**Rollback:** Roll back latest deployment if latency regression post-release —
`ops/runbooks/rollback.md`.

**Post-incident:** File performance ticket with trace IDs and slow query evidence.
