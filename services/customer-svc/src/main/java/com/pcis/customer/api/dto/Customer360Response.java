package com.pcis.customer.api.dto;

import java.math.BigDecimal;
import java.util.List;

public record Customer360Response(
    Integer custId,
    SectionWrapper<CustomerResponse> profile,
    SectionWrapper<PolicySection> policies,
    SectionWrapper<BillingSection> billing,
    SectionWrapper<ClaimsSection> claims) {

  public record PolicySection(int activeCount, List<PolicyItem> items) {}

  public record PolicyItem(String policyId, String policyType, String status, BigDecimal premium) {}

  public record BillingSection(BigDecimal balanceDue, int openInvoiceCount) {}

  public record ClaimsSection(int openClaimCount, List<ClaimItem> items) {}

  public record ClaimItem(String claimId, String status, BigDecimal reserveAmount) {}
}
