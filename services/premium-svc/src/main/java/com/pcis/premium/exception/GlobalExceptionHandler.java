package com.pcis.premium.exception;

import com.pcis.error.ReasonCode;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<Map<String, Object>> handleValidation(
      MethodArgumentNotValidException ex, HttpServletRequest request) {
    List<Map<String, String>> violations =
        ex.getBindingResult().getFieldErrors().stream().map(this::toViolation).toList();
    Map<String, Object> body = problemBody(
        ReasonCode.SYS_VALIDATION,
        HttpStatus.BAD_REQUEST,
        "Validation failed",
        request.getRequestURI());
    body.put("violations", violations);
    return ResponseEntity.badRequest()
        .contentType(MediaType.APPLICATION_PROBLEM_JSON)
        .body(body);
  }

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<Map<String, Object>> handleIllegalArgument(
      IllegalArgumentException ex, HttpServletRequest request) {
    Map<String, Object> body =
        problemBody(
            ReasonCode.SYS_VALIDATION,
            HttpStatus.BAD_REQUEST,
            ex.getMessage(),
            request.getRequestURI());
    return ResponseEntity.badRequest()
        .contentType(MediaType.APPLICATION_PROBLEM_JSON)
        .body(body);
  }

  @ExceptionHandler(AuthenticationException.class)
  public ResponseEntity<Map<String, Object>> handleAuthentication(
      AuthenticationException ex, HttpServletRequest request) {
    Map<String, Object> body =
        problemBody(
            ReasonCode.SYS_UNAUTHORIZED,
            HttpStatus.UNAUTHORIZED,
            "Unauthenticated",
            request.getRequestURI());
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
        .contentType(MediaType.APPLICATION_PROBLEM_JSON)
        .body(body);
  }

  @ExceptionHandler(AccessDeniedException.class)
  public ResponseEntity<Map<String, Object>> handleAccessDenied(
      AccessDeniedException ex, HttpServletRequest request) {
    Map<String, Object> body =
        problemBody(
            ReasonCode.SYS_FORBIDDEN,
            HttpStatus.FORBIDDEN,
            "Forbidden",
            request.getRequestURI());
    return ResponseEntity.status(HttpStatus.FORBIDDEN)
        .contentType(MediaType.APPLICATION_PROBLEM_JSON)
        .body(body);
  }

  private Map<String, String> toViolation(FieldError error) {
    Map<String, String> violation = new LinkedHashMap<>();
    violation.put("field", error.getField());
    violation.put("message", error.getDefaultMessage());
    violation.put("rejectedValue", String.valueOf(error.getRejectedValue()));
    return violation;
  }

  private static Map<String, Object> problemBody(
      ReasonCode code, HttpStatus status, String detail, String instance) {
    Map<String, Object> body = new LinkedHashMap<>();
    body.put("type", code.type().toString());
    body.put("title", code.title());
    body.put("status", status.value());
    body.put("detail", detail);
    body.put("instance", instance);
    body.put("code", code.code());
    body.put(
        "correlation_id",
        MDC.get("correlationId") != null ? MDC.get("correlationId") : "unknown");
    return body;
  }
}
