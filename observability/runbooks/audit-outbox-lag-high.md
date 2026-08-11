# Audit Outbox Lag High (WO-144)

**Alert:** `AuditOutboxLagHigh`  
**Runbook key:** `audit-outbox-lag-high`  
**Owner:** Platform / SRE on-call

## Trigger and Alert Reference

Fires when `pcis:audit_outbox_lag_seconds > 30` for 2 minutes, or when the metric is absent
during business hours (06:00–22:00). Critical audit trail delay — mutations may be committed
before audit events are relayed. Rule: `observability/prometheus/alerting-rules.yaml`
(`pcis-audit-alerts` group).

## Severity and First Responder

| Field | Value |
|-------|-------|
| Severity | critical |
| First responder | Platform on-call (PagerDuty page) |
| Target MTTR | 15 minutes to restore relay throughput |

## Prerequisites

- kubectl access to the service namespace running the outbox relay
- PostgreSQL read access to `outbox_events` (via read-only role)
- Grafana Audit / Outbox dashboard
- audit-svc health endpoint access

## Diagnostic Queries and Log Filters

**Prometheus:**

```promql
pcis:audit_outbox_lag_seconds{service="<SERVICE>"}
pcis:audit_outbox_pending_count{service="<SERVICE>"}
rate(pcis_audit_outbox_relayed_total{service="<SERVICE>"}[5m])
```

**Database (read-only):**

```sql
SELECT COUNT(*), MIN(crt_timestamp), MAX(crt_timestamp)
FROM outbox_events
WHERE published_at IS NULL;
```

**Logs:**

```text
service="<SERVICE>" AND logger=~"OutboxMetrics|OutboxRelay" AND level>=WARN
MDC: correlationId, service
```

## Step-by-Step Recovery

1. Check audit-svc and downstream consumer health (`/actuator/health`).
2. Inspect outbox relay pod logs for connection errors, rate limiting, or serialization failures.
3. Verify HikariCP pool is not exhausted: `pcis:hikaricp_connections_active`.
4. If audit-svc is degraded, scale replicas or restart deployment:

   ```bash
   kubectl -n <NS> rollout restart deploy/<SERVICE>
   kubectl -n <NS> rollout status deploy/<SERVICE>
   ```

5. If lag is due to bulk backlog (not relay stall), also follow `audit-outbox-backlog.md`.
6. For sustained relay failure with committed mutations, initiate unrecorded-mutation
   reconciliation per incident response procedures.

## Verification, Escalation, and Post-Incident

**Verification:**

- [ ] `pcis:audit_outbox_lag_seconds` < 30s for 10 consecutive minutes
- [ ] Oldest pending `outbox_events.CRT_TIMESTAMP` is within SLO
- [ ] Relay rate restored (`pcis_audit_outbox_relayed_total` increasing)
- [ ] No new audit-write failure reason codes in logs

**Escalation:** Page Compliance liaison if lag exceeded 5 minutes with active financial
mutations in production.

**Rollback:** Roll back service release if relay regression started after deploy —
`ops/runbooks/rollback.md`.

**Post-incident:** Record peak lag, pending count, and root cause. Tune relay poll interval
or batch size if needed.
