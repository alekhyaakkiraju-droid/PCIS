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
import com.pcis.billing.domain.exception.DuplicatePaymentException;
import com.pcis.billing.domain.exception.NoOutstandingBalanceException;
import com.pcis.billing.domain.exception.OverApplicationException;
import com.pcis.billing.domain.exception.PolicyNotFoundException;
import org.springframework.dao.OptimisticLockingFailureException;

@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class GlobalExceptionHandler {

  private static final URI VALIDATION_TYPE =
      URI.create("https://pcis.example/problems/validation-error");
  private static final URI ACCESS_DENIED_TYPE =
      URI.create("https://pcis.example/problems/access-denied");
  private static final URI OVER_APPLICATION_TYPE =
      URI.create("https://pcis.example/problems/over-application");
  private static final URI DUPLICATE_PAYMENT_TYPE =
      URI.create("https://pcis.example/problems/duplicate-payment");
  private static final URI POLICY_NOT_FOUND_TYPE =
      URI.create("https://pcis.example/problems/policy-not-found");
  private static final URI NO_OUTSTANDING_TYPE =
      URI.create("https://pcis.example/problems/no-outstanding-balance");
  private static final URI CONCURRENT_MODIFICATION_TYPE =
      URI.create("https://pcis.example/problems/concurrent-modification");

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

  @ExceptionHandler(OverApplicationException.class)
  ResponseEntity<ProblemDetail> handleOverApplication(
      OverApplicationException ex, HttpServletRequest request) {
    ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
    problem.setType(OVER_APPLICATION_TYPE);
    problem.setTitle("Over-application rejected");
    problem.setProperty("maxApplicableAmount", ex.getMaxApplicableAmount().toPlainString());
    problem.setInstance(URI.create(request.getRequestURI()));
    addCorrelationId(problem);
    return ResponseEntity.badRequest().body(problem);
  }

  @ExceptionHandler(NoOutstandingBalanceException.class)
  ResponseEntity<ProblemDetail> handleNoOutstanding(
      NoOutstandingBalanceException ex, HttpServletRequest request) {
    ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
    problem.setType(NO_OUTSTANDING_TYPE);
    problem.setTitle("No outstanding balance");
    problem.setInstance(URI.create(request.getRequestURI()));
    addCorrelationId(problem);
    return ResponseEntity.badRequest().body(problem);
  }

  @ExceptionHandler(DuplicatePaymentException.class)
  ResponseEntity<ProblemDetail> handleDuplicatePayment(
      DuplicatePaymentException ex, HttpServletRequest request) {
    ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
    problem.setType(DUPLICATE_PAYMENT_TYPE);
    problem.setTitle("Duplicate payment");
    problem.setProperty("paymentId", ex.getPaymentRef());
    problem.setInstance(URI.create(request.getRequestURI()));
    addCorrelationId(problem);
    return ResponseEntity.status(HttpStatus.CONFLICT).body(problem);
  }

  @ExceptionHandler(PolicyNotFoundException.class)
  ResponseEntity<ProblemDetail> handlePolicyNotFound(
      PolicyNotFoundException ex, HttpServletRequest request) {
    ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    problem.setType(POLICY_NOT_FOUND_TYPE);
    problem.setTitle("Policy not found");
    problem.setInstance(URI.create(request.getRequestURI()));
    addCorrelationId(problem);
    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(problem);
  }

  @ExceptionHandler(OptimisticLockingFailureException.class)
  ResponseEntity<ProblemDetail> handleOptimisticLock(
      OptimisticLockingFailureException ex, HttpServletRequest request) {
    ProblemDetail problem =
        ProblemDetail.forStatusAndDetail(
            HttpStatus.CONFLICT, "Concurrent modification detected; retry the payment submission");
    problem.setType(CONCURRENT_MODIFICATION_TYPE);
    problem.setTitle("Concurrent modification");
    problem.setInstance(URI.create(request.getRequestURI()));
    addCorrelationId(problem);
    return ResponseEntity.status(HttpStatus.CONFLICT).body(problem);
  }

  private static void addCorrelationId(ProblemDetail problem) {
    String correlationId = MDC.get("correlationId");
    if (correlationId != null) {
      problem.setProperty("correlationId", correlationId);
    }
  }
}
