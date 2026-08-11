package com.pcis.config;

import static org.assertj.core.api.Assertions.assertThat;

import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CodeTableServiceIntegrationTest {

  private static final DockerImageName PG17 =
      DockerImageName.parse("postgres:17-alpine").asCompatibleSubstituteFor("postgres");

  private PostgreSQLContainer<?> container;
  private CodeTableService service;

  @BeforeAll
  void startDatabase() {
    org.junit.jupiter.api.Assumptions.assumeTrue(
        DockerClientFactory.instance().isDockerAvailable(), "Docker unavailable");

    container =
        new PostgreSQLContainer<>(PG17)
            .withDatabaseName("pcis_config")
            .withUsername("pcis")
            .withPassword("pcis");
    container.start();

    DataSource dataSource =
        new DriverManagerDataSource(
            container.getJdbcUrl(), container.getUsername(), container.getPassword());
    SharedMigrationSupport.migrateSchemaAndConfig(dataSource);

    service = new CodeTableService(new CodeTableRepository(new JdbcTemplate(dataSource)), new PcisCodeTableProperties());
  }

  @Test
  void loadsSeededCodeTableRowsFromPcisSchema() {
    assertThat(service.lookup(CodeDomain.BILL_SCHED_STATUS, "V").description()).isEqualTo("Void");
    assertThat(service.validateMembership(CodeDomain.RESERVE_STATUS, "AP")).isTrue();
    assertThat(service.listByDomain(CodeDomain.CLAIM_TYPE))
        .extracting(CodeTableEntry::codeValue)
        .contains("AUTO", "PROP", "LIAB");
  }

  @Test
  void loadsCancellationReasonCodesFromSchemaSeed() {
    assertThat(service.lookup(CodeDomain.CANCEL_REASON, "NPAY").description())
        .isEqualTo("Non-payment of premium");
  }
}
