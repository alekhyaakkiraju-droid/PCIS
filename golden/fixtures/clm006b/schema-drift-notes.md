# CLM006B golden schema drift notes (WO-180)

Reference date: `2024-06-15`. These fixtures document **legacy baseline behavior** for
reserve drawdown parity, not aspirational prologue claims.

## Authority check gap (G-02)

- Prologue / design docs claim `SECCHK01` authority verification before payment.
- **Actual COBOL:** zero `CALL 'SECCHK01'` in PROCEDURE DIVISION.
- Golden oracles therefore show payments proceeding when `RESERVE_AMT` exceeds
  `AUTHORITY_LIMIT` (`authority-limit-exceeded`, `multiple-reserves` scenarios).
- See `golden/contradictions.yaml` id `CLM006B-SECCHK01`.

## Reinsurance cession threshold

- Legacy tunable: `WS-REI-CESSION-THRESHOLD = 100000.00` (COMP-3).
- Comparison is **strictly greater than** the threshold:
  - `$100000.00` → **no** `RECOVERY_T` row (`cession-at-threshold`).
  - `$100000.01` → **yes** `RECOVERY_T` referral row (`cession-above-threshold`).
  - `$99999.99` → **no** `RECOVERY_T` row (`cession-below-threshold`).
- RECOVERY_T insert failures above threshold are silent in legacy COBOL (no SQLCODE check).

## Column / table drift vs design docs

| Topic | Design doc | Golden fixture runtime set |
|-------|------------|----------------------------|
| Reserve key | `APPROVED_AMT` in some docs | `RESERVE_AMT` + `RESERVE_STATUS` per `schema_pg_subset.sql` |
| Reserve identity | `RESERVE_HIST_ID` in Flyway V1 | `RESERVE_ID` business key in golden subset |
| Payment columns | Full `CLAIM_PAYMENT_T` DDL | `PAYMENT_ID`, `CLAIM_ID`, `PAYMENT_AMT`, `CREATED_AT` |
| Recovery columns | Full `RECOVERY_T` DDL | `RECOVERY_ID`, `CLAIM_ID`, `RECOVERY_AMT`, `RECOVERY_STATUS`, `RECOVERY_DATE` |

## RPT_RUN_LOG_T counters

WO-180 goldens include `REC_SELECTED`, `REC_UPDATED`, `REC_ERRORS`, and `ROWS_PROCESSED`
for counter assertions in `ClaimsPaymentGoldenTest`. Legacy CLM006B maps:

- `REC_SELECTED` ← reserves/claims read
- `REC_UPDATED` ← payments issued
- `REC_ERRORS` ← error count (0 on happy path)
- `ROWS_PROCESSED` ← same as payments issued for parity harness
