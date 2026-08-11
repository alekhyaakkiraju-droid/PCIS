package com.pcis.premium.controller;

import com.pcis.error.ReasonCode;
import com.pcis.premium.application.PremiumRatingService;
import com.pcis.premium.application.PremiumRatingService.CalculationNotFoundException;
import com.pcis.premium.dto.CreateCalculationRequest;
import com.pcis.premium.dto.PremiumCalculationResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.LinkedHashMap;
import java.util.Map;
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
      produces = MediaType.APPLICATION_JSON_VALUE)
  @PreAuthorize("hasAuthority('ROLE_premium:rate')")
  public PremiumCalculationResponse createCalculation(
      @Valid @RequestBody CreateCalculationRequest request) {
    return ratingService.createCalculation(request);
  }

  @GetMapping(value = "/calculations/{calculationId}", produces = MediaType.APPLICATION_JSON_VALUE)
  @PreAuthorize("hasAuthority('ROLE_premium:read')")
  public PremiumCalculationResponse getCalculation(@PathVariable String calculationId) {
    return ratingService.getCalculation(calculationId);
  }

  @org.springframework.web.bind.annotation.ExceptionHandler(CalculationNotFoundException.class)
  public ResponseEntity<Map<String, Object>> handleNotFound(
      CalculationNotFoundException ex, HttpServletRequest request) {
    Map<String, Object> body = new LinkedHashMap<>();
    body.put("type", ReasonCode.SYS_NOT_FOUND.type().toString());
    body.put("title", ReasonCode.SYS_NOT_FOUND.title());
    body.put("status", HttpStatus.NOT_FOUND.value());
    body.put("detail", ex.getMessage());
    body.put("instance", request.getRequestURI());
    body.put("code", ReasonCode.SYS_NOT_FOUND.code());
    body.put(
        "correlation_id",
        MDC.get("correlationId") != null ? MDC.get("correlationId") : "unknown");
    return ResponseEntity.status(HttpStatus.NOT_FOUND)
        .contentType(MediaType.APPLICATION_PROBLEM_JSON)
        .body(body);
  }
}
