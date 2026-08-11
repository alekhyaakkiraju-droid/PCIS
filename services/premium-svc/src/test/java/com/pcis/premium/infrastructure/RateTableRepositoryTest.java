package com.pcis.premium.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.pcis.premium.support.PostgresTestContainer;
import com.pcis.premium.support.TestEnvironment;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.util.StreamUtils;

@EnabledIf("com.pcis.premium.support.TestEnvironment#isDockerAvailable")
class RateTableRepositoryTest {

  private RateTableRepository repository;

  @BeforeEach
  void setUp() throws Exception {
    var postgres = PostgresTestContainer.container();
    var dataSource =
        new DriverManagerDataSource(
            postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
    var jdbcClient = JdbcClient.create(dataSource);
    repository = new RateTableRepository(jdbcClient);

    applyMigrationsAndFixtures(jdbcClient);
  }

  @Test
  void loadsFactorsInSingleSetBasedQuery() {
    var factors = repository.loadFactorsForPolicyType("HOME");
    assertThat(factors).hasSize(1);
    assertThat(factors.getFirst().factorCode()).isEqualTo("OCCUPANCY");
  }

  @Test
  void findsEffectiveBaseRateForPolicyTypeAndTerritory() {
    var rateTable = repository.findEffectiveRateTable("HOME", "TX");
    assertThat(rateTable).isPresent();
    assertThat(rateTable.get().baseRate()).isEqualByComparingTo("1200.00");
    assertThat(rateTable.get().policyType()).isEqualTo("HOME");
    assertThat(rateTable.get().territory()).isEqualTo("TX");
  }

  @Test
  void loadsFactorsForSpecificRateTable() {
    var rateTableId = repository.findEffectiveRateTable("HOME", "TX").orElseThrow().rateTableId();
    var factors = repository.loadFactorsForRateTable(rateTableId);
    assertThat(factors).hasSize(1);
    assertThat(factors.getFirst().factorValue()).isEqualByComparingTo("1.0500");
  }

  @Test
  void lookupBaseRateAndFactorsEndToEnd() {
    var rateTable = repository.findEffectiveRateTable("HOME", "TX").orElseThrow();
    var factors = repository.loadFactorsForRateTable(rateTable.rateTableId());
    var combined =
        factors.stream()
            .map(RateTableRepository.RateFactorRow::factorValue)
            .reduce(java.math.BigDecimal.ONE, java.math.BigDecimal::multiply);
    var basePremium = rateTable.baseRate().multiply(combined).setScale(2, java.math.RoundingMode.HALF_UP);
    assertThat(basePremium).isEqualByComparingTo("1260.00");
  }

  private static void applyMigrationsAndFixtures(JdbcClient jdbcClient) throws Exception {
    jdbcClient.sql("DROP SCHEMA IF EXISTS public CASCADE").update();
    jdbcClient.sql("CREATE SCHEMA public").update();
    String migration =
        StreamUtils.copyToString(
            new ClassPathResource("db/migration/V1__premium_rating_tables.sql").getInputStream(),
            StandardCharsets.UTF_8);
    jdbcClient.sql(migration).update();

    String fixtures =
        StreamUtils.copyToString(
            new ClassPathResource("test-data/premium-rating-fixtures.sql").getInputStream(),
            StandardCharsets.UTF_8);
    for (String statement : fixtures.split(";")) {
      if (!statement.isBlank()) {
        jdbcClient.sql(statement).update();
      }
    }
  }
}
