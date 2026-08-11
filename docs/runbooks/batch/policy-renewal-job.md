# Policy Renewal Batch (`policyRenewalJob`) — WO-213

**Legacy:** POL006B | **Schedule:** ASSUMPTION `0 4 * * *`

## Exit codes

0 success, 1 failures/skips, 2 archive verify mismatch (if chained), 4 audit, 5 config.

## Failure modes

- **Deductible carry-forward gap (P-P8):** renewal must copy `DEDUCTIBLE_T` — verify WO-193 schema and renewal processor.
- **UW referral path:** return code 01 policies held for review.

## Restart

Partition by policy range if supported; otherwise full job restart from Spring Batch execution id.

## Escalation

Policy domain lead + Batch Operations.
