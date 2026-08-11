package com.pcis.premium.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.pcis.premium.support.PostgresTestContainer;
import com.pcis.premium.support.TestEnvironment;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.util.StreamUtils;
import com.pcis.premium.infrastructure.RateTableRepository;

@EnabledIf("com.pcis.premium.support.TestEnvironment#isDockerAvailable")
class BaseRateServiceIntegrationTest {

  private BaseRateService baseRateService;

  @BeforeEach
  void setUp() throws Exception {
    var postgres = PostgresTestContainer.container();
    var dataSource =
        new DriverManagerDataSource(
            postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
    var jdbcClient = JdbcClient.create(dataSource);
    applySchema(jdbcClient);
    baseRateService = new BaseRateService(new RateTableRepository(jdbcClient));
  }

  @Test
  void homPolicyEndToEndMatchesHandCalculatedPremium() {
    var result =
        baseRateService.computeBasePremium("HOM", "COV1", "TX", LocalDate.parse("2026-06-01"));
    assertThat(result.basePremium()).isEqualByComparingTo("1119.20");
    assertThat(result.missingFactorTypes()).isEmpty();
  }

  private static void applySchema(JdbcClient jdbcClient) throws Exception {
    var migration =
        StreamUtils.copyToString(
            new ClassPathResource("db/migration/V1__premium_rating_tables.sql")
                .getInputStream(),
            StandardCharsets.UTF_8);
    jdbcClient.sql(migration).update();
    var fixtures =
        StreamUtils.copyToString(
            new ClassPathResource("fixtures/rate_data.sql").getInputStream(),
            StandardCharsets.UTF_8);
    jdbcClient.sql(fixtures).update();
  }
}
