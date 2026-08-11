package com.pcis.reporting;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.pcis.reporting.config.ReadOnlyViolationException;
import com.pcis.reporting.config.ReportingDataSourceConfig;
import com.pcis.reporting.support.IntegrationTestDataSourceConfig;
import com.pcis.reporting.support.PostgresTestContainer;
import com.pcis.reporting.support.TestEnvironment;
import java.sql.DriverManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest
@Import(IntegrationTestDataSourceConfig.class)
@ActiveProfiles("test")
@EnabledIf("com.pcis.reporting.support.TestEnvironment#isDockerAvailable")
class ReportingReplicaIntegrationTest {
  @DynamicPropertySource
  static void register(DynamicPropertyRegistry registry) {
    PostgresTestContainer.registerReplicaProperties(registry);
  }

  @Autowired @Qualifier(ReportingDataSourceConfig.REPORTING_JDBC_TEMPLATE) JdbcTemplate reportingJdbc;

  @Test
  void seededFixturesReadable() {
    assertThat(reportingJdbc.queryForObject("SELECT COUNT(*) FROM POLICY_T", Integer.class)).isEqualTo(3);
  }

  @Test
  void primaryWriteVisibleToReader() throws Exception {
    var pg = PostgresTestContainer.container();
    try (var c = DriverManager.getConnection(pg.getJdbcUrl(), pg.getUsername(), pg.getPassword());
        var st = c.createStatement()) {
      st.execute("INSERT INTO RPT_RUN_LOG_T (PGM_NAME, RUN_DATE, REC_SELECTED, REC_UPDATED, REC_ERRORS, START_TIMESTAMP, END_TIMESTAMP, CRT_USER) VALUES ('RPT002A', CURRENT_DATE, 1, 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'IT')");
    }
    assertThat(reportingJdbc.queryForObject("SELECT COUNT(*) FROM RPT_RUN_LOG_T WHERE PGM_NAME='RPT002A'", Integer.class)).isEqualTo(1);
  }

  @Test
  void writeThroughReportingDatasourceFails() {
    assertThatThrownBy(
            () ->
                reportingJdbc.update(
                    "INSERT INTO POLICY_T (POL_NBR, POL_TYPE, POL_STATUS, PREM_ANNUAL) VALUES ('POL999999999','HOM','A',1)"))
        .satisfiesAnyOf(
            ex -> assertThat(ex).isInstanceOf(ReadOnlyViolationException.class),
            ex -> assertThat(ex.getMessage()).containsIgnoringCase("read-only"));
  }
}
