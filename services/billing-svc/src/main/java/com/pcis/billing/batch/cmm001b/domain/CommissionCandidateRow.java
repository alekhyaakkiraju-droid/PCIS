package com.pcis.billing.batch.cmm001b.domain;

import java.math.BigDecimal;

public record CommissionCandidateRow(
    long billSchedId, String polNbr, String agtId, BigDecimal amtPaid, BigDecimal commRate) {}
