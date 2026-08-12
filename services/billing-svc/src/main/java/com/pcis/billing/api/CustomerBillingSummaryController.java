package com.pcis.billing.api;

import com.pcis.billing.application.CustomerBillingSummaryService;
import com.pcis.billing.dto.CustomerBillingSummaryResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/customers/{custId}/billing")
public class CustomerBillingSummaryController {

  private final CustomerBillingSummaryService customerBillingSummaryService;

  public CustomerBillingSummaryController(
      CustomerBillingSummaryService customerBillingSummaryService) {
    this.customerBillingSummaryService = customerBillingSummaryService;
  }

  @GetMapping("/summary")
  @PreAuthorize("isAuthenticated()")
  public CustomerBillingSummaryResponse getBillingSummary(@PathVariable("custId") Integer custId) {
    return customerBillingSummaryService.getSummary(custId);
  }
}
