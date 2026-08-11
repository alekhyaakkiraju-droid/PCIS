package com.pcis.config;

import static org.assertj.core.api.Assertions.assertThat;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TunableResolverIntegrationTest {

  private static final DockerImageName PG17 =
      DockerImageName.parse("postgres:17-alpine").asCompatibleSubstituteFor("postgres");

  private PostgreSQLContainer<?> container;
  private TunableResolver resolver;

  @BeforeAll
  void startDatabase() {
    if (!DockerClientFactory.instance().isDockerAvailable()) {
      org.junit.jupiter.api.Assumptions.assumeTrue(false, "Docker unavailable");
    }
    container =
        new PostgreSQLContainer<>(PG17)
            .withDatabaseName("pcis_config")
            .withUsername("pcis")
            .withPassword("pcis");
    container.start();

    DataSource dataSource =
        new DriverManagerDataSource(container.getJdbcUrl(), container.getUsername(), container.getPassword());
    SharedMigrationSupport.migrateConfig(dataSource);

    PcisTunableProperties properties = new PcisTunableProperties();
    resolver =
        new TunableResolver(
            new TunableRepository(new JdbcTemplate(dataSource)), properties, new SimpleMeterRegistry());
    resolver.validateRequiredTunablesOnStartup();
  }

  @Test
  void loadsSeededTunablesFromDatabase() {
    assertThat(resolver.getInt(TunableKey.BILLING_LEAD_DAYS)).isEqualTo(15);
    assertThat(resolver.getBigDecimal(TunableKey.CLAIMS_REINSURANCE_CESSION_THRESHOLD))
        .isEqualByComparingTo("100000.00");
  }
}
