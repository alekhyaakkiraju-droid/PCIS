package com.pcis.policy.batch.pol006b;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.pcis.batch.common.BatchCommonAutoConfiguration;
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
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
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
class PolicyRenewalFaultInjectionIntegrationTest {

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
    registry.add("pcis.batch.policy-renewal.skip-limit", () -> "10");
    registry.add("pcis.batch.skip-threshold", () -> "1");
  }

  @Autowired private JobLauncherTestUtils jobLauncherTestUtils;
  @Autowired private Job policyRenewalJob;
  @Autowired private DataSource dataSource;
  @Autowired private JdbcTemplate jdbcTemplate;
  @Autowired private ApplicationContext applicationContext;

  @BeforeEach
  void setUp() throws Exception {
    jobLauncherTestUtils.setJob(policyRenewalJob);
    premiumSvc.resetAll();
    premiumSvc.stubFor(
        post(urlEqualTo("/api/v1/premium/calculations"))
            .willReturn(aResponse().withStatus(500).withBody("premium-svc failure")));

    ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
    populator.addScript(new ClassPathResource("fixtures/fault-injection-renewal.sql"));
    populator.setSqlScriptEncoding(StandardCharsets.UTF_8.name());
    populator.execute(dataSource);
  }

  @Test
  void breachesSkipThresholdWithExitCodeOne() throws Exception {
    JobExecution execution =
        jobLauncherTestUtils.launchJob(
            new JobParametersBuilder()
                .addLong("run.id", System.currentTimeMillis())
                .toJobParameters());

    assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
    assertThat(
            jdbcTemplate.queryForObject("SELECT COUNT(*) FROM batch_exceptions", Integer.class))
        .isGreaterThanOrEqualTo(2);
    assertThat(
            jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM RPT_RUN_LOG_T WHERE PGM_NAME = 'POL006B'", Integer.class))
        .isEqualTo(1);

    BatchCommonAutoConfiguration.BatchProcessExitCode exitCode =
        applicationContext.getBean(BatchCommonAutoConfiguration.BatchProcessExitCode.class);
    exitCode.registerFromJobExecution(execution);
    assertThat(exitCode.getExitCode()).isEqualTo(1);
  }
}
