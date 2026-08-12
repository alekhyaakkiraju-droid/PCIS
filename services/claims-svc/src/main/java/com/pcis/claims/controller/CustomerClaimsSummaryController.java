package com.pcis.claims.controller;

import com.pcis.claims.application.CustomerClaimsSummaryService;
import com.pcis.claims.dto.CustomerClaimsSummaryResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/customers/{custId}/claims")
public class CustomerClaimsSummaryController {

  private final CustomerClaimsSummaryService customerClaimsSummaryService;

  public CustomerClaimsSummaryController(
      CustomerClaimsSummaryService customerClaimsSummaryService) {
    this.customerClaimsSummaryService = customerClaimsSummaryService;
  }

  @GetMapping("/summary")
  @PreAuthorize("hasAuthority('claims:read')")
  public CustomerClaimsSummaryResponse getClaimsSummary(@PathVariable("custId") Integer custId) {
    return customerClaimsSummaryService.getSummary(custId);
  }
}
