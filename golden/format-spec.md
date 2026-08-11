# Golden Output Format Specification

**Version:** `1.0.0`  
**Work orders:** WO-176 (JSON golden format + determinism), WO-148 (CSV/DISPLAY harness)  
**Status:** Canonical

This document defines the machine-readable golden artifact format used as the
cent-level truth baseline for COBOL → Java financial parity. Downstream stories
(WO-177 comparison framework, WO-178/179 domain fixtures) consume artifacts that
conform to this specification.

## Versioning

| Field | Meaning |
|-------|---------|
| `formatVersion` | SemVer of this JSON schema. Breaking changes bump major. |
| `capturedAt` | ISO-8601 instant of capture (normalized away for byte-identity). |
| `referenceDate` | Pinned business date (`YYYY-MM-DD`) injected via `Clock`. |

Evolution rules:

1. **Additive (minor):** new optional fields allowed; readers ignore unknowns.
2. **Breaking (major):** rename/remove fields, change monetary encoding, or alter
   normalization semantics for non-deny columns.
3. Approved goldens are immutable; re-capture requires a documented reason in the
   PR and a `formatVersion` bump when schema changes.

## Artifact location

```
golden/outputs/{program}/{scenario}.golden.json
```

Program directory names are lowercase COBOL program ids:
`aud002b`, `bil003b`, `clm006b`, `cmm001b`, `pol006b`, `prm005b`.

The WO-148 harness continues to store normalized CSV/DISPLAY trees under
`golden/{PROGRAM}/{scenario}/`. JSON goldens under `outputs/` are the canonical
format for Java comparison; CSV trees remain the IBM i capture side-channel.

## Top-level JSON schema

```json
{
  "formatVersion": "1.0.0",
  "program": "BIL003B",
  "scenario": "scenario-01",
  "referenceDate": "2024-06-15",
  "completionStatus": "COMPLETED",
  "displayOutput": "BIL003B START\n...\nBIL003B END\n",
  "runLog": {
    "programName": "BIL003B",
    "status": "COMPLETED",
    "rowsProcessed": 12,
    "runStarted": "NORMALIZED_TS",
    "runEnded": "NORMALIZED_TS"
  },
  "tables": [
    {
      "tableName": "BILLING_INSTALLMENT_T",
      "businessKeys": ["POLICY_ID", "INSTALLMENT_NO"],
      "columns": [
        {"name": "INSTALLMENT_ID", "type": "SURROGATE"},
        {"name": "POLICY_ID", "type": "STRING"},
        {"name": "INSTALLMENT_NO", "type": "INTEGER"},
        {"name": "AMOUNT", "type": "NUMERIC(11,2)"}
      ],
      "rows": [
        {
          "INSTALLMENT_ID": "SEQ_001",
          "POLICY_ID": "POLBIL0001",
          "INSTALLMENT_NO": 1,
          "AMOUNT": "100.00"
        }
      ]
    }
  ]
}
```

### Field rules

| Field | Rules |
|-------|--------|
| `tables[].tableName` | Exact DDL table name (uppercase). |
| `tables[].businessKeys` | Columns used for stable row ordering (must appear in every row). |
| `tables[].columns[].type` | One of `STRING`, `INTEGER`, `NUMERIC(9,2)`, `NUMERIC(11,2)`, `DATE`, `TIMESTAMP`, `SURROGATE`, `STATUS`. |
| `tables[].rows` | Objects keyed by column name. Keys sorted lexicographically when serialized. |
| Monetary values | Always decimal **strings** with exact scale 2 (`"1000.01"`). Never IEEE float. |
| Empty result sets | `rows: []` with counters `0` — still a valid golden. |

Serialization for byte-identity:

1. UTF-8, LF newlines.
2. Object keys sorted lexicographically at every level.
3. Pretty-print with 2-space indent and trailing newline.
4. No floating-point numbers in JSON (use strings for `NUMERIC(*)`).

## Determinism controls

Applied by `com.pcis.golden` in `pcis-test-support` (and mirrored by
`golden/scripts/normalize.py` for CSV/DISPLAY):

1. **Clock pinning** — `CURRENT DATE` / `CURRENT TIMESTAMP` resolve to
   `referenceDate` (+ fixed time-of-day `00:00:00Z` unless overridden).
2. **Surrogate ordinals** — IDENTITY/SEQUENCE values rewritten to `SEQ_001`,
   `SEQ_002`, … in business-key sort order.
3. **ORDER BY enforcement** — every capture query must include an explicit
   `ORDER BY` on business keys; unordered cursors are rejected.

## Normalization allow / deny

See `golden/normalization-rules.yaml`.

| Class | Policy |
|-------|--------|
| Timestamps / surrogate ids | **May** be normalized |
| `NUMERIC(9,2)`, `NUMERIC(11,2)` | **Must never** be normalized |
| Status columns (`STATUS`, `*_STATUS`, `COMM_CALC_FLAG`, …) | **Must never** be normalized |

`NormalizationConfigValidator` rejects any attempt to add a deny-list column to
the allow-list.

## Reproducibility gate

Three consecutive captures of the same program/scenario must produce
byte-identical JSON after normalization. Failures are written to:

```
golden/quarantine/{program}/{scenario}-quarantine.json
```

and **must not** be committed as goldens.

## Legacy schema notes

- `CLAIM_RESERVE_T.APPROVED_AMT` appears in some design docs but not in the
  COBOL host-variable set used by CLM006B; goldens follow the runtime column
  set documented in `seeds/schema_pg_subset.sql`.
- Gap contradictions that affect expected rows (e.g. no SECCHK01 in CLM006B)
  are recorded in `golden/contradictions.yaml` (WO-148) and remain authoritative.
