package com.pcis.batch.common;

import java.time.Instant;
import java.time.LocalDate;

public record BatchRunLogEntry(
    String programName,
    LocalDate runDate,
    int recSelected,
    int recUpdated,
    int recErrors,
    Integer recDelinquent,
    Instant startTimestamp,
    Instant endTimestamp,
    String crtUser) {}
