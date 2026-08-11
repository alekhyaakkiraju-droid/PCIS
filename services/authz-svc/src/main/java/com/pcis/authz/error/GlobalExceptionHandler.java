package com.pcis.authz.error;

import com.fasterxml.jackson.annotation.JsonInclude;
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

  private static final URI VALIDATION_TYPE =
      URI.create("https://pcis.example/problems/authz-validation");
  private static final URI ACCESS_DENIED_TYPE =
      URI.create("https://pcis.example/problems/access-denied");

  @ExceptionHandler(MethodArgumentNotValidException.class)
  ResponseEntity<ProblemDetail> handleValidation(
      MethodArgumentNotValidException ex, HttpServletRequest request) {
    ProblemDetail problem =
        ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Request validation failed");
    problem.setType(VALIDATION_TYPE);
    problem.setTitle("Authorization request validation failed");
    problem.setInstance(URI.create(request.getRequestURI()));
    problem.setProperty(
        "violations",
        ex.getBindingResult().getFieldErrors().stream()
            .map(error -> error.getField() + ": " + error.getDefaultMessage())
            .toList());
    return ResponseEntity.badRequest().body(problem);
  }

  @ExceptionHandler(AccessDeniedException.class)
  ResponseEntity<ProblemDetail> handleAccessDenied(
      AccessDeniedException ex, HttpServletRequest request) {
    ProblemDetail problem =
        ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, "Access denied");
    problem.setType(ACCESS_DENIED_TYPE);
    problem.setTitle("Forbidden");
    problem.setInstance(URI.create(request.getRequestURI()));
    return ResponseEntity.status(HttpStatus.FORBIDDEN).body(problem);
  }

  /** RFC 9457-compatible extension payload for OpenAPI consumers. */
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public record ProblemExtensions(Map<String, Object> extensions) {
    public static Map<String, Object> of(String key, Object value) {
      Map<String, Object> map = new LinkedHashMap<>();
      map.put(key, value);
      return map;
    }
  }
}
