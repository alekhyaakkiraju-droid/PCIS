package com.pcis.customer.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.pcis.customer.support.PostgresTestContainer;
import com.pcis.customer.support.TestEnvironment;
import com.pcis.customer.support.TestSecurityConfig;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestSecurityConfig.class)
@EnabledIf("com.pcis.customer.support.TestEnvironment#isDockerAvailable")
class FlywayMigrationTest {

  @DynamicPropertySource
  static void registerDataSource(DynamicPropertyRegistry registry) {
    PostgresTestContainer.registerProperties(registry);
  }

  @Autowired private JdbcTemplate jdbcTemplate;

  private static final List<String> EXPECTED_TABLES =
      List.of("customer", "customer_address", "customer_contact", "outbox_events");

  @Test
  void allCustomerDomainTablesExist() {
    for (String table : EXPECTED_TABLES) {
      Integer count =
          jdbcTemplate.queryForObject(
              "SELECT COUNT(*) FROM information_schema.tables "
                  + "WHERE table_schema = 'public' AND table_name = ?",
              Integer.class,
              table);
      assertThat(count).as("Table '%s' should exist", table).isEqualTo(1);
    }
  }

  @Test
  void customerTaxIdHasPartialUniqueIndex() {
    Integer count =
        jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM pg_indexes "
                + "WHERE schemaname = 'public' AND tablename = 'customer' "
                + "AND indexname = 'uq_customer_tax_id'",
            Integer.class);
    assertThat(count).isEqualTo(1);
  }

  @Test
  void outboxEventsPrimaryKeyIsUuid() {
    String dataType =
        jdbcTemplate.queryForObject(
            "SELECT data_type FROM information_schema.columns "
                + "WHERE table_schema = 'public' AND table_name = 'outbox_events' "
                + "AND column_name = 'id'",
            String.class);
    assertThat(dataType).isEqualTo("uuid");
  }
}
