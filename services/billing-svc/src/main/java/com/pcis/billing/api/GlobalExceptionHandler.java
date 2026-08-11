package com.pcis.billing.api;

import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class GlobalExceptionHandler {

  private static final URI VALIDATION_TYPE =
      URI.create("https://pcis.example/problems/validation");
  private static final URI ACCESS_DENIED_TYPE =
      URI.create("https://pcis.example/problems/access-denied");

  @ExceptionHandler(IllegalArgumentException.class)
  ResponseEntity<ProblemDetail> handleIllegalArgument(
      IllegalArgumentException ex, HttpServletRequest request) {
    ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
    problem.setType(VALIDATION_TYPE);
    problem.setTitle("Invalid request");
    problem.setInstance(URI.create(request.getRequestURI()));
    addCorrelationId(problem);
    return ResponseEntity.badRequest().body(problem);
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  ResponseEntity<ProblemDetail> handleValidation(
      MethodArgumentNotValidException ex, HttpServletRequest request) {
    ProblemDetail problem =
        ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Request validation failed");
    problem.setType(VALIDATION_TYPE);
    problem.setTitle("Validation failed");
    problem.setInstance(URI.create(request.getRequestURI()));
    addCorrelationId(problem);
    return ResponseEntity.badRequest().body(problem);
  }

  @ExceptionHandler(AccessDeniedException.class)
  ResponseEntity<ProblemDetail> handleAccessDenied(
      AccessDeniedException ex, HttpServletRequest request) {
    ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, "Access denied");
    problem.setType(ACCESS_DENIED_TYPE);
    problem.setTitle("Forbidden");
    problem.setInstance(URI.create(request.getRequestURI()));
    addCorrelationId(problem);
    return ResponseEntity.status(HttpStatus.FORBIDDEN).body(problem);
  }

  private static void addCorrelationId(ProblemDetail problem) {
    String correlationId = MDC.get("correlationId");
    if (correlationId != null) {
      problem.setProperty("correlationId", correlationId);
    }
  }
}
