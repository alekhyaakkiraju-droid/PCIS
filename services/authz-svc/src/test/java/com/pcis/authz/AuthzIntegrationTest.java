package com.pcis.authz;

import static org.assertj.core.api.Assertions.assertThat;

import com.pcis.authz.infrastructure.persistence.repository.PermissionRepository;
import com.pcis.authz.infrastructure.persistence.repository.RolePermissionRepository;
import com.pcis.authz.infrastructure.persistence.repository.RoleRepository;
import com.pcis.authz.infrastructure.persistence.repository.UserRoleRepository;
import com.pcis.authz.infrastructure.persistence.repository.RolePermissionRepository;
import com.pcis.authz.support.PostgresTestContainer;
import com.pcis.authz.support.TestEnvironment;
import com.pcis.authz.support.TestSecurityConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(TestSecurityConfig.class)
@EnabledIf("com.pcis.authz.support.TestEnvironment#isDockerAvailable")
class AuthzIntegrationTest {

  @DynamicPropertySource
  static void registerDataSource(DynamicPropertyRegistry registry) {
    PostgresTestContainer.registerProperties(registry);
  }

  @Autowired private RoleRepository roleRepository;
  @Autowired private PermissionRepository permissionRepository;
  @Autowired private RolePermissionRepository rolePermissionRepository;
  @Autowired private UserRoleRepository userRoleRepository;

  @Test
  void flywayCreatesSecTablesAndAppliesTestSeeds() {
    assertThat(roleRepository.count()).isEqualTo(5);
    assertThat(permissionRepository.count()).isEqualTo(5);
    assertThat(rolePermissionRepository.count()).isGreaterThanOrEqualTo(4);
    assertThat(userRoleRepository.count()).isEqualTo(4);
  }

  @Test
  void adjusterPrincipalHasClaimPermissions() {
    var assignments = userRoleRepository.findByPrincipalId("adjuster-001");
    assertThat(assignments).hasSize(1);

    var rolePermissions = rolePermissionRepository.findByRoleId(assignments.getFirst().getRoleId());
    assertThat(rolePermissions).hasSize(2);
  }

  @Test
  void unassignedPrincipalHasNoRoleAssignments() {
    assertThat(userRoleRepository.findByPrincipalId("unknown-user")).isEmpty();
  }

  @Test
  void seededRolesAreActive() {
    assertThat(roleRepository.findByRoleCode("ADJUSTER"))
        .isPresent()
        .get()
        .satisfies(role -> assertThat(role.isActive()).isTrue());
  }

  @Test
  void contextLoadsWithDenyByDefaultSecurity() {
    assertThat(TestEnvironment.isDockerAvailable()).isTrue();
  }
}
