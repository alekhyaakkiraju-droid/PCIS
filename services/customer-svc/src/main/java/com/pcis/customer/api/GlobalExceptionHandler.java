package com.pcis.customer.api;

import com.pcis.customer.domain.exception.CustomerNotFoundException;
import com.pcis.customer.domain.exception.DuplicateTaxIdException;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

  private static final URI DUPLICATE_TAX_ID_TYPE =
      URI.create("https://pcis.example/problems/duplicate-tax-id");
  private static final URI VALIDATION_TYPE =
      URI.create("https://pcis.example/problems/validation");
  private static final URI ACCESS_DENIED_TYPE =
      URI.create("https://pcis.example/problems/access-denied");
  private static final URI NOT_FOUND_TYPE =
      URI.create("https://pcis.example/problems/not-found");

  @ExceptionHandler(CustomerNotFoundException.class)
  ResponseEntity<ProblemDetail> handleCustomerNotFound(
      CustomerNotFoundException ex, HttpServletRequest request) {
    ProblemDetail problem =
        ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    problem.setType(NOT_FOUND_TYPE);
    problem.setTitle("Customer not found");
    problem.setInstance(URI.create(request.getRequestURI()));
    problem.setProperty("code", "CUSTOMER_NOT_FOUND");
    problem.setProperty("custId", ex.getCustId());
    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(problem);
  }

  @ExceptionHandler(DuplicateTaxIdException.class)
  ResponseEntity<ProblemDetail> handleDuplicateTaxId(
      DuplicateTaxIdException ex, HttpServletRequest request) {
    ProblemDetail problem =
        ProblemDetail.forStatusAndDetail(
            HttpStatus.CONFLICT, "A customer with this tax ID already exists");
    problem.setType(DUPLICATE_TAX_ID_TYPE);
    problem.setTitle("Duplicate tax ID");
    problem.setInstance(URI.create(request.getRequestURI()));
    problem.setProperty("code", ex.getReasonCode());
    problem.setProperty("existingCustId", ex.getExistingCustomer().custId());
    problem.setProperty("existingCustName", ex.getExistingCustomer().custName());
    problem.setProperty("existingCustStatus", ex.getExistingCustomer().custStatus());
    return ResponseEntity.status(HttpStatus.CONFLICT).body(problem);
  }

  @ExceptionHandler(IllegalArgumentException.class)
  ResponseEntity<ProblemDetail> handleIllegalArgument(
      IllegalArgumentException ex, HttpServletRequest request) {
    ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
    problem.setType(VALIDATION_TYPE);
    problem.setTitle("Invalid request");
    problem.setInstance(URI.create(request.getRequestURI()));
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
    return ResponseEntity.badRequest().body(problem);
  }

  @ExceptionHandler(AccessDeniedException.class)
  ResponseEntity<ProblemDetail> handleAccessDenied(
      AccessDeniedException ex, HttpServletRequest request) {
    ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, "Access denied");
    problem.setType(ACCESS_DENIED_TYPE);
    problem.setTitle("Forbidden");
    problem.setInstance(URI.create(request.getRequestURI()));
    return ResponseEntity.status(HttpStatus.FORBIDDEN).body(problem);
  }
}
