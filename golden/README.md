# PCIS Golden Output Capture Harness (WO-148)

Deterministic COBOL golden baselines for financial parity. IBM i **execution** is separated from **normalization/comparison** so CI can test the latter without a live partition.

## Layout

```
golden/
  scripts/           capture, seed, collect, normalize, compare, verify_determinism
  seeds/<PROGRAM>/   scenario SQL + mutated_tables.txt
  <PROGRAM>/<SCENARIO>/   normalized golden artifacts (after capture)
  test-fixtures/     raw → expected normalized pairs for unit tests
  contradictions.yaml
  quarantine/        non-deterministic triples land here (do not commit goldens)
```

## One command per program/scenario

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
cd golden && python3 -m unittest discover -s tests -v
```

Tests cover timestamp/surrogate normalization, comparison diffs, and seed SQL loadability.

## Determinism rules

1. Capture three times via `verify_determinism.sh`.
2. Pairwise `compare.py` must PASS.
3. On FAIL, diffs go to `golden/quarantine/<PROGRAM>/<SCENARIO>/` with `REASON.txt` — **do not commit** that scenario as a golden.

## Contradictions

See `contradictions.yaml`. Goldens document **actual** PROCEDURE DIVISION behavior (e.g. no SECCHK01 in CLM006B), not prologue claims.

## Troubleshooting

| Symptom | Action |
|---------|--------|
| Seed script exit 1 | Set `PCIS_IBMI_*` and `PCIS_SQL_RUNNER` |
| Spool empty | Check `PCIS_SPLF_NAME` / job completion |
| Non-deterministic quarantine | Inspect DISPLAY timestamps not covered by `normalize.py`; extend `TIMESTAMP_RE` |
| Remainder penny mismatch | Confirm BIL003B scenario-02 seed; legacy puts remainder on **first** installment |
