package com.pcis.sync.config;

import com.zaxxer.hikari.HikariDataSource;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.flyway.FlywayDataSource;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
@EnableConfigurationProperties(SyncAgentProperties.class)
public class DataSourceConfig {

  @Bean
  @Primary
  @FlywayDataSource
  @Qualifier("targetDataSource")
  public DataSource targetDataSource(
      org.springframework.boot.autoconfigure.jdbc.DataSourceProperties properties) {
    return properties.initializeDataSourceBuilder().type(HikariDataSource.class).build();
  }

  @Bean
  @Qualifier("sourceDataSource")
  public DataSource sourceDataSource(SyncAgentProperties properties) {
    SyncAgentProperties.SourceProperties source = properties.getSource();
    HikariDataSource dataSource = new HikariDataSource();
    dataSource.setJdbcUrl(source.getUrl());
    dataSource.setUsername(source.getUsername());
    dataSource.setPassword(source.getPassword());
    dataSource.setDriverClassName(source.getDriverClassName());
    dataSource.setMaximumPoolSize(4);
    dataSource.setPoolName("sync-source");
    return dataSource;
  }

  @Bean
  @Primary
  public JdbcTemplate targetJdbcTemplate(@Qualifier("targetDataSource") DataSource targetDataSource) {
    return new JdbcTemplate(targetDataSource);
  }

  @Bean
  public JdbcTemplate sourceJdbcTemplate(@Qualifier("sourceDataSource") DataSource sourceDataSource) {
    return new JdbcTemplate(sourceDataSource);
  }
}
