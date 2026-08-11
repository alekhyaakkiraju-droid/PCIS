package com.pcis.audit;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

class FlywayMigrationTest {

  @Test
  void v1CreatesPartitionedAuditLog() throws IOException {
    assertMigrationContains("db/migration/V1__create_audit_log.sql", "PARTITION BY RANGE");
    assertMigrationContains("db/migration/V1__create_audit_log.sql", "GENERATED ALWAYS AS IDENTITY");
    assertMigrationContains("db/migration/V1__create_audit_log.sql", "REVOKE UPDATE, DELETE");
  }

  @Test
  void v2DefinesPartitionMaintenanceFunction() throws IOException {
    assertMigrationContains(
        "db/migration/V2__partition_maintenance.sql", "maintain_audit_log_partitions");
  }

  private static void assertMigrationContains(String path, String fragment) throws IOException {
    var resource = new ClassPathResource(path);
    var sql = resource.getContentAsString(StandardCharsets.UTF_8);
    assertThat(sql).contains(fragment);
  }
}
