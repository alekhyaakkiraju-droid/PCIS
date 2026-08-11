package com.pcis.masking.scanner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.logging.Logger;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

@Tag("integration")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PiiScannerIT {

  private static final DockerImageName PG17 =
      DockerImageName.parse("postgres:17-alpine").asCompatibleSubstituteFor("postgres");

  private PostgreSQLContainer<?> container;
  private AuditTablePiiScanner auditScanner;

  @BeforeAll
  void startDatabase() throws Exception {
    assumeTrue(DockerClientFactory.instance().isDockerAvailable(), "Docker required");
    container =
        new PostgreSQLContainer<>(PG17)
            .withDatabaseName("pcis_pii_scan")
            .withUsername("pcis")
            .withPassword("pcis");
    container.start();

    try (Connection conn =
            DriverManager.getConnection(
                container.getJdbcUrl(), container.getUsername(), container.getPassword());
        Statement st = conn.createStatement()) {
      st.execute("DROP SCHEMA public CASCADE");
      st.execute("CREATE SCHEMA public");
    }

    Path schema =
        Path.of("src/test/resources/schema/audit_log_minimal.sql").toAbsolutePath().normalize();
    try (Connection conn =
            DriverManager.getConnection(
                container.getJdbcUrl(), container.getUsername(), container.getPassword());
        Statement st = conn.createStatement()) {
      st.execute(Files.readString(schema));
    }

    auditScanner = new AuditTablePiiScanner();
  }

  @Test
  void detectsUnmaskedAuditRowsAndWritesReport() throws Exception {
    runSqlResource("pii-scan-fixtures.sql");

    var report =
        auditScanner.scanAuditTables(dataSource(), List.of("AUDIT_LOG_T", "AUDIT_LOG_ARCHIVE_T"));

    Path reportPath = Path.of("build/reports/pii-scan-results.json");
    Files.createDirectories(reportPath.getParent());
    Files.writeString(reportPath, report.toJson());

    assertThat(report.overallStatus()).isEqualTo(PiiScanReport.ScanStatus.FAIL);
    assertThat(report.detections()).isNotEmpty();
    assertThat(report.detections())
        .anyMatch(
            detection ->
                "AUDIT_LOG_T".equals(detection.source())
                    && "OLD_VALUE".equals(detection.location())
                    && detection.patternType() == PiiPattern.SSN_DASHED);
    assertThat(Files.readString(reportPath)).contains("\"overallStatus\" : \"FAIL\"");
  }

  @Test
  void passesWhenOnlyMaskedRowsPresent() throws Exception {
    runSqlResource("pii-scan-fixtures-masked-only.sql");

    var report = auditScanner.scanAuditTables(dataSource(), List.of("AUDIT_LOG_T"));

    assertThat(report.overallStatus()).isEqualTo(PiiScanReport.ScanStatus.PASS);
    assertThat(report.detections()).isEmpty();
  }

  private void runSqlResource(String resourceName) throws Exception {
    String sql =
        Files.readString(
            Path.of("src/test/resources/" + resourceName).toAbsolutePath().normalize());
    try (Connection conn =
            DriverManager.getConnection(
                container.getJdbcUrl(), container.getUsername(), container.getPassword());
        Statement st = conn.createStatement()) {
      for (String statement : sql.split(";")) {
        if (!statement.isBlank()) {
          st.execute(statement.trim());
        }
      }
    }
  }

  private DataSource dataSource() {
    return new DataSource() {
      @Override
      public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(
            container.getJdbcUrl(), container.getUsername(), container.getPassword());
      }

      @Override
      public Connection getConnection(String username, String password) throws SQLException {
        return DriverManager.getConnection(container.getJdbcUrl(), username, password);
      }

      @Override
      public java.io.PrintWriter getLogWriter() {
        return null;
      }

      @Override
      public void setLogWriter(java.io.PrintWriter out) {}

      @Override
      public void setLoginTimeout(int seconds) {}

      @Override
      public int getLoginTimeout() {
        return 0;
      }

      @Override
      public Logger getParentLogger() {
        return Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
      }

      @Override
      public <T> T unwrap(Class<T> iface) throws SQLException {
        throw new SQLException("Not a wrapper");
      }

      @Override
      public boolean isWrapperFor(Class<?> iface) {
        return false;
      }
    };
  }
}
