# AUDLOG01 Parameter Contracts

Machine-readable contracts inferred from the seven COBOL callers of the missing
`AUDLOG01` audit writer (gap **G-03**). Source for `AUDLOG01` is not in this
repository; the caller's `CALL … USING` blocks and WORKING-STORAGE PIC clauses
are the authoritative legacy interface.

## Purpose

- Document exact per-caller parameter shapes (PIC widths from source, not truncated content).
- Capture the known batch vs interactive width drift (**G-04**).
- Define a unified **v1** audit event schema that maps both shapes without truncation.
- Provide the binding input for the future **audit-svc** Spring Boot service
  (Shared Kernel — Audit Logging Service epic).

## Relationship to audit-svc

| Artifact | Role |
|---|---|
| `audlog01-v1-contract.yaml` | Binding contract for audit-svc v1 event schema and legacy→v1 mapping |
| `extract_audit_contract.py` | Regenerates the YAML from `Property_Casualty_Insurance_System/*.cbl` |
| `validate_audit_contract.py` | Gate: every manifest AUDLOG01 caller has a contract entry; AUD002B is a BLOCK non-caller |
| `test-fixtures/` | Batch-style and interactive-style CALL + WORKING-STORAGE samples |
| `tests/` | Unit tests for the extractor |

Downstream audit-svc work must size fields to `unified_v1_schema` and apply
`mapping_table` transformations (including `ADD→CREATE`, `UPD→UPDATE`, `DEL→DELETE`
and replacement of batch actor literals with authenticated workload principals).

## Regeneration

```bash
python3 contracts/extract_audit_contract.py
python3 contracts/validate_audit_contract.py
python3 -m unittest discover -s contracts/tests -v
```

## Callers vs non-callers

- **Callers (7):** BIL003B, CLM006B, CMM001B, POL006B, PRM005B, CUS001A, POL001A
- **Non-caller (BLOCK):** AUD002B — audit archive job does not call AUDLOG01
