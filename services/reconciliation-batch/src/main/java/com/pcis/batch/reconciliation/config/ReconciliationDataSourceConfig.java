package com.pcis.batch.reconciliation.config;

import com.zaxxer.hikari.HikariDataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Objects;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
public class ReconciliationDataSourceConfig {

  @Bean
  @Primary
  @ConfigurationProperties("spring.datasource")
  DataSourceProperties primaryDataSourceProperties() {
    return new DataSourceProperties();
  }

  @Bean
  @Primary
  DataSource dataSource(DataSourceProperties primaryDataSourceProperties) {
    return primaryDataSourceProperties.initializeDataSourceBuilder().build();
  }

  @Bean
  DataSource readReplicaDataSource(
      ReconciliationProperties reconciliationProperties,
      DataSource primaryDataSource) {
    ReconciliationProperties.ReadReplicaProperties replica =
        reconciliationProperties.getReadReplica();
    if (Objects.equals(replica.getUrl(), primaryJdbcUrl(primaryDataSource))) {
      return new ReadOnlyDataSourceWrapper(primaryDataSource);
    }
    HikariDataSource replicaDataSource = new HikariDataSource();
    replicaDataSource.setJdbcUrl(replica.getUrl());
    replicaDataSource.setUsername(replica.getUsername());
    replicaDataSource.setPassword(replica.getPassword());
    replicaDataSource.setReadOnly(true);
    replicaDataSource.setPoolName("recon-read-replica");
    return replicaDataSource;
  }

  @Bean
  JdbcTemplate readReplicaJdbcTemplate(
      @Qualifier("readReplicaDataSource") DataSource readReplicaDataSource) {
    JdbcTemplate jdbcTemplate = new JdbcTemplate(readReplicaDataSource);
    jdbcTemplate.setQueryTimeout(30);
    return jdbcTemplate;
  }

  @Bean
  @Primary
  PlatformTransactionManager transactionManager(DataSource dataSource) {
    return new DataSourceTransactionManager(dataSource);
  }

  private static String primaryJdbcUrl(DataSource dataSource) {
    if (dataSource instanceof HikariDataSource hikariDataSource) {
      return hikariDataSource.getJdbcUrl();
    }
    return null;
  }

  /** Wraps the primary datasource with read-only connections for test/local parity. */
  static final class ReadOnlyDataSourceWrapper implements DataSource {

    private final DataSource delegate;

    ReadOnlyDataSourceWrapper(DataSource delegate) {
      this.delegate = delegate;
    }

    @Override
    public Connection getConnection() throws SQLException {
      Connection connection = delegate.getConnection();
      connection.setReadOnly(true);
      return connection;
    }

    @Override
    public Connection getConnection(String username, String password) throws SQLException {
      Connection connection = delegate.getConnection(username, password);
      connection.setReadOnly(true);
      return connection;
    }

    @Override
    public java.io.PrintWriter getLogWriter() throws SQLException {
      return delegate.getLogWriter();
    }

    @Override
    public void setLogWriter(java.io.PrintWriter out) throws SQLException {
      delegate.setLogWriter(out);
    }

    @Override
    public void setLoginTimeout(int seconds) throws SQLException {
      delegate.setLoginTimeout(seconds);
    }

    @Override
    public int getLoginTimeout() throws SQLException {
      return delegate.getLoginTimeout();
    }

    @Override
    public java.util.logging.Logger getParentLogger() {
      return java.util.logging.Logger.getLogger(ReadOnlyDataSourceWrapper.class.getName());
    }

    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
      return delegate.unwrap(iface);
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) throws SQLException {
      return delegate.isWrapperFor(iface);
    }
  }
}
