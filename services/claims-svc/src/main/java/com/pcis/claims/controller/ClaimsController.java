package com.pcis.claims.controller;

import com.pcis.claims.application.ClaimsApplicationService;
import com.pcis.claims.dto.ApprovalResponse;
import com.pcis.claims.dto.ClaimResponse;
import com.pcis.claims.dto.ClaimResponseMapper;
import com.pcis.claims.dto.CreateApprovalRequest;
import com.pcis.claims.dto.CreateClaimRequest;
import com.pcis.claims.dto.CreateReserveRequest;
import com.pcis.claims.dto.ReserveResponse;
import jakarta.validation.Valid;
import java.util.List;
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
@RequestMapping("/api/v1/claims")
public class ClaimsController {

  private final ClaimsApplicationService claimsApplicationService;
  private final ClaimResponseMapper claimResponseMapper;

  public ClaimsController(
      ClaimsApplicationService claimsApplicationService, ClaimResponseMapper claimResponseMapper) {
    this.claimsApplicationService = claimsApplicationService;
    this.claimResponseMapper = claimResponseMapper;
  }

  @GetMapping
  @PreAuthorize("hasAuthority('claims:read')")
  public List<ClaimResponse> listClaims() {
    return claimsApplicationService.listClaims().stream()
        .map(claimResponseMapper::toClaimResponse)
        .toList();
  }

  @GetMapping("/customer/{custId}")
  @PreAuthorize("hasAuthority('claims:read')")
  public List<ClaimResponse> listClaimsByCustomer(@PathVariable("custId") Integer custId) {
    return claimsApplicationService.listClaimsByCustomer(custId).stream()
        .map(claimResponseMapper::toClaimResponse)
        .toList();
  }

  @GetMapping("/{claimNbr}")
  @PreAuthorize("hasAuthority('claims:read')")
  public ClaimResponse getClaim(@PathVariable("claimNbr") String claimNbr) {
    return claimResponseMapper.toClaimResponse(claimsApplicationService.getClaim(claimNbr));
  }

  @PostMapping
  @PreAuthorize("hasAuthority('claims:write')")
  public ResponseEntity<ClaimResponse> createClaim(@Valid @RequestBody CreateClaimRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(claimResponseMapper.toClaimResponse(claimsApplicationService.createClaim(request)));
  }

  @GetMapping("/{claimNbr}/reserves")
  @PreAuthorize("hasAuthority('claims:read')")
  public List<ReserveResponse> listReserves(@PathVariable("claimNbr") String claimNbr) {
    return claimsApplicationService.listReserves(claimNbr).stream()
        .map(claimResponseMapper::toReserveResponse)
        .toList();
  }

  @PostMapping("/{claimNbr}/reserves")
  @PreAuthorize("hasAuthority('claims:write')")
  public ResponseEntity<ReserveResponse> createReserve(
      @PathVariable("claimNbr") String claimNbr, @Valid @RequestBody CreateReserveRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(
            claimResponseMapper.toReserveResponse(
                claimsApplicationService.createReserve(claimNbr, request)));
  }

  @GetMapping("/{claimNbr}/approvals")
  @PreAuthorize("hasAuthority('claims:read')")
  public List<ApprovalResponse> listApprovals(@PathVariable("claimNbr") String claimNbr) {
    return claimsApplicationService.listApprovals(claimNbr).stream()
        .map(claimResponseMapper::toApprovalResponse)
        .toList();
  }

  @PostMapping("/{claimNbr}/approvals")
  @PreAuthorize("hasAuthority('claims:write')")
  public ResponseEntity<ApprovalResponse> createApproval(
      @PathVariable("claimNbr") String claimNbr, @Valid @RequestBody CreateApprovalRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(
            claimResponseMapper.toApprovalResponse(
                claimsApplicationService.createApproval(claimNbr, request)));
  }
}
