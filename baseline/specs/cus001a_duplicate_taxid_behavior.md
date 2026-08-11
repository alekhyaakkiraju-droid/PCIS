# CUS001A Duplicate Tax-ID Behavior Specification (WO-239)

**Source of truth:** `Property_Casualty_Insurance_System/CUS001A.cbl` (284 lines, shipped baseline)  
**Analysis date:** 2026-08-11  
**Story conflict resolved:** edge-case “legacy hard stop” vs gap-analysis “soft warning” vs US-007 permission-gated override

---

## Summary Finding

**Neither hard block nor soft warning exists in the shipped PROCEDURE DIVISION.**

There is **no** `4000-CHECK-DUPLICATE-CUSTOMER` paragraph, **no** `WS-DUPLICATE-FOUND-SW`, **no** `CUS0015` message path, and **no** `SELECT COUNT(*)` (or any other) duplicate tax-ID check before insert.

Customer create (`WHEN 'ADD'`) flows directly to `5000-ADD-CUSTOMER`, which inserts into `CUSTOMER_T` after `CUSVAL01` validation only. Duplicate tax IDs are **not prevented and not warned** by this program as shipped.

| Claim (artifact) | Verified against source |
|---|---|
| Edge-case “legacy hard stop” | **False** for shipped CUS001A.cbl |
| Gap-analysis “soft warning” | **False** for shipped CUS001A.cbl |
| Assumed paragraph `4000-CHECK-DUPLICATE-CUSTOMER` | **Absent** |
| Assumed column `CUST_SSN_TAXID` | **Absent** — code uses `TAX_ID` |

---

## Control Flow Trace (line-number evidence)

### Entry — `0000-MAIN` (L88–L109)

1. `PERFORM 1000-INITIALIZE` (L89)
2. `PERFORM 2000-CHECK-AUTHORIZATION` (L90) — `SECCHK01` gate only
3. `IF WS-AUTH-FLAG = 'Y'` (L91)
   - `EVALUATE WS-SCREEN-ACTION` (L92)
     - `'INQR'` → `3000-INQUIRE-CUSTOMER` (L94)
     - `'UPDT'` → `4000-UPDATE-CUSTOMER` (L96) — **update path, not duplicate check**
     - `'ADD'` → `5000-ADD-CUSTOMER` (L98) — **create path**
     - `OTHER` → display invalid action, `WS-RETURN-CODE = 4` (L99–L103)
4. Else authorization denied, `WS-RETURN-CODE = 8` (L104–L107)
5. `STOP RUN` (L109)

**Note:** Paragraph `4000-*` in this source is `4000-UPDATE-CUSTOMER` (update), **not** a duplicate-check paragraph. The story’s assumed name does not exist.

### Create path — `5000-ADD-CUSTOMER` (L241–L282)

| Step | Lines | Behavior |
|------|-------|----------|
| Validate via `CUSVAL01` | L242–L247 | On failure: set `WS-ERROR-MSG`, `WS-VALID-FLAG = 'N'` |
| Guard insert | L248 | `IF WS-VALID-FLAG = 'Y'` only — **no duplicate flag** |
| `INSERT INTO CUSTOMER_T` | L249–L266 | Columns include `TAX_ID` ← `:HV-TAX-ID` |
| Success | L268–L275 | `COMMIT`, audit `AUDLOG01` with action `INSERT` |
| Failure | L276–L280 | Display SQLCODE, `WS-RETURN-CODE = 8` |

No branch between validation and insert inspects tax-ID uniqueness. No EXFMT/indicator loop exists in this program (no 5250 I/O in PROCEDURE DIVISION — screen action is assumed pre-loaded in `WS-SCREEN-ACTION`).

### Related paths (not duplicate detection)

- `3000-INQUIRE-CUSTOMER` (L136–L163): `SELECT` by `CUSTOMER_ID` including `C.TAX_ID` (L142)
- `4000-UPDATE-CUSTOMER` (L166–L199): updates name/status/type; **does not update `TAX_ID`**
- `2000-CHECK-AUTHORIZATION` (L121–L133): `SECCHK01` only

---

## Column Name Drift

| Location | Identifier |
|----------|------------|
| CUS001A host var | `HV-TAX-ID` PIC X(15) (L73) |
| CUS001A SQL | `TAX_ID` (SELECT L142; INSERT L255) |
| `PCIS_Database_Design.md` CUSTOMER_T | `TAX_ID` |
| Story-assumed `CUST_SSN_TAXID` | **Not present** in shipped CUS001A.cbl |

**Reconciliation recommendation:** Keep PostgreSQL canonical column `tax_id` (matches shipped COBOL + current DDL). Treat any `CUST_SSN_TAXID` references in older gap notes as **stale documentation** relative to this baseline, unless a future recovery of fuller IBM i source reintroduces that name — in which case map via the data dictionary (WO-128).

---

## Delta Analysis vs US-007 (permission-gated override + audit)

US-007 target: detect duplicate tax ID; allow override only with explicit permission; write audit trail.

| Behavior | Legacy (shipped CUS001A) | US-007 target | Disposition |
|----------|--------------------------|---------------|-------------|
| Duplicate tax-ID detection | None | Required | **NEW** |
| Hard block without override | N/A (no check) | Default block | **NEW** |
| Soft warning / operator bypass | None | Not the primary model | — |
| Permission-gated override | None | Required for override path | **NEW** |
| Audit of override decision | Insert audited via AUDLOG01 only on success; no override event | Explicit override audit | **IMPROVE** / **NEW** |
| Column name for tax ID | `TAX_ID` | Align with data dictionary `tax_id` | **PARITY** |
| Authorization before mutate | `SECCHK01` (L121–L133) | Retain / modernize as authz-svc | **PARITY** → **IMPROVE** |

**Implication for modernization:** Customer create must **add** duplicate detection (not preserve a hard/soft legacy gate). Treat US-007 as greenfield control relative to this baseline, not a softening of an existing hard stop.

---

## Edge Cases Documented from Source

1. **SQLCODE ≠ 0 on INSERT** (L276–L280): error displayed; no retry/duplicate-specific handling — unique-index collision (if DB has one) would surface only as generic SQLCODE.
2. **Blank tax ID:** Insert still proceeds with `:HV-TAX-ID` as provided; no blank-tax-ID guard in PROCEDURE DIVISION.
3. **Inactive customers:** N/A — no duplicate query exists to filter by status.
4. **Display-file bypass:** N/A — no EXFMT/READ loop; program is action-driven via `WS-SCREEN-ACTION`.

---

## Recommendations

1. Update edge-case register A-P1-1 to **“no duplicate check in shipped CUS001A”** (this document).
2. Implement US-007 as **NEW** control with authz + audit; do not claim PARITY with a hard/soft legacy gate.
3. Confirm production Db2 unique constraints on tax ID separately (outside this source member); absence of COBOL check does not prove absence of DB constraint.
4. If a fuller historical CUS001A is later recovered from IBM i, re-run this analysis and amend this spec under a new version header.
