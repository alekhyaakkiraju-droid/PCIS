package com.pcis.billing.dto;

import java.math.BigDecimal;

public record AgingBucketResponse(String bucket, int invoiceCount, BigDecimal amountDue) {}
