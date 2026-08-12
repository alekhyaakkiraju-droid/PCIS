package com.pcis.policy.dto;

import java.math.BigDecimal;
import java.util.List;

public record CustomerPolicySummaryResponse(int activeCount, List<PolicyItem> items) {

  public record PolicyItem(String policyId, String policyType, String status, BigDecimal premium) {}
}
