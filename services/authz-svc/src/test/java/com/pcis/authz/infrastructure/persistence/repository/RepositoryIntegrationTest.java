package com.pcis.authz.infrastructure.persistence.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.pcis.authz.infrastructure.persistence.entity.PermissionEntity;
import com.pcis.authz.infrastructure.persistence.entity.RoleEntity;
import com.pcis.authz.infrastructure.persistence.entity.RolePermissionEntity;
import com.pcis.authz.infrastructure.persistence.entity.UserRoleEntity;
import com.pcis.authz.support.PostgresTestContainer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import com.pcis.authz.config.AuditorAwareImpl;
import com.pcis.authz.config.JpaAuditingConfig;

@DataJpaTest
@ActiveProfiles("test")
@EnabledIf("com.pcis.authz.support.TestEnvironment#isDockerAvailable")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({JpaAuditingConfig.class, AuditorAwareImpl.class})
class RepositoryIntegrationTest {

  @DynamicPropertySource
  static void registerDataSource(DynamicPropertyRegistry registry) {
    PostgresTestContainer.registerProperties(registry);
    registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
    registry.add("spring.flyway.enabled", () -> "true");
  }

  @Autowired private RoleRepository roleRepository;
  @Autowired private PermissionRepository permissionRepository;
  @Autowired private RolePermissionRepository rolePermissionRepository;
  @Autowired private UserRoleRepository userRoleRepository;

  @Test
  void repositoriesPersistAndLoadEntities() {
    var role = new RoleEntity();
    role.setRoleCode("TEST_ROLE");
    role.setRoleName("Test Role");
    role.setDescription("Integration test role");
    role.setActive(true);
    role = roleRepository.saveAndFlush(role);

    var permission = new PermissionEntity();
    permission.setPermissionCode("test:read");
    permission.setResource("test");
    permission.setOperation("read");
    permission = permissionRepository.saveAndFlush(permission);

    var mapping = new RolePermissionEntity();
    mapping.setRoleId(role.getRoleId());
    mapping.setPermissionId(permission.getPermissionId());
    rolePermissionRepository.saveAndFlush(mapping);

    var userRole = new UserRoleEntity();
    userRole.setPrincipalId("test-principal");
    userRole.setRoleId(role.getRoleId());
    userRoleRepository.saveAndFlush(userRole);

    assertThat(roleRepository.findByRoleCode("TEST_ROLE")).isPresent();
    assertThat(permissionRepository.findByPermissionCode("test:read")).isPresent();
    assertThat(rolePermissionRepository.findByRoleId(role.getRoleId())).hasSize(1);
    assertThat(userRoleRepository.findByPrincipalId("test-principal")).hasSize(1);
  }
}
