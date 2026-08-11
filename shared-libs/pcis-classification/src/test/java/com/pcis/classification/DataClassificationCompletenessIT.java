package com.pcis.classification;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

@Tag("integration")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DataClassificationCompletenessIT {

  private static final DockerImageName PG17 =
      DockerImageName.parse("postgres:17-alpine").asCompatibleSubstituteFor("postgres");

  private PostgreSQLContainer<?> container;
  private DataClassificationRegistry registry;

  @BeforeAll
  void startPostgresAndLoadRegistry() throws Exception {
    assumeTrue(DockerClientFactory.instance().isDockerAvailable(), "Docker required");
    container = new PostgreSQLContainer<>(PG17).withDatabaseName("pcis").withUsername("pcis").withPassword("pcis");
    container.start();

    try (Connection conn =
        DriverManager.getConnection(
            container.getJdbcUrl(), container.getUsername(), container.getPassword());
        var st = conn.createStatement()) {
      st.execute("DROP SCHEMA public CASCADE");
      st.execute("CREATE SCHEMA public");
    }

    Path migrations = Path.of("../pcis-schema/db/migration").toAbsolutePath().normalize();
    Flyway.configure()
        .dataSource(container.getJdbcUrl(), container.getUsername(), container.getPassword())
        .locations("filesystem:" + migrations)
        .load()
        .migrate();

    Path registryPath =
        Path.of("../../config/pcis-data-classification.yaml").toAbsolutePath().normalize();
    registry =
        InMemoryDataClassificationRegistry.fromDocument(ClassificationRegistryParser.parse(registryPath))
            .registry();
  }

  @Test
  void currentSchemaMatchesProductionRegistry() throws Exception {
    CompletenessReport report =
        ClassificationCompletenessChecker.checkCompleteness(loadBaseTables(), registry);
    ClassificationCompletenessReporter.writeReport(
        report, Path.of("build/reports/classification-completeness.json"));
    assertTrue(
        report.passed(),
        () -> ClassificationCompletenessChecker.formatFailureMessage(report));
  }

  @Test
  void unclassifiedTableFailsGate() throws SQLException {
    try (Connection conn =
            DriverManager.getConnection(
                container.getJdbcUrl(), container.getUsername(), container.getPassword());
        var st = conn.createStatement()) {
      st.execute("CREATE TABLE gate_probe_unclassified (id INT PRIMARY KEY)");
    }
    CompletenessReport report =
        ClassificationCompletenessChecker.checkCompleteness(loadBaseTables(), registry);
    assertFalse(report.passed());
    assertTrue(report.unclassifiedTables().contains("GATE_PROBE_UNCLASSIFIED"));
  }

  private Set<String> loadBaseTables() throws SQLException {
    Set<String> tables = new HashSet<>();
    try (Connection conn =
            DriverManager.getConnection(
                container.getJdbcUrl(), container.getUsername(), container.getPassword());
        ResultSet rs =
            conn.createStatement()
                .executeQuery(
                    """
                    SELECT c.relname
                    FROM pg_class c
                    JOIN pg_namespace n ON n.oid = c.relnamespace
                    WHERE n.nspname = 'public'
                      AND c.relkind IN ('r', 'p')
                      AND NOT EXISTS (SELECT 1 FROM pg_inherits i WHERE i.inhrelid = c.oid)
                    """)) {
      while (rs.next()) {
        String name = rs.getString(1).toUpperCase();
        if (!ClassificationCompletenessChecker.EXCLUDED_SYSTEM_TABLES.contains(name)) {
          tables.add(name);
        }
      }
    }
    return tables;
  }
}
