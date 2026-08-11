package com.pcis.billing.batch.prm005b;

import static org.assertj.core.api.Assertions.assertThat;

import com.pcis.batch.common.BatchJobExecutionListener;
import com.pcis.billing.support.BatchTestSupport;
import com.pcis.billing.support.PostgresTestContainer;
import com.pcis.billing.support.TestEnvironment;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
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
@EnabledIf("com.pcis.billing.support.TestEnvironment#isDockerAvailable")
class DelinquencyAgingJobIntegrationTest {

  @DynamicPropertySource
  static void registerDataSource(DynamicPropertyRegistry registry) {
    PostgresTestContainer.registerProperties(registry);
  }

  @Autowired private JobLauncherTestUtils jobLauncherTestUtils;
  @Autowired private Job delinquencyAgingJob;
  @Autowired private DataSource dataSource;
  @Autowired private JdbcTemplate jdbcTemplate;

  @BeforeEach
  void setUp() {
    jobLauncherTestUtils.setJob(delinquencyAgingJob);
  }

  @Test
  void multiScenarioMatchesGoldenOutput() throws Exception {
    loadFixture("fixtures/prm005b-multi-scenario.sql");

    JobExecution execution = jobLauncherTestUtils.launchJob();

    assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);

    List<Map<String, Object>> actual =
        jdbcTemplate.queryForList(
            """
            SELECT POL_NBR, INSTALLMENT_NBR, SCHED_STATUS, COALESCE(REC_DELINQUENT, 0) AS REC_DELINQUENT
            FROM BILLING_SCHEDULE_T
            WHERE POL_NBR LIKE 'POLDLQ%'
            ORDER BY POL_NBR
            """);
    List<GoldenScheduleRow> expected =
        loadGoldenSchedule("golden/delinquency-aging/multi-scenario-schedule.csv");
    assertThat(toGoldenRows(actual)).containsExactlyElementsOf(expected);

    StepExecution agingStep = BatchTestSupport.stepByName(execution, "delinquencyAgingStep");
    assertThat(agingStep.getReadCount()).isEqualTo(8);
    assertThat(agingStep.getFilterCount()).isEqualTo(4);
    assertThat(agingStep.getWriteCount()).isEqualTo(4);

    assertThat(
            jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM outbox_events WHERE EVENT_TYPE = 'DelinquencyStatusChanged'",
                Integer.class))
        .isEqualTo(4);

    assertThat(
            jdbcTemplate.queryForObject(
                """
                SELECT REC_SELECTED FROM RPT_RUN_LOG_T
                WHERE PGM_NAME = 'PRM005B'
                ORDER BY CRT_TIMESTAMP DESC LIMIT 1
                """,
                Integer.class))
        .isEqualTo(8);
    assertThat(
            jdbcTemplate.queryForObject(
                """
                SELECT REC_UPDATED FROM RPT_RUN_LOG_T
                WHERE PGM_NAME = 'PRM005B'
                ORDER BY CRT_TIMESTAMP DESC LIMIT 1
                """,
                Integer.class))
        .isEqualTo(4);
    assertThat(
            jdbcTemplate.queryForObject(
                """
                SELECT REC_DELINQUENT FROM RPT_RUN_LOG_T
                WHERE PGM_NAME = 'PRM005B'
                ORDER BY CRT_TIMESTAMP DESC LIMIT 1
                """,
                Integer.class))
        .isEqualTo(2);
  }

  @Test
  void idempotentSecondRunProducesNoUpdates() throws Exception {
    loadFixture("fixtures/prm005b-multi-scenario.sql");

    JobExecution firstRun = jobLauncherTestUtils.launchJob();
    assertThat(firstRun.getStatus()).isEqualTo(BatchStatus.COMPLETED);

    int outboxAfterFirst =
        jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM outbox_events WHERE EVENT_TYPE = 'DelinquencyStatusChanged'",
            Integer.class);

    JobExecution secondRun = jobLauncherTestUtils.launchJob();
    assertThat(secondRun.getStatus()).isEqualTo(BatchStatus.COMPLETED);

    StepExecution secondStep = BatchTestSupport.stepByName(secondRun, "delinquencyAgingStep");
    assertThat(secondStep.getWriteCount()).isZero();
    assertThat(
            jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM outbox_events WHERE EVENT_TYPE = 'DelinquencyStatusChanged'",
                Integer.class))
        .isEqualTo(outboxAfterFirst);
  }

  @Test
  void outboxFailureRollsBackStatusUpdateAndSetsExitCodeFour() throws Exception {
    loadFixture("fixtures/prm005b-multi-scenario.sql");
    jdbcTemplate.execute("DROP TABLE IF EXISTS outbox_events CASCADE");
    try {
      JobExecution execution = jobLauncherTestUtils.launchJob();

      assertThat(execution.getStatus()).isEqualTo(BatchStatus.FAILED);
      assertThat(
              jdbcTemplate.queryForObject(
                  """
                  SELECT COUNT(*) FROM BILLING_SCHEDULE_T
                  WHERE POL_NBR = 'POLDLQ004' AND SCHED_STATUS = 'L'
                  """,
                  Integer.class))
          .isZero();
      assertThat(BatchJobExecutionListener.resolveExitCode(execution, 100)).isEqualTo(4);
    } finally {
      restoreOutboxEventsTable();
    }
  }

  @Test
  void zeroCandidateRunCompletesCleanly() throws Exception {
    loadFixture("fixtures/prm005b-zero-candidates.sql");

    JobExecution execution = jobLauncherTestUtils.launchJob();

    assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
    assertThat(BatchTestSupport.stepByName(execution, "delinquencyAgingStep").getReadCount())
        .isZero();
    assertThat(
            jdbcTemplate.queryForObject(
                """
                SELECT REC_SELECTED FROM RPT_RUN_LOG_T
                WHERE PGM_NAME = 'PRM005B'
                ORDER BY CRT_TIMESTAMP DESC LIMIT 1
                """,
                Integer.class))
        .isZero();
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
                parts[0], Integer.parseInt(parts[1]), parts[2], Integer.parseInt(parts[3])));
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
                    ((String) row.get("sched_status")).trim(),
                    ((Number) row.get("rec_delinquent")).intValue()))
        .collect(Collectors.toList());
  }

  private record GoldenScheduleRow(
      String polNbr, int installmentNbr, String schedStatus, int recDelinquent) {}
}
