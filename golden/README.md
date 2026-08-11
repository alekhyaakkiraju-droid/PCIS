# PCIS Golden Output Capture Harness

Deterministic COBOL golden baselines for financial parity.

| Work order | Contribution |
|------------|--------------|
| **WO-148** | Capture scripts, scenario seeds (`golden/seeds/`), CSV/DISPLAY normalize+compare, contradictions, quarantine |
| **WO-176** | Canonical JSON format (`format-spec.md`), `normalization-rules.yaml`, `fixtures/`, `outputs/`, Java determinism utilities in `shared-libs/pcis-test-support` |

IBM i **execution** is separated from **normalization/comparison** so CI can test the latter without a live partition.

## Layout

```
golden/
  format-spec.md              canonical JSON golden schema + versioning (WO-176)
  normalization-rules.yaml    allow timestamps/surrogates; deny money+status (WO-176)
  fixtures/{program}/seed.sql known starting state per program (WO-176)
  outputs/{program}/*.golden.json  expected JSON goldens (WO-176)
  scripts/                    capture, seed, collect, normalize, compare, verify_determinism
  seeds/<PROGRAM>/            scenario SQL + mutated_tables.txt (WO-148)
  <PROGRAM>/<SCENARIO>/       normalized CSV/DISPLAY trees after IBM i capture (WO-148)
  test-fixtures/              raw → expected normalized pairs for unit tests
  contradictions.yaml
  quarantine/                 non-deterministic triples land here (do not commit goldens)
```

## JSON golden format (WO-176)

See [`format-spec.md`](format-spec.md). Artifacts live at:

```
golden/outputs/{aud002b|bil003b|clm006b|cmm001b|pol006b|prm005b}/{scenario}.golden.json
```

Regenerate expected JSON from mock post-run semantics:

```bash
python3 golden/scripts/generate_expected_goldens.py
```

Java capture API (`com.pcis.golden` in `pcis-test-support`):

- `GoldenCaptureContext` — pinned `Clock`, sequence ordinals
- `OrderByEnforcer` — reject unordered capture SQL
- `NormalizationConfigValidator` — reject money/status on allow-list
- `GoldenOutputCapture` — query → canonical JSON
- `GoldenReproducibilityValidator` — 3× byte-identity + quarantine JSON

## One command per program/scenario (IBM i / WO-148)

```bash
export PCIS_IBMI_JDBC_URL='jdbc:as400://host/PCISLIB'
export PCIS_IBMI_USER=...
export PCIS_IBMI_PASSWORD=...
export PCIS_REFERENCE_DATE=2024-06-15
export PCIS_SQL_RUNNER=/path/to/site-jdbc-runner

./golden/scripts/capture.sh CLM006B scenario-01
./golden/scripts/verify_determinism.sh CLM006B scenario-01
```

Artifacts land at `golden/CLM006B/scenario-01/{tables,display.txt,run_log.csv,metadata.yaml}`.

Dry-run (no IBM i):

```bash
PCIS_SEED_DRY_RUN=1 ./golden/scripts/seed_data.sh CLM006B scenario-01
PCIS_CAPTURE_DRY_RUN=1 PCIS_COLLECT_DRY_RUN=1 ./golden/scripts/capture.sh CLM006B scenario-01
```

## CI (no IBM i)

```bash
# Python harness (WO-148 + WO-176 rules/fixtures/outputs)
cd golden && python3 -m unittest discover -s tests -v

# Java determinism utilities (WO-176)
mvn -pl shared-libs/pcis-test-support test
# Optional PostgreSQL Testcontainers:
# PCIS_USE_TESTCONTAINERS=1 mvn -pl shared-libs/pcis-test-support test
```

## Determinism rules

1. Capture three times via `verify_determinism.sh` (CSV) or `GoldenReproducibilityValidator` (JSON).
2. Pairwise compare must PASS (byte-identical after normalize).
3. On FAIL:
   - CSV: `golden/quarantine/<PROGRAM>/<SCENARIO>/` with `REASON.txt`
   - JSON: `golden/quarantine/{program}/{scenario}-quarantine.json`
   — **do not commit** that scenario as a golden.

## Normalization policy

| Class | Policy |
|-------|--------|
| Timestamps / surrogate ids | May normalize → `NORMALIZED_TS` / `SEQ_NNN` |
| `NUMERIC(9,2)`, `NUMERIC(11,2)` | **Never** normalize |
| Status columns | **Never** normalize |

Validator rejects adding deny-list columns to the allow-list.

## Contradictions

See `contradictions.yaml`. Goldens document **actual** PROCEDURE DIVISION behavior (e.g. no SECCHK01 in CLM006B), not prologue claims.

## Troubleshooting

| Symptom | Action |
|---------|--------|
| Seed script exit 1 | Set `PCIS_IBMI_*` and `PCIS_SQL_RUNNER` |
| Spool empty | Check `PCIS_SPLF_NAME` / job completion |
| Non-deterministic quarantine | Inspect DISPLAY timestamps; extend allow-list timestamps only |
| Remainder penny mismatch | Confirm BIL003B scenario-02 seed; legacy puts remainder on **first** installment |
| JaCoCo / money rewritten | Ensure column is on deny-list in `normalization-rules.yaml` |
