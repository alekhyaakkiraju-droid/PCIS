package com.pcis.billing.batch.bil003b;

import static org.assertj.core.api.Assertions.assertThat;

import com.pcis.batch.common.BatchJobExecutionListener;
import com.pcis.billing.support.PostgresTestContainer;
import com.pcis.billing.support.TestEnvironment;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
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
@EnabledIf("com.pcis.billing.support.TestEnvironment#isDockerAvailable")
class BillingGenerationJobIntegrationTest {

  @DynamicPropertySource
  static void registerDataSource(DynamicPropertyRegistry registry) {
    PostgresTestContainer.registerProperties(registry);
  }

  @Autowired private JobLauncherTestUtils jobLauncherTestUtils;
  @Autowired private Job billingGenerationJob;
  @Autowired private DataSource dataSource;
  @Autowired private JdbcTemplate jdbcTemplate;

  @BeforeEach
  void setUp() {
    jobLauncherTestUtils.setJob(billingGenerationJob);
  }

  @Test
  void generatesFirstMonthlyInstallment() throws Exception {
    loadFixture("fixtures/bil003b-monthly-frequency.sql");

    JobExecution execution = jobLauncherTestUtils.launchJob();

    assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
    assertThat(
            jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM BILLING_SCHEDULE_T WHERE POL_NBR = 'POLBILMON'",
                Integer.class))
        .isEqualTo(1);
    assertThat(
            jdbcTemplate.queryForObject(
                "SELECT AMT_DUE FROM BILLING_SCHEDULE_T WHERE POL_NBR = 'POLBILMON'",
                BigDecimal.class))
        .isEqualByComparingTo("50.00");
    assertThat(
            jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM INVOICE_T i JOIN BILLING_SCHEDULE_T bs "
                    + "ON i.BILL_SCHED_ID = bs.BILL_SCHED_ID WHERE bs.POL_NBR = 'POLBILMON'",
                Integer.class))
        .isEqualTo(1);
    assertThat(
            jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM outbox_events WHERE EVENT_TYPE = 'InstallmentGenerated'",
                Integer.class))
        .isEqualTo(1);
    assertThat(
            jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM RPT_RUN_LOG_T WHERE PGM_NAME = 'BIL003B'", Integer.class))
        .isEqualTo(1);
  }

  @Test
  void multiFrequencyScenarioMatchesGoldenOutput() throws Exception {
    loadFixture("fixtures/bil003b-multi-scenario.sql");

    JobExecution execution = jobLauncherTestUtils.launchJob();

    assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
    assertThat(
            jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM BILLING_SCHEDULE_T", Integer.class))
        .isEqualTo(6);
    assertThat(
            jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM BILLING_SCHEDULE_T WHERE POL_NBR = 'POLBILOUT'",
                Integer.class))
        .isEqualTo(1);

    List<Map<String, Object>> actual =
        jdbcTemplate.queryForList(
            """
            SELECT POL_NBR, INSTALLMENT_NBR, DUE_DATE, AMT_DUE
            FROM BILLING_SCHEDULE_T
            WHERE POL_NBR IN ('POLBILMON','POLBILQTR','POLBILSEM','POLBILANN','POLBILPEN')
            ORDER BY POL_NBR
            """);
    List<GoldenScheduleRow> expected = loadGoldenSchedule("golden/billing-generation/multi-scenario-schedule.csv");
    assertThat(toGoldenRows(actual)).containsExactlyInAnyOrderElementsOf(expected);
    assertThat(
            jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM outbox_events WHERE EVENT_TYPE = 'InstallmentGenerated'",
                Integer.class))
        .isEqualTo(5);
  }

  @Test
  void outboxFailureRollsBackInstallmentAndSetsExitCodeFour() throws Exception {
    loadFixture("fixtures/bil003b-monthly-frequency.sql");
    jdbcTemplate.execute("DROP TABLE IF EXISTS outbox_events CASCADE");
    try {
      JobExecution execution = jobLauncherTestUtils.launchJob();

      assertThat(execution.getStatus()).isEqualTo(BatchStatus.FAILED);
      assertThat(
              jdbcTemplate.queryForObject(
                  "SELECT COUNT(*) FROM BILLING_SCHEDULE_T WHERE POL_NBR = 'POLBILMON'",
                  Integer.class))
          .isZero();
      assertThat(BatchJobExecutionListener.resolveExitCode(execution, 100)).isEqualTo(4);
    } finally {
      restoreOutboxEventsTable();
    }
  }

  private void restoreOutboxEventsTable() {
    jdbcTemplate.execute(
        """
        CREATE TABLE IF NOT EXISTS outbox_events (
            ID BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
            AGGREGATE_TYPE VARCHAR(100) NOT NULL,
            AGGREGATE_ID VARCHAR(100) NOT NULL,
            EVENT_TYPE VARCHAR(100) NOT NULL,
            PAYLOAD JSONB NOT NULL,
            IDEMPOTENCY_KEY UUID NOT NULL,
            STATUS VARCHAR(20) NOT NULL DEFAULT 'PENDING',
            ATTEMPT_COUNT INTEGER NOT NULL DEFAULT 0,
            NEXT_ATTEMPT_AT TIMESTAMP,
            LAST_ERROR VARCHAR(500),
            CRT_USER VARCHAR(10),
            CRT_TIMESTAMP TIMESTAMP,
            UPD_USER VARCHAR(10),
            UPD_TIMESTAMP TIMESTAMP,
            CONSTRAINT uq_outbox_idempotency UNIQUE (IDEMPOTENCY_KEY)
        )
        """);
    jdbcTemplate.execute(
        """
        CREATE INDEX IF NOT EXISTS idx_outbox_relay
        ON outbox_events (STATUS, NEXT_ATTEMPT_AT) WHERE STATUS = 'PENDING'
        """);
  }

  @Test
  void zeroCandidateRunCompletesCleanly() throws Exception {
    loadFixture("fixtures/bil003b-zero-candidates.sql");

    JobExecution execution = jobLauncherTestUtils.launchJob();

    assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
    assertThat(
            jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM BILLING_SCHEDULE_T", Integer.class))
        .isZero();
    assertThat(
            jdbcTemplate.queryForObject(
                "SELECT REC_SELECTED FROM RPT_RUN_LOG_T WHERE PGM_NAME = 'BIL003B'", Integer.class))
        .isZero();
  }

  private void loadFixture(String classpath) {
    ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
    populator.addScript(new ClassPathResource(classpath));
    populator.setSqlScriptEncoding(StandardCharsets.UTF_8.name());
    populator.execute(dataSource);
  }

  private List<GoldenScheduleRow> loadGoldenSchedule(String classpath) throws Exception {
    List<GoldenScheduleRow> rows = new ArrayList<>();
    try (BufferedReader reader =
        new BufferedReader(
            new InputStreamReader(
                new ClassPathResource(classpath).getInputStream(), StandardCharsets.UTF_8))) {
      reader.readLine();
      String line;
      while ((line = reader.readLine()) != null) {
        if (line.isBlank()) {
          continue;
        }
        String[] parts = line.split(",");
        rows.add(
            new GoldenScheduleRow(
                parts[0],
                Integer.parseInt(parts[1]),
                LocalDate.parse(parts[2]),
                new BigDecimal(parts[3])));
      }
    }
    return rows;
  }

  private List<GoldenScheduleRow> toGoldenRows(List<Map<String, Object>> actual) {
    return actual.stream()
        .map(
            row ->
                new GoldenScheduleRow(
                    (String) row.get("pol_nbr"),
                    ((Number) row.get("installment_nbr")).intValue(),
                    ((java.sql.Date) row.get("due_date")).toLocalDate(),
                    (BigDecimal) row.get("amt_due")))
        .collect(Collectors.toList());
  }

  private record GoldenScheduleRow(
      String polNbr, int installmentNbr, LocalDate dueDate, BigDecimal amtDue) {}
}
