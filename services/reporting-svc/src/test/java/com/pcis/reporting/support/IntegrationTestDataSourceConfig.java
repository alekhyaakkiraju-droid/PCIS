package com.pcis.reporting.support;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import javax.sql.DataSource;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/**
 * Writable primary datasource for Flyway migrations in integration tests while the reporting
 * replica bean remains read-only.
 */
@TestConfiguration
@ConditionalOnProperty(prefix = "pcis.reporting.datasource", name = "url")
public class IntegrationTestDataSourceConfig {

  @Bean
  @Primary
  DataSource primaryDataSource(
      org.springframework.core.env.Environment environment) {
    HikariConfig config = new HikariConfig();
    config.setPoolName("test-primary");
    config.setJdbcUrl(environment.getRequiredProperty("spring.datasource.url"));
    config.setUsername(environment.getRequiredProperty("spring.datasource.username"));
    config.setPassword(environment.getRequiredProperty("spring.datasource.password"));
    return new HikariDataSource(config);
  }
}
