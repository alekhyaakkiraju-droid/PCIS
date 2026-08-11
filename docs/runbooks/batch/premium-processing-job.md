# Premium Processing Batch (`premiumProcessingJob`) — WO-213

**Legacy:** PRM005B | **Schedule:** ASSUMPTION with nightly driver JOBSCHD4

## Exit codes

0–5 per `BatchJobExecutionListener` contract.

## Failure modes

- **Delinquency aging miscalculation:** verify BigDecimal scale-2 rounding in calculator tests.
- **Audit gap (legacy behaviour):** modern job must fail (exit 4) if outbox write fails.

## Restart

Re-run from last checkpoint; confirm delinquency candidates idempotent on re-processing.

## Escalation

Platform on-call → Premium/rating squad.
