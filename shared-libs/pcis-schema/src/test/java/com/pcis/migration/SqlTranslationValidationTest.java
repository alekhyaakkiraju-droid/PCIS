package com.pcis.migration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Validates every Db2-to-PostgreSQL SQL construct translation documented in
 * docs/db2-to-postgresql-translation.md (WO-158 / Risk R-02).
 *
 * <p>Each test method corresponds to a section in the reference document and verifies that the
 * PostgreSQL equivalent produces the same result as the documented Db2 for i behavior.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SqlTranslationValidationTest {

    private static final DockerImageName PG17 = DockerImageName.parse("postgres:17-alpine")
            .asCompatibleSubstituteFor("postgres");

    private PostgreSQLContainer<?> postgres;
    private String jdbcUrl;
    private String username;
    private String password;

    @BeforeAll
    void startPostgres() {
        assumeTrue(DockerClientFactory.instance().isDockerAvailable(),
                "Docker unavailable — skipping SqlTranslationValidationTest");

        postgres = new PostgreSQLContainer<>(PG17)
                .withDatabaseName("pcis_translate")
                .withUsername("pcis")
                .withPassword("pcis");
        postgres.start();

        jdbcUrl = postgres.getJdbcUrl();
        username = postgres.getUsername();
        password = postgres.getPassword();

        seedMinimalSchema();
    }

    @AfterAll
    void stopPostgres() {
        if (postgres != null) {
            postgres.stop();
        }
    }

    // -------------------------------------------------------------------------
    // §1 — Row Limiting: FETCH FIRST n ROWS ONLY → LIMIT n
    // -------------------------------------------------------------------------

    /** Db2: SELECT … FETCH FIRST 3 ROWS ONLY → PostgreSQL: SELECT … LIMIT 3 */
    @Test
    void fetchFirstNRowsMapsToLimitN() throws SQLException {
        // seed 5 rows, fetch only 3
        try (Connection conn = open(); Statement st = conn.createStatement()) {
            st.execute(
                    "CREATE TEMP TABLE t_limit (id INT, val INT);"
                            + "INSERT INTO t_limit VALUES (1,10),(2,20),(3,30),(4,40),(5,50)");
            int count = 0;
            try (ResultSet rs = st.executeQuery("SELECT id FROM t_limit ORDER BY id LIMIT 3")) {
                while (rs.next()) count++;
            }
            assertEquals(3, count, "LIMIT 3 must return exactly 3 rows (§1)");
        }
    }

    // -------------------------------------------------------------------------
    // §2 — Scalar expressions: VALUES expr → SELECT expr (no FROM)
    // -------------------------------------------------------------------------

    /** Db2: VALUES CURRENT TIMESTAMP INTO :hv → PostgreSQL: SELECT CURRENT_TIMESTAMP */
    @Test
    void valuesExpressionMapsToSelectExpr() throws SQLException {
        try (Connection conn = open();
             ResultSet rs = conn.createStatement()
                     .executeQuery("SELECT CURRENT_TIMESTAMP")) {
            assertTrue(rs.next(), "SELECT CURRENT_TIMESTAMP must return a row (§2)");
            assertTrue(rs.getTimestamp(1) != null, "CURRENT_TIMESTAMP must not be null");
        }
    }

    /** Db2: SELECT 1 FROM SYSIBM.SYSDUMMY1 → PostgreSQL: SELECT 1 */
    @Test
    void sysdummyOneReplacedByNoFromClause() throws SQLException {
        try (Connection conn = open();
             ResultSet rs = conn.createStatement()
                     .executeQuery("SELECT 1")) {
            assertTrue(rs.next(), "SELECT 1 without FROM must return a row (§3)");
            assertEquals(1, rs.getInt(1));
        }
    }

    /** SELECT CURRENT_DATE with no FROM clause (no SYSIBM.SYSDUMMY1 needed) */
    @Test
    void currentDateRequiresNoFromClause() throws SQLException {
        try (Connection conn = open();
             ResultSet rs = conn.createStatement()
                     .executeQuery("SELECT CURRENT_DATE")) {
            assertTrue(rs.next());
            assertTrue(rs.getDate(1) != null);
        }
    }

    // -------------------------------------------------------------------------
    // §4 — Date arithmetic: labelled durations → INTERVAL
    // -------------------------------------------------------------------------

    /** BIL003B monthly: DATE + 1 MONTH → date + INTERVAL '1 month' */
    @Test
    void dateArithmeticMonthlyFrequency() throws SQLException {
        try (Connection conn = open();
             ResultSet rs = conn.createStatement()
                     .executeQuery("SELECT DATE '2025-03-15' + INTERVAL '1 month'")) {
            assertTrue(rs.next());
            assertEquals(LocalDate.of(2025, 4, 15), rs.getDate(1).toLocalDate(),
                    "2025-03-15 + 1 month must equal 2025-04-15 (§4)");
        }
    }

    /** BIL003B quarterly: DATE + 3 MONTHS */
    @Test
    void dateArithmeticQuarterlyFrequency() throws SQLException {
        try (Connection conn = open();
             ResultSet rs = conn.createStatement()
                     .executeQuery("SELECT DATE '2025-03-15' + INTERVAL '3 months'")) {
            assertTrue(rs.next());
            assertEquals(LocalDate.of(2025, 6, 15), rs.getDate(1).toLocalDate(),
                    "2025-03-15 + 3 months must equal 2025-06-15 (§4)");
        }
    }

    /** BIL003B semi-annual: DATE + 6 MONTHS */
    @Test
    void dateArithmeticSemiAnnualFrequency() throws SQLException {
        try (Connection conn = open();
             ResultSet rs = conn.createStatement()
                     .executeQuery("SELECT DATE '2025-03-15' + INTERVAL '6 months'")) {
            assertTrue(rs.next());
            assertEquals(LocalDate.of(2025, 9, 15), rs.getDate(1).toLocalDate(),
                    "2025-03-15 + 6 months must equal 2025-09-15 (§4)");
        }
    }

    /** BIL003B annual: DATE + 1 YEAR */
    @Test
    void dateArithmeticYearlyInterval() throws SQLException {
        try (Connection conn = open();
             ResultSet rs = conn.createStatement()
                     .executeQuery("SELECT DATE '2025-03-15' + INTERVAL '1 year'")) {
            assertTrue(rs.next());
            assertEquals(LocalDate.of(2026, 3, 15), rs.getDate(1).toLocalDate(),
                    "2025-03-15 + 1 year must equal 2026-03-15 (§4)");
        }
    }

    /**
     * BIL003B critical edge case: Jan 31 + 1 MONTH = Feb 28 (2025, non-leap).
     * PostgreSQL clamps to last day of February — identical to Db2 for i behavior.
     */
    @Test
    void dateArithmeticMonthEndClampingJan31NonLeap() throws SQLException {
        try (Connection conn = open();
             ResultSet rs = conn.createStatement()
                     .executeQuery("SELECT DATE '2025-01-31' + INTERVAL '1 month'")) {
            assertTrue(rs.next());
            assertEquals(LocalDate.of(2025, 2, 28), rs.getDate(1).toLocalDate(),
                    "2025-01-31 + 1 month must clamp to 2025-02-28 (§4.1)");
        }
    }

    /**
     * BIL003B critical edge case: Jan 31 + 1 MONTH = Feb 29 (2024, leap year).
     */
    @Test
    void dateArithmeticMonthEndClampingJan31LeapYear() throws SQLException {
        try (Connection conn = open();
             ResultSet rs = conn.createStatement()
                     .executeQuery("SELECT DATE '2024-01-31' + INTERVAL '1 month'")) {
            assertTrue(rs.next());
            assertEquals(LocalDate.of(2024, 2, 29), rs.getDate(1).toLocalDate(),
                    "2024-01-31 + 1 month must clamp to 2024-02-29 (leap year) (§4.1)");
        }
    }

    /**
     * BIL003B critical edge case: Nov 30 + 3 MONTHS = Feb 28 (2025, non-leap).
     */
    @Test
    void dateArithmeticMonthEndClampingNov30Plus3Months() throws SQLException {
        try (Connection conn = open();
             ResultSet rs = conn.createStatement()
                     .executeQuery("SELECT DATE '2024-11-30' + INTERVAL '3 months'")) {
            assertTrue(rs.next());
            assertEquals(LocalDate.of(2025, 2, 28), rs.getDate(1).toLocalDate(),
                    "2024-11-30 + 3 months must clamp to 2025-02-28 (§4.1)");
        }
    }

    /**
     * BIL003B critical edge case: Mar 31 + 1 MONTH = Apr 30 (month shorter than March).
     */
    @Test
    void dateArithmeticMonthEndClampingMar31() throws SQLException {
        try (Connection conn = open();
             ResultSet rs = conn.createStatement()
                     .executeQuery("SELECT DATE '2025-03-31' + INTERVAL '1 month'")) {
            assertTrue(rs.next());
            assertEquals(LocalDate.of(2025, 4, 30), rs.getDate(1).toLocalDate(),
                    "2025-03-31 + 1 month must clamp to 2025-04-30 (§4.1)");
        }
    }

    /**
     * AUD002B retention cutoff: CURRENT TIMESTAMP - n DAYS.
     * Db2: VALUES CURRENT TIMESTAMP - 365 DAYS → PostgreSQL: NOW() - INTERVAL '1 day' * 365
     */
    @Test
    void retentionCutoffWithDaysInterval() throws SQLException {
        try (Connection conn = open();
             ResultSet rs = conn.createStatement()
                     .executeQuery("SELECT (CURRENT_DATE - INTERVAL '365 days')::date")) {
            assertTrue(rs.next(), "Retention cutoff calculation must return a date (§4)");
            LocalDate expected = LocalDate.now().minusDays(365);
            assertEquals(expected, rs.getDate(1).toLocalDate(),
                    "CURRENT_DATE - 365 days must equal today minus 365 days");
        }
    }

    /**
     * BIL003B/PRM005B days-out: DAYS(a) - DAYS(b) → (a - b)::int
     */
    @Test
    void daysSubtractionCastToInt() throws SQLException {
        try (Connection conn = open();
             ResultSet rs = conn.createStatement()
                     .executeQuery("SELECT (DATE '2025-04-30' - DATE '2025-01-31')::int")) {
            assertTrue(rs.next());
            assertEquals(89, rs.getInt(1),
                    "DAYS subtraction: 2025-04-30 minus 2025-01-31 must equal 89 days (§4)");
        }
    }

    // -------------------------------------------------------------------------
    // §5 — Special registers: CURRENT USER → current_user
    // -------------------------------------------------------------------------

    /** Db2: SET :HV-CURRENT-USER = CURRENT USER → PostgreSQL: SELECT current_user */
    @Test
    void currentUserRegister() throws SQLException {
        try (Connection conn = open();
             ResultSet rs = conn.createStatement()
                     .executeQuery("SELECT current_user")) {
            assertTrue(rs.next(), "current_user must return a row (§5)");
            String user = rs.getString(1);
            assertTrue(user != null && !user.isBlank(),
                    "current_user must return a non-empty string, got: " + user);
        }
    }

    // -------------------------------------------------------------------------
    // §6 — Sequence access: NEXT VALUE FOR → nextval()
    // -------------------------------------------------------------------------

    /** Db2: VALUES NEXT VALUE FOR SEQ_X → PostgreSQL: SELECT nextval('seq_x') */
    @Test
    void nextvalIncrements() throws SQLException {
        try (Connection conn = open(); Statement st = conn.createStatement()) {
            st.execute("CREATE SEQUENCE IF NOT EXISTS seq_test_increment START WITH 1 INCREMENT BY 1");
            long v1 = queryLong(st, "SELECT nextval('seq_test_increment')");
            long v2 = queryLong(st, "SELECT nextval('seq_test_increment')");
            long v3 = queryLong(st, "SELECT nextval('seq_test_increment')");
            assertEquals(v1 + 1, v2, "nextval must increment by 1 (§6)");
            assertEquals(v2 + 1, v3, "nextval must increment by 1 (§6)");
        }
    }

    // -------------------------------------------------------------------------
    // §8 — DECIMAL/NUMERIC precision: COMP-3 → NUMERIC(p,s)
    // -------------------------------------------------------------------------

    /**
     * BIL003B: COMPUTE HV-INSTALLMENT-AMT ROUNDED = HV-PREM-ANNUAL / HV-INSTALLMENT-CNT.
     * COBOL ROUNDED = HALF_UP. PostgreSQL ROUND() uses HALF_UP for positive numbers.
     */
    @Test
    void numericDivisionRoundHalfUp() throws SQLException {
        try (Connection conn = open(); Statement st = conn.createStatement()) {
            // 1500.00 / 12 = 125.00 (exact — no rounding needed)
            BigDecimal r1 = queryBigDecimal(st, "SELECT ROUND(1500.00::NUMERIC(11,2) / 12, 2)");
            assertEquals(new BigDecimal("125.00"), r1, "1500.00 / 12 must equal 125.00 (§8)");

            // 1250.00 / 12 = 104.1666... → HALF_UP to 104.17
            BigDecimal r2 = queryBigDecimal(st, "SELECT ROUND(1250.00::NUMERIC(11,2) / 12, 2)");
            assertEquals(new BigDecimal("104.17"), r2,
                    "1250.00 / 12 must round HALF_UP to 104.17 (§8)");

            // 1000.00 / 3 = 333.3333... → 333.33
            BigDecimal r3 = queryBigDecimal(st, "SELECT ROUND(1000.00::NUMERIC(11,2) / 3, 2)");
            assertEquals(new BigDecimal("333.33"), r3,
                    "1000.00 / 3 must round HALF_UP to 333.33 (§8)");

            // 100.005 → rounds to 100.01 (HALF_UP: .5 rounds up)
            BigDecimal r4 = queryBigDecimal(st, "SELECT ROUND(100.005::NUMERIC(11,3), 2)");
            assertEquals(new BigDecimal("100.01"), r4,
                    "100.005 must round HALF_UP to 100.01 (§8)");
        }
    }

    /**
     * CMM001B: COMPUTE HV-COMMISSION-AMT ROUNDED = HV-PAID-AMT * (HV-COMM-RATE / 100).
     * Validates S9(11)V99 (NUMERIC(13,2)) and S9(5)V9999 (NUMERIC(7,4)) interaction.
     */
    @Test
    void numericMultiplicationCommissionRate() throws SQLException {
        try (Connection conn = open(); Statement st = conn.createStatement()) {
            // 850.00 * 0.1500 = 127.50
            BigDecimal r1 = queryBigDecimal(st,
                    "SELECT ROUND(850.00::NUMERIC(11,2) * 0.1500::NUMERIC(7,4), 2)");
            assertEquals(new BigDecimal("127.50"), r1,
                    "850.00 * 0.15 must equal 127.50 (§8)");

            // 1200.00 * 0.0850 = 102.00
            BigDecimal r2 = queryBigDecimal(st,
                    "SELECT ROUND(1200.00::NUMERIC(11,2) * 0.0850::NUMERIC(7,4), 2)");
            assertEquals(new BigDecimal("102.00"), r2,
                    "1200.00 * 0.085 must equal 102.00 (§8)");

            // Verify NUMERIC(13,2) precision for large premium values
            BigDecimal r3 = queryBigDecimal(st,
                    "SELECT ROUND(99999999999.99::NUMERIC(13,2) * 1.0000::NUMERIC(7,4), 2)");
            assertEquals(new BigDecimal("99999999999.99"), r3,
                    "NUMERIC(13,2) must preserve full precision (§8)");
        }
    }

    /**
     * Rate factor multiplication: NUMERIC(7,4) base rate * factor.
     * E.g., PRM005B: base_rate * risk_factor
     */
    @Test
    void numericRateFactorMultiplication() throws SQLException {
        try (Connection conn = open(); Statement st = conn.createStatement()) {
            // 0.0500 (5% base rate) * 1.2500 (factor) = 0.0625
            BigDecimal r = queryBigDecimal(st,
                    "SELECT ROUND(0.0500::NUMERIC(7,4) * 1.2500::NUMERIC(7,4), 4)");
            assertEquals(new BigDecimal("0.0625"), r,
                    "0.0500 * 1.2500 must equal 0.0625 (§8)");
        }
    }

    // -------------------------------------------------------------------------
    // §9 — SQLCODE → SQLSTATE mapping
    // -------------------------------------------------------------------------

    /**
     * SQLCODE -803 (duplicate key) → SQLSTATE 23505.
     * Verified by attempting a duplicate INSERT and inspecting the thrown SQLState.
     */
    @Test
    void duplicateKeyRaisesSqlstate23505() throws SQLException {
        try (Connection conn = open(); Statement st = conn.createStatement()) {
            st.execute(
                    "CREATE TEMP TABLE t_dup (id INT PRIMARY KEY, val TEXT);"
                            + "INSERT INTO t_dup VALUES (1, 'first')");
            try {
                st.execute("INSERT INTO t_dup VALUES (1, 'duplicate')");
                fail("Expected unique_violation (SQLSTATE 23505) was not raised");
            } catch (SQLException e) {
                assertEquals("23505", e.getSQLState(),
                        "Duplicate key insert must raise SQLSTATE 23505 (§9)");
            }
        }
    }

    /**
     * SQLCODE -530 (FK violation) → SQLSTATE 23503.
     */
    @Test
    void foreignKeyViolationRaisesSqlstate23503() throws SQLException {
        try (Connection conn = open(); Statement st = conn.createStatement()) {
            st.execute(
                    "CREATE TEMP TABLE t_parent (id INT PRIMARY KEY);"
                            + "CREATE TEMP TABLE t_child (id INT PRIMARY KEY, parent_id INT "
                            + "  REFERENCES t_parent(id))");
            try {
                st.execute("INSERT INTO t_child VALUES (1, 999)");
                fail("Expected foreign_key_violation (SQLSTATE 23503) was not raised");
            } catch (SQLException e) {
                assertEquals("23503", e.getSQLState(),
                        "FK violation must raise SQLSTATE 23503 (§9)");
            }
        }
    }

    /**
     * No-row result (SQLCODE +100) → ResultSet.next() returns false — no exception.
     * In COBOL: SQLCODE +100 sets WS-END-OF-CURSOR. In JDBC: rs.next() returns false.
     */
    @Test
    void emptyResultSetMapsToSqlcodePositive100() throws SQLException {
        try (Connection conn = open(); Statement st = conn.createStatement()) {
            st.execute("CREATE TEMP TABLE t_empty (id INT)");
            try (ResultSet rs = st.executeQuery("SELECT id FROM t_empty WHERE id = -1")) {
                boolean hasRow = rs.next();
                assertEquals(false, hasRow,
                        "Empty result set must return rs.next()=false (SQLCODE +100 equivalent) (§9)");
            }
        }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private void seedMinimalSchema() {
        // No V1 migration needed — all tests create their own temp tables or use literals.
    }

    private Connection open() throws SQLException {
        return DriverManager.getConnection(jdbcUrl, username, password);
    }

    private long queryLong(Statement st, String sql) throws SQLException {
        try (ResultSet rs = st.executeQuery(sql)) {
            assertTrue(rs.next(), "Query returned no rows: " + sql);
            return rs.getLong(1);
        }
    }

    private BigDecimal queryBigDecimal(Statement st, String sql) throws SQLException {
        try (ResultSet rs = st.executeQuery(sql)) {
            assertTrue(rs.next(), "Query returned no rows: " + sql);
            return rs.getBigDecimal(1);
        }
    }
}
