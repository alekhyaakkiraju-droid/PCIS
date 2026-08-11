# Error Rate High (WO-144)

**Alert:** `ErrorRateHigh`  
**Runbook key:** `error-rate-high`  
**Owner:** Platform / SRE on-call

## Trigger and Alert Reference

Fires when `error_rate_5m > 0.01` (5xx responses exceed 1% of requests over 5 minutes), or
when the metric is absent during business hours (06:00–22:00). Rule:
`observability/prometheus/alerting-rules.yaml` (`pcis-api-slo-alerts` group).

## Severity and First Responder

| Field | Value |
|-------|-------|
| Severity | critical |
| First responder | Platform on-call (PagerDuty page) |
| Target MTTR | 15 minutes to restore error rate below 1% |

## Prerequisites

- kubectl access and ability to view pod logs
- Grafana Error Rate panel and logs datasource
- Recent deployment / change ticket history
- RFC 9457 reason-code registry reference (`pcis-error` module)

## Diagnostic Queries and Log Filters

**Prometheus:**

```promql
error_rate_5m{service="<SERVICE>"}
rate(http_server_requests_seconds_count{service="<SERVICE>", status=~"5.."}[5m])
rate(http_server_requests_seconds_count{service="<SERVICE>"}[5m])
```

**Logs:**

```text
service="<SERVICE>" AND level=ERROR
MDC: correlationId, reasonCode, status
```

Group errors by `reasonCode` and `uri` to find dominant failure mode.

## Step-by-Step Recovery

1. Determine scope: single service vs. platform-wide (gateway, DB, auth).
2. Check pod health and restart counts:

   ```bash
   kubectl -n <NS> get pods -l app=<SERVICE>
   kubectl -n <NS> logs deploy/<SERVICE> --tail=100 --since=10m
   ```

3. If errors started after deploy, initiate rollback — `ops/runbooks/rollback.md`.
4. For DB connectivity errors, verify Aurora/cluster health and connection pool limits.
5. For auth errors (401/403 spikes), check Keycloak/token validation latency separately.
6. Enable circuit breaker or traffic shed only via approved runbook — document in ticket.
7. If metric absent during business hours, restore observability scrape before assuming health.

## Verification, Escalation, and Post-Incident

**Verification:**

- [ ] `error_rate_5m` < 0.01 for 15 minutes
- [ ] No elevated 5xx in access logs
- [ ] Smoke test endpoints return 200
- [ ] PagerDuty alert resolved

**Escalation:** Escalate to service owner and Incident Commander if error rate > 5% or
financial mutation endpoints affected.

**Rollback:** Primary recovery for deploy-induced errors — target ≤ 15 minutes per
`ops/runbooks/rollback.md`.

**Post-incident:** Blameless postmortem with reason-code breakdown, timeline, and action items.
