package com.pcis.premium.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pcis.batch.common.OutboxEventWriter;
import com.pcis.premium.config.PremiumRatingProperties;
import com.pcis.premium.domain.RatingOutcome;
import com.pcis.premium.infrastructure.DiscountRuleRepository;
import com.pcis.premium.infrastructure.PremiumCalcRepository;
import com.pcis.premium.infrastructure.RateTableRepository;
import com.pcis.premium.infrastructure.SurchargeRuleRepository;
import com.pcis.premium.infrastructure.TaxTableRepository;
import com.pcis.premium.infrastructure.UwRuleRepository;
import com.pcis.premium.model.RatingRequest;
import com.pcis.premium.model.UnderwritingDecision;
import com.pcis.premium.support.PostgresTestContainer;
import com.pcis.premium.support.TestEnvironment;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.util.StreamUtils;

@EnabledIf("com.pcis.premium.support.TestEnvironment#isDockerAvailable")
class RatingPipelineOrchestratorIntegrationTest {

  private RatingPipelineOrchestrator orchestrator;
  private JdbcClient jdbcClient;

  @BeforeEach
  void setUp() throws Exception {
    var postgres = PostgresTestContainer.container();
    var dataSource =
        new DriverManagerDataSource(
            postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
    jdbcClient = JdbcClient.create(dataSource);
    applySql("db/migration/V1__premium_rating_tables.sql");
    applySql("db/migration/V2__rating_pipeline_extensions.sql");
    applySql("fixtures/rating_pipeline_data.sql");

    PremiumRatingProperties properties = new PremiumRatingProperties();
    BaseRateService baseRateService = new BaseRateService(new RateTableRepository(jdbcClient));
    orchestrator =
        new RatingPipelineOrchestrator(
            baseRateService,
            new RiskScoreService(),
            new UnderwritingRuleService(new UwRuleRepository(jdbcClient)),
            new DiscountService(new DiscountRuleRepository(jdbcClient), properties),
            new SurchargeService(new SurchargeRuleRepository(jdbcClient), properties),
            new TaxService(new TaxTableRepository(jdbcClient)),
            new InstallmentDivisionService(),
            new PremiumCalcRepository(jdbcClient),
            new OutboxEventWriter(
                new org.springframework.jdbc.core.JdbcTemplate(dataSource),
                new ObjectMapper(),
                "TEST"),
            properties);
  }

  @Test
  void fullPipelinePersistsCalculationAndOutbox() {
    RatingRequest request =
        new RatingRequest(
            "HOME",
            "HO3",
            "TX",
            "TX",
            LocalDate.parse("2026-06-01"),
            new BigDecimal("250000.00"),
            null,
            "HO1234567890",
            "A",
            new RatingRequest.CustomerRiskData(40, 0, 700),
            null);

    var response = orchestrator.orchestrate(request);

    assertThat(response.returnCode()).isEqualTo(RatingOutcome.ACCEPT.returnCode());
    assertThat(response.underwritingDecision()).isEqualTo(UnderwritingDecision.APPROVE);
    assertThat(response.basePremium()).isEqualByComparingTo("1260.00");
    assertThat(response.finalPremium()).isNotNull();

    Integer calcCount =
        jdbcClient
            .sql("SELECT COUNT(*) FROM premium_calc_t WHERE snapshot_id = :id")
            .param("id", response.calculationId())
            .query(Integer.class)
            .single();
    assertThat(calcCount).isEqualTo(1);

    Integer detailCount =
        jdbcClient
            .sql(
                """
                SELECT COUNT(*) FROM premium_calc_detail_t d
                JOIN premium_calc_t c ON c.calc_id = d.calc_id
                WHERE c.snapshot_id = :id
                """)
            .param("id", response.calculationId())
            .query(Integer.class)
            .single();
    assertThat(detailCount).isGreaterThan(0);

    Integer outboxCount =
        jdbcClient
            .sql("SELECT COUNT(*) FROM outbox_events WHERE aggregate_id = :id")
            .param("id", response.calculationId())
            .query(Integer.class)
            .single();
    assertThat(outboxCount).isEqualTo(1);
  }

  @Test
  void declineShortCircuitsWithoutPremiumCalcRow() {
    RatingRequest request =
        new RatingRequest(
            "HOME",
            "HO3",
            "TX",
            "TX",
            LocalDate.parse("2026-06-01"),
            new BigDecimal("750000.00"),
            null,
            "HO9999999999",
            "A",
            null,
            null);

    var response = orchestrator.orchestrate(request);

    assertThat(response.returnCode()).isEqualTo(RatingOutcome.DECLINE.returnCode());
    assertThat(response.underwritingDecision()).isEqualTo(UnderwritingDecision.DECLINE);

    Integer calcCount =
        jdbcClient
            .sql("SELECT COUNT(*) FROM premium_calc_t WHERE pol_nbr = :pol")
            .param("pol", "HO9999999999")
            .query(Integer.class)
            .single();
    assertThat(calcCount).isZero();
  }

  private void applySql(String classpathLocation) throws Exception {
    if ("db/migration/V1__premium_rating_tables.sql".equals(classpathLocation)) {
      jdbcClient.sql("DROP SCHEMA IF EXISTS public CASCADE").update();
      jdbcClient.sql("CREATE SCHEMA public").update();
    }
    String sql =
        StreamUtils.copyToString(
            new ClassPathResource(classpathLocation).getInputStream(), StandardCharsets.UTF_8);
    jdbcClient.sql(sql).update();
  }
}
