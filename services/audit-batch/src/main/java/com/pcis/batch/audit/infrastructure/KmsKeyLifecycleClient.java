package com.pcis.batch.audit.infrastructure;

/** Schedules KMS key destruction for cryptographic erasure (WO-171). */
public interface KmsKeyLifecycleClient {

  KmsKeyDeletionSchedule scheduleKeyDeletion(String keyArn, int waitingPeriodDays);

  void cancelKeyDeletion(String keyArn);
}
