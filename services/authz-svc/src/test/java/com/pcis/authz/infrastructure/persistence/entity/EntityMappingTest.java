package com.pcis.authz.infrastructure.persistence.entity;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class EntityMappingTest {

  @Test
  void roleEntityStoresFields() {
    var role = new RoleEntity();
    role.setRoleId(1L);
    role.setRoleCode("ADJUSTER");
    role.setRoleName("Claims Adjuster");
    role.setDescription("desc");
    role.setActive(true);

    assertThat(role.getRoleId()).isEqualTo(1L);
    assertThat(role.getRoleCode()).isEqualTo("ADJUSTER");
    assertThat(role.isActive()).isTrue();
  }

  @Test
  void permissionEntityStoresFields() {
    var permission = new PermissionEntity();
    permission.setPermissionId(2L);
    permission.setPermissionCode("claim:read");
    permission.setResource("claim");
    permission.setOperation("read");
    permission.setDescription("Read claims");

    assertThat(permission.getPermissionCode()).isEqualTo("claim:read");
    assertThat(permission.getResource()).isEqualTo("claim");
  }

  @Test
  void rolePermissionIdEqualityUsesCompositeKey() {
    var left = new RolePermissionId(1L, 2L);
    var right = new RolePermissionId(1L, 2L);
    var different = new RolePermissionId(1L, 3L);

    assertThat(left).isEqualTo(right).hasSameHashCodeAs(right);
    assertThat(left).isNotEqualTo(different);
  }

  @Test
  void rolePermissionEntityStoresCompositeKey() {
    var mapping = new RolePermissionEntity();
    mapping.setRoleId(1L);
    mapping.setPermissionId(2L);

    assertThat(mapping.getRoleId()).isEqualTo(1L);
    assertThat(mapping.getPermissionId()).isEqualTo(2L);
    assertThat(mapping.getRole()).isNull();
    assertThat(mapping.getPermission()).isNull();
  }

  @Test
  void permissionEntityStoresDescription() {
    var permission = new PermissionEntity();
    permission.setDescription("desc");
    assertThat(permission.getDescription()).isEqualTo("desc");
  }

  @Test
  void roleEntityStoresDescription() {
    var role = new RoleEntity();
    role.setDescription("desc");
    assertThat(role.getDescription()).isEqualTo("desc");
  }

  @Test
  void auditableEntityExposesAuditMetadata() throws Exception {
    var role = new RoleEntity();
    role.setRoleCode("AUDIT");

    var crtUser = RoleEntity.class.getSuperclass().getDeclaredField("crtUser");
    crtUser.setAccessible(true);
    crtUser.set(role, "SYSTEM");

    var crtTimestamp = RoleEntity.class.getSuperclass().getDeclaredField("crtTimestamp");
    crtTimestamp.setAccessible(true);
    crtTimestamp.set(role, java.time.Instant.parse("2026-01-01T00:00:00Z"));

    assertThat(role.getCrtUser()).isEqualTo("SYSTEM");
    assertThat(role.getCrtTimestamp()).isEqualTo(java.time.Instant.parse("2026-01-01T00:00:00Z"));

    var updUser = RoleEntity.class.getSuperclass().getDeclaredField("updUser");
    updUser.setAccessible(true);
    updUser.set(role, "ADMIN");

    var updTimestamp = RoleEntity.class.getSuperclass().getDeclaredField("updTimestamp");
    updTimestamp.setAccessible(true);
    updTimestamp.set(role, java.time.Instant.parse("2026-01-02T00:00:00Z"));

    assertThat(role.getUpdUser()).isEqualTo("ADMIN");
    assertThat(role.getUpdTimestamp()).isEqualTo(java.time.Instant.parse("2026-01-02T00:00:00Z"));
  }
}
