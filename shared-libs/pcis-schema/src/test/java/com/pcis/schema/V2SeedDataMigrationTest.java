package com.pcis.schema;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class V2SeedDataMigrationTest {

    private static final DockerImageName PG17 = DockerImageName.parse("postgres:17-alpine")
            .asCompatibleSubstituteFor("postgres");

    private static final Map<String, Integer> EXPECTED_BILL_FREQ_INTERVALS = Map.of(
            "M", 1,
            "Q", 3,
            "S", 6,
            "A", 12
    );

    private static PostgreSQLContainer<?> container;
    private static String jdbcUrl;
    private static String username;
    private static String password;

    @BeforeAll
    void startDatabaseAndMigrate() {
        String overrideUrl = System.getProperty("pcis.test.jdbc.url");
        if (overrideUrl != null && !overrideUrl.isBlank()) {
            jdbcUrl = overrideUrl;
            username = System.getProperty("pcis.test.jdbc.user", "postgres");
            password = System.getProperty("pcis.test.jdbc.password", "");
        } else if (DockerClientFactory.instance().isDockerAvailable()) {
            container = new PostgreSQLContainer<>(PG17)
                    .withDatabaseName("pcis")
                    .withUsername("pcis")
                    .withPassword("pcis");
            container.start();
            jdbcUrl = container.getJdbcUrl();
            username = container.getUsername();
            password = container.getPassword();
        } else {
            assumeTrue(false, "Docker unavailable; set -Dpcis.test.jdbc.url for local PostgreSQL 17");
        }
        runFlywayMigrate(true);
    }

    @Test
    void seedsReferenceDataWithExpectedRowCounts() throws SQLException {
        try (Connection conn = openConnection()) {
            assertEquals(4, countRows(conn,
                    "SELECT COUNT(*) FROM code_table_t WHERE code_type = 'BILL_FREQ'"));
            assertEquals(4, countRows(conn,
                    "SELECT COUNT(*) FROM code_table_t WHERE code_type = 'BILL_FREQ_INTERVAL'"));
            assertEquals(5, countRows(conn,
                    "SELECT COUNT(*) FROM code_table_t WHERE code_type = 'BILL_SCHED_STATUS'"));
            assertEquals(3, countRows(conn,
                    "SELECT COUNT(*) FROM code_table_t WHERE code_type = 'RESERVE_STATUS'"));
            assertEquals(3, countRows(conn,
                    "SELECT COUNT(*) FROM code_table_t WHERE code_type = 'CLAIM_TYPE'"));
            assertEquals(4, countRows(conn, "SELECT COUNT(*) FROM coverage_type_t"));
            assertEquals(4, countRows(conn, "SELECT COUNT(*) FROM cancellation_reason_t"));
            assertEquals(3, countRows(conn, "SELECT COUNT(*) FROM commission_rate_t"));
            assertEquals(2, countRows(conn, "SELECT COUNT(*) FROM rate_table_t"));
            assertEquals(2, countRows(conn, "SELECT COUNT(*) FROM rate_factor_t"));
        }
    }

    @Test
    void billingFrequencyIntervalsMatchBil003bSemantics() throws SQLException {
        try (Connection conn = openConnection()) {
            for (var entry : EXPECTED_BILL_FREQ_INTERVALS.entrySet()) {
                int intervalMonths = queryIntervalMonths(conn, entry.getKey());
                assertEquals(entry.getValue(), intervalMonths,
                        () -> "BILL_FREQ_INTERVAL for " + entry.getKey());
            }
        }
    }

    @Test
    void monetaryReferenceColumnsUseExplicitNumericScale() throws SQLException {
        try (Connection conn = openConnection()) {
            assertNumericScale(conn, "commission_rate_t", "comm_rate", 7, 4);
            assertNumericScale(conn, "rate_table_t", "base_rate", 7, 4);
            assertNumericScale(conn, "rate_factor_t", "factor_value", 7, 4);
            assertNumericScale(conn, "discount_rule_t", "disc_pct", 7, 4);
            assertNumericScale(conn, "surcharge_rule_t", "sur_pct", 7, 4);
            assertNumericScale(conn, "tax_table_t", "tax_pct", 7, 4);
            assertNumericScale(conn, "risk_score_factor_t", "factor_value", 7, 4);
            assertNumericScale(conn, "uw_rule_t", "threshold_amt", 13, 2);
        }
    }

    @Test
    void testFixturesLoadExpectedTransactionalRowCounts() throws SQLException {
        try (Connection conn = openConnection()) {
            assertEquals(5, countRows(conn,
                    "SELECT COUNT(*) FROM customer_t WHERE cust_id LIKE 'CUST100%'"));
            assertEquals(10, countRows(conn,
                    "SELECT COUNT(*) FROM policy_t WHERE pol_nbr LIKE 'POL100%'"));
            assertEquals(20, countRows(conn,
                    "SELECT COUNT(*) FROM billing_schedule_t WHERE pol_nbr LIKE 'POL100%'"));
            assertEquals(5, countRows(conn,
                    "SELECT COUNT(*) FROM claim_t WHERE claim_id LIKE 'CLM100%'"));
            assertEquals(3, countRows(conn,
                    "SELECT COUNT(*) FROM claim_adjuster_t WHERE adjuster_id LIKE 'ADJ100%'"));
        }
    }

    @Test
    void sequencesRestartAboveFixtureHighWaterMarks() throws SQLException {
        try (Connection conn = openConnection()) {
            assertEquals(100006L, nextSequenceValue(conn, "seq_customer_id"));
            assertEquals(100011L, nextSequenceValue(conn, "seq_policy_nbr"));
            assertEquals(100006L, nextSequenceValue(conn, "seq_claim_id"));
            assertEquals(100004L, nextSequenceValue(conn, "seq_adjuster_id"));
        }
    }

    @Test
    void repeatableFixturesAreIdempotent() {
        runFlywayMigrate(false);
        try (Connection conn = openConnection()) {
            assertEquals(5, countRows(conn,
                    "SELECT COUNT(*) FROM customer_t WHERE cust_id LIKE 'CUST100%'"));
            assertEquals(20, countRows(conn,
                    "SELECT COUNT(*) FROM billing_schedule_t WHERE pol_nbr LIKE 'POL100%'"));
        } catch (SQLException e) {
            throw new AssertionError(e);
        }
    }

    private static int queryIntervalMonths(Connection conn, String freqCode) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                """
                SELECT CAST(code_desc AS INTEGER)
                FROM code_table_t
                WHERE code_type = 'BILL_FREQ_INTERVAL' AND code_value = ?
                """)) {
            ps.setString(1, freqCode);
            try (ResultSet rs = ps.executeQuery()) {
                assertTrue(rs.next(), "missing interval for " + freqCode);
                return rs.getInt(1);
            }
        }
    }

    private static void assertNumericScale(
            Connection conn, String table, String column, int precision, int scale) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                """
                SELECT numeric_precision, numeric_scale
                FROM information_schema.columns
                WHERE table_schema = 'public' AND table_name = ? AND column_name = ?
                """)) {
            ps.setString(1, table);
            ps.setString(2, column);
            try (ResultSet rs = ps.executeQuery()) {
                assertTrue(rs.next(), table + "." + column);
                assertEquals(precision, rs.getInt(1));
                assertEquals(scale, rs.getInt(2));
            }
        }
    }

    private static long nextSequenceValue(Connection conn, String sequenceName) throws SQLException {
        try (ResultSet rs = conn.createStatement().executeQuery("SELECT nextval('" + sequenceName + "')")) {
            assertTrue(rs.next());
            return rs.getLong(1);
        }
    }

    private static int countRows(Connection conn, String sql) throws SQLException {
        try (ResultSet rs = conn.createStatement().executeQuery(sql)) {
            assertTrue(rs.next());
            return rs.getInt(1);
        }
    }

    private Connection openConnection() throws SQLException {
        return DriverManager.getConnection(jdbcUrl, username, password);
    }

    private void runFlywayMigrate(boolean cleanFirst) {
        if (cleanFirst) {
            try (Connection conn = openConnection();
                 var st = conn.createStatement()) {
                st.execute("DROP SCHEMA public CASCADE");
                st.execute("CREATE SCHEMA public");
            } catch (SQLException e) {
                throw new IllegalStateException("Failed to reset schema", e);
            }
        }
        Path migrations = Path.of("db/migration").toAbsolutePath();
        if (!migrations.resolve("V1__baseline_schema.sql").toFile().exists()) {
            migrations = Path.of("..", "db", "migration").toAbsolutePath();
        }
        Flyway.configure()
                .dataSource(jdbcUrl, username, password)
                .locations("filesystem:" + migrations)
                .load()
                .migrate();
    }
}
