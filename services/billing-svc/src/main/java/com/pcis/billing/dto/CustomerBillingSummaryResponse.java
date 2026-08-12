package com.pcis.billing.dto;

import java.math.BigDecimal;

public record CustomerBillingSummaryResponse(BigDecimal balanceDue, int openInvoiceCount) {}
