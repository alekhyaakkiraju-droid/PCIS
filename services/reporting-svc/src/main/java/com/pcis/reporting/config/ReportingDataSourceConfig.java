package com.pcis.reporting.config;

import com.pcis.reporting.infrastructure.ReadOnlyDataSource;
import com.pcis.reporting.infrastructure.ReadOnlyViolationLogger;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
public class ReportingDataSourceConfig {
  public static final String REPORTING_DATASOURCE = "reportingDataSource";
  public static final String REPORTING_JDBC_TEMPLATE = "reportingJdbcTemplate";

  @Bean(name = REPORTING_DATASOURCE)
  @ConditionalOnProperty(prefix = "pcis.reporting.datasource", name = "url")
  DataSource reportingDataSource(
      ReportingDataSourceProperties properties, ReadOnlyViolationLogger violationLogger) {
    HikariConfig config = new HikariConfig();
    config.setPoolName("reporting-replica");
    config.setJdbcUrl(properties.url());
    config.setUsername(properties.username());
    config.setPassword(properties.password());
    config.setMaximumPoolSize(properties.maximumPoolSize());
    config.setConnectionTimeout(properties.connectionTimeoutMs());
    config.setReadOnly(true);
    config.setConnectionInitSql("SET default_transaction_read_only = on");
    return new ReadOnlyDataSource(new HikariDataSource(config), violationLogger);
  }

  @Bean(name = REPORTING_JDBC_TEMPLATE)
  @ConditionalOnProperty(prefix = "pcis.reporting.datasource", name = "url")
  JdbcTemplate reportingJdbcTemplate(@Qualifier(REPORTING_DATASOURCE) DataSource ds) {
    return new JdbcTemplate(ds);
  }
}
