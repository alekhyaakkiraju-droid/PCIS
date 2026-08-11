package com.pcis.reporting.api.dto;

import java.time.Instant;

/** Audit archive export and job statistics from read replica (WO-234). */
public record AuditArchiveStatsResponse(
    long exportCount,
    long exportsPendingPurge,
    Instant lastExportAt,
    long archiveJobRunCount,
    Instant lastArchiveJobStart,
    boolean archiveTablesPresent) {}
