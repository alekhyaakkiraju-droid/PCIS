package com.pcis.batch.audit.infrastructure;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Local/test KMS client that records scheduled deletions without calling AWS.
 * Activated when {@code pcis.audit.purge.kms-enabled=false}.
 */
@Component
@ConditionalOnProperty(name = "pcis.audit.purge.kms-enabled", havingValue = "false", matchIfMissing = true)
public class StubKmsKeyLifecycleClient implements KmsKeyLifecycleClient {

  private static final Logger log = LoggerFactory.getLogger(StubKmsKeyLifecycleClient.class);

  private final Clock clock;

  public StubKmsKeyLifecycleClient(Clock clock) {
    this.clock = clock;
  }

  @Override
  public KmsKeyDeletionSchedule scheduleKeyDeletion(String keyArn, int waitingPeriodDays) {
    if (waitingPeriodDays < 7) {
      throw new IllegalArgumentException(
          "KMS waiting period must be at least 7 days, got " + waitingPeriodDays);
    }
    Instant scheduled = Instant.now(clock).plus(waitingPeriodDays, ChronoUnit.DAYS);
    log.info("Scheduled stub KMS key deletion keyArn={} at={}", keyArn, scheduled);
    return new KmsKeyDeletionSchedule(keyArn, scheduled);
  }

  @Override
  public void cancelKeyDeletion(String keyArn) {
    log.info("Cancelled stub KMS key deletion keyArn={}", keyArn);
  }
}
