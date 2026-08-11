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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class V1BaselineMigrationTest {

    private static final DockerImageName PG17 = DockerImageName.parse("postgres:17-alpine")
            .asCompatibleSubstituteFor("postgres");

    private static PostgreSQLContainer<?> container;
    private static String jdbcUrl;
    private static String username;
    private static String password;

    private static final Set<String> EXPECTED_BASE_TABLES = Set.of(
            "AGENT_COMMISSION_T", "AGENT_LICENSE_T", "AGENT_T", "APPROVAL_T",
            "AUDIT_LOG_ARCHIVE_T", "AUDIT_LOG_T", "BILLING_NOTICE_T", "BILLING_PLAN_T",
            "BILLING_SCHEDULE_T", "CANCELLATION_REASON_T", "CLAIM_ADJUSTER_T",
            "CLAIM_DOCUMENT_T", "CLAIM_NOTE_T", "CLAIM_PAYMENT_T", "CLAIM_RESERVE_HISTORY_T",
            "CLAIM_RESERVE_T", "CLAIM_T", "CODE_TABLE_T", "COMMISSION_LEDGER_T",
            "COMMISSION_RATE_T", "COMMISSION_T", "COVERAGE_T", "COVERAGE_TYPE_T",
            "CUSTOMER_ADDRESS_T", "CUSTOMER_CONTACT_T", "CUSTOMER_T", "DEDUCTIBLE_T",
            "DISCOUNT_RULE_T", "DOCUMENT_T", "ENDORSEMENT_T", "INVOICE_T", "OUTBOX_EVENTS",
            "PAYMENT_T", "POLICY_HISTORY_T", "POLICY_PROPERTY_T", "POLICY_T",
            "POLICY_VEHICLE_T", "PREMIUM_CALC_DETAIL_T", "PREMIUM_CALC_T", "QUOTE_COVERAGE_T",
            "QUOTE_T", "RATE_FACTOR_T", "RATE_TABLE_T", "RECOVERY_T", "REFUND_T",
            "REINSURANCE_CESSION_T", "REINSURANCE_TREATY_T", "RISK_SCORE_FACTOR_T",
            "ROLE_MENU_T", "RPT_PARM_T", "RPT_RUN_LOG_T", "SEC_USER_T", "SURCHARGE_RULE_T",
            "TAX_TABLE_T", "UW_DECISION_T", "UW_REFERRAL_T", "UW_RULE_T"
    );

    private static final Set<String> EXPECTED_SEQUENCES = Set.of(
            "SEQ_CUSTOMER_ID", "SEQ_AGENT_ID", "SEQ_QUOTE_ID", "SEQ_POLICY_NBR",
            "SEQ_COVERAGE_ID", "SEQ_CLAIM_ID", "SEQ_ADJUSTER_ID", "SEQ_TREATY_ID",
            "SEQ_COMMISSION_ID", "SEQ_AUDIT_LOG_ID", "SEQ_INVOICE_ID", "SEQ_PAYMENT_ID",
            "SEQ_REFUND_ID", "SEQ_DOCUMENT_ID", "SEQ_LEDGER_ID"
    );

    private static final List<String[]> EXPECTED_FOREIGN_KEYS = List.of(
            new String[]{"approval_t", "reserve_hist_id", "claim_reserve_t", "reserve_hist_id"},
            new String[]{"approval_t", "approver_id", "claim_adjuster_t", "adjuster_id"},
            new String[]{"commission_ledger_t", "bill_sched_id", "billing_schedule_t", "bill_sched_id"},
            new String[]{"claim_payment_t", "claim_id", "claim_t", "claim_id"},
            new String[]{"policy_t", "cust_id", "customer_t", "cust_id"}
    );

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
    void appliesMigrationAndCreatesExpectedTables() throws SQLException {
        try (Connection conn = openConnection()) {
            Set<String> baseTables = loadBaseTables(conn);
            assertTrue(baseTables.size() >= 55, () -> "expected at least 55 base tables, got " + baseTables);
            assertEquals(57, baseTables.size(), () -> "unexpected base table set: " + baseTables);
            assertTrue(baseTables.containsAll(EXPECTED_BASE_TABLES));
        }
    }

    @Test
    void createsAllSequencesStartingAt100000() throws SQLException {
        try (Connection conn = openConnection()) {
            Set<String> sequences = new HashSet<>();
            try (ResultSet rs = conn.createStatement().executeQuery(
                    "SELECT sequencename FROM pg_sequences WHERE schemaname = 'public'")) {
                while (rs.next()) {
                    sequences.add(rs.getString(1).toUpperCase());
                }
            }
            assertTrue(sequences.containsAll(EXPECTED_SEQUENCES),
                    () -> "missing sequences: " + diff(EXPECTED_SEQUENCES, sequences));

            for (String seq : EXPECTED_SEQUENCES) {
                long startValue = queryLong(conn,
                        "SELECT start_value FROM pg_sequences WHERE sequencename = ?",
                        seq.toLowerCase());
                assertEquals(100000L, startValue, seq + " should START WITH 100000");
            }
        }
    }

    @Test
    void definesExpectedForeignKeys() throws SQLException {
        try (Connection conn = openConnection()) {
            for (String[] fk : EXPECTED_FOREIGN_KEYS) {
                assertTrue(hasForeignKey(conn, fk[0], fk[1], fk[2], fk[3]),
                        () -> "missing FK " + fk[0] + "." + fk[1] + " -> " + fk[2] + "." + fk[3]);
            }
        }
    }

    @Test
    void definesCheckConstraints() throws SQLException {
        try (Connection conn = openConnection()) {
            assertTrue(constraintExists(conn, "billing_schedule_t", "chk_bill_sched_status"));
            assertTrue(constraintExists(conn, "claim_payment_t", "chk_claim_pmt_status"));
            assertTrue(constraintExists(conn, "approval_t", "chk_approval_status"));
        }
    }

    @Test
    void migrateTwiceIsIdempotent() {
        runFlywayMigrate(false);
        runFlywayMigrate(false);
        try (Connection conn = openConnection()) {
            int count = queryInt(conn,
                    "SELECT COUNT(*) FROM flyway_schema_history WHERE version = '1' AND success = true");
            assertEquals(1, count);
        } catch (SQLException e) {
            throw new AssertionError(e);
        }
    }

    @Test
    void auditLogTableIsRangePartitionedWithMonthlyPartitionsAndDefault() throws SQLException {
        try (Connection conn = openConnection()) {
            String strategy = queryString(conn,
                    """
                    SELECT partstrat FROM pg_partitioned_table pt
                    JOIN pg_class c ON c.oid = pt.partrelid
                    WHERE c.relname = 'audit_log_t'
                    """);
            assertEquals("r", strategy);

            List<String> partitionColumns = new ArrayList<>();
            try (ResultSet rs = conn.createStatement().executeQuery(
                    """
                    SELECT a.attname
                    FROM pg_partitioned_table pt
                    JOIN pg_class c ON c.oid = pt.partrelid
                    JOIN pg_attribute a ON a.attrelid = c.oid AND a.attnum = ANY (pt.partattrs)
                    WHERE c.relname = 'audit_log_t'
                    ORDER BY a.attnum
                    """)) {
                while (rs.next()) {
                    partitionColumns.add(rs.getString(1).toLowerCase());
                }
            }
            assertEquals(List.of("crt_timestamp"), partitionColumns);

            Set<String> childPartitions = new HashSet<>();
            try (ResultSet rs = conn.createStatement().executeQuery(
                    """
                    SELECT c.relname
                    FROM pg_inherits i
                    JOIN pg_class c ON c.oid = i.inhrelid
                    JOIN pg_class p ON p.oid = i.inhparent
                    WHERE p.relname = 'audit_log_t'
                    """)) {
                while (rs.next()) {
                    childPartitions.add(rs.getString(1).toLowerCase());
                }
            }
            assertEquals(13, childPartitions.size(), childPartitions::toString);
            assertTrue(childPartitions.stream().anyMatch(n -> n.contains("default")));
            assertEquals(12, childPartitions.stream().filter(n -> n.contains("y2026m")).count());
        }
    }

    @Test
    void monetaryColumnsUseNumericWithoutFloatTypes() throws SQLException {
        try (Connection conn = openConnection()) {
            try (ResultSet rs = conn.createStatement().executeQuery(
                    """
                    SELECT table_name, column_name, data_type
                    FROM information_schema.columns
                    WHERE table_schema = 'public'
                      AND data_type IN ('numeric', 'double precision', 'real')
                    ORDER BY table_name, column_name
                    """)) {
                while (rs.next()) {
                    String tableName = rs.getString("table_name");
                    String columnName = rs.getString("column_name");
                    String dataType = rs.getString("data_type");
                    assertEquals("numeric", dataType, tableName + "." + columnName);
                }
            }

            assertMonetary(conn, "policy_t", "prem_annual", 13, 2);
            assertMonetary(conn, "claim_reserve_t", "reserve_amt", 13, 2);
            assertMonetary(conn, "claim_reserve_t", "paid_to_date", 13, 2);
            assertMonetary(conn, "billing_schedule_t", "amt_due", 11, 2);
            assertMonetary(conn, "rate_table_t", "base_rate", 7, 4);
            assertMonetary(conn, "rate_factor_t", "factor_value", 7, 4);
            assertMonetary(conn, "commission_ledger_t", "commission_amt", 11, 2);
        }
    }

    @Test
    void everyBaseTableHasAuditColumns() throws SQLException {
        try (Connection conn = openConnection()) {
            for (String table : EXPECTED_BASE_TABLES) {
                for (String auditCol : List.of("crt_user", "crt_timestamp", "upd_user", "upd_timestamp")) {
                    assertTrue(columnExists(conn, table, auditCol),
                            () -> table + " missing " + auditCol);
                }
            }
        }
    }

    private static void assertMonetary(
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

    private static Set<String> diff(Set<String> expected, Set<String> actual) {
        Set<String> missing = new HashSet<>(expected);
        missing.removeAll(actual);
        return missing;
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
                tables.add(rs.getString(1).toUpperCase());
            }
        }
        tables.remove("FLYWAY_SCHEMA_HISTORY");
        return tables;
    }

    private static boolean hasForeignKey(
            Connection conn, String child, String childCol, String parent, String parentCol)
            throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                """
                SELECT 1
                FROM information_schema.table_constraints tc
                JOIN information_schema.key_column_usage kcu
                  ON tc.constraint_name = kcu.constraint_name
                 AND tc.table_schema = kcu.table_schema
                JOIN information_schema.constraint_column_usage ccu
                  ON ccu.constraint_name = tc.constraint_name
                 AND ccu.table_schema = tc.table_schema
                WHERE tc.constraint_type = 'FOREIGN KEY'
                  AND tc.table_schema = 'public'
                  AND tc.table_name = ?
                  AND kcu.column_name = ?
                  AND ccu.table_name = ?
                  AND ccu.column_name = ?
                """)) {
            ps.setString(1, child);
            ps.setString(2, childCol);
            ps.setString(3, parent);
            ps.setString(4, parentCol);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    private static boolean constraintExists(Connection conn, String table, String constraint)
            throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                """
                SELECT 1 FROM information_schema.table_constraints
                WHERE table_schema = 'public' AND table_name = ? AND constraint_name = ?
                """)) {
            ps.setString(1, table);
            ps.setString(2, constraint);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    private static boolean columnExists(Connection conn, String table, String column)
            throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                """
                SELECT 1 FROM information_schema.columns
                WHERE table_schema = 'public' AND table_name = ? AND column_name = ?
                """)) {
            ps.setString(1, table.toLowerCase());
            ps.setString(2, column);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    private static long queryLong(Connection conn, String sql, String param) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, param);
            try (ResultSet rs = ps.executeQuery()) {
                assertTrue(rs.next());
                return rs.getLong(1);
            }
        }
    }

    private static int queryInt(Connection conn, String sql) throws SQLException {
        try (ResultSet rs = conn.createStatement().executeQuery(sql)) {
            assertTrue(rs.next());
            return rs.getInt(1);
        }
    }

    private static String queryString(Connection conn, String sql) throws SQLException {
        try (ResultSet rs = conn.createStatement().executeQuery(sql)) {
            assertTrue(rs.next());
            return rs.getString(1);
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
