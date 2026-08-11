package com.pcis.batch.claims.domain;

import com.pcis.claims.domain.ApprovalEntity;
import com.pcis.claims.domain.ClaimAdjusterEntity;
import com.pcis.claims.domain.ClaimReserveEntity;
import java.math.BigDecimal;

public record ClaimPaymentBatchItem(
    ClaimReserveEntity reserve,
    BigDecimal paymentAmount,
    ApprovalEntity approval,
    ClaimAdjusterEntity adjuster) {}
