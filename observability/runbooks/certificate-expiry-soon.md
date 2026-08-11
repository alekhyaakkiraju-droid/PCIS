# Certificate Expiry Soon (WO-144)

**Alert:** `CertificateExpirySoon`  
**Runbook key:** `certificate-expiry-soon`  
**Owner:** Platform / SRE on-call

## Trigger and Alert Reference

Fires when `ssl_certificate_expiry_seconds < 1209600` (14 days) for 1 hour. Rule:
`observability/prometheus/alerting-rules.yaml` (`pcis-infra-alerts` group). Applies to
TLS certificates scraped by Prometheus blackbox or ingress exporters.

## Severity and First Responder

| Field | Value |
|-------|-------|
| Severity | warning |
| First responder | Platform on-call (`#pcis-ops` Slack) |
| Target MTTR | Renew before 7 days remaining |

## Prerequisites

- Access to cert-manager or cloud certificate manager
- kubectl access to ingress/gateway namespace
- Change ticket for production cert rotation
- DNS / ACME challenge validation capability

## Diagnostic Queries and Log Filters

**Prometheus:**

```promql
ssl_certificate_expiry_seconds{service="<SERVICE>"}
ssl_certificate_expiry_seconds < 604800
```

**Kubernetes (cert-manager):**

```bash
kubectl -n <NS> get certificate,certificaterequest,order
kubectl -n <NS> describe certificate <CERT-NAME>
```

## Step-by-Step Recovery

1. Identify certificate from alert labels (`service`, `instance`, `secret_name`).
2. Confirm expiry date and issuing CA (Let's Encrypt, internal CA, or cloud ACM).
3. **cert-manager:** Trigger renewal or fix failed CertificateRequest:

   ```bash
   kubectl -n <NS> delete certificaterequest <STUCK-REQUEST>
   kubectl -n <NS> annotate certificate <CERT-NAME> cert-manager.io/issue-temporary-certificate="true" --overwrite
   ```

4. **ACM / cloud:** Request new cert in console or IaC pipeline; validate DNS records.
5. Verify ingress/gateway picks up renewed secret (may require pod restart).
6. Schedule production rotation in change window if manual upload required.

## Verification, Escalation, and Post-Incident

**Verification:**

- [ ] `ssl_certificate_expiry_seconds` > 1209600 (14 days)
- [ ] TLS handshake succeeds: `curl -vI https://<HOST>/`
- [ ] No certificate warnings in browser/API client tests
- [ ] Alert cleared

**Escalation:** Escalate to security platform team if expiry < 72 hours or ACME renewal blocked.

**Rollback:** Restore previous cert secret from sealed backup if new cert breaks clients —
document client trust store requirements.

**Post-incident:** Update cert inventory; ensure auto-renewal is enabled for the issuer.
