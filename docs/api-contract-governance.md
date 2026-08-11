# API Contract Governance (WO-221)

## Layout

| Path | Purpose |
|------|---------|
| `api/contracts/baseline/` | Last released contracts — updated only at release |
| `api/contracts/` | Working copies modified on feature branches |
| `api/contracts/test-fixtures/` | Breaking/non-breaking pairs for CI simulation |

## Breaking changes (fail-closed)

The `scripts/openapi-diff-gate.sh` gate fails on:

- Response field removal
- Type narrowing
- Required request field added
- Enum value removed
- Path or HTTP method removed

## Non-breaking changes (allowed)

- Optional response fields added
- New paths or methods
- New enum values
- Type widening

## Version bumps

Breaking changes require a new major API version (`/v2/`) and migration guide.

## Tooling

- **Tool:** [oasdiff](https://github.com/Tufin/oasdiff) pinned in CI image
- **Command:** `oasdiff breaking baseline current --fail-on ERR`
- **Local test:** `scripts/test-openapi-gate.sh`

## Baseline update procedure

1. Merge release branch to `main`
2. Copy `api/contracts/*.yaml` → `api/contracts/baseline/`
3. Tag release

## Pipeline placement

Runs in Maven `verify` phase under profile `openapi-contract-gate`, after compile, before container push.
