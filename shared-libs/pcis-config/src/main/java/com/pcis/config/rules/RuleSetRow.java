package com.pcis.config.rules;

import java.time.LocalDate;

public record RuleSetRow(
    String ruleSetKey,
    int versionNo,
    String payload,
    LocalDate effectiveFrom,
    LocalDate effectiveTo,
    String statusCd) {}
