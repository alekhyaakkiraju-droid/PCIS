# PCIS Operations Runbook

## Batch Schedule
| Program  | Schedule   | Description                |
|----------|------------|----------------------------|
| CLM006B  | Daily 02:00| Claim Payment Processing   |
| CMM001B  | Weekly Mon | Commission Calculation     |
| POL006B  | Daily 01:00| Policy Renewal             |
| PRM005B  | Daily 03:00| Premium Delinquency Aging  |
| BIL003B  | Daily 04:00| Billing Installments       |
| AUD002B  | Weekly Sun | Audit Archive              |

## Scheduler Topology (G-08)
JOBSCHD1-3: Runtime-only, no source in repository.
JOBSCHD4-7: Defined in JOBSCHD_NEW_DRIVERS.clle.

## Monitoring
Check spool file output for DISPLAY statements.
Non-zero SQLCODE values indicate database errors.
WS-CNT-ERRORS > 0 requires manual investigation.
