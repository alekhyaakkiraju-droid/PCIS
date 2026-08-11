package com.pcis.batch.audit.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class StubKmsKeyLifecycleClientTest {

  private final StubKmsKeyLifecycleClient client =
      new StubKmsKeyLifecycleClient(Clock.fixed(Instant.parse("2026-08-11T00:00:00Z"), ZoneOffset.UTC));

  @Test
  void schedulesDeletionWithMinimumWaitingPeriod() {
    KmsKeyDeletionSchedule schedule =
        client.scheduleKeyDeletion("arn:aws:kms:us-east-1:123:key/abc", 7);
    assertThat(schedule.keyArn()).isEqualTo("arn:aws:kms:us-east-1:123:key/abc");
    assertThat(schedule.scheduledDeletionAt()).isEqualTo(Instant.parse("2026-08-18T00:00:00Z"));
  }

  @Test
  void rejectsWaitingPeriodBelowSevenDays() {
    assertThatThrownBy(() -> client.scheduleKeyDeletion("arn:aws:kms:us-east-1:123:key/abc", 3))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("7 days");
  }
}
