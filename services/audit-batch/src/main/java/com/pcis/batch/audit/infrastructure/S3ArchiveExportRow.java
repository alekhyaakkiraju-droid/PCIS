package com.pcis.batch.audit.infrastructure;

import java.time.Instant;

public record S3ArchiveExportRow(
    long exportId,
    String s3Bucket,
    String s3Key,
    String kmsKeyArn,
    String tier,
    String partitionName,
    Instant exportedAt,
    int retentionDays) {}
