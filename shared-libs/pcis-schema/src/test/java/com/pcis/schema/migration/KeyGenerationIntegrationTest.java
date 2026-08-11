package com.pcis.schema.migration;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Integration test: verifies key generation behaviour against a real PostgreSQL 17 instance.
 *
 * <ol>
 *   <li>Business-key sequences yield values {@code >= 10,000,000} (WO-155 parallel-run safety).</li>
 *   <li>IDENTITY-backed surrogate key columns auto-assign a positive BIGINT on insert.</li>
 * </ol>
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class KeyGenerationIntegrationTest {

    private static final long SEQUENCE_MINIMUM_START = 10_000_000L;

    private static final DockerImageName PG17 = DockerImageName.parse("postgres:17-alpine")
            .asCompatibleSubstituteFor("postgres");

    private static final List<String> BUSINESS_KEY_SEQUENCES = List.of(
            "SEQ_CUSTOMER_ID", "SEQ_POLICY_NBR", "SEQ_CLAIM_ID",
            "SEQ_AGENT_ID", "SEQ_QUOTE_ID", "SEQ_INVOICE_ID"
    );

    private PostgreSQLContainer<?> container;
    private String jdbcUrl;
    private String username;
    private String password;

    @BeforeAll
    void startPostgresAndMigrate() {
        assumeTrue(DockerClientFactory.instance().isDockerAvailable(),
                "Docker unavailable — skipping KeyGenerationIntegrationTest");

        container = new PostgreSQLContainer<>(PG17)
                .withDatabaseName("pcis")
                .withUsername("pcis")
                .withPassword("pcis");
        container.start();

        jdbcUrl = container.getJdbcUrl();
        username = container.getUsername();
        password = container.getPassword();

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

    @Test
    void businessKeySequencesStartAboveParallelRunMinimum() throws SQLException {
        try (Connection conn = openConnection()) {
            for (String seq : BUSINESS_KEY_SEQUENCES) {
                long nextVal = queryLong(conn, "SELECT NEXTVAL('" + seq.toLowerCase() + "')");
                assertTrue(nextVal >= SEQUENCE_MINIMUM_START,
                        seq + " first NEXTVAL was " + nextVal
                                + " but must be >= " + SEQUENCE_MINIMUM_START
                                + " (parallel-run safety: WO-155)");
            }
        }
    }

    @Test
    void identityColumnAutoAssignsPositiveSurrogateKey() throws SQLException {
        // AUDIT_LOG_ARCHIVE_T has no FK constraints — safe to insert without seeding parents.
        try (Connection conn = openConnection()) {
            long generatedId = queryLong(conn,
                    "INSERT INTO AUDIT_LOG_ARCHIVE_T "
                            + "(PROGRAM_NAME, ACTION_CODE, TABLE_NAME, RECORD_KEY, USER_ID, "
                            + "LOG_TIMESTAMP, ARCHIVE_DATE, CRT_USER, CRT_TIMESTAMP, UPD_USER, UPD_TIMESTAMP) "
                            + "VALUES ('KEYGEN_TST', 'INSERT', 'AUDIT_LOG_T', 'TEST-001', 'SYSTEM', "
                            + "NOW(), CURRENT_DATE, 'SYSTEM', NOW(), 'SYSTEM', NOW()) RETURNING LOG_ID");

            assertTrue(generatedId >= 1,
                    "AUDIT_LOG_ARCHIVE_T.LOG_ID (IDENTITY) should be a positive BIGINT, got " + generatedId);
            assertTrue(generatedId < SEQUENCE_MINIMUM_START,
                    "AUDIT_LOG_ARCHIVE_T.LOG_ID (IDENTITY) should be far below the business-sequence start ("
                            + SEQUENCE_MINIMUM_START + ") — got " + generatedId);
        }
    }

    private long queryLong(Connection conn, String sql) throws SQLException {
        try (ResultSet rs = conn.createStatement().executeQuery(sql)) {
            assertTrue(rs.next(), "Query returned no rows: " + sql);
            return rs.getLong(1);
        }
    }

    private Connection openConnection() throws SQLException {
        return DriverManager.getConnection(jdbcUrl, username, password);
    }
}
