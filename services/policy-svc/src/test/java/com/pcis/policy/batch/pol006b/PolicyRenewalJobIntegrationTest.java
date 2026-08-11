package com.pcis.policy.batch.pol006b;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.pcis.policy.support.BatchTestSupport;
import com.pcis.policy.support.PostgresTestContainer;
import com.pcis.policy.support.TestEnvironment;
import java.nio.charset.StandardCharsets;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest
@SpringBatchTest
@ActiveProfiles("test")
@EnabledIf("com.pcis.policy.support.TestEnvironment#isDockerAvailable")
class PolicyRenewalJobIntegrationTest {

  @RegisterExtension
  static WireMockExtension premiumSvc =
      WireMockExtension.newInstance()
          .options(
              com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig()
                  .dynamicPort())
          .build();

  @DynamicPropertySource
  static void registerProperties(DynamicPropertyRegistry registry) {
    PostgresTestContainer.registerProperties(registry);
    registry.add("pcis.premium.svc-url", premiumSvc::baseUrl);
    registry.add("pcis.batch.policy-renewal.reference-date", () -> "2026-08-01");
    registry.add("pcis.batch.policy-renewal.renewal-window-days", () -> "90");
  }

  @Autowired private JobLauncherTestUtils jobLauncherTestUtils;
  @Autowired private Job policyRenewalJob;
  @Autowired private DataSource dataSource;
  @Autowired private JdbcTemplate jdbcTemplate;

  @BeforeEach
  void setUp() throws Exception {
    jobLauncherTestUtils.setJob(policyRenewalJob);
    premiumSvc.resetAll();
    premiumSvc.stubFor(
        post(urlEqualTo("/api/v1/premium/calculations"))
            .atPriority(10)
            .willReturn(
                aResponse()
                    .withHeader("Content-Type", "application/json")
                    .withBody(
                        new ClassPathResource("wiremock/premium-renewal-approve.json")
                            .getContentAsString(StandardCharsets.UTF_8))));
    ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
    populator.addScript(new ClassPathResource("fixtures/test-data-renewal.sql"));
    populator.setSqlScriptEncoding(StandardCharsets.UTF_8.name());
    populator.execute(dataSource);
  }

  @Test
  void renewsEligiblePoliciesWithinWindow() throws Exception {
    assertThat(
            jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM policy WHERE pol_nbr LIKE 'POLR%'", Integer.class))
        .isEqualTo(5);
    assertThat(
            jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*) FROM policy
                WHERE pol_status = 'ACTV'
                  AND renewal_of_pol IS NULL
                  AND exp_date >= DATE '2026-08-01'
                  AND exp_date <= DATE '2026-10-30'
                """,
                Integer.class))
        .isEqualTo(5);

    JobExecution execution =
        jobLauncherTestUtils.launchJob(
            new JobParametersBuilder()
                .addLong("run.id", System.currentTimeMillis())
                .toJobParameters());

    assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
    StepExecution renewalStep = BatchTestSupport.stepByName(execution, "policyRenewalStep");
    if (renewalStep.getFailureExceptions() != null && !renewalStep.getFailureExceptions().isEmpty()) {
      throw new AssertionError(renewalStep.getFailureExceptions().getFirst());
    }
    assertThat(renewalStep.getReadCount()).isEqualTo(5);
    assertThat(renewalStep.getSkipCount()).isEqualTo(0);
    assertThat(renewalStep.getWriteCount()).isEqualTo(5);

    assertThat(
            jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM policy WHERE renewal_of_pol IS NOT NULL",
                Integer.class))
        .isEqualTo(5);

    assertThat(
            jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM deductible d
                JOIN coverage c ON c.coverage_id = d.coverage_id
                JOIN policy p ON p.pol_nbr = c.pol_nbr
                WHERE p.pol_nbr = 'POLR000000R3'
                """,
                Integer.class))
        .isEqualTo(3);

    assertThat(
            jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM batch_exceptions WHERE error_type = 'DECLINE'", Integer.class))
        .isZero();

    assertThat(
            jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM outbox_events WHERE event_type = 'PolicyRenewed'",
                Integer.class))
        .isEqualTo(5);

    assertThat(
            jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM RPT_RUN_LOG_T WHERE PGM_NAME = 'POL006B'", Integer.class))
        .isEqualTo(1);

    premiumSvc.verify(5, postRequestedFor(urlEqualTo("/api/v1/premium/calculations")));
  }

  @Test
  void skipsDeclinedPoliciesAndRecordsBatchExceptions() throws Exception {
    stubDeclines();
    ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
    populator.addScript(new ClassPathResource("fixtures/test-data-renewal.sql"));
    populator.setSqlScriptEncoding(StandardCharsets.UTF_8.name());
    populator.execute(dataSource);

    JobExecution execution =
        jobLauncherTestUtils.launchJob(
            new JobParametersBuilder()
                .addLong("run.id", System.currentTimeMillis())
                .toJobParameters());

    assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
    StepExecution renewalStep = BatchTestSupport.stepByName(execution, "policyRenewalStep");
    assertThat(renewalStep.getSkipCount()).isEqualTo(2);
    assertThat(renewalStep.getWriteCount()).isEqualTo(3);
    assertThat(
            jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM batch_exceptions WHERE error_type = 'DECLINE'", Integer.class))
        .isEqualTo(2);
  }

  private void stubDeclines() throws Exception {
    String declineBody =
        new ClassPathResource("wiremock/premium-renewal-decline.json")
            .getContentAsString(StandardCharsets.UTF_8);
    premiumSvc.stubFor(
        post(urlEqualTo("/api/v1/premium/calculations"))
            .atPriority(1)
            .withRequestBody(matchingJsonPath("$.policyNumber", equalTo("POLR00000004")))
            .willReturn(aResponse().withHeader("Content-Type", "application/json").withBody(declineBody)));
    premiumSvc.stubFor(
        post(urlEqualTo("/api/v1/premium/calculations"))
            .atPriority(1)
            .withRequestBody(matchingJsonPath("$.policyNumber", equalTo("POLR00000005")))
            .willReturn(aResponse().withHeader("Content-Type", "application/json").withBody(declineBody)));
  }
}
