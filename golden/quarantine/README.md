# Quarantined non-deterministic captures — do not treat as goldens

## WO-148 (CSV / DISPLAY)

Failures from `verify_determinism.sh` land at:

```
golden/quarantine/<PROGRAM>/<SCENARIO>/{REASON.txt,diff-*.txt,run-*}
```

## WO-176 (JSON)

Failures from `GoldenReproducibilityValidator` land at:

```
golden/quarantine/{program}/{scenario}-quarantine.json
```

Never commit quarantined artifacts as approved goldens.
