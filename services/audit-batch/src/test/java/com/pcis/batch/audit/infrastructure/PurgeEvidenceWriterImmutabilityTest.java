package com.pcis.batch.audit.infrastructure;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.pcis.batch.audit.support.PostgresTestContainer;
import com.pcis.batch.audit.support.TestEnvironment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest
@ActiveProfiles("test")
@EnabledIf("com.pcis.batch.audit.support.TestEnvironment#isDockerAvailable")
class PurgeEvidenceWriterImmutabilityTest {

  @DynamicPropertySource
  static void registerDataSource(DynamicPropertyRegistry registry) {
    PostgresTestContainer.registerProperties(registry);
  }

  @Autowired private JdbcTemplate jdbcTemplate;

  @BeforeEach
  void seedEvidence() {
    jdbcTemplate.update(
        """
        INSERT INTO purge_evidence (
            purge_type, target_identifier, tier, retention_days,
            purge_timestamp, actor, evidence_hash)
        VALUES ('PARTITION_DROP', 'audit_log_t_y2018m01', 'INTERNAL', 365,
                NOW(), 'TEST', 'abc123')
        """);
  }

  @Test
  void rejectsUpdateOnPurgeEvidence() {
    assertThatThrownBy(
            () ->
                jdbcTemplate.update(
                    "UPDATE purge_evidence SET actor = 'HACK' WHERE target_identifier = 'audit_log_t_y2018m01'"))
        .hasMessageContaining("immutable");
  }

  @Test
  void rejectsDeleteOnPurgeEvidence() {
    assertThatThrownBy(
            () ->
                jdbcTemplate.update(
                    "DELETE FROM purge_evidence WHERE target_identifier = 'audit_log_t_y2018m01'"))
        .hasMessageContaining("immutable");
  }
}
