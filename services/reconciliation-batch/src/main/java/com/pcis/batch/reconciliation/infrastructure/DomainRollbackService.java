package com.pcis.batch.reconciliation.infrastructure;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;

@Service
public class DomainRollbackService {

  private final JdbcTemplate jdbcTemplate;

  public DomainRollbackService(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  public RollbackResult rollbackDomain(String domain) {
    String scriptPath = "rollback/" + domain.toLowerCase() + "_shadow_cleanup.sql";
    ClassPathResource resource = new ClassPathResource(scriptPath);
    if (!resource.exists()) {
      throw new IllegalArgumentException("No rollback script for domain: " + domain);
    }
    try {
      String sql = StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
      int statementsExecuted = 0;
      for (String statement : sql.split(";")) {
        String trimmed = statement.trim();
        if (trimmed.isEmpty() || trimmed.startsWith("--")) {
          continue;
        }
        jdbcTemplate.execute(trimmed);
        statementsExecuted++;
      }
      return new RollbackResult(domain, statementsExecuted, "Shadow data cleanup completed");
    } catch (IOException ex) {
      throw new IllegalStateException("Failed to read rollback script: " + scriptPath, ex);
    }
  }

  public record RollbackResult(String domain, int statementsExecuted, String message) {}
}
