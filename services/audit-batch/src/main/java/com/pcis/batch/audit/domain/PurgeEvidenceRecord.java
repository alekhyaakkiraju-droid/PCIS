package com.pcis.batch.audit.domain;

import java.time.Instant;

public record PurgeEvidenceRecord(
    PurgeType purgeType,
    String targetIdentifier,
    String tier,
    int retentionDays,
    Instant purgeTimestamp,
    String actor,
    String evidenceHash,
    Instant scheduledDeletionAt) {}
