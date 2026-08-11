package com.pcis.error;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class PcisExceptionHandler {

  private final ObjectMapper objectMapper;

  public PcisExceptionHandler(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  @ExceptionHandler(PcisException.class)
  public ResponseEntity<PcisProblemDetail> handlePcisException(PcisException ex, HttpServletRequest request) {
    HttpStatus status = mapStatus(ex.reasonCode());
    PcisProblemDetail body =
        ProblemDetailFactory.fromReason(
            ex.reasonCode(),
            status,
            ex.getMessage(),
            URI.create(request.getRequestURI()),
            correlationId(),
            null);
    return ResponseEntity.status(status)
        .contentType(MediaType.APPLICATION_PROBLEM_JSON)
        .body(body);
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  ResponseEntity<PcisProblemDetail> handleValidation(
      MethodArgumentNotValidException ex, HttpServletRequest request) {
    List<ProblemErrorEntry> errors =
        ex.getBindingResult().getFieldErrors().stream()
            .limit(20)
            .map(
                error ->
                    new ProblemErrorEntry(
                        ReasonCode.SYS_VALIDATION.code(),
                        error.getDefaultMessage(),
                        error.getField()))
            .toList();
    PcisProblemDetail body =
        ProblemDetailFactory.fromReason(
            ReasonCode.SYS_VALIDATION,
            HttpStatus.BAD_REQUEST,
            "Request validation failed",
            URI.create(request.getRequestURI()),
            correlationId(),
            errors);
    return ResponseEntity.badRequest()
        .contentType(MediaType.APPLICATION_PROBLEM_JSON)
        .body(body);
  }

  @ExceptionHandler(Exception.class)
  ResponseEntity<PcisProblemDetail> handleUnexpected(Exception ex, HttpServletRequest request) {
    if (ex instanceof PcisException pcisEx) {
      return handlePcisException(pcisEx, request);
    }
    PcisProblemDetail body =
        ProblemDetailFactory.unexpected(
            "An unexpected error occurred",
            URI.create(request.getRequestURI()),
            correlationId());
    return ResponseEntity.internalServerError()
        .contentType(MediaType.APPLICATION_PROBLEM_JSON)
        .body(body);
  }

  private static HttpStatus mapStatus(ReasonCode reason) {
    return switch (reason) {
      case SYS_VALIDATION -> HttpStatus.BAD_REQUEST;
      case SYS_UNAUTHORIZED -> HttpStatus.UNAUTHORIZED;
      case SYS_FORBIDDEN, AUTHZ_DENIED_NO_APPROVAL, AUTHZ_LIMIT_EXCEEDED -> HttpStatus.FORBIDDEN;
      case SYS_NOT_FOUND, CFG_TUNABLE_NOT_FOUND -> HttpStatus.NOT_FOUND;
      case SYS_CONFLICT -> HttpStatus.CONFLICT;
      case SYS_BUSINESS_RULE -> HttpStatus.UNPROCESSABLE_ENTITY;
      case PRM_NOT_IMPLEMENTED -> HttpStatus.NOT_IMPLEMENTED;
      default -> HttpStatus.INTERNAL_SERVER_ERROR;
    };
  }

  private static String correlationId() {
    String fromMdc = MDC.get("correlationId");
    return fromMdc != null && !fromMdc.isBlank() ? fromMdc : UUID.randomUUID().toString();
  }
}
