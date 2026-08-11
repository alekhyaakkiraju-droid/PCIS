# Golden-Output Pipeline Operations

Operational reference for the PCIS golden-output regression gate in the Forge Shipping CI/CD
pipeline (WO-178).

## Overview

The golden-output gate runs all `@Tag("GoldenOutput")` JUnit 5 tests as a fail-closed gate
within the `build:maven` stage. Any cent-level divergence between expected (committed golden JSON)
and actual (Testcontainers PostgreSQL run) blocks image promotion and publishes structured diff
reports as retained build artifacts.

## Pipeline Configuration

File: `forge-pipeline.yaml` (repo root)

### Triggers

| Event | Branches | Full Suite |
|-------|----------|------------|
| Push / PR | `main`, `forge/*` | No (Testcontainers optional) |
| Nightly cron `0 2 * * *` | All | Yes (`PCIS_USE_TESTCONTAINERS=1`) |

### Stages

```
build:maven
  ├── compile
  ├── unit-tests              (excludes GoldenOutput tag)
  ├── golden-output-gate      (fail-closed, retains reports on failure)
  └── jacoco-check            (90% line coverage on monetary packages)
publish:image                 (blocked until build:maven passes)
promote:staging
```

## Fail-Closed Gate

The golden-output gate is **fail-closed**. It fails rather than skips when:

| Condition | Failure mode |
|-----------|-------------|
| No `@GoldenOutput` tests found | `failIfNoTests=true` → BUILD FAIL |
| Docker daemon unavailable | Testcontainers throws → test failure → BUILD FAIL |
| Missing fixture file | `executeSqlFile` throws `IOException` → test failure → BUILD FAIL |
| Cent-level divergence | `assertTrue(diff.isMatch())` fails → BUILD FAIL |
| Test exceeds per-class timeout | Surefire kills JVM → BUILD FAIL |

To run the gate locally:

```bash
PCIS_USE_TESTCONTAINERS=1 mvn test -Pgolden-output -Dgroups=GoldenOutput
```

## Timeout Configuration

| Parameter | Default | Override |
|-----------|---------|---------|
| `golden.surefire.timeout` | `300` s (forked JVM timeout) | `-Dgolden.surefire.timeout=600` |
| `golden.test.timeout.per.class` | `120` s | `-Dgolden.test.timeout.per.class=180` |
| Pipeline stage timeout | `3600` s | Edit `forge-pipeline.yaml` `timeout:` |

The `forkedProcessTimeoutInSeconds=300` (5 minutes) is a hard ceiling on the forked Maven
Surefire JVM. If a test hangs (e.g., Testcontainers waiting for Docker), the JVM is killed
and the test is reported as a timeout failure — not left hanging indefinitely.

To raise the timeout for a long CI environment (e.g., cold Docker pull):

```bash
mvn test -Pgolden-output -Dgolden.surefire.timeout=600
```

## JaCoCo Coverage Enforcement

Profile: `jacoco` (in parent `pom.xml`)

Enforced packages:

| Pattern | Examples |
|---------|---------|
| `com/pcis/*/calc/**` | `com.pcis.billing.calc`, `com.pcis.commission.calc`, `com.pcis.premium.calc` |
| `com/pcis/*/payment/**` | `com.pcis.claims.payment` |

Threshold: **90% line coverage** (`COVEREDRATIO >= 0.90`).

To change the threshold:

```bash
# Temporarily lower for a WIP branch (CI dry-run only):
mvn verify -Pjacoco -Dgolden.coverage.minimum=0.80
```

To permanently change it, update `<golden.coverage.minimum>` in the parent `pom.xml`
`<properties>` block.

## Retained Artifacts

On golden-output test failure, Forge Shipping retains:

| Pattern | Label | Retention |
|---------|-------|-----------|
| `golden/reports/*.json` | `golden-diff-json` | 30 days |
| `golden/reports/*.txt` | `golden-diff-text` | 30 days |

The JSON diff report contains: scenario path, diff category counts, first 5 divergence details
(table, key, column, expected, actual). The text report is the human-readable equivalent for
direct reading in the Forge Shipping build results page.

Reports are written to `golden/reports/` by `GoldenDiffJsonWriter` and `GoldenDiffTextWriter`
when `GoldenComparator.compare()` returns a non-matching diff. If the test passes, no report
file is written.

## Nightly Schedule

The nightly run at `0 2 * * *` executes the full golden-output suite with:
- `PCIS_USE_TESTCONTAINERS=1` — forces Testcontainers PostgreSQL 17 (ignores H2 fallback)
- `PCIS_GOLDEN_FULL_SUITE=1` — signals test classes to run all scenarios (no fast-exit)
- `attachSurefireClassDurations: true` — per-class `time` field from `TEST-*.xml` attached as
  `golden_class_duration_seconds` build metadata
- `suiteDurationKey: golden_suite_duration_seconds` — sum of all class durations attached as
  total suite duration metadata

Duration metadata is visible on the Forge Shipping build results page and can be used to
alert on regressions in test performance.

## Adding New Golden Tests

1. Annotate the test class: `@Tag("GoldenOutput")`
2. Place golden JSON fixtures under `golden/outputs/<program>/<scenario>.golden.json`
3. Place seed SQL fixtures under `golden/fixtures/<program>/<scenario>.sql`
4. Run locally: `PCIS_USE_TESTCONTAINERS=1 mvn test -Pgolden-output -Dtest=YourGoldenTest`

The gate automatically picks up new `@Tag("GoldenOutput")` classes — no pipeline change required.

## Troubleshooting

### Build fails with "No tests were executed" on golden-output gate

Cause: No `@Tag("GoldenOutput")` test classes in scope (e.g., wrong module).

Fix: Run from the module containing the golden tests, or from the root with `--pl` flag:

```bash
mvn test -Pgolden-output -pl shared-libs/pcis-test-support
```

### Testcontainers fails with "Docker daemon unavailable"

Cause: Docker not running in the CI executor.

Fix: Ensure the Forge Shipping executor is configured with Docker-in-Docker (DinD) or a
Docker socket mount. The build must fail; do not set `PCIS_USE_TESTCONTAINERS=0` on CI as
that silently bypasses PostgreSQL 17 parity testing.

### Coverage check fails on a package with no classes yet

Cause: A `calc` or `payment` package is declared in JaCoCo `<includes>` but the package does
not exist yet (no compiled classes). JaCoCo reports 0% coverage for an empty package.

Fix: Either exclude the empty package from `<includes>` until the first class is added, or
add a placeholder test asserting true to seed minimal coverage. Remove the placeholder once
real tests land.
