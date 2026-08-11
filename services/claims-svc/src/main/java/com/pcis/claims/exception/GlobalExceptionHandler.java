package com.pcis.claims.exception;

import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class GlobalExceptionHandler {

  private static final URI CONFLICT_TYPE = URI.create("https://pcis.example/problems/conflict");
  private static final URI AUTHORIZATION_DENIED_TYPE =
      URI.create("https://pcis.example/problems/authorization-denied");

  @ExceptionHandler(PaymentAuthorizationException.class)
  ResponseEntity<ProblemDetail> handlePaymentAuthorization(
      PaymentAuthorizationException ex, HttpServletRequest request) {
    ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, ex.getMessage());
    problem.setType(AUTHORIZATION_DENIED_TYPE);
    problem.setTitle("Authorization denied");
    problem.setInstance(URI.create(request.getRequestURI()));
    problem.setProperty("code", "PAYMENT_AUTHORIZATION_DENIED");
    if (ex instanceof AuthorityLimitExceededException limitEx) {
      problem.setProperty("authorityLimit", limitEx.getAuthorityLimit());
      problem.setProperty("requestedAmount", limitEx.getRequestedAmount());
    }
    if (ex instanceof InsufficientReserveException reserveEx) {
      problem.setProperty("outstanding", reserveEx.getOutstanding());
    }
    addCorrelationId(problem);
    return ResponseEntity.status(HttpStatus.FORBIDDEN).body(problem);
  }

  @ExceptionHandler(DuplicateApprovalException.class)
  ResponseEntity<ProblemDetail> handleDuplicateApproval(
      DuplicateApprovalException ex, HttpServletRequest request) {
    ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
    problem.setType(CONFLICT_TYPE);
    problem.setTitle("Duplicate approval");
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

  private static void addCorrelationId(ProblemDetail problem) {
    String correlationId = MDC.get("correlationId");
    if (correlationId != null) {
      problem.setProperty("correlationId", correlationId);
    }
  }
}
