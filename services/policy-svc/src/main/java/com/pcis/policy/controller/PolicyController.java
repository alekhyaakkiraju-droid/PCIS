package com.pcis.policy.controller;

import com.pcis.policy.dto.PolicyCancelRequest;
import com.pcis.policy.dto.PolicyCreateRequest;
import com.pcis.policy.dto.PolicyEndorseRequest;
import com.pcis.policy.dto.PolicyListResponse;
import com.pcis.policy.dto.PolicyMapper;
import com.pcis.policy.dto.PolicyResponse;
import com.pcis.policy.service.PolicyService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
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
@RequestMapping("/api/v1/policies")
public class PolicyController {

  private final PolicyService policyService;
  private final PolicyMapper policyMapper;

  public PolicyController(PolicyService policyService, PolicyMapper policyMapper) {
    this.policyService = policyService;
    this.policyMapper = policyMapper;
  }

  @PostMapping
  @PreAuthorize("hasRole('UNDERWRITER')")
  public ResponseEntity<PolicyResponse> createPolicy(
      @Valid @RequestBody PolicyCreateRequest request) {
    var created = policyService.createPolicy(request);
    return ResponseEntity.status(HttpStatus.CREATED).body(policyMapper.toResponse(created));
  }

  @GetMapping("/{policyNumber}")
  @PreAuthorize("isAuthenticated()")
  public PolicyResponse getPolicy(@PathVariable("policyNumber") String policyNumber) {
    return policyMapper.toResponse(policyService.findByPolicyNumber(policyNumber));
  }

  @GetMapping
  @PreAuthorize("isAuthenticated()")
  public PolicyListResponse listPolicies(
      @RequestParam(value = "customerId", required = false) Integer customerId,
      @RequestParam(value = "status", required = false) String status,
      @PageableDefault(size = 20) Pageable pageable) {
    Page<PolicyResponse> page =
        policyService
            .findPolicies(customerId, status, pageable)
            .map(policyMapper::toResponse);
    return policyMapper.toListResponse(
        page.getContent(),
        page.getNumber(),
        page.getSize(),
        page.getTotalElements());
  }

  @PutMapping("/{policyNumber}/endorse")
  @PreAuthorize("hasRole('UNDERWRITER')")
  public PolicyResponse endorsePolicy(
      @PathVariable("policyNumber") String policyNumber, @Valid @RequestBody PolicyEndorseRequest request) {
    return policyMapper.toResponse(policyService.endorsePolicy(policyNumber, request));
  }

  @PostMapping("/{policyNumber}/cancel")
  @PreAuthorize("hasRole('UNDERWRITER')")
  public PolicyResponse cancelPolicy(
      @PathVariable("policyNumber") String policyNumber, @Valid @RequestBody PolicyCancelRequest request) {
    return policyMapper.toResponse(policyService.cancelPolicy(policyNumber, request));
  }
}
