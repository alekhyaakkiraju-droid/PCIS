package com.pcis.batch.audit.infrastructure;

import java.time.Instant;

public record KmsKeyDeletionSchedule(String keyArn, Instant scheduledDeletionAt) {}
