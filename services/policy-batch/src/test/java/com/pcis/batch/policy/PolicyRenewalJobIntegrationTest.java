package com.pcis.batch.policy;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.pcis.batch.policy.support.PostgresTestContainer;
import com.pcis.batch.policy.support.TestEnvironment;
import java.nio.charset.StandardCharsets;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
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
@EnabledIf("com.pcis.batch.policy.support.TestEnvironment#isDockerAvailable")
class PolicyRenewalJobIntegrationTest {

  @RegisterExtension
  static WireMockExtension premiumSvc =
      WireMockExtension.newInstance().options(com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig().dynamicPort()).build();

  @DynamicPropertySource
  static void registerProperties(DynamicPropertyRegistry registry) {
    PostgresTestContainer.registerProperties(registry);
    registry.add("pcis.policy.renewal.premium-svc-url", premiumSvc::baseUrl);
  }

  @Autowired private JobLauncherTestUtils jobLauncherTestUtils;
  @Autowired private Job policyRenewalJob;
  @Autowired private DataSource dataSource;
  @Autowired private JdbcTemplate jdbcTemplate;

  @BeforeEach
  void setUp() {
    jobLauncherTestUtils.setJob(policyRenewalJob);
    premiumSvc.stubFor(
        post(urlEqualTo("/api/v1/premium/calculations"))
            .willReturn(
                aResponse()
                    .withHeader("Content-Type", "application/json")
                    .withBody(
                        """
                        {
                          "calculationId": "calc-it-1",
                          "returnCode": "00",
                          "underwritingDecision": "APPROVE",
                          "baseRate": "1200.00",
                          "ratingFactor": "1.0500",
                          "basePremium": "1260.00",
                          "finalPremium": "1323.00"
                        }
                        """)));
    ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
    populator.addScript(new ClassPathResource("fixtures/renewal-window-policies.sql"));
    populator.setSqlScriptEncoding(StandardCharsets.UTF_8.name());
    populator.execute(dataSource);
  }

  @Test
  void renewsPoliciesWithinWindowAndCarriesDeductibles() throws Exception {
    JobExecution execution = jobLauncherTestUtils.launchJob();

    assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
    assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM POLICY_T WHERE POL_NBR = 'POLREN00001R'", Integer.class))
        .isEqualTo(1);
    assertThat(
            jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM COVERAGE_T WHERE POL_NBR = 'POLREN00001R'", Integer.class))
        .isEqualTo(1);
    assertThat(
            jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM DEDUCTIBLE_T d
                JOIN COVERAGE_T c ON c.COVERAGE_ID = d.COVERAGE_ID
                WHERE c.POL_NBR = 'POLREN00001R'
                """,
                Integer.class))
        .isEqualTo(1);
    assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM POLICY_T WHERE POL_NBR = 'POLREN00002'", Integer.class))
        .isEqualTo(1);
    assertThat(
            jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM outbox_events WHERE EVENT_TYPE = 'PolicyRenewed'", Integer.class))
        .isEqualTo(1);
    assertThat(
            jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM RPT_RUN_LOG_T WHERE PGM_NAME = 'POL006B'", Integer.class))
        .isEqualTo(1);
  }
}
