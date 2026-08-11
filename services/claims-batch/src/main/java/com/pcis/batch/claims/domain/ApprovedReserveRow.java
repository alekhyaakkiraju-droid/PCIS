package com.pcis.batch.claims.domain;

import java.math.BigDecimal;

public record ApprovedReserveRow(long reserveHistId, String claimId, BigDecimal reserveAmt) {}
