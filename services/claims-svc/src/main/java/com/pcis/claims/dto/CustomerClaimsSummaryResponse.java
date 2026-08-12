package com.pcis.claims.dto;

import java.math.BigDecimal;
import java.util.List;

public record CustomerClaimsSummaryResponse(int openClaimCount, List<ClaimItem> items) {

  public record ClaimItem(String claimId, String status, BigDecimal reserveAmount) {}
}
