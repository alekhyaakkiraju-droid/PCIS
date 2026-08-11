# Secret Rotation Overdue (WO-144)

**Alert:** `SecretRotationOverdue`  
**Runbook key:** `secret-rotation-overdue`  
**Owner:** Platform / Security on-call

## Trigger and Alert Reference

Fires when `(time() - pcis_secret_last_rotation_timestamp_seconds) > 7776000` (90 days) for
1 hour. Info-level hygiene alert — secret material exceeds rotation policy. Rule:
`observability/prometheus/alerting-rules.yaml` (`pcis-infra-alerts` group).

## Severity and First Responder

| Field | Value |
|-------|-------|
| Severity | info |
| First responder | Platform on-call (`#pcis-ops` Slack annotation) |
| Target MTTR | 5 business days to rotate |

## Prerequisites

- Access to secret manager (Kubernetes Secrets, AWS Secrets Manager, or Vault — per env policy)
- Deployment pipeline access to roll pods after secret update
- Change ticket for production rotation
- List of consuming services for the affected secret

## Diagnostic Queries and Log Filters

**Prometheus:**

```promql
time() - pcis_secret_last_rotation_timestamp_seconds{secret_name="<SECRET>", service="<SERVICE>"}
pcis_secret_last_rotation_timestamp_seconds
```

**Kubernetes:**

```bash
kubectl -n <NS> get secret <SECRET-NAME> -o jsonpath='{.metadata.annotations}'
```

## Step-by-Step Recovery

1. Identify secret from alert labels (`secret_name`, `service`).
2. Confirm rotation policy (90-day org standard) and last rotation timestamp.
3. Generate new secret material in the approved secret manager — never commit values to git.
4. Update secret reference and roll dependent workloads:

   ```bash
   kubectl -n <NS> rollout restart deploy/<SERVICE>
   kubectl -n <NS> rollout status deploy/<SERVICE>
   ```

5. Update rotation timestamp metric/gauge after successful deploy:

   ```text
   pcis_secret_last_rotation_timestamp_seconds → current unix time
   ```

6. Revoke old credentials after grace period per security policy.
7. Verify dual-write period if database credentials rotated (update pool config before revoke).

## Verification, Escalation, and Post-Incident

**Verification:**

- [ ] `(time() - pcis_secret_last_rotation_timestamp_seconds) < 7776000`
- [ ] All dependent pods healthy post-restart
- [ ] No auth failures in service logs after rotation
- [ ] Alert cleared or annotated in Grafana

**Escalation:** Escalate to Security Engineering if secret is compromised or rotation breaks
production auth.

**Rollback:** Restore previous secret version from secret manager version history if new
credentials fail validation.

**Post-incident:** Record rotation date in secret inventory; automate rotation where possible.
