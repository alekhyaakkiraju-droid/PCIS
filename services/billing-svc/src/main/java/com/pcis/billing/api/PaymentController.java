package com.pcis.billing.api;

import com.pcis.billing.api.dto.PaymentRequest;
import com.pcis.billing.api.dto.PaymentResponse;
import com.pcis.billing.application.PaymentAllocationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/payments")
public class PaymentController {

  private final PaymentAllocationService paymentAllocationService;

  public PaymentController(PaymentAllocationService paymentAllocationService) {
    this.paymentAllocationService = paymentAllocationService;
  }

  @PostMapping
  @PreAuthorize("hasAuthority('billing:write')")
  public ResponseEntity<PaymentResponse> createPayment(@Valid @RequestBody PaymentRequest request) {
    PaymentResponse response = paymentAllocationService.applyPayment(request);
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  @GetMapping("/{paymentId}")
  @PreAuthorize("hasAuthority('billing:read')")
  public ResponseEntity<Void> getPayment(@PathVariable String paymentId) {
    return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
  }
}
