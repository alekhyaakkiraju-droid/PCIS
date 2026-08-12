package com.pcis.customer.api;

import com.pcis.customer.api.dto.CreateCustomerRequest;
import com.pcis.customer.api.dto.Customer360Response;
import com.pcis.customer.api.dto.CustomerResponse;
import com.pcis.customer.api.dto.CustomerResponseMapper;
import com.pcis.customer.api.dto.DuplicateCheckResponse;
import com.pcis.customer.api.dto.DuplicateOverrideRequest;
import com.pcis.customer.api.dto.UpdateCustomerRequest;
import com.pcis.customer.application.Customer360Service;
import com.pcis.customer.application.CustomerApplicationService;
import com.pcis.customer.domain.CustomerEntity;
import com.pcis.customer.domain.DuplicateCandidate;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/customers")
public class CustomerController {

  private final CustomerApplicationService customerApplicationService;
  private final Customer360Service customer360Service;
  private final CustomerResponseMapper customerResponseMapper;

  public CustomerController(
      CustomerApplicationService customerApplicationService,
      Customer360Service customer360Service,
      CustomerResponseMapper customerResponseMapper) {
    this.customerApplicationService = customerApplicationService;
    this.customer360Service = customer360Service;
    this.customerResponseMapper = customerResponseMapper;
  }

  @PostMapping
  @PreAuthorize("hasAuthority('customer:write')")
  public ResponseEntity<CustomerResponse> createCustomer(
      @Valid @RequestBody CreateCustomerRequest request) {
    CustomerEntity created = customerApplicationService.create(request.toCommand());
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(customerResponseMapper.toResponse(created));
  }

  @PostMapping("/duplicate-overrides")
  @PreAuthorize("hasAuthority('customer:duplicate-override')")
  public ResponseEntity<CustomerResponse> createWithDuplicateOverride(
      @Valid @RequestBody DuplicateOverrideRequest request) {
    CustomerEntity created =
        customerApplicationService.createWithOverride(
            request.customer().toCommand(), request.overrideReason());
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(customerResponseMapper.toResponse(created));
  }

  @GetMapping("/{id}")
  @PreAuthorize("hasAuthority('customer:read')")
  public CustomerResponse getCustomer(@PathVariable("id") Integer id) {
    return customerResponseMapper.toResponse(customerApplicationService.findById(id));
  }

  @PutMapping("/{id}")
  @PreAuthorize("hasAuthority('customer:write')")
  public CustomerResponse updateCustomer(
      @PathVariable("id") Integer id, @Valid @RequestBody UpdateCustomerRequest request) {
    CustomerEntity updated = customerApplicationService.update(request.toCommand(id));
    return customerResponseMapper.toResponse(updated);
  }

  @GetMapping("/search")
  @PreAuthorize("hasAuthority('customer:read')")
  public List<CustomerResponse> searchCustomers(@RequestParam("q") String query) {
    return customerApplicationService.search(query).stream()
        .map(customerResponseMapper::toResponse)
        .toList();
  }

  @GetMapping
  @PreAuthorize("hasAuthority('customer:read')")
  public List<CustomerResponse> listCustomers() {
    return customerApplicationService.list().stream()
        .map(customerResponseMapper::toResponse)
        .toList();
  }

  @GetMapping("/{id}/duplicate-check")
  @PreAuthorize("hasAuthority('customer:read')")
  public DuplicateCheckResponse duplicateCheck(@PathVariable("id") Integer id) {
    java.util.Optional<DuplicateCandidate> duplicate =
        customerApplicationService.duplicateCheck(id);
    return DuplicateCheckResponse.from(id, duplicate);
  }

  @GetMapping("/{id}/360")
  @PreAuthorize("hasAuthority('customer:read')")
  public Customer360Response getCustomer360(@PathVariable("id") Integer id) {
    return customer360Service.getCustomer360(id);
  }
}
