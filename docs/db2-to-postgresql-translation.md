# Db2 for i to PostgreSQL SQL Construct Translation Reference (WO-158 / Risk R-02)

This document is the **single authoritative reference** for every Db2 for i SQL construct found
in the 8 PCIS COBOL programs (`BIL003B`, `CMM001B`, `PRM005B`, `POL006B`, `CLM006B`, `CUS001A`,
`AUD002B`, `AUDLOG01`) and its validated PostgreSQL 17 equivalent.

Every translation below has a corresponding executable test in
`SqlTranslationValidationTest.java` (package `com.pcis.migration`).

---

## 1. Row Limiting — `FETCH FIRST n ROWS ONLY` → `LIMIT n`

| | SQL |
|---|---|
| **Db2 for i** | `SELECT … FROM t FETCH FIRST 1 ROW ONLY` |
| **PostgreSQL** | `SELECT … FROM t LIMIT 1` |

**Affected programs:** `CMM001B` (fetch first 1 row — in-force commission plan look-up),
`PRM005B` (fetch first 1 row — active premium rule).

**Worked example (Db2):**
```sql
SELECT COMM_RATE INTO :HV-COMM-RATE
  FROM COMMISSION_RATE_T
 WHERE EFF_DATE <= CURRENT DATE
   AND (EXP_DATE IS NULL OR EXP_DATE > CURRENT DATE)
 FETCH FIRST 1 ROW ONLY
```

**Worked example (PostgreSQL):**
```sql
SELECT comm_rate
  FROM commission_rate_t
 WHERE eff_date <= CURRENT_DATE
   AND (exp_date IS NULL OR exp_date > CURRENT_DATE)
 ORDER BY eff_date DESC
 LIMIT 1
```

> **Note:** When using `LIMIT 1` without `ORDER BY` the row returned is non-deterministic.
> Always add an `ORDER BY` clause (e.g., `ORDER BY eff_date DESC`) to preserve deterministic
> semantics. Db2 cursors without `ORDER BY` are also non-deterministic but COBOL programs
> typically assume the first physical row; add `ORDER BY` when translating to make the intent
> explicit.

---

## 2. Scalar Expressions — `VALUES expr INTO :hv` → `SELECT expr`

| | SQL |
|---|---|
| **Db2 for i** | `VALUES CURRENT TIMESTAMP INTO :HV-RUN-START` |
| **PostgreSQL** | `SELECT CURRENT_TIMESTAMP` (or `SELECT NOW()`) |

**Affected programs:** `BIL003B`, `CMM001B`, `PRM005B`, `POL006B`, `CLM006B`, `AUD002B`
(all programs capture run timestamps using this pattern).

**Worked example (Db2):**
```sql
EXEC SQL
    VALUES CURRENT TIMESTAMP INTO :WS-RUN-TIMESTAMP
END-EXEC
```

**Worked example (PostgreSQL — JDBC):**
```java
// JDBC: fetch scalar value directly
Timestamp runTs = stmt.executeQuery("SELECT CURRENT_TIMESTAMP")
                       .getTimestamp(1);
```

> **Note:** `VALUES expr` is a Db2 extension for evaluating expressions without a table.
> PostgreSQL does not require `FROM DUAL` or `FROM SYSIBM.SYSDUMMY1` — a plain `SELECT expr`
> is valid without a `FROM` clause.

---

## 3. System Tables — `SYSIBM.SYSDUMMY1` → No `FROM` Clause

| | SQL |
|---|---|
| **Db2 for i** | `SELECT 1 FROM SYSIBM.SYSDUMMY1` |
| **PostgreSQL** | `SELECT 1` (or `SELECT 1 FROM (VALUES(1)) AS t(x)`) |

**Affected programs:** Used implicitly in connectivity checks and expression evaluation.

**Worked example (Db2):**
```sql
SELECT CURRENT DATE FROM SYSIBM.SYSDUMMY1
```

**Worked example (PostgreSQL):**
```sql
SELECT CURRENT_DATE
```

---

## 4. Date Arithmetic — Labelled Durations → `INTERVAL`

| Db2 for i | PostgreSQL | Notes |
|---|---|---|
| `date + 1 MONTH` | `date + INTERVAL '1 month'` | Month-end clamping identical (see §4.1) |
| `date + 3 MONTHS` | `date + INTERVAL '3 months'` | |
| `date + 6 MONTHS` | `date + INTERVAL '6 months'` | |
| `date + 1 YEAR` | `date + INTERVAL '1 year'` | |
| `date + n DAYS` | `date + INTERVAL '1 day' * n` or `date + n` | |
| `CURRENT TIMESTAMP - n DAYS` | `NOW() - INTERVAL '1 day' * n` or `NOW() - MAKE_INTERVAL(days => n)` | AUD002B retention cutoff |
| `DAYS(a) - DAYS(b)` | `(a - b)::int` or `EXTRACT(EPOCH FROM a::timestamp - b::timestamp) / 86400` | BIL003B/PRM005B days-out |

**Affected programs:** `BIL003B` (billing frequency: monthly/quarterly/semi-annual/annual),
`PRM005B` (grace period: `EXPIRY_DATE + WS-GRACE-DAYS DAYS`),
`POL006B` (renewal window: `EXP_DATE - WS-RENEWAL-WINDOW-DAYS DAYS`),
`AUD002B` (retention cutoff: `CURRENT TIMESTAMP - WS-RETENTION-DAYS DAYS`).

### 4.1 Month-End Clamping (BIL003B Critical Edge Cases)

Both Db2 for i and PostgreSQL clamp to the last day of the target month when the source day
does not exist in that month. **The behavior is identical** and the tests in
`SqlTranslationValidationTest` confirm this.

| Base Date | + 1 MONTH | + 3 MONTHS | + 6 MONTHS |
|---|---|---|---|
| 2025-01-31 | 2025-02-28 | 2025-04-30 | 2025-07-31 |
| 2024-01-31 (leap) | 2024-02-29 | 2024-04-30 | 2024-07-31 |
| 2024-11-30 | 2024-12-30 | 2025-02-28 | 2025-05-30 |
| 2025-03-31 | 2025-04-30 | 2025-06-30 | 2025-09-30 |

**Worked example (Db2 — BIL003B billing frequency):**
```sql
MOVE 'M' TO WS-BILL-FREQ
EVALUATE WS-BILL-FREQ
    WHEN 'M'
        EXEC SQL SET :HV-NEXT-DUE = :HV-LAST-DUE + 1 MONTH END-EXEC
    WHEN 'Q'
        EXEC SQL SET :HV-NEXT-DUE = :HV-LAST-DUE + 3 MONTHS END-EXEC
    WHEN 'S'
        EXEC SQL SET :HV-NEXT-DUE = :HV-LAST-DUE + 6 MONTHS END-EXEC
    WHEN 'A'
        EXEC SQL SET :HV-NEXT-DUE = :HV-LAST-DUE + 1 YEAR END-EXEC
END-EVALUATE
```

**Worked example (PostgreSQL — Java/Spring Batch):**
```java
String sql = switch (billingFreq) {
    case "M" -> "SELECT $1::date + INTERVAL '1 month'";
    case "Q" -> "SELECT $1::date + INTERVAL '3 months'";
    case "S" -> "SELECT $1::date + INTERVAL '6 months'";
    case "A" -> "SELECT $1::date + INTERVAL '1 year'";
    default  -> throw new IllegalArgumentException("Unknown billing frequency: " + billingFreq);
};
```

---

## 5. Special Registers — `CURRENT USER`, `CURRENT TIMESTAMP`

| Db2 for i | PostgreSQL | Notes |
|---|---|---|
| `CURRENT USER` | `current_user` | Returns the current session role name |
| `CURRENT DATE` | `CURRENT_DATE` | Date only, no time component |
| `CURRENT TIMESTAMP` | `CURRENT_TIMESTAMP` | **Returns TIMESTAMPTZ in PostgreSQL** (see §5.1) |
| `CURRENT TIME` | `CURRENT_TIME` | Returns TIMETZ in PostgreSQL |

**Affected programs:** `CUS001A` (`SET :HV-CURRENT-USER = CURRENT USER` in 1100-RETRIEVE-CURRENT-USER).
All 8 programs use `CURRENT DATE` or `CURRENT TIMESTAMP` for audit columns.

### 5.1 Timezone Handling

Db2 for i `CURRENT TIMESTAMP` returns a local timestamp without timezone information.
PostgreSQL `CURRENT_TIMESTAMP` returns a `TIMESTAMPTZ` value.

**Strategy:** All `TIMESTAMP` columns in `V1__baseline_schema.sql` are declared as plain
`TIMESTAMP` (not `TIMESTAMPTZ`). Application code writes `Instant.now()` (UTC) and converts
to `LocalDateTime` at the JDBC boundary. The database stores UTC values without timezone info.
This is consistent with the Db2 behavior (which stored local-time timestamps).

---

## 6. Sequence Access — `NEXT VALUE FOR seq` → `nextval('seq')`

| | SQL |
|---|---|
| **Db2 for i** | `VALUES NEXT VALUE FOR SEQ_BILL_SCHED_ID INTO :HV-BILL-SCHED-ID` |
| **PostgreSQL** | `SELECT nextval('seq_bill_sched_id')` |

**Affected programs:** `BIL003B` (allocates `BILL_SCHED_ID`), `CMM001B` (allocates `COMMISSION_ID`).

**Worked example (Db2):**
```sql
EXEC SQL
    VALUES NEXT VALUE FOR SEQ_BILL_SCHED_ID
        INTO :HV-BILL-SCHED-ID
END-EXEC
```

**Worked example (PostgreSQL):**
```sql
SELECT nextval('seq_bill_sched_id')
```

> **Key constraint:** `allocationSize = 1` in JPA `@SequenceGenerator` is mandatory.
> See `docs/key-generation-strategy.md` for the full rule.

---

## 7. Row Identification — `RRN(table)` → Limitation Notice

| | |
|---|---|
| **Db2 for i** | `RRN(table)` returns the relative record number — a stable physical row address |
| **PostgreSQL** | `ctid` is the closest equivalent but **is NOT stable across VACUUM, CLUSTER, or table rewrites** |

**Affected programs:** No RRN() usage found in the 8 shipped COBOL programs.

**Recommendation:** Do not use `ctid` in application code. Use business keys (CUST_ID, POL_NBR,
CLAIM_ID) or surrogate IDENTITY columns for row identification. If a stable per-row identifier
is needed, add a UUID column instead.

---

## 8. DECIMAL/NUMERIC Precision — COMP-3 → `NUMERIC(p,s)`

| COBOL / Db2 type | PostgreSQL type | Notes |
|---|---|---|
| `COMP-3 S9(9)V99` (`DECIMAL(9,2)`) | `NUMERIC(11,2)` | 9 integer digits + 2 decimal = 11 total |
| `COMP-3 S9(11)V99` (`DECIMAL(13,2)`) | `NUMERIC(13,2)` | Premiums, reserves, authority limits |
| `COMP-3 S9(5)V9999` (rate factor) | `NUMERIC(7,4)` | Base rate and factor columns |
| `COMP-3 S9(7)V99` (`DECIMAL(9,2)`) | `NUMERIC(9,2)` | Commission amounts |

**Arithmetic behavior:** PostgreSQL `NUMERIC` arithmetic is exact (no floating-point error),
matching COBOL COMP-3 which is also a packed-decimal (exact) type. Division in PostgreSQL
returns an exact result with full precision; explicit `ROUND(expr, 2)` is required to match
COBOL `COMPUTE … ROUNDED` (HALF_UP) behavior.

**Worked examples:**

```sql
-- Annual premium division (BIL003B installment calculation)
-- COBOL: COMPUTE HV-INSTALLMENT-AMT ROUNDED = HV-PREM-ANNUAL / HV-INSTALLMENT-CNT
SELECT ROUND(1500.00 / 12, 2);       -- 125.00 (exact)
SELECT ROUND(1250.00 / 12, 2);       -- 104.17 (HALF_UP rounds 104.1666... → 104.17)
SELECT ROUND(1000.00 / 3, 2);        -- 333.33

-- Commission rate multiplication (CMM001B)
-- COBOL: COMPUTE HV-COMMISSION-AMT ROUNDED = HV-PAID-AMT * (HV-COMM-RATE / 100)
SELECT ROUND(850.00 * (15.00 / 100), 2);   -- 127.50
SELECT ROUND(1200.00 * (8.50 / 100), 2);   -- 102.00
```

> **Note:** `ROUND(x, 2)` in PostgreSQL uses "round half away from zero" (HALF_UP for positive
> numbers), which matches COBOL `ROUNDED` behavior for standard insurance arithmetic.

---

## 9. SQLCODE to SQLSTATE Mapping

COBOL programs check `SQLCODE` directly. PostgreSQL raises `SQLSTATE` codes instead.
Use the JDBC `SQLException.getSQLState()` method in Spring Batch exception handlers.

| SQLCODE | Condition | PostgreSQL SQLSTATE | Notes |
|---|---|---|---|
| `0` | Success | `00000` | |
| `+100` | Row not found (`NOT FOUND`) | `02000` | End of cursor, no row in `SELECT INTO` |
| `-803` | Duplicate key on INSERT | `23505` (unique_violation) | |
| `-530` | FK violation on INSERT/UPDATE | `23503` (foreign_key_violation) | |
| `-803` variants | Check constraint violation | `23514` (check_violation) | |
| `< 0` (generic) | Unrecoverable SQL error | `HY000` or specific | Catch `DataAccessException` in Spring |
| `-204` | Undefined name (table not found) | `42P01` (undefined_table) | |
| `-551` | Not authorized | `42501` (insufficient_privilege) | |

**COBOL pattern:**
```cobol
EXEC SQL ... END-EXEC
EVALUATE SQLCODE
    WHEN 0      CONTINUE
    WHEN +100   MOVE 'Y' TO WS-END-OF-CURSOR
    WHEN OTHER  MOVE 'Y' TO WS-ERROR-FLAG
                MOVE SQLCODE TO WS-SQLCODE-DISPLAY
END-EVALUATE
```

**Spring Batch / JDBC equivalent:**
```java
try {
    jdbcTemplate.update(...);
} catch (DuplicateKeyException e) {         // SQLSTATE 23505
    log.warn("Duplicate key: {}", e.getMessage());
} catch (DataIntegrityViolationException e) { // SQLSTATE 23xxx
    throw new SkipException("Integrity violation", e);
} catch (DataAccessException e) {
    throw new NonTransientResourceException("Unrecoverable SQL error", e);
}
```

---

## 10. Commitment Control — `COMMIT`/`ROLLBACK`

Commitment control semantics are **identical** between Db2 for i and PostgreSQL. The COBOL
programs use journal-based commit boundaries that map directly to JDBC transaction boundaries.

| Db2 for i | PostgreSQL / JDBC | Notes |
|---|---|---|
| `COMMIT` | `connection.commit()` / `@Transactional` | Identical semantics |
| `ROLLBACK` | `connection.rollback()` / exception propagation | Identical semantics |
| Isolation level | `READ COMMITTED` (default) | Both systems default to READ COMMITTED |

Spring `@Transactional` (or Spring Batch chunk transactions) provides the equivalent of Db2
journal entries automatically.

---

## 11. Host Variable Binding — `:HV-xxx` → JDBC `?` Parameters

| Db2 for i | JDBC / Spring | Notes |
|---|---|---|
| `:HV-CUSTOMER-ID` | `?` (positional) or `:custId` (named) | Use Spring `NamedParameterJdbcTemplate` |
| `SET :HV-X = value` | Not needed — JDBC returns result sets | |

---

## Appendix A: COBOL Program SQL Construct Inventory

| Program | Constructs Used |
|---|---|
| `BIL003B` | `FETCH FIRST n ROWS ONLY`, date + n MONTHS (1/3/6/12), `NEXT VALUE FOR`, `DAYS(a)-DAYS(b)`, `CURRENT TIMESTAMP`, `CURRENT DATE` |
| `CMM001B` | `FETCH FIRST 1 ROW ONLY`, `CURRENT DATE`, `NEXT VALUE FOR`, `CURRENT TIMESTAMP` |
| `PRM005B` | `FETCH FIRST 1 ROW ONLY`, date + n DAYS, `CURRENT DATE`, `CURRENT TIMESTAMP` |
| `POL006B` | `FETCH FIRST n ROWS ONLY`, date arithmetic, `CURRENT DATE`, `CURRENT TIMESTAMP` |
| `CLM006B` | `FETCH FIRST n ROWS ONLY`, `CURRENT DATE`, `CURRENT TIMESTAMP`, `NEXT VALUE FOR` |
| `CUS001A` | `CURRENT USER`, `CURRENT DATE`, `CURRENT TIMESTAMP`, `FETCH FIRST 1 ROW ONLY` |
| `AUD002B` | `CURRENT TIMESTAMP - n DAYS`, `FETCH FIRST n ROWS ONLY`, `CURRENT DATE` |
| `AUDLOG01` | `CURRENT TIMESTAMP`, `CURRENT USER`, `NEXT VALUE FOR SEQ_AUDIT_LOG_ID` |

## Appendix B: Validated Translation Test Coverage

All translations are validated by `SqlTranslationValidationTest` (Testcontainers PostgreSQL 17):

| Section | Test Method |
|---|---|
| §1 Row limiting | `fetchFirstNRowsMapsToLimitN` |
| §2 Scalar expressions | `valuesExpressionMapsToSelectExpr` |
| §3 SYSIBM.SYSDUMMY1 | `sysdummyOneReplacedByNoFromClause` |
| §4 Date arithmetic | `dateArithmeticMonthlyFrequency`, `dateArithmeticMonthEndClamping`, `dateArithmeticYearlyInterval`, `retentionCutoffWithDaysInterval` |
| §5 Special registers | `currentUserRegister`, `daysSubtractionCastToInt` |
| §6 Sequence access | `nextvalIncrements` |
| §7 RRN limitation | (documented only — no stable equivalent) |
| §8 Numeric precision | `numericDivisionRoundHalfUp`, `numericMultiplicationCommissionRate` |
| §9 SQLCODE → SQLSTATE | `duplicateKeyRaisesSqlstate23505`, `foreignKeyViolationRaisesSqlstate23503` |
| §10 Commitment control | (no test — standard JDBC transaction) |
