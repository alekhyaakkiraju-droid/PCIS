package com.pcis.claims.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.pcis.claims.support.ClaimsTestSecurityConfig;
import com.pcis.claims.support.PostgresTestContainer;
import com.pcis.claims.support.TestEnvironment;
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

/**
 * Verifies that the Flyway V1 migration creates all 8 claims-domain tables with
 * the expected column types and constraints against a real PostgreSQL 17 container.
 */
@SpringBootTest
@ActiveProfiles("test")
@Import(ClaimsTestSecurityConfig.class)
@EnabledIf("com.pcis.claims.support.TestEnvironment#isDockerAvailable")
class FlywayMigrationTest {

  @DynamicPropertySource
  static void registerDataSource(DynamicPropertyRegistry registry) {
    PostgresTestContainer.registerProperties(registry);
  }

  @Autowired
  private JdbcTemplate jdbcTemplate;

  private static final List<String> EXPECTED_TABLES = List.of(
      "claim", "claim_reserve", "claim_payment", "claim_adjuster",
      "claim_note", "approval", "recovery", "outbox_events"
  );

  @Test
  void allEightTablesExist() {
    for (String table : EXPECTED_TABLES) {
      Integer count = jdbcTemplate.queryForObject(
          "SELECT COUNT(*) FROM information_schema.tables " +
          "WHERE table_schema = 'public' AND table_name = ?",
          Integer.class, table);
      assertThat(count).as("Table '%s' should exist", table).isEqualTo(1);
    }
  }

  @Test
  void monetaryColumnsAreNumeric() {
    List<String> monetaryCols = jdbcTemplate.queryForList(
        "SELECT table_name || '.' || column_name " +
        "FROM information_schema.columns " +
        "WHERE table_schema = 'public' " +
        "  AND data_type = 'numeric' " +
        "  AND numeric_precision = 11 " +
        "  AND numeric_scale = 2 " +
        "ORDER BY 1",
        String.class);
    assertThat(monetaryCols)
        .contains(
            "claim_adjuster.authority_limit",
            "claim_reserve.approved_amt",
            "claim_reserve.paid_to_date",
            "claim_payment.payment_amt",
            "recovery.recovery_amt");
  }

  @Test
  void outboxEventsPrimaryKeyIsUuid() {
    String dataType = jdbcTemplate.queryForObject(
        "SELECT data_type FROM information_schema.columns " +
        "WHERE table_schema = 'public' AND table_name = 'outbox_events' AND column_name = 'id'",
        String.class);
    assertThat(dataType).isEqualTo("uuid");
  }

  @Test
  void claimNbrIsVarchar12() {
    String dataType = jdbcTemplate.queryForObject(
        "SELECT character_maximum_length FROM information_schema.columns " +
        "WHERE table_schema = 'public' AND table_name = 'claim' AND column_name = 'claim_nbr'",
        Integer.class).toString();
    assertThat(dataType).isEqualTo("12");
  }

  @Test
  void noFloatColumnsInClaimsDomain() {
    Integer floatCount = jdbcTemplate.queryForObject(
        "SELECT COUNT(*) FROM information_schema.columns " +
        "WHERE table_schema = 'public' " +
        "  AND data_type IN ('real', 'double precision', 'float') " +
        "  AND table_name IN (" +
        "    'claim','claim_reserve','claim_payment','claim_adjuster'," +
        "    'claim_note','approval','recovery','outbox_events')",
        Integer.class);
    assertThat(floatCount).as("No floating-point columns allowed in claims domain").isZero();
  }
}
