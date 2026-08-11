# PCIS Modernization Edge Cases

## Overview
100+ domain-specific edge cases categorized by disposition:
PARITY | IMPROVE | NEW | BLOCK

## Phase 0 Gate-Blocking Cases
- A-P0-1: Golden suite must be green on actual COBOL (not empty-file assumption)
- A-P0-2: Audit replay must not truncate X(100) interactive values
- A-P0-3: All 55 tables classified (BR-16)

## Customer Domain
- A-P1-1: **Verified (WO-239)** — Shipped `CUS001A.cbl` does **not** hard-stop or soft-warn on duplicate tax IDs. There is no `4000-CHECK-DUPLICATE-CUSTOMER` / `WS-DUPLICATE-FOUND-SW` / `CUS0015` path; `5000-ADD-CUSTOMER` inserts after `CUSVAL01` only (uses column `TAX_ID`, not `CUST_SSN_TAXID`). US-007 permission-gated override is **NEW** relative to this baseline. Spec: `baseline/specs/cus001a_duplicate_taxid_behavior.md`.

## Claims Domain
- ADR-003: CLM006B pays full outstanding (PARITY) vs partial payment in UI (IMPROVE)
- P-P2: RECOVERY_T insert failure handling must be decided before Phase 2

## Billing Domain
- I-02/P-06: POL001A creates billing without BILLING_PLAN_T row
- U-B1: Billing schedule ID type mismatch (string vs numeric)
