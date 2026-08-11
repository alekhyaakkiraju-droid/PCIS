package com.pcis.premium.controller;

import com.pcis.error.ReasonCode;
import com.pcis.premium.application.PremiumRatingService;
import com.pcis.premium.dto.CreateCalculationRequest;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/premium")
public class PremiumRatingController {

  private final PremiumRatingService ratingService;

  public PremiumRatingController(PremiumRatingService ratingService) {
    this.ratingService = ratingService;
  }

  @PostMapping(
      value = "/calculations",
      consumes = MediaType.APPLICATION_JSON_VALUE,
      produces = MediaType.APPLICATION_PROBLEM_JSON_VALUE)
  @PreAuthorize("hasAuthority('ROLE_premium:rate')")
  public ResponseEntity<java.util.Map<String, Object>> createCalculation(
      @RequestBody CreateCalculationRequest request, HttpServletRequest httpRequest) {
    return notImplemented(httpRequest);
  }

  @GetMapping(value = "/calculations/{calculationId}", produces = MediaType.APPLICATION_PROBLEM_JSON_VALUE)
  @PreAuthorize("hasAuthority('ROLE_premium:read')")
  public ResponseEntity<java.util.Map<String, Object>> getCalculation(
      @PathVariable String calculationId, HttpServletRequest httpRequest) {
    ratingService.ensureReadPathWired(calculationId);
    return notImplemented(httpRequest);
  }

  private static ResponseEntity<java.util.Map<String, Object>> notImplemented(HttpServletRequest request) {
    java.util.Map<String, Object> body = new java.util.LinkedHashMap<>();
    body.put("type", ReasonCode.PRM_NOT_IMPLEMENTED.type().toString());
    body.put("title", ReasonCode.PRM_NOT_IMPLEMENTED.title());
    body.put("status", HttpStatus.NOT_IMPLEMENTED.value());
    body.put("detail", ReasonCode.PRM_NOT_IMPLEMENTED.title());
    body.put("instance", request.getRequestURI());
    body.put("code", ReasonCode.PRM_NOT_IMPLEMENTED.code());
    body.put(
        "correlation_id",
        MDC.get("correlationId") != null ? MDC.get("correlationId") : "unknown");
    return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED)
        .contentType(MediaType.APPLICATION_PROBLEM_JSON)
        .body(body);
  }
}
