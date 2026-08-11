package com.pcis.batch.reconciliation.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.pcis.batch.reconciliation.support.PostgresTestContainer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest(
    properties = {
      "spring.batch.job.enabled=false",
      "spring.main.web-application-type=servlet"
    })
@AutoConfigureMockMvc
@ActiveProfiles("test")
@EnabledIf("com.pcis.batch.reconciliation.support.TestEnvironment#isDockerAvailable")
class CutoverScorecardControllerTest {

  @DynamicPropertySource
  static void registerDataSource(DynamicPropertyRegistry registry) {
    PostgresTestContainer.registerProperties(registry);
  }

  @Autowired private MockMvc mockMvc;
  @Autowired private JdbcTemplate jdbcTemplate;

  @BeforeEach
  void seedSummary() {
    jdbcTemplate.update("DELETE FROM reconciliation_run_summary");
    jdbcTemplate.update(
        """
        INSERT INTO reconciliation_run_summary
            (domain, business_date, started_at, completed_at, entity_count, rows_compared,
             break_count, unexplained_break_count, gate_verdict, consecutive_clean_days)
        VALUES
            ('billing', DATE '2026-08-10', NOW(), NOW(), 1, 10, 0, 0, 'PASS', 5),
            ('billing', DATE '2026-08-09', NOW(), NOW(), 1, 10, 0, 0, 'PASS', 4)
        """);
  }

  @Test
  void returnsScorecardWithConsecutiveCleanDays() throws Exception {
    MvcResult result =
        mockMvc.perform(get("/api/v1/cutover/scorecard")).andExpect(status().isOk()).andReturn();

    assertThat(result.getResponse().getContentAsString()).contains("consecutiveCleanDays");
    assertThat(result.getResponse().getContentAsString()).contains("billing");
  }
}
