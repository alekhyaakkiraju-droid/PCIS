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
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.YearMonth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AuditLogPartitionMaintenanceTest {

    private static final DockerImageName PG17 =
            DockerImageName.parse("postgres:17-alpine").asCompatibleSubstituteFor("postgres");

    private static PostgreSQLContainer<?> container;
    private static String jdbcUrl;
    private static String username;
    private static String password;

    @BeforeAll
    void migrateBaselineAndMaintenance() {
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
        resetAndMigrate();
    }

    @Test
    void v2DefinesPartitionMaintenanceFunctions() throws SQLException {
        try (Connection conn = openConnection();
                Statement st = conn.createStatement();
                ResultSet rs =
                        st.executeQuery(
                                """
                                SELECT proname FROM pg_proc
                                WHERE proname IN ('maintain_audit_log_t_partitions', 'detach_audit_log_t_partition')
                                ORDER BY proname
                                """)) {
            assertTrue(rs.next());
            assertEquals("detach_audit_log_t_partition", rs.getString(1));
            assertTrue(rs.next());
            assertEquals("maintain_audit_log_t_partitions", rs.getString(1));
            assertFalse(rs.next());
        }
    }

    @Test
    void maintainFunctionCreatesFuturePartitions() throws SQLException {
        YearMonth future = YearMonth.from(LocalDate.now()).plusMonths(6);
        String expectedPartition =
                "audit_log_t_y"
                        + future.getYear()
                        + "m"
                        + String.format("%02d", future.getMonthValue());

        try (Connection conn = openConnection();
                Statement st = conn.createStatement()) {
            st.execute("SELECT maintain_audit_log_t_partitions(6)");
            try (ResultSet rs =
                    st.executeQuery(
                            "SELECT to_regclass('"
                                    + expectedPartition
                                    + "') IS NOT NULL AS exists")) {
                assertTrue(rs.next());
                assertTrue(rs.getBoolean("exists"), "expected partition " + expectedPartition);
            }
        }
    }

    @Test
    void detachFunctionDoesNotIssueRowLevelDelete() throws SQLException {
        try (Connection conn = openConnection();
                Statement st = conn.createStatement()) {
            st.execute(
                    """
                    INSERT INTO AUDIT_LOG_T (
                        PROGRAM_NAME, ACTION_CODE, TABLE_NAME, RECORD_KEY, USER_ID,
                        CRT_TIMESTAMP
                    ) VALUES ('AUDTEST', 'ADD', 'CUSTOMER_T', 'C-1', 'TESTUSER', TIMESTAMP '2026-03-15 10:00:00')
                    """);

            st.execute("SELECT detach_audit_log_t_partition('audit_log_t_y2026m03'::regclass)");

            try (ResultSet rs =
                    st.executeQuery("SELECT COUNT(*) AS cnt FROM audit_log_t_y2026m03")) {
                assertTrue(rs.next());
                assertEquals(1, rs.getInt("cnt"));
            }
        }
    }

    private static void resetAndMigrate() {
        try (Connection conn = openConnection();
                Statement st = conn.createStatement()) {
            st.execute("DROP SCHEMA public CASCADE");
            st.execute("CREATE SCHEMA public");
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to reset schema", e);
        }

        Path migrations = resolveMigrationsPath();
        Flyway.configure()
                .dataSource(jdbcUrl, username, password)
                .locations("filesystem:" + migrations)
                .load()
                .migrate();
    }

    private static Path resolveMigrationsPath() {
        Path migrations = Path.of("db/migration").toAbsolutePath();
        if (!migrations.resolve("V1__baseline_schema.sql").toFile().exists()) {
            migrations = Path.of("..", "db", "migration").toAbsolutePath();
        }
        return migrations;
    }

    private static Connection openConnection() throws SQLException {
        return DriverManager.getConnection(jdbcUrl, username, password);
    }
}
