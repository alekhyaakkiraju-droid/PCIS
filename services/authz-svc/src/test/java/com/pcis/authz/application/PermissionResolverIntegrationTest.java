package com.pcis.authz.application;

import static org.assertj.core.api.Assertions.assertThat;

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

@SpringBootTest
@ActiveProfiles("test")
@Import(TestSecurityConfig.class)
@EnabledIf("com.pcis.authz.support.TestEnvironment#isDockerAvailable")
class PermissionResolverIntegrationTest {

  @DynamicPropertySource
  static void registerDataSource(DynamicPropertyRegistry registry) {
    PostgresTestContainer.registerProperties(registry);
  }

  @Autowired private PermissionResolver permissionResolver;

  @Test
  void resolvesAdjusterPermissionsFromSeedFixtures() {
    assertThat(permissionResolver.resolvePermissionCodes("adjuster-001"))
        .containsExactlyInAnyOrder("claim:read", "claim:pay");
  }

  @Test
  void unassignedPrincipalHasNoPermissions() {
    assertThat(permissionResolver.resolvePermissionCodes("unknown-user")).isEmpty();
  }
}
