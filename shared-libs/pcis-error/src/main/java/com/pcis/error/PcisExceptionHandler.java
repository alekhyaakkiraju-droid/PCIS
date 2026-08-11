package com.pcis.error;

import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class PcisExceptionHandler {

  private static final Logger log = LoggerFactory.getLogger(PcisExceptionHandler.class);

  @ExceptionHandler(PcisException.class)
  public ResponseEntity<PcisProblemDetail> handlePcisException(
      PcisException ex, HttpServletRequest request) {
    HttpStatus status = mapStatus(ex.reasonCode());
    return respond(ex.reasonCode(), status, ex.getMessage(), request, ex);
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
    return respondWithErrors(
        ReasonCode.SYS_VALIDATION,
        HttpStatus.BAD_REQUEST,
        "Request validation failed",
        request,
        ex,
        errors);
  }

  @ExceptionHandler(AccessDeniedException.class)
  ResponseEntity<PcisProblemDetail> handleAccessDenied(
      AccessDeniedException ex, HttpServletRequest request) {
    return respond(
        ReasonCode.SYS_FORBIDDEN,
        HttpStatus.FORBIDDEN,
        "Access denied",
        request,
        ex);
  }

  @ExceptionHandler(AuthenticationException.class)
  ResponseEntity<PcisProblemDetail> handleAuthentication(
      AuthenticationException ex, HttpServletRequest request) {
    return respond(
        ReasonCode.SYS_UNAUTHORIZED,
        HttpStatus.UNAUTHORIZED,
        "Unauthenticated",
        request,
        ex);
  }

  @ExceptionHandler(DataIntegrityViolationException.class)
  ResponseEntity<PcisProblemDetail> handleDataIntegrityViolation(
      DataIntegrityViolationException ex, HttpServletRequest request) {
    return respond(
        ReasonCode.SYS_CONFLICT,
        HttpStatus.CONFLICT,
        "Resource conflict",
        request,
        ex);
  }

  @ExceptionHandler(Exception.class)
  ResponseEntity<PcisProblemDetail> handleUnexpected(Exception ex, HttpServletRequest request) {
    if (ex instanceof PcisException pcisEx) {
      return handlePcisException(pcisEx, request);
    }
    return respond(
        ReasonCode.SYS_UNEXPECTED,
        HttpStatus.INTERNAL_SERVER_ERROR,
        "An unexpected error occurred",
        request,
        ex);
  }

  private ResponseEntity<PcisProblemDetail> respond(
      ReasonCode reason, HttpStatus status, String detail, HttpServletRequest request, Exception ex) {
    return respondWithErrors(reason, status, detail, request, ex, null);
  }

  private ResponseEntity<PcisProblemDetail> respondWithErrors(
      ReasonCode reason,
      HttpStatus status,
      String detail,
      HttpServletRequest request,
      Exception ex,
      List<ProblemErrorEntry> errors) {
    String correlationId = correlationId();
    logStructured(status, reason.code(), correlationId, request.getRequestURI(), detail, ex);
    PcisProblemDetail body =
        ProblemDetailFactory.fromReason(
            reason,
            status,
            detail,
            URI.create(request.getRequestURI()),
            correlationId,
            errors);
    return ResponseEntity.status(status)
        .contentType(MediaType.APPLICATION_PROBLEM_JSON)
        .body(body);
  }

  private static void logStructured(
      HttpStatus status,
      String code,
      String correlationId,
      String path,
      String detail,
      Exception ex) {
    if (status.is4xxClientError()) {
      log.warn(
          "pcis.error.handled code={} status={} correlationId={} path={} detail={}",
          code,
          status.value(),
          correlationId,
          path,
          detail);
    } else {
      log.error(
          "pcis.error.handled code={} status={} correlationId={} path={} detail={}",
          code,
          status.value(),
          correlationId,
          path,
          detail,
          ex);
    }
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
      case AUD_WRITE_FAILURE -> HttpStatus.INTERNAL_SERVER_ERROR;
      default -> HttpStatus.INTERNAL_SERVER_ERROR;
    };
  }

  private static String correlationId() {
    String fromMdc = MDC.get("correlationId");
    return fromMdc != null && !fromMdc.isBlank() ? fromMdc : UUID.randomUUID().toString();
  }
}
