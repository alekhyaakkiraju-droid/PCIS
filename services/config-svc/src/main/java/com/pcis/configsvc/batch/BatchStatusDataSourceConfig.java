package com.pcis.configsvc.batch;

import com.zaxxer.hikari.HikariDataSource;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Read-only JDBC access to the Spring Batch metadata tables (BATCH_JOB_EXECUTION,
 * BATCH_STEP_EXECUTION) in each batch module's own database. These are standard
 * spring-batch-managed tables, not owned by config-svc.
 */
@Configuration
public class BatchStatusDataSourceConfig {

  @Bean
  BatchStatusJdbcTemplates batchStatusJdbcTemplates(
      @Value("${pcis.batch-status.audit-db-url:jdbc:postgresql://localhost:5434/pcis_audit}")
          String auditDbUrl,
      @Value("${pcis.batch-status.claims-db-url:jdbc:postgresql://localhost:5434/pcis_claims}")
          String claimsDbUrl,
      @Value(
              "${pcis.batch-status.policy-db-url:jdbc:postgresql://localhost:5434/pcis_policy_batch}")
          String policyDbUrl,
      @Value("${pcis.batch-status.db-username:pcis}") String username,
      @Value("${pcis.batch-status.db-password:pcis}") String password) {
    Map<String, JdbcTemplate> templates = new LinkedHashMap<>();
    templates.put("audit", new JdbcTemplate(dataSource(auditDbUrl, username, password)));
    templates.put("claims", new JdbcTemplate(dataSource(claimsDbUrl, username, password)));
    templates.put("policy", new JdbcTemplate(dataSource(policyDbUrl, username, password)));
    return new BatchStatusJdbcTemplates(templates);
  }

  private DataSource dataSource(String url, String username, String password) {
    HikariDataSource ds = new HikariDataSource();
    ds.setJdbcUrl(url);
    ds.setUsername(username);
    ds.setPassword(password);
    ds.setMaximumPoolSize(2);
    ds.setPoolName("batch-status-" + url.substring(url.lastIndexOf('/') + 1));
    return ds;
  }
}
