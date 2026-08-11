package com.pcis.audit;

import static org.assertj.core.api.Assertions.assertThat;

import com.pcis.audit.support.PostgresTestContainer;
import com.pcis.audit.support.TestEnvironment;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(com.pcis.audit.support.TestSecurityConfig.class)
@EnabledIf("com.pcis.audit.support.TestEnvironment#isDockerAvailable")
class AuditApplicationTest {

  @DynamicPropertySource
  static void registerDataSource(DynamicPropertyRegistry registry) {
    PostgresTestContainer.registerProperties(registry);
  }

  @Test
  void contextLoadsWithPostgres() {
    assertThat(TestEnvironment.isDockerAvailable()).isTrue();
  }
}
