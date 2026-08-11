package com.pcis.schema;

import com.pcis.schema.classification.ClassificationTier;
import com.pcis.schema.classification.EntityClassificationEntry;
import com.pcis.schema.classification.EntityClassificationManifest;
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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ClassificationCompletenessGateTest {

    private static final DockerImageName PG17 = DockerImageName.parse("postgres:17-alpine")
            .asCompatibleSubstituteFor("postgres");

    private static PostgreSQLContainer<?> container;
    private static String jdbcUrl;
    private static String username;
    private static String password;

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
    }

    @Test
    void everyV1TableIsClassifiedInManifest() throws Exception {
        EntityClassificationManifest manifest =
                EntityClassificationManifest.load(EntityClassificationManifest.resolveManifestPath());
        Set<String> schemaTables = loadBaseTables(openConnection());

        List<String> errors = manifest.validateCompleteness(schemaTables);
        assertTrue(errors.isEmpty(), () -> "Classification completeness gate failed:\n" + String.join("\n", errors));
        assertEquals(57, schemaTables.size(), schemaTables::toString);
        assertEquals(57, manifest.entries().size());
    }

    @Test
    void restrictedAndConfidentialEntriesMeetRetentionMinimum() throws Exception {
        EntityClassificationManifest manifest =
                EntityClassificationManifest.load(EntityClassificationManifest.resolveManifestPath());

        List<String> violations = manifest.entries().stream()
                .filter(entry -> entry.classificationTier() == ClassificationTier.RESTRICTED
                        || entry.classificationTier() == ClassificationTier.CONFIDENTIAL)
                .filter(entry -> entry.retentionDays() < 365)
                .map(entry -> entry.tableName() + " retention_days=" + entry.retentionDays())
                .toList();

        assertTrue(violations.isEmpty(), () -> "Retention violations:\n" + String.join("\n", violations));
    }

    @Test
    void requiredRestrictedTablesListPiiColumns() throws Exception {
        EntityClassificationManifest manifest =
                EntityClassificationManifest.load(EntityClassificationManifest.resolveManifestPath());

        List<String> requiredRestricted = List.of(
                "CUSTOMER_T",
                "CUSTOMER_ADDRESS_T",
                "CUSTOMER_CONTACT_T",
                "CLAIM_ADJUSTER_T",
                "AUDIT_LOG_T");

        for (String tableName : requiredRestricted) {
            EntityClassificationEntry entry = manifest.entriesByTableName().get(tableName);
            assertEquals(ClassificationTier.RESTRICTED, entry.classificationTier(), tableName);
            assertTrue(
                    !entry.piiColumns().isEmpty(),
                    () -> tableName + " must declare pii_columns");
        }
    }

    @Test
    void requiredConfidentialFinancialTablesAreClassified() throws Exception {
        EntityClassificationManifest manifest =
                EntityClassificationManifest.load(EntityClassificationManifest.resolveManifestPath());

        List<String> financialTables = List.of(
                "CLAIM_PAYMENT_T",
                "BILLING_SCHEDULE_T",
                "INVOICE_T",
                "PREMIUM_CALC_T",
                "COMMISSION_T");

        for (String tableName : financialTables) {
            EntityClassificationEntry entry = manifest.entriesByTableName().get(tableName);
            assertEquals(ClassificationTier.CONFIDENTIAL, entry.classificationTier(), tableName);
            assertTrue(entry.retentionDays() >= 365, tableName);
        }
    }

    private static Set<String> loadBaseTables(Connection conn) throws SQLException {
        Set<String> tables = new HashSet<>();
        try (ResultSet rs = conn.createStatement().executeQuery(
                """
                SELECT c.relname
                FROM pg_class c
                JOIN pg_namespace n ON n.oid = c.relnamespace
                WHERE n.nspname = 'public'
                  AND c.relkind IN ('r', 'p')
                  AND NOT EXISTS (
                      SELECT 1 FROM pg_inherits i WHERE i.inhrelid = c.oid
                  )
                """)) {
            while (rs.next()) {
                tables.add(EntityClassificationEntry.normalizeTableName(rs.getString(1)));
            }
        }
        tables.remove("FLYWAY_SCHEMA_HISTORY");
        return tables;
    }

    private Connection openConnection() throws SQLException {
        return DriverManager.getConnection(jdbcUrl, username, password);
    }

    private void runFlywayMigrate(boolean cleanFirst) throws SQLException {
        if (cleanFirst) {
            try (Connection conn = openConnection();
                 var st = conn.createStatement()) {
                st.execute("DROP SCHEMA public CASCADE");
                st.execute("CREATE SCHEMA public");
            }
        }
        Path migrations = Path.of("db/migration").toAbsolutePath();
        if (!migrations.resolve("V1__baseline_schema.sql").toFile().exists()) {
            migrations = Path.of("..", "db", "migration").toAbsolutePath();
        }
        Flyway.configure()
                .dataSource(jdbcUrl, username, password)
                .locations("filesystem:" + migrations)
                .target("1")
                .load()
                .migrate();
    }
}
