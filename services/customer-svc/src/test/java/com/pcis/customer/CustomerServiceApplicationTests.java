package com.pcis.customer;

import static org.assertj.core.api.Assertions.assertThat;

import com.pcis.customer.support.PostgresTestContainer;
import com.pcis.customer.support.TestEnvironment;
import com.pcis.customer.support.TestSecurityConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * Verifies the Spring context loads with Testcontainers PostgreSQL 17, Flyway migration
 * executes successfully, and HikariCP obtains a valid connection.
 * Skipped when Docker is unavailable (e.g., lightweight CI executors without DinD).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(TestSecurityConfig.class)
@EnabledIf("com.pcis.customer.support.TestEnvironment#isDockerAvailable")
class CustomerServiceApplicationTests {

  @DynamicPropertySource
  static void registerDataSource(DynamicPropertyRegistry registry) {
    PostgresTestContainer.registerProperties(registry);
  }

  @Autowired
  private ApplicationContext applicationContext;

  @Test
  void contextLoadsWithPostgres() {
    assertThat(TestEnvironment.isDockerAvailable()).isTrue();
  }

  @Test
  void customerServiceApplicationBeanIsPresent() {
    assertThat(applicationContext.containsBean("customerServiceApplication")).isTrue();
  }

  @Test
  void securityFilterChainBeanIsPresent() {
    assertThat(applicationContext.containsBean("securityFilterChain")).isTrue();
  }

  @Test
  void correlationIdFilterBeanIsPresent() {
    assertThat(applicationContext.containsBean("correlationIdFilter")).isTrue();
  }
}
