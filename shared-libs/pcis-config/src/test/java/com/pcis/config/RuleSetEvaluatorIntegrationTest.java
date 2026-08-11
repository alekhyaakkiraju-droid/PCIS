package com.pcis.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.pcis.config.rules.RuleSetEvaluator;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RuleSetEvaluatorIntegrationTest {

  private static final DockerImageName PG17 =
      DockerImageName.parse("postgres:17-alpine").asCompatibleSubstituteFor("postgres");

  private PostgreSQLContainer<?> container;
  private RuleSetEvaluator evaluator;

  @BeforeAll
  void startDatabase() {
    org.junit.jupiter.api.Assumptions.assumeTrue(
        DockerClientFactory.instance().isDockerAvailable(), "Docker unavailable");

    container =
        new PostgreSQLContainer<>(PG17)
            .withDatabaseName("pcis_config")
            .withUsername("pcis")
            .withPassword("pcis");
    container.start();

    DataSource dataSource =
        new DriverManagerDataSource(
            container.getJdbcUrl(), container.getUsername(), container.getPassword());
    SharedMigrationSupport.migrateSchemaAndConfig(dataSource);

    evaluator =
        SharedMigrationSupport.ruleSetEvaluator(dataSource, new PcisCodeTableProperties());
  }

  @Test
  void billingFrequencyIntervalsMatchCobolSemantics() {
    assertThat(evaluator.resolveBillingIntervalMonths("M").intervalMonths()).isEqualTo(1);
    assertThat(evaluator.resolveBillingIntervalMonths("Q").intervalMonths()).isEqualTo(3);
    assertThat(evaluator.resolveBillingIntervalMonths("S").intervalMonths()).isEqualTo(6);
    assertThat(evaluator.resolveBillingIntervalMonths("A").intervalMonths()).isEqualTo(12);
    assertThat(evaluator.resolveBillingIntervalMonths("X").intervalMonths()).isEqualTo(12);
    assertThat(evaluator.resolveBillingIntervalMonths("X").usedFallback()).isTrue();
  }

  @Test
  void delinquencyTransitionRuleSetIsLoaded() {
    assertThat(evaluator.delinquencyStatusTransitionRuleSet().resolveNextStatus("O", "LATE"))
        .contains("L");
    assertThat(evaluator.delinquencyStatusTransitionRuleSet().resolveNextStatus("L", "DELINQUENT"))
        .contains("D");
    assertThat(evaluator.delinquencyStatusTransitionRuleSet().resolveNextStatus("O", "VOID"))
        .contains("V");
  }
}
