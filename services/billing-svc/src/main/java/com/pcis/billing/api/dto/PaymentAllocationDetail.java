package com.pcis.billing.api.dto;

import java.math.BigDecimal;

public record PaymentAllocationDetail(
    Long billSchedId, BigDecimal appliedAmt, BigDecimal newBalance, String newStatus) {}
