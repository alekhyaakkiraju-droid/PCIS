package com.pcis.authz;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

class FlywayMigrationTest {

  @Test
  void versionedMigrationsDefineSecTables() throws IOException {
    assertMigrationContains("db/migration/V1__create_roles.sql", "CREATE TABLE roles");
    assertMigrationContains("db/migration/V2__create_permissions.sql", "CREATE TABLE permissions");
    assertMigrationContains("db/migration/V3__create_role_permission.sql", "CREATE TABLE role_permission");
    assertMigrationContains("db/migration/V4__create_user_role.sql", "CREATE TABLE user_role");
    assertMigrationContains("db/migration/V5__create_outbox_events.sql", "CREATE TABLE outbox_events");
    assertMigrationContains("db/migration/V6__create_claim_authority_tables.sql", "CREATE TABLE APPROVAL_T");
  }

  @Test
  void migrationsIncludeAuditColumns() throws IOException {
    assertMigrationContains("db/migration/V1__create_roles.sql", "crt_user");
    assertMigrationContains("db/migration/V1__create_roles.sql", "upd_timestamp");
  }

  private static void assertMigrationContains(String path, String fragment) throws IOException {
    var resource = new ClassPathResource(path);
    var sql = resource.getContentAsString(StandardCharsets.UTF_8);
    assertThat(sql).contains(fragment);
  }
}
