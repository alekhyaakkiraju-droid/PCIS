package com.pcis.sync;

import static org.assertj.core.api.Assertions.assertThat;

import com.pcis.sync.health.SyncHealthIndicator;
import com.pcis.sync.support.H2SourceInitializer;
import com.pcis.sync.support.PostgresTestContainer;
import com.pcis.sync.support.TestEnvironment;
import com.pcis.sync.sync.SyncAgentService;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest
@ActiveProfiles("test")
@EnabledIf("com.pcis.sync.support.TestEnvironment#isDockerAvailable")
class SyncHealthAndMetricsTest {

  @DynamicPropertySource
  static void registerDataSource(DynamicPropertyRegistry registry) {
    PostgresTestContainer.registerProperties(registry);
  }

  @Autowired private SyncAgentService syncAgentService;
  @Autowired private SyncHealthIndicator healthIndicator;
  @Autowired private MeterRegistry meterRegistry;
  @Autowired @Qualifier("targetJdbcTemplate") private JdbcTemplate targetJdbcTemplate;
  @Autowired private H2SourceInitializer h2SourceInitializer;

  @BeforeEach
  void setUp() {
    targetJdbcTemplate.update("DELETE FROM sync_customer");
    targetJdbcTemplate.update("DELETE FROM sync_run_log");
    targetJdbcTemplate.update("DELETE FROM sync_watermark_state");
    h2SourceInitializer.resetSourceData();
  }

  @Test
  void healthEndpointReportsDatabaseAndDomainStatus() {
    syncAgentService.syncDomain("customer");

    var health = healthIndicator.health();

    assertThat(health.getStatus().getCode()).isEqualTo("UP");
    assertThat(health.getDetails()).containsKeys("targetDatabase", "sourceDatabase", "domains");
    @SuppressWarnings("unchecked")
    var domains = (java.util.Map<String, Object>) health.getDetails().get("domains");
    assertThat(domains).containsKey("customer");
  }

  @Test
  void metricsRecordExtractedAndUpsertedRows() {
    double extractedBefore =
        meterRegistry.get("pcis.sync.rows.extracted").tag("domain", "customer").counter().count();
    double upsertedBefore =
        meterRegistry.get("pcis.sync.rows.upserted").tag("domain", "customer").counter().count();
    double runsBefore =
        meterRegistry
            .get("pcis.sync.runs")
            .tag("domain", "customer")
            .tag("status", "success")
            .counter()
            .count();

    syncAgentService.syncDomain("customer");

    assertThat(
            meterRegistry.get("pcis.sync.rows.extracted").tag("domain", "customer").counter().count())
        .isEqualTo(extractedBefore + 3.0);
    assertThat(
            meterRegistry.get("pcis.sync.rows.upserted").tag("domain", "customer").counter().count())
        .isEqualTo(upsertedBefore + 3.0);
    assertThat(
            meterRegistry
                .get("pcis.sync.runs")
                .tag("domain", "customer")
                .tag("status", "success")
                .counter()
                .count())
        .isEqualTo(runsBefore + 1.0);
  }
}
