package com.pcis.batch.audit.infrastructure;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.kms.KmsClient;
import software.amazon.awssdk.services.kms.model.ScheduleKeyDeletionRequest;
import software.amazon.awssdk.services.kms.model.ScheduleKeyDeletionResponse;

@Component
@ConditionalOnProperty(name = "pcis.audit.purge.kms-enabled", havingValue = "true")
public class AwsKmsKeyLifecycleClient implements KmsKeyLifecycleClient {

  private static final Logger log = LoggerFactory.getLogger(AwsKmsKeyLifecycleClient.class);

  private final KmsClient kmsClient;

  public AwsKmsKeyLifecycleClient(KmsClient kmsClient) {
    this.kmsClient = kmsClient;
  }

  @Override
  public KmsKeyDeletionSchedule scheduleKeyDeletion(String keyArn, int waitingPeriodDays) {
    if (waitingPeriodDays < 7) {
      throw new IllegalArgumentException(
          "KMS waiting period must be at least 7 days, got " + waitingPeriodDays);
    }
    ScheduleKeyDeletionResponse response =
        kmsClient.scheduleKeyDeletion(
            ScheduleKeyDeletionRequest.builder()
                .keyId(keyArn)
                .pendingWindowInDays(waitingPeriodDays)
                .build());
    Instant scheduled =
        response.deletionDate() == null
            ? Instant.now().plus(waitingPeriodDays, ChronoUnit.DAYS)
            : response.deletionDate().atZone(ZoneOffset.UTC).toInstant();
    log.info("Scheduled KMS key deletion keyArn={} at={}", keyArn, scheduled);
    return new KmsKeyDeletionSchedule(keyArn, scheduled);
  }

  @Override
  public void cancelKeyDeletion(String keyArn) {
    kmsClient.cancelKeyDeletion(builder -> builder.keyId(keyArn));
    log.info("Cancelled KMS key deletion keyArn={}", keyArn);
  }
}
