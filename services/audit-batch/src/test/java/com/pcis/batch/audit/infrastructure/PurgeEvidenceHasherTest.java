package com.pcis.batch.audit.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.pcis.batch.audit.domain.PurgeType;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class PurgeEvidenceHasherTest {

  @Test
  void hashIsDeterministicForKnownInputs() {
    Instant ts = Instant.parse("2026-01-15T12:00:00Z");
    String first =
        PurgeEvidenceHasher.computeHash(
            PurgeType.PARTITION_DROP, "audit_log_t_y2022m01", "RESTRICTED", 2555, ts, "AUDPURGE");
    String second =
        PurgeEvidenceHasher.computeHash(
            PurgeType.PARTITION_DROP, "audit_log_t_y2022m01", "RESTRICTED", 2555, ts, "AUDPURGE");
    assertThat(first).isEqualTo(second);
    assertThat(first).hasSize(64);
  }

  @Test
  void hashChangesWhenTargetChanges() {
    Instant ts = Instant.parse("2026-01-15T12:00:00Z");
    String a =
        PurgeEvidenceHasher.computeHash(
            PurgeType.S3_KEY_DESTROY, "s3://bucket/a", "INTERNAL", 365, ts, "AUDPURGE");
    String b =
        PurgeEvidenceHasher.computeHash(
            PurgeType.S3_KEY_DESTROY, "s3://bucket/b", "INTERNAL", 365, ts, "AUDPURGE");
    assertThat(a).isNotEqualTo(b);
  }
}
