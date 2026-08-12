package com.pcis.billing.api;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/billing")
public class DelinquencyController {

  @PostMapping("/aging")
  @PreAuthorize("hasAuthority('billing:write')")
  public ResponseEntity<Void> triggerAging() {
    return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
  }

  @GetMapping("/delinquency/{polNbr}")
  @PreAuthorize("hasAuthority('billing:read')")
  public ResponseEntity<Void> getDelinquency(@PathVariable String polNbr) {
    return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
  }
}
