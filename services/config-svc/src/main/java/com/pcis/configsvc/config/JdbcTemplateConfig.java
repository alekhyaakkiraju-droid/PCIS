package com.pcis.configsvc.config;

import javax.sql.DataSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * PcisConfigAutoConfiguration's TunableResolver bean requires a JdbcTemplate to already be
 * registered. Defining it explicitly here (a regular @Configuration, processed before deferred
 * auto-configuration imports) guarantees it is visible regardless of auto-configuration ordering.
 */
@Configuration
public class JdbcTemplateConfig {

  @Bean
  JdbcTemplate jdbcTemplate(DataSource dataSource) {
    return new JdbcTemplate(dataSource);
  }
}
