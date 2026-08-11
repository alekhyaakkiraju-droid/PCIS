package com.pcis.reporting.support;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

public final class PostgresTestContainer {
  private static final DockerImageName PG17 =
      DockerImageName.parse("postgres:17-alpine").asCompatibleSubstituteFor("postgres");
  private static PostgreSQLContainer<?> container;

  private PostgresTestContainer() {}

  public static void registerReplicaProperties(DynamicPropertyRegistry registry) {
    PostgreSQLContainer<?> postgres = container();
    registry.add("pcis.reporting.datasource.url", postgres::getJdbcUrl);
    registry.add("pcis.reporting.datasource.username", postgres::getUsername);
    registry.add("pcis.reporting.datasource.password", postgres::getPassword);
    registry.add("spring.datasource.url", postgres::getJdbcUrl);
    registry.add("spring.datasource.username", postgres::getUsername);
    registry.add("spring.datasource.password", postgres::getPassword);
    registry.add("spring.flyway.enabled", () -> "true");
    registry.add("spring.flyway.locations", () -> "classpath:db/migration");
    registry.add("spring.flyway.baseline-on-migrate", () -> "true");
  }

  public static PostgreSQLContainer<?> container() {
    if (container == null) {
      container = new PostgreSQLContainer<>(PG17).withDatabaseName("pcis_reporting_test").withUsername("pcis").withPassword("pcis");
      container.start();
    }
    return container;
  }
}
