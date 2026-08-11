package com.pcis.claims.reconciliation;

public record ReconciliationBreak(
    BreakClass breakClass,
    String claimNbr,
    String columnName,
    String legacyValue,
    String targetValue) {}
