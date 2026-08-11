# Commission Calculation Batch (`commissionCalculationJob`) — WO-213

**Legacy:** CMM001B | **Schedule:** ASSUMPTION after billing installment job

## Exit codes

Standard PCIS batch 0–5 mapping.

## Failure modes

- **Agent with no commission plan:** legacy skips — verify skip counters in `pcis_batch_items_skipped_total`.
- **Ledger imbalance:** cent-level golden test WO-179 commission fixtures.

## Restart

Safe to restart; upsert ledger rows must be idempotent by agent/plan key.

## Escalation

Batch Operations → Finance operations.
