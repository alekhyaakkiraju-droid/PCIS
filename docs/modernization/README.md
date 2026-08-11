# PCIS Modernization Artifacts (WO-006 / WOREF-006)

Authoritative modernization specifications committed into version control so the repository can self-describe its target state.

**Source:** Imported as-is from the programme artifact bundle (`Downloads/PCIS/`). Stale claims are documented here rather than edited into the artifacts (audit integrity).

**Sensitive data review:** No credential literals, API keys, or employee-identifying secrets found. Mentions of “secrets management” are architectural requirements only. No redactions required.

## Artifact index

| File | Purpose | Lines | Relationship to codebase |
|------|---------|------:|--------------------------|
| `Intent_Profile.md` | Programme intent, constraints, and non-negotiable modernization decisions | 157 | Guides epic/WO prioritization; not executable |
| `Architecture_Options.md` | Selected architecture (services, data, mesh, IaC, observability) | 1509 | Target design for infra (`infra/`), services, and frontend |
| `PRD-Spec.md` | Product requirements and success criteria | 527 | Binding product scope for domain migrations |
| `Requirements_Traceability.md` | REQ ↔ PRD/Arch/WO mapping | 16 | Compact RTM from artifact bundle; Forge RTM regenerates richer WO linkage |
| `User_Stories.md` | Full user-story corpus with acceptance criteria | 2494 | Source narrative for Forge work orders |
| `UI_Design.md` | UI/UX design for the React SPA replacing 5250 DDS | 626 | Informs `frontend/` implementation |
| `Testing.md` | Prose test cases (404 cases) for verification | 4465 | Not yet executable; consumed by golden-output / QA WOs |

## Gap analysis cross-references

Already in the repository under `Property_Casualty_Insurance_System/`:

- `PCIS_Gap_Analysis.md`
- `PCIS_Artifact_Gap_Analysis.md`
- `PCIS_Modernization_Edge_Cases.md`

### Stale claims corrected by later work orders

| Gap | Claim in artifacts | Corrected by |
|-----|-------------------|--------------|
| **G-01** | Stale program/inventory claims (e.g. design-only vs shipped) | **WOREF-001** `manifest/pcis-manifest.yaml` |
| **X-03 / G-02** | Prologue authority / CALL claims that contradict PROCEDURE DIVISION (CLM006B SECCHK01, PRM005B PRMCLC01, AUD002B AUDLOG01) | **WOREF-002** `baseline/cobol-baseline.yaml` |
| **X-04** | Testing.md may reference WO IDs not present in the compact RTM | Tracked in gap analysis; do not edit Testing.md |

Local path references such as `Downloads/PCIS/` inside artifacts are historical source paths and are not valid inside the clone — use `docs/modernization/` instead.

## Large files

`Testing.md` (~4.4k lines) and `User_Stories.md` (~2.5k lines) may render slowly on GitHub; use raw view if the UI truncates.

## Validation

```bash
./docs/modernization/validate-artifacts.sh
```

Exit code 0 means all seven artifacts and this README meet minimum completeness thresholds.
