# PCIS

Property & Casualty Insurance System — modernization repository.

## Repository Contents

The `Property_Casualty_Insurance_System/` directory contains 49 source members:

- 8 COBOL programs (ILE COBOL with embedded SQL)
- 22 DDS display files (5250 green-screen panels)
- 2 CL members (build and scheduler scripts)
- 17 design/modernization markdown documents

## Design-Only Programs

The following programs are fully specified in design documents but do not yet
have COBOL source members in the repository:

- CLM001A (uses DDS: CLMFNLD1) — Claim FNOL Interactive
- CLM002A (uses DDS: CLMRSVD1) — Claim Reserve Interactive
- CLM003A (uses DDS: CLMADJD1) — Claim Adjuster Assignment Interactive
- CLM004A (uses DDS: CLMINQD1) — Claim Inquiry Interactive
- CLM005A (uses DDS: CLMCLSD1) — Claim Closure Interactive
- CUS002A (uses DDS: CUSINQD1) — Customer Inquiry Interactive
- CUS003A (uses DDS: CUSLSTD1) — Customer List Interactive
- CUS004A (uses DDS: CUSDELD1) — Customer Delete Interactive
- CUS005A (uses DDS: CUSCNTD1) — Customer Contacts Interactive
- RPT001A (uses DDS: RPTMNUD1) — Report Menu Interactive
- RPT006A (uses DDS: COMRPTD1) — Commission Report Interactive

## Repository Manifest

`manifest/pcis-manifest.yaml` is the machine-readable inventory of all files.
Generate it with:

```bash
python3 manifest/generate_manifest.py
```

Validate it with:

```bash
python3 manifest/validate_manifest.py
```

Run unit tests with:

```bash
python3 manifest/tests/test_generator.py
```
