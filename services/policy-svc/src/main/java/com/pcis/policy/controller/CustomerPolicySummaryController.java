package com.pcis.policy.controller;

import com.pcis.policy.dto.CustomerPolicySummaryResponse;
import com.pcis.policy.service.CustomerPolicySummaryService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/customers/{custId}/policies")
public class CustomerPolicySummaryController {

  private final CustomerPolicySummaryService customerPolicySummaryService;

  public CustomerPolicySummaryController(
      CustomerPolicySummaryService customerPolicySummaryService) {
    this.customerPolicySummaryService = customerPolicySummaryService;
  }

  @GetMapping("/summary")
  @PreAuthorize("isAuthenticated()")
  public CustomerPolicySummaryResponse getPolicySummary(@PathVariable("custId") Integer custId) {
    return customerPolicySummaryService.getSummary(custId);
  }
}
