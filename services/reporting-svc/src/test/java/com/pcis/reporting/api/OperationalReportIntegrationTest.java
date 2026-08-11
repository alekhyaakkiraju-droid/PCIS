package com.pcis.reporting.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.pcis.reporting.support.IntegrationTestDataSourceConfig;
import com.pcis.reporting.support.PostgresTestContainer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@Import(IntegrationTestDataSourceConfig.class)
@ActiveProfiles("test")
@EnabledIf("com.pcis.reporting.support.TestEnvironment#isDockerAvailable")
class OperationalReportIntegrationTest {

  @DynamicPropertySource
  static void register(DynamicPropertyRegistry registry) {
    PostgresTestContainer.registerReplicaProperties(registry);
  }

  @Autowired private MockMvc mockMvc;

  @Test
  void operationalSummaryReturnsRunCountsAndOpenBreaks() throws Exception {
    mockMvc
        .perform(get("/api/v1/reports/operational/summary"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.batchRunCount").value(greaterThanOrEqualTo(1)))
        .andExpect(jsonPath("$.batchRunsWithErrors").value(0))
        .andExpect(jsonPath("$.lastBatchRunDate").exists())
        .andExpect(jsonPath("$.openReconciliationBreaks").value(1))
        .andExpect(jsonPath("$.reconciliationTablePresent").value(true));
  }

  @Test
  void auditArchiveStatsReturnsExportAndJobMetadata() throws Exception {
    mockMvc
        .perform(get("/api/v1/reports/operational/audit-archive"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.exportCount").value(2))
        .andExpect(jsonPath("$.exportsPendingPurge").value(1))
        .andExpect(jsonPath("$.archiveJobRunCount").value(1))
        .andExpect(jsonPath("$.archiveTablesPresent").value(true))
        .andExpect(jsonPath("$.lastExportAt").exists())
        .andExpect(jsonPath("$.lastArchiveJobStart").exists());
  }
}
