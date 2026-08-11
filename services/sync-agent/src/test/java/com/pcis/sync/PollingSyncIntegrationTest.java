package com.pcis.sync;

import static org.assertj.core.api.Assertions.assertThat;

import com.pcis.sync.support.H2SourceInitializer;
import com.pcis.sync.support.PostgresTestContainer;
import com.pcis.sync.support.TestEnvironment;
import com.pcis.sync.sync.SyncAgentService;
import com.pcis.sync.sync.SyncRunResult;
import com.pcis.sync.watermark.WatermarkRepository;
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
class PollingSyncIntegrationTest {

  @DynamicPropertySource
  static void registerDataSource(DynamicPropertyRegistry registry) {
    PostgresTestContainer.registerProperties(registry);
  }

  @Autowired private SyncAgentService syncAgentService;
  @Autowired private WatermarkRepository watermarkRepository;
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
  void extractsFromH2SourceAndUpsertsToPostgres() {
    SyncRunResult result = syncAgentService.syncDomain("customer");

    assertThat(result.status()).isEqualTo("SUCCESS");
    assertThat(result.rowsExtracted()).isEqualTo(3);
    assertThat(result.rowsUpserted()).isEqualTo(3);

    assertThat(targetJdbcTemplate.queryForObject("SELECT COUNT(*) FROM sync_customer", Integer.class))
        .isEqualTo(3);
    assertThat(
            targetJdbcTemplate.queryForObject(
                "SELECT cust_name FROM sync_customer WHERE cust_id = 1", String.class))
        .isEqualTo("Acme Insurance");

    var watermark = watermarkRepository.findByDomain("customer").orElseThrow();
    assertThat(watermark.watermarkColumn()).isEqualTo("UPD_TIMESTAMP");
    assertThat(watermark.lastRunStatus()).isEqualTo("SUCCESS");
    assertThat(watermark.rowsExtracted()).isEqualTo(3);
  }

  @Test
  void incrementalSyncPullsOnlyRowsAfterWatermark() {
    syncAgentService.syncDomain("customer");

    h2SourceInitializer.insertAdditionalSourceRow();

    SyncRunResult secondRun = syncAgentService.syncDomain("customer");

    assertThat(secondRun.rowsExtracted()).isEqualTo(1);
    assertThat(secondRun.rowsUpserted()).isEqualTo(1);
    assertThat(targetJdbcTemplate.queryForObject("SELECT COUNT(*) FROM sync_customer", Integer.class))
        .isEqualTo(4);
  }

  @Test
  void idempotentReRunProducesIdenticalTargetState() {
    syncAgentService.syncDomain("customer");

    Integer countAfterFirst =
        targetJdbcTemplate.queryForObject("SELECT COUNT(*) FROM sync_customer", Integer.class);
    String nameAfterFirst =
        targetJdbcTemplate.queryForObject(
            "SELECT cust_name FROM sync_customer WHERE cust_id = 2", String.class);

    SyncRunResult rerun = syncAgentService.syncDomain("customer");

    assertThat(rerun.rowsExtracted()).isZero();
    assertThat(rerun.rowsUpserted()).isZero();
    assertThat(targetJdbcTemplate.queryForObject("SELECT COUNT(*) FROM sync_customer", Integer.class))
        .isEqualTo(countAfterFirst);
    assertThat(
            targetJdbcTemplate.queryForObject(
                "SELECT cust_name FROM sync_customer WHERE cust_id = 2", String.class))
        .isEqualTo(nameAfterFirst);
  }

  @Test
  void respectsConfiguredChunkSize() {
    SyncRunResult result = syncAgentService.syncDomain("customer");

    assertThat(result.rowsExtracted()).isEqualTo(3);
    assertThat(
            targetJdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM sync_run_log WHERE domain_name = 'customer'", Integer.class))
        .isEqualTo(1);
  }
}
