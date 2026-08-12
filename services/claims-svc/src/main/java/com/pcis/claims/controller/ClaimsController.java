package com.pcis.claims.controller;

import com.pcis.claims.application.ClaimsApplicationService;
import com.pcis.claims.dto.ApprovalResponse;
import com.pcis.claims.dto.ClaimDetailResponse;
import com.pcis.claims.dto.ClaimListItemResponse;
import com.pcis.claims.dto.ClaimResponse;
import com.pcis.claims.dto.ClaimResponseMapper;
import com.pcis.claims.dto.CreateApprovalRequest;
import com.pcis.claims.dto.CreateClaimRequest;
import com.pcis.claims.dto.CreateNoteRequest;
import com.pcis.claims.dto.CreatePaymentRequest;
import com.pcis.claims.dto.CreateReserveRequest;
import com.pcis.claims.dto.NoteResponse;
import com.pcis.claims.dto.PaymentResponse;
import com.pcis.claims.dto.ReserveResponse;
import com.pcis.claims.dto.UpdateClaimRequest;
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
import org.springframework.web.bind.annotation.RequestHeader;
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
  public List<ClaimListItemResponse> listClaims(
      @org.springframework.web.bind.annotation.RequestParam(value = "status", required = false)
          String status,
      @org.springframework.web.bind.annotation.RequestParam(value = "view", required = false)
          String view) {
    return claimsApplicationService.listClaimSummaries(status, view);
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
  public ResponseEntity<ClaimDetailResponse> getClaim(@PathVariable("claimNbr") String claimNbr) {
    ClaimDetailResponse detail = claimsApplicationService.getClaimDetail(claimNbr);
    return ResponseEntity.ok()
        .eTag(String.valueOf(detail.version()))
        .body(detail);
  }

  @PostMapping
  @PreAuthorize("hasAnyAuthority('CLAIMS_ADJUSTER', 'claims:write')")
  public ResponseEntity<ClaimResponse> createClaim(@Valid @RequestBody CreateClaimRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(claimResponseMapper.toClaimResponse(claimsApplicationService.createClaim(request)));
  }

  @PutMapping("/{claimNbr}")
  @PreAuthorize("hasAnyAuthority('CLAIMS_ADJUSTER', 'claims:write')")
  public ResponseEntity<ClaimDetailResponse> updateClaim(
      @PathVariable("claimNbr") String claimNbr,
      @RequestHeader(value = "If-Match", required = false) String ifMatch,
      @Valid @RequestBody UpdateClaimRequest request) {
    Long expectedVersion = parseVersion(ifMatch);
    claimsApplicationService.updateClaim(claimNbr, expectedVersion, request);
    ClaimDetailResponse detail = claimsApplicationService.getClaimDetail(claimNbr);
    return ResponseEntity.ok()
        .eTag(String.valueOf(detail.version()))
        .body(detail);
  }

  @GetMapping("/{claimNbr}/reserves")
  @PreAuthorize("hasAuthority('claims:read')")
  public List<ReserveResponse> listReserves(@PathVariable("claimNbr") String claimNbr) {
    return claimsApplicationService.listReserves(claimNbr).stream()
        .map(claimResponseMapper::toReserveResponse)
        .toList();
  }

  @PostMapping("/{claimNbr}/reserves")
  @PreAuthorize("hasAnyAuthority('CLAIMS_ADJUSTER', 'claims:write')")
  public ResponseEntity<ReserveResponse> createReserve(
      @PathVariable("claimNbr") String claimNbr, @Valid @RequestBody CreateReserveRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(
            claimResponseMapper.toReserveResponse(
                claimsApplicationService.createReserve(claimNbr, request)));
  }

  @GetMapping("/{claimNbr}/payments")
  @PreAuthorize("hasAuthority('claims:read')")
  public List<PaymentResponse> listPayments(@PathVariable("claimNbr") String claimNbr) {
    return claimsApplicationService.listPayments(claimNbr).stream()
        .map(claimResponseMapper::toPaymentResponse)
        .toList();
  }

  @PostMapping("/{claimNbr}/payments")
  @PreAuthorize("hasAnyAuthority('CLAIMS_ADJUSTER', 'claims:write')")
  public ResponseEntity<PaymentResponse> createPayment(
      @PathVariable("claimNbr") String claimNbr, @Valid @RequestBody CreatePaymentRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(
            claimResponseMapper.toPaymentResponse(
                claimsApplicationService.createPayment(claimNbr, request)));
  }

  @GetMapping("/{claimNbr}/approvals")
  @PreAuthorize("hasAuthority('claims:read')")
  public List<ApprovalResponse> listApprovals(@PathVariable("claimNbr") String claimNbr) {
    return claimsApplicationService.listApprovals(claimNbr).stream()
        .map(claimResponseMapper::toApprovalResponse)
        .toList();
  }

  @PostMapping("/{claimNbr}/approvals")
  @PreAuthorize("hasAnyAuthority('CLAIMS_SUPERVISOR', 'claims:write')")
  public ResponseEntity<ApprovalResponse> createApproval(
      @PathVariable("claimNbr") String claimNbr, @Valid @RequestBody CreateApprovalRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(
            claimResponseMapper.toApprovalResponse(
                claimsApplicationService.createApproval(claimNbr, request)));
  }

  @PostMapping("/{claimNbr}/notes")
  @PreAuthorize("hasAnyAuthority('CLAIMS_ADJUSTER', 'CLAIMS_SUPERVISOR', 'claims:write')")
  public ResponseEntity<NoteResponse> createNote(
      @PathVariable("claimNbr") String claimNbr, @Valid @RequestBody CreateNoteRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(
            claimResponseMapper.toNoteResponse(
                claimsApplicationService.createNote(claimNbr, request)));
  }

  private static Long parseVersion(String ifMatch) {
    if (ifMatch == null || ifMatch.isBlank()) {
      return null;
    }
    String trimmed = ifMatch.trim();
    if (trimmed.startsWith("\"") && trimmed.endsWith("\"")) {
      trimmed = trimmed.substring(1, trimmed.length() - 1);
    }
    if (trimmed.startsWith("W/\"") && trimmed.endsWith("\"")) {
      trimmed = trimmed.substring(3, trimmed.length() - 1);
    }
    return Long.parseLong(trimmed);
  }
}
