package com.pcis.batch.claims.domain;

public record SkipRecord(
    String claimNbr, Long reserveId, SkipReasonCode reasonCode, String detail) {}
