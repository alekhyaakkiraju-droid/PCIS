package com.pcis.reporting.api.dto;

import java.time.LocalDate;

/** Operational reporting summary sourced from read replica (WO-234). */
public record OperationalSummaryResponse(
    long batchRunCount,
    long batchRunsWithErrors,
    LocalDate lastBatchRunDate,
    long openReconciliationBreaks,
    boolean reconciliationTablePresent) {}
