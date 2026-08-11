package com.pcis.configsvc.api.dto;

import java.time.Instant;

public record TunableHistoryResponse(
    Integer version,
    String changedBy,
    String oldValue,
    String newValue,
    String changeReason,
    Instant changedAt) {}
