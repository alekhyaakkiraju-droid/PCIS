package com.pcis.schema.migration;

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
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * CI gate: validates V1 Flyway schema monetary columns against docs/data-dictionary.yaml (WO-152).
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MonetaryPrecisionGateTest {

    private static final DockerImageName PG17 = DockerImageName.parse("postgres:17-alpine")
            .asCompatibleSubstituteFor("postgres");

    private static PostgreSQLContainer<?> container;
    private static String jdbcUrl;
    private static String username;
    private static String password;

    private List<MonetaryColumnSpec> monetaryColumns;
    private MonetaryPrecisionValidator validator = new MonetaryPrecisionValidator();

    @BeforeAll
    void startDatabaseAndMigrate() throws Exception {
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
        monetaryColumns = MonetaryColumnSpecResolver.resolve(
                RepoPaths.dataDictionary(), RepoPaths.flywayBaselineSql());
        assertTrue(monetaryColumns.size() >= 30, () -> "expected at least 30 monetary columns, got " + monetaryColumns.size());
    }

    @Test
    void allMonetaryColumnsMatchDataDictionary() throws SQLException {
        try (Connection conn = openConnection()) {
            JdbcColumnMetadataProvider provider = new JdbcColumnMetadataProvider(conn, monetaryColumns);
            List<ColumnCheckResult> results = validator.validateAll(monetaryColumns, provider);
            MonetaryPrecisionValidator.MonetaryPrecisionReport report = validator.buildReport(results);

            System.out.println(report.format());

            long amountColumns = monetaryColumns.stream()
                    .filter(c -> c.kind() == MonetaryKind.AMOUNT)
                    .count();
            long rateFactorColumns = monetaryColumns.stream()
                    .filter(c -> c.kind() == MonetaryKind.RATE_FACTOR)
                    .count();
            System.out.printf(
                    "Dictionary monetary columns: %d total (%d AMOUNT scale-2, %d RATE_FACTOR scale-4)%n",
                    monetaryColumns.size(),
                    amountColumns,
                    rateFactorColumns);

            assertTrue(report.allPassed(), () -> report.format());
            assertEquals(0, report.failCount());
            assertEquals(monetaryColumns.size(), report.passCount());
        }
    }

    @Test
    void noMonetaryColumnUsesForbiddenFloatingTypes() throws SQLException {
        try (Connection conn = openConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(
                     """
                     SELECT table_name, column_name, data_type
                     FROM information_schema.columns
                     WHERE table_schema = 'public'
                       AND data_type IN ('real', 'double precision', 'money')
                     ORDER BY table_name, column_name
                     """)) {
            StringBuilder forbidden = new StringBuilder();
            while (rs.next()) {
                forbidden.append(rs.getString("table_name"))
                        .append('.')
                        .append(rs.getString("column_name"))
                        .append(" (")
                        .append(rs.getString("data_type"))
                        .append(")")
                        .append(System.lineSeparator());
            }
            assertTrue(
                    forbidden.isEmpty(),
                    () -> "Forbidden floating-point or money types found:%n" + forbidden);
        }
    }

    private Connection openConnection() throws SQLException {
        return DriverManager.getConnection(jdbcUrl, username, password);
    }

    private void runFlywayMigrate(boolean cleanFirst) {
        if (cleanFirst) {
            try (Connection conn = openConnection();
                 Statement st = conn.createStatement()) {
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
