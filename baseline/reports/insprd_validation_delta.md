# INSPRD Validation Delta Report (WO-240)

Generated: `2026-08-11T04:19:55Z`

Goal: Validate the production library inventory implied by `PCIS_CRTOBJ.clle` against the repository manifest under `Property_Casualty_Insurance_System`, and produce an operator checklist for `DSPOBJD` against `INSPRD` / `INSCOM`.

## AC-1 — CRTOBJ manifest extraction

| Field | Value |
|---|---|
| Source | `/Users/alekhyaakkiraju/PCIS/Property_Casualty_Insurance_System/PCIS_CRTOBJ.clle` |
| Object entries | 42 |
| Submitted jobs | 3 |
| Default libraries | `{"SRCLIB": "PCIS", "PGMLIB": "PCISPGM", "DSPLIB": "PCISPGM"}` |

**Status: PASS** — `baseline/reports/crtobj_manifest.json` generated.

## AC-2 — Repository manifest extraction

| Field | Value |
|---|---|
| Source dir | `/Users/alekhyaakkiraju/PCIS/Property_Casualty_Insurance_System` |
| Total files | 49 |
| Counts by type | `{"clle": 2, "cobol": 8, "display-file": 22, "markdown": 17}` |

**Status: PASS** — `baseline/reports/repo_manifest.json` generated.

## AC-3 — Cross-reference delta (CRTOBJ ↔ repo)

| Metric | Count |
|---|---|
| Matched object names | 30 |
| In CRTOBJ, missing repo source | 4 |
| In repo, missing CRTOBJ entry | 0 |

### In CRTOBJ but no repository source

| Object | Categories | Libraries |
|---|---|---|
| `CLMPAYP1` | printer-file | PCISPGM |
| `POLPOLP1` | printer-file | PCISPGM |
| `RPT001P1` | printer-file | PCISPGM |
| `RPT006P1` | printer-file | PCISPGM |

### In repository but not in CRTOBJ

_None (or only covered by JOBSCHD_NEW_DRIVERS)._

**Status: PASS** — `baseline/reports/delta.json` generated.

## AC-4 — Known missing service programs

The following shared callees are documented gaps (G-02/G-03/PRM) and must be reconciled against production `INSCOM` / `*LIBL` even though they are absent from the repository:

| Program | In repo | In CRTOBJ | Status |
|---|---|---|---|
| `AUDLOG01` | False | False | `missing-service-program` |
| `SECCHK01` | False | False | `missing-service-program` |
| `PRMCLC01` | False | False | `missing-service-program` |

**Status: PASS** — AUDLOG01, SECCHK01, PRMCLC01 recorded as missing service programs.

## AC-5 — JOBSCHD reconciliation

JOBSCHD1-3 are runtime-only (SBMJOB in PCIS_CRTOBJ.clle, no source). JOBSCHD4-7 are defined in JOBSCHD_NEW_DRIVERS.clle.

| Item | Value |
|---|---|
| CRTOBJ SBMJOB programs | `JOBSCHD1, JOBSCHD2, JOBSCHD3` |
| Runtime-only (no source) | `JOBSCHD1, JOBSCHD2, JOBSCHD3` |
| NEW_DRIVERS defined | `JOBSCHD4, JOBSCHD5, JOBSCHD6, JOBSCHD7` |
| Expected JOBSCHD4-7 complete | `True` |
| Drivers source | `/Users/alekhyaakkiraju/PCIS/Property_Casualty_Insurance_System/JOBSCHD_NEW_DRIVERS.clle` |

**Status: PASS** — JOBSCHD1-3 runtime-only vs JOBSCHD4-7 drivers reconciled.

## AC-6 — INSTEST typo detection

`INSTEST` is a known misspelling of `INSTST` (test program library in `build/build.yaml` environments.tst.pgm_lib). The CRTOBJ parser flags any occurrence in comments or CRT*/library parameters.

No `INSTEST` token found in current `PCIS_CRTOBJ.clle`. Detector is covered by unit fixtures under `baseline/test-fixtures/crtobj_parser/`.

**Status: PASS** — detector implemented; no typo in production CRTOBJ.

## AC-7 — DSPOBJD production checklist (INSPRD / INSCOM)

Run these commands on the IBM i partition (or capture via batch) to validate that production libraries match the repository-derived inventory. Mark each row after execution.

| Done | Object | Library | Type | Repo source? | Command |
|---|---|---|---|---|---|
| [ ] | `AUD002B` | `INSPRD` | `*PGM` | yes | `DSPOBJD OBJ(INSPRD/AUD002B) OBJTYPE(*PGM)` |
| [ ] | `BIL003B` | `INSPRD` | `*PGM` | yes | `DSPOBJD OBJ(INSPRD/BIL003B) OBJTYPE(*PGM)` |
| [ ] | `BILSTSD1` | `INSPRD` | `*FILE` | yes | `DSPOBJD OBJ(INSPRD/BILSTSD1) OBJTYPE(*FILE)` |
| [ ] | `CLM006B` | `INSPRD` | `*PGM` | yes | `DSPOBJD OBJ(INSPRD/CLM006B) OBJTYPE(*PGM)` |
| [ ] | `CLMADJD1` | `INSPRD` | `*FILE` | yes | `DSPOBJD OBJ(INSPRD/CLMADJD1) OBJTYPE(*FILE)` |
| [ ] | `CLMAPRD1` | `INSPRD` | `*FILE` | yes | `DSPOBJD OBJ(INSPRD/CLMAPRD1) OBJTYPE(*FILE)` |
| [ ] | `CLMCLSD1` | `INSPRD` | `*FILE` | yes | `DSPOBJD OBJ(INSPRD/CLMCLSD1) OBJTYPE(*FILE)` |
| [ ] | `CLMFNLD1` | `INSPRD` | `*FILE` | yes | `DSPOBJD OBJ(INSPRD/CLMFNLD1) OBJTYPE(*FILE)` |
| [ ] | `CLMINQD1` | `INSPRD` | `*FILE` | yes | `DSPOBJD OBJ(INSPRD/CLMINQD1) OBJTYPE(*FILE)` |
| [ ] | `CLMPAYD1` | `INSPRD` | `*FILE` | yes | `DSPOBJD OBJ(INSPRD/CLMPAYD1) OBJTYPE(*FILE)` |
| [ ] | `CLMPAYP1` | `INSPRD` | `*FILE` | NO | `DSPOBJD OBJ(INSPRD/CLMPAYP1) OBJTYPE(*FILE)` |
| [ ] | `CLMRSVD1` | `INSPRD` | `*FILE` | yes | `DSPOBJD OBJ(INSPRD/CLMRSVD1) OBJTYPE(*FILE)` |
| [ ] | `CMM001B` | `INSPRD` | `*PGM` | yes | `DSPOBJD OBJ(INSPRD/CMM001B) OBJTYPE(*PGM)` |
| [ ] | `COMRPTD1` | `INSPRD` | `*FILE` | yes | `DSPOBJD OBJ(INSPRD/COMRPTD1) OBJTYPE(*FILE)` |
| [ ] | `CUS001A` | `INSPRD` | `*PGM` | yes | `DSPOBJD OBJ(INSPRD/CUS001A) OBJTYPE(*PGM)` |
| [ ] | `CUSCNTD1` | `INSPRD` | `*FILE` | yes | `DSPOBJD OBJ(INSPRD/CUSCNTD1) OBJTYPE(*FILE)` |
| [ ] | `CUSDELD1` | `INSPRD` | `*FILE` | yes | `DSPOBJD OBJ(INSPRD/CUSDELD1) OBJTYPE(*FILE)` |
| [ ] | `CUSINQD1` | `INSPRD` | `*FILE` | yes | `DSPOBJD OBJ(INSPRD/CUSINQD1) OBJTYPE(*FILE)` |
| [ ] | `CUSLSTD1` | `INSPRD` | `*FILE` | yes | `DSPOBJD OBJ(INSPRD/CUSLSTD1) OBJTYPE(*FILE)` |
| [ ] | `CUSMNTD1` | `INSPRD` | `*FILE` | yes | `DSPOBJD OBJ(INSPRD/CUSMNTD1) OBJTYPE(*FILE)` |
| [ ] | `MENUMD1` | `INSPRD` | `*FILE` | yes | `DSPOBJD OBJ(INSPRD/MENUMD1) OBJTYPE(*FILE)` |
| [ ] | `POL001A` | `INSPRD` | `*PGM` | yes | `DSPOBJD OBJ(INSPRD/POL001A) OBJTYPE(*PGM)` |
| [ ] | `POL006B` | `INSPRD` | `*PGM` | yes | `DSPOBJD OBJ(INSPRD/POL006B) OBJTYPE(*PGM)` |
| [ ] | `POLENDD1` | `INSPRD` | `*FILE` | yes | `DSPOBJD OBJ(INSPRD/POLENDD1) OBJTYPE(*FILE)` |
| [ ] | `POLINQD1` | `INSPRD` | `*FILE` | yes | `DSPOBJD OBJ(INSPRD/POLINQD1) OBJTYPE(*FILE)` |
| [ ] | `POLLSTD1` | `INSPRD` | `*FILE` | yes | `DSPOBJD OBJ(INSPRD/POLLSTD1) OBJTYPE(*FILE)` |
| [ ] | `POLMNTD1` | `INSPRD` | `*FILE` | yes | `DSPOBJD OBJ(INSPRD/POLMNTD1) OBJTYPE(*FILE)` |
| [ ] | `POLPOLP1` | `INSPRD` | `*FILE` | NO | `DSPOBJD OBJ(INSPRD/POLPOLP1) OBJTYPE(*FILE)` |
| [ ] | `POLREND1` | `INSPRD` | `*FILE` | yes | `DSPOBJD OBJ(INSPRD/POLREND1) OBJTYPE(*FILE)` |
| [ ] | `PRM005B` | `INSPRD` | `*PGM` | yes | `DSPOBJD OBJ(INSPRD/PRM005B) OBJTYPE(*PGM)` |
| [ ] | `PRMINQD1` | `INSPRD` | `*FILE` | yes | `DSPOBJD OBJ(INSPRD/PRMINQD1) OBJTYPE(*FILE)` |
| [ ] | `RPT001P1` | `INSPRD` | `*FILE` | NO | `DSPOBJD OBJ(INSPRD/RPT001P1) OBJTYPE(*FILE)` |
| [ ] | `RPT006P1` | `INSPRD` | `*FILE` | NO | `DSPOBJD OBJ(INSPRD/RPT006P1) OBJTYPE(*FILE)` |
| [ ] | `RPTMNUD1` | `INSPRD` | `*FILE` | yes | `DSPOBJD OBJ(INSPRD/RPTMNUD1) OBJTYPE(*FILE)` |
| [ ] | `AUDLOG01` | `INSCOM` | `*PGM` | NO | `DSPOBJD OBJ(INSCOM/AUDLOG01) OBJTYPE(*PGM)  /* shared service program — verify presence in INSCOM or *LIBL */` |
| [ ] | `SECCHK01` | `INSCOM` | `*PGM` | NO | `DSPOBJD OBJ(INSCOM/SECCHK01) OBJTYPE(*PGM)  /* shared service program — verify presence in INSCOM or *LIBL */` |
| [ ] | `PRMCLC01` | `INSCOM` | `*PGM` | NO | `DSPOBJD OBJ(INSCOM/PRMCLC01) OBJTYPE(*PGM)  /* shared service program — verify presence in INSCOM or *LIBL */` |

### Operator notes

1. Use `DSPOBJD OBJ(INSPRD/*ALL) OBJTYPE(*ALL) OUTPUT(*OUTFILE)` for a full dump.
2. Compare outfile object names against `baseline/reports/crtobj_manifest.json`.
3. Confirm `INSPRDDTA` holds data objects only — do not expect *PGM there.
4. Shared callees AUDLOG01 / SECCHK01 / PRMCLC01 typically resolve from `INSCOM`.
5. Printer files POLPOLP1 / CLMPAYP1 / RPT001P1 / RPT006P1 are CRTPRTF targets with missing DDS (G-08).

**Status: PASS** — checklist emitted for operator execution.

## Artifact index

| Artifact | Path |
|---|---|
| CRTOBJ manifest | `baseline/reports/crtobj_manifest.json` |
| Repo manifest | `baseline/reports/repo_manifest.json` |
| Delta JSON | `baseline/reports/delta.json` |
| This report | `baseline/reports/insprd_validation_delta.md` |

