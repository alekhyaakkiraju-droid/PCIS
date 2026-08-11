# Runbook Title (WO-144 template)

**Alert:** `<AlertName>`  
**Runbook key:** `<runbook-key>`  
**Owner:** Platform / SRE on-call

## Trigger and Alert Reference

Describe when this runbook applies: the Prometheus alert name, firing conditions, typical
labels (`service`, `job_name`, `namespace`), and links to the alert rule in
`observability/prometheus/alerting-rules.yaml`.

## Severity and First Responder

| Field | Value |
|-------|-------|
| Severity | `<critical\|warning\|info>` |
| First responder | Platform on-call (PagerDuty for critical; `#pcis-ops` Slack for warning/info) |
| Target MTTR | `<minutes>` |

## Prerequisites

List access, tooling, and context required before recovery (kubectl context, Grafana
dashboard, Argo CD login, change ticket, etc.). Use named placeholders only — no real
secrets or endpoints.

## Diagnostic Queries and Log Filters

Provide PromQL/Grafana queries, kubectl commands, and structured log filters (MDC keys:
`correlationId`, `service`, `job_name`) to confirm the alert and isolate root cause.

## Step-by-Step Recovery

Numbered recovery steps with commands. State rollback-safe actions first; call out
destructive steps explicitly.

## Verification, Escalation, and Post-Incident

Verification checklist, when to escalate (role names, not individuals), rollback path
reference, and post-incident actions (ticket, timeline, runbook updates).
