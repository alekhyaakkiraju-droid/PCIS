package com.pcis.batch.audit.domain;

import java.time.Instant;

public record AuditLogRow(
    long logId,
    String programName,
    String actionCode,
    String tableName,
    String recordKey,
    String userId,
    Instant logTimestamp) {}
