# Audit Outbox Backlog (WO-144)

**Alert:** `AuditOutboxBacklog`  
**Runbook key:** `audit-outbox-backlog`  
**Owner:** Platform / SRE on-call

## Trigger and Alert Reference

Fires when `pcis:audit_outbox_pending_count > 100` for 5 minutes. Warning-level depth
indicator — relay is falling behind but lag may still be under the critical 30s threshold.
Rule: `observability/prometheus/alerting-rules.yaml` (`pcis-audit-alerts` group). Inhibited
when `AuditOutboxLagHigh` fires for the same `service`.

## Severity and First Responder

| Field | Value |
|-------|-------|
| Severity | warning |
| First responder | Platform on-call (`#pcis-ops` Slack) |
| Target MTTR | 30 minutes to drain backlog below threshold |

## Prerequisites

- kubectl access to affected service namespace
- Grafana Audit Outbox panels
- Read-only DB access to `outbox_events`

## Diagnostic Queries and Log Filters

**Prometheus:**

```promql
pcis:audit_outbox_pending_count{service="<SERVICE>"}
pcis:audit_outbox_lag_seconds{service="<SERVICE>"}
rate(pcis_audit_outbox_relayed_total{service="<SERVICE>"}[5m])
```

**Database:**

```sql
SELECT DATE_TRUNC('minute', crt_timestamp) AS minute, COUNT(*) AS pending
FROM outbox_events
WHERE published_at IS NULL
GROUP BY 1 ORDER BY 1 DESC LIMIT 20;
```

**Logs:**

```text
service="<SERVICE>" AND message=~"outbox|relay" AND level>=INFO
```

## Step-by-Step Recovery

1. Check whether `AuditOutboxLagHigh` is also active — if yes, prioritize
   `audit-outbox-lag-high.md`.
2. Identify burst source: batch job completion, bulk import, or relay slowdown.
3. If relay throughput dropped, restart relay pods and verify audit-svc consumer capacity.
4. Temporarily scale service replicas if CPU-bound:

   ```bash
   kubectl -n <NS> scale deploy/<SERVICE> --replicas=<N>
   ```

5. Monitor pending count trend — backlog should decrease monotonically after fix.
6. Do **not** delete pending outbox rows manually; use supported relay recovery paths only.

## Verification, Escalation, and Post-Incident

**Verification:**

- [ ] `pcis:audit_outbox_pending_count` < 100
- [ ] Pending count stable or decreasing over 15 minutes
- [ ] No dead-letter or poison-message errors in relay logs

**Escalation:** Escalate to critical path if pending count exceeds 500 or lag crosses 30s.

**Rollback:** Revert deployment if backlog started after a release — `ops/runbooks/rollback.md`.

**Post-incident:** Document burst cause; consider rate limiting on bulk audit producers.
