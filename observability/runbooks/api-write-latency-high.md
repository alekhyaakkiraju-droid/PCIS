# API Write Latency High (WO-144)

**Alert:** `ApiWriteLatencyHigh`  
**Runbook key:** `api-write-latency-high`  
**Owner:** Platform / SRE on-call

## Trigger and Alert Reference

Fires when `pcis:api_request_duration:p95{method=~"POST|PUT|PATCH|DELETE"} > 1.0` (1000ms)
for 5 minutes. Rule: `observability/prometheus/alerting-rules.yaml` (`pcis-api-slo-alerts`
group).

## Severity and First Responder

| Field | Value |
|-------|-------|
| Severity | warning |
| First responder | Platform on-call (`#pcis-ops` Slack) |
| Target MTTR | 30 minutes to restore p95 ≤ 1000ms |

## Prerequisites

- Grafana API Latency dashboard (write methods)
- kubectl access to service namespace
- Trace and DB slow-query visibility
- Audit outbox metrics (writes often trigger audit events)

## Diagnostic Queries and Log Filters

**Prometheus:**

```promql
pcis:api_request_duration:p95{service="<SERVICE>", method=~"POST|PUT|PATCH|DELETE"}
pcis:audit_outbox_lag_seconds{service="<SERVICE>"}
pcis:hikaricp_connections_active{service="<SERVICE>"}
histogram_quantile(0.95, rate(http_server_requests_seconds_bucket{service="<SERVICE>", method=~"POST|PUT|PATCH|DELETE"}[5m]))
```

**Logs:**

```text
service="<SERVICE>" AND method=~"POST|PUT|PATCH|DELETE" AND level>=WARN
MDC: correlationId, reasonCode, actor
```

## Step-by-Step Recovery

1. Segment latency by `uri` — identify mutating endpoints over threshold.
2. Check audit outbox lag/backlog — synchronous audit paths can inflate write latency.
3. Verify DB lock contention or long transactions on mutation tables.
4. Inspect downstream HTTP client timeouts (RestTemplate/WebClient) in service logs.
5. Scale pods if thread pool exhaustion is observed.
6. For authorization latency, check auth decision p99 separately from handler time.
7. Avoid increasing write timeouts without fixing root cause — masked slowness hides SLO breaches.

## Verification, Escalation, and Post-Incident

**Verification:**

- [ ] Write p95 ≤ 1000ms for 15 minutes
- [ ] Audit outbox lag < 30s (no critical audit delay)
- [ ] Mutation success rate stable
- [ ] Alert resolved

**Escalation:** Escalate if write p95 > 2s with active user traffic or batch cutover window.

**Rollback:** Revert release if regression is deploy-correlated — `ops/runbooks/rollback.md`.

**Post-incident:** Capture slow traces; open backlog item for query or audit-path optimization.
