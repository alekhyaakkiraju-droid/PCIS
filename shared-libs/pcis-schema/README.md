# PCIS Schema

Flyway baseline migration for the PCIS PostgreSQL 17 target schema (WO-149).

## Contents

- `db/migration/V1__baseline_schema.sql` — 55 design-doc tables plus `COMMISSION_LEDGER_T` and `outbox_events`, sequences, FKs, check constraints, and `AUDIT_LOG_T` monthly range partitions
- `src/main/resources/flyway.conf` — Flyway defaults (`classpath:db/migration`)
- `src/test/java/com/pcis/schema/V1BaselineMigrationTest.java` — Testcontainers PostgreSQL 17 validation (with optional local JDBC override)
- `src/test/java/com/pcis/schema/migration/MonetaryPrecisionGateTest.java` — CI gate validating monetary column NUMERIC precision vs data dictionary + V1 Flyway (WO-152)
- `src/test/java/com/pcis/schema/migration/EntityBigDecimalGateTest.java` — CI gate ensuring `@Entity` monetary fields use `BigDecimal` (WO-152)

Regenerate the SQL from the design document:

```bash
python3 docs/generate_baseline_schema.py
```

## Test

With Docker (Testcontainers):

```bash
cd shared-libs/pcis-schema && mvn test
make test-monetary-precision
```

With local PostgreSQL 17:

```bash
mvn test -Dpcis.test.jdbc.url=jdbc:postgresql://localhost:5432/pcis_test
```
