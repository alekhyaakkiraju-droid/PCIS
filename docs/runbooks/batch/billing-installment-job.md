# Billing Installment Batch (`billingInstallmentJob`) — WO-213

**Legacy:** BIL003B | **Schedule:** ASSUMPTION `0 1 * * *`

## Exit codes

0 success, 1 item errors, 4 outbox failure, 5 config failure.

## Failure modes

- **Lead-day window skips:** candidates outside 15-day lead counted as skipped — verify `billing.leadDays` tunable.
- **Installment remainder drift:** run golden comparison `WO-179` fixtures.
- **Policy join missing:** ensure policy-svc data or sync-agent populated `POLICY_T`.

## Restart

Use Spring Batch metadata; do not manually delete `BILLING_SCHEDULE_T` rows without reconciliation sign-off.

## Escalation

Batch Operations → Billing domain lead.
