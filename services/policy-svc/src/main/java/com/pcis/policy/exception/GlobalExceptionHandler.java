package com.pcis.policy.exception;

import com.pcis.error.ResourceNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.MDC;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

  private static final URI VALIDATION_TYPE =
      URI.create("https://pcis.example/problems/validation");
  private static final URI NOT_FOUND_TYPE = URI.create("https://pcis.example/problems/not-found");
  private static final URI CONFLICT_TYPE = URI.create("https://pcis.example/problems/conflict");
  private static final URI ACCESS_DENIED_TYPE =
      URI.create("https://pcis.example/problems/access-denied");
  private static final URI AUTHZ_UNAVAILABLE_TYPE =
      URI.create("https://pcis.example/problems/authz-unavailable");
  private static final URI INTERNAL_TYPE =
      URI.create("https://pcis.example/problems/internal-error");

  @ExceptionHandler(PolicyNotFoundException.class)
  ResponseEntity<ProblemDetail> handlePolicyNotFound(
      PolicyNotFoundException ex, HttpServletRequest request) {
    ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    problem.setType(NOT_FOUND_TYPE);
    problem.setTitle("Policy not found");
    problem.setInstance(URI.create(request.getRequestURI()));
    problem.setProperty("code", "POLICY_NOT_FOUND");
    problem.setProperty("policyNumber", ex.getPolicyNumber());
    addCorrelationId(problem);
    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(problem);
  }

  @ExceptionHandler(ResourceNotFoundException.class)
  ResponseEntity<ProblemDetail> handleResourceNotFound(
      ResourceNotFoundException ex, HttpServletRequest request) {
    ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    problem.setType(NOT_FOUND_TYPE);
    problem.setTitle("Not found");
    problem.setInstance(URI.create(request.getRequestURI()));
    addCorrelationId(problem);
    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(problem);
  }

  @ExceptionHandler(InvalidStateTransitionException.class)
  ResponseEntity<ProblemDetail> handleInvalidState(
      InvalidStateTransitionException ex, HttpServletRequest request) {
    ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
    problem.setType(CONFLICT_TYPE);
    problem.setTitle("Invalid state transition");
    problem.setInstance(URI.create(request.getRequestURI()));
    addCorrelationId(problem);
    return ResponseEntity.status(HttpStatus.CONFLICT).body(problem);
  }

  @ExceptionHandler(OptimisticLockingFailureException.class)
  ResponseEntity<ProblemDetail> handleOptimisticLock(
      OptimisticLockingFailureException ex, HttpServletRequest request) {
    ProblemDetail problem =
        ProblemDetail.forStatusAndDetail(
            HttpStatus.CONFLICT, "Concurrent modification detected — please retry");
    problem.setType(CONFLICT_TYPE);
    problem.setTitle("Concurrent modification");
    problem.setInstance(URI.create(request.getRequestURI()));
    addCorrelationId(problem);
    return ResponseEntity.status(HttpStatus.CONFLICT).body(problem);
  }

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
    Map<String, String> errors = new LinkedHashMap<>();
    ex.getBindingResult()
        .getFieldErrors()
        .forEach(error -> errors.put(error.getField(), error.getDefaultMessage()));
    problem.setProperty("errors", errors);
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

  @ExceptionHandler(AuthzServiceUnavailableException.class)
  ResponseEntity<ProblemDetail> handleAuthzUnavailable(
      AuthzServiceUnavailableException ex, HttpServletRequest request) {
    ProblemDetail problem =
        ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE, ex.getMessage());
    problem.setType(AUTHZ_UNAVAILABLE_TYPE);
    problem.setTitle("Authorization service unavailable");
    problem.setInstance(URI.create(request.getRequestURI()));
    addCorrelationId(problem);
    return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(problem);
  }

  @ExceptionHandler(Exception.class)
  ResponseEntity<ProblemDetail> handleGeneric(Exception ex, HttpServletRequest request) {
    ProblemDetail problem =
        ProblemDetail.forStatusAndDetail(
            HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred");
    problem.setType(INTERNAL_TYPE);
    problem.setTitle("Internal server error");
    problem.setInstance(URI.create(request.getRequestURI()));
    addCorrelationId(problem);
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(problem);
  }

  private static void addCorrelationId(ProblemDetail problem) {
    String correlationId = MDC.get("correlationId");
    if (correlationId != null) {
      problem.setProperty("correlationId", correlationId);
    }
  }
}
