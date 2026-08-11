package com.pcis.schema.migration;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.output.MigrateResult;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * CI gate (WO-157): validates Flyway migrations apply cleanly to a fresh PostgreSQL 17
 * database and that repeat migration is idempotent.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FlywayMigrationValidationGateTest {

    private static final DockerImageName PG17 =
            DockerImageName.parse("postgres:17-alpine").asCompatibleSubstituteFor("postgres");

    private static PostgreSQLContainer<?> container;
    private static String jdbcUrl;
    private static String username;
    private static String password;

    @BeforeAll
    void startDatabase() {
        String overrideUrl = System.getProperty("pcis.test.jdbc.url");
        if (overrideUrl != null && !overrideUrl.isBlank()) {
            jdbcUrl = overrideUrl;
            username = System.getProperty("pcis.test.jdbc.user", "postgres");
            password = System.getProperty("pcis.test.jdbc.password", "");
        } else if (DockerClientFactory.instance().isDockerAvailable()) {
            container = new PostgreSQLContainer<>(PG17)
                    .withDatabaseName("pcis_flyway_gate")
                    .withUsername("pcis")
                    .withPassword("pcis");
            container.start();
            jdbcUrl = container.getJdbcUrl();
            username = container.getUsername();
            password = container.getPassword();
        } else {
            assumeTrue(false, "Docker unavailable; set -Dpcis.test.jdbc.url for local PostgreSQL 17");
        }
    }

    @Test
    void pcisSchemaMigrationsApplyCleanlyToFreshDatabase() throws SQLException {
        resetSchema();
        Path migrations = resolvePcisSchemaMigrations();
        assertTrue(Files.isDirectory(migrations), "pcis-schema migrations missing: " + migrations);

        Flyway flyway =
                Flyway.configure()
                        .dataSource(jdbcUrl, username, password)
                        .locations("filesystem:" + migrations)
                        .load();

        MigrateResult result = flyway.migrate();
        assertTrue(result.success, () -> "migration failed: " + result);
        assertFalse(result.migrations.isEmpty(), "expected at least one migration");

        try (Connection conn = openConnection();
                Statement st = conn.createStatement();
                ResultSet rs =
                        st.executeQuery(
                                "SELECT COUNT(*) FROM flyway_schema_history WHERE success = true")) {
            assertTrue(rs.next());
            assertEquals(result.migrations.size(), rs.getInt(1));
        }
    }

    @Test
    void pcisSchemaMigrationsAreIdempotentOnSecondRun() {
        resetSchema();
        Path migrations = resolvePcisSchemaMigrations();
        Flyway flyway =
                Flyway.configure()
                        .dataSource(jdbcUrl, username, password)
                        .locations("filesystem:" + migrations)
                        .load();

        flyway.migrate();
        MigrateResult second = flyway.migrate();
        assertTrue(second.success);
        assertEquals(0, second.migrationsExecuted, "second migrate should apply zero scripts");
    }

    @Test
    void serviceMigrationScriptsHaveValidVersionPrefix() throws IOException {
        Path repoRoot = resolveRepoRoot();
        List<Path> invalid = new ArrayList<>();

        try (Stream<Path> paths = Files.walk(repoRoot)) {
            paths.filter(p -> p.toString().contains("/db/migration/"))
                    .filter(p -> p.getFileName().toString().endsWith(".sql"))
                    .forEach(
                            sql -> {
                                String name = sql.getFileName().toString();
                                if (!name.matches("V\\d+__.*\\.sql")) {
                                    invalid.add(sql);
                                }
                            });
        }

        assertTrue(
                invalid.isEmpty(),
                () -> "invalid Flyway script names (expected V{version}__description.sql): " + invalid);
    }

    @Test
    void serviceMigrationVersionsAreMonotonicWithinEachModule() throws IOException {
        Path repoRoot = resolveRepoRoot();
        List<String> violations = new ArrayList<>();

        try (Stream<Path> dirs =
                Files.walk(repoRoot)
                        .filter(p -> p.endsWith("db/migration") && Files.isDirectory(p))) {
            dirs.forEach(
                    dir -> {
                        try (Stream<Path> scripts = Files.list(dir)) {
                            List<Integer> versions =
                                    scripts.filter(p -> p.toString().endsWith(".sql"))
                                            .map(p -> p.getFileName().toString())
                                            .filter(name -> name.matches("V\\d+__.*\\.sql"))
                                            .map(name -> Integer.parseInt(name.substring(1, name.indexOf("__"))))
                                            .sorted(Comparator.naturalOrder())
                                            .toList();
                            for (int i = 1; i < versions.size(); i++) {
                                if (versions.get(i) <= versions.get(i - 1)) {
                                    violations.add(
                                            dir + " has non-monotonic versions: " + versions);
                                    break;
                                }
                            }
                        } catch (IOException e) {
                            throw new IllegalStateException(e);
                        }
                    });
        }

        assertTrue(violations.isEmpty(), () -> String.join("\n", violations));
    }

    private static void resetSchema() {
        try (Connection conn = openConnection();
                Statement st = conn.createStatement()) {
            st.execute("DROP SCHEMA IF EXISTS public CASCADE");
            st.execute("CREATE SCHEMA public");
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to reset schema", e);
        }
    }

    private static Path resolvePcisSchemaMigrations() {
        Path migrations = Path.of("db/migration").toAbsolutePath();
        if (!migrations.resolve("V1__baseline_schema.sql").toFile().exists()) {
            migrations = Path.of("..", "db", "migration").toAbsolutePath();
        }
        return migrations;
    }

    private static Path resolveRepoRoot() {
        Path current = Path.of("").toAbsolutePath();
        if (Files.isDirectory(current.resolve("shared-libs"))) {
            return current;
        }
        if (Files.isDirectory(current.resolve("..").resolve("shared-libs"))) {
            return current.resolve("..").normalize();
        }
        throw new IllegalStateException("Unable to locate repository root from " + current);
    }

    private static Connection openConnection() throws SQLException {
        return DriverManager.getConnection(jdbcUrl, username, password);
    }
}
