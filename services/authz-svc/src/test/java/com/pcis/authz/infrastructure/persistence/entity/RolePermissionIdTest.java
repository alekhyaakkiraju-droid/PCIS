package com.pcis.authz.infrastructure.persistence.entity;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class RolePermissionIdTest {

  @Test
  void defaultConstructorAndHashCode() {
    var id = new RolePermissionId();
    assertThat(id).isEqualTo(new RolePermissionId());
  }

  @Test
  void rejectsUnequalTypes() {
    var id = new RolePermissionId(1L, 2L);
    assertThat(id).isNotEqualTo("not-an-id");
  }
}
