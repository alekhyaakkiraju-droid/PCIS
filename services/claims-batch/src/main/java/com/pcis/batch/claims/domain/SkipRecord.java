package com.pcis.batch.claims.domain;

import java.io.Serializable;

public record SkipRecord(String claimNbr, Long reserveId, SkipReasonCode reasonCode, String detail)
    implements Serializable {}
