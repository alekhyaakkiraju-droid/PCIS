package com.pcis.sync.watermark;

import java.time.Instant;

public record WatermarkState(
    String domainName,
    String sourceTable,
    String watermarkColumn,
    String watermarkValue,
    Instant lastRunAt,
    String lastRunStatus,
    long rowsExtracted,
    long rowsUpserted,
    Instant updatedAt) {}
