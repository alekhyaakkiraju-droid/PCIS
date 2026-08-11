package com.pcis.customer.api;

import com.pcis.customer.api.dto.CreateCustomerRequest;
import com.pcis.customer.api.dto.CustomerResponse;
import com.pcis.customer.api.dto.DuplicateOverrideRequest;
import com.pcis.customer.application.CustomerApplicationService;
import com.pcis.customer.domain.CustomerEntity;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/customers")
public class CustomerController {

  private final CustomerApplicationService customerApplicationService;

  public CustomerController(CustomerApplicationService customerApplicationService) {
    this.customerApplicationService = customerApplicationService;
  }

  @PostMapping
  @PreAuthorize("hasAuthority('customer:write')")
  public ResponseEntity<CustomerResponse> createCustomer(
      @Valid @RequestBody CreateCustomerRequest request) {
    CustomerEntity created = customerApplicationService.create(request.toCommand());
    return ResponseEntity.status(HttpStatus.CREATED).body(CustomerResponse.from(created));
  }

  @PostMapping("/duplicate-overrides")
  @PreAuthorize("hasAuthority('customer:duplicate-override')")
  public ResponseEntity<CustomerResponse> createWithDuplicateOverride(
      @Valid @RequestBody DuplicateOverrideRequest request) {
    CustomerEntity created =
        customerApplicationService.createWithOverride(
            request.customer().toCommand(), request.overrideReason());
    return ResponseEntity.status(HttpStatus.CREATED).body(CustomerResponse.from(created));
  }
}
