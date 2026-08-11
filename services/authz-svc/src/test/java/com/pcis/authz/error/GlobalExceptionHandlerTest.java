package com.pcis.authz.error;

import static org.assertj.core.api.Assertions.assertThat;

import com.pcis.authz.error.GlobalExceptionHandler.ProblemExtensions;
import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

class GlobalExceptionHandlerTest {

  private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

  @Test
  void mapsValidationExceptionToProblemDetail() throws Exception {
    var target = new Object();
    var bindingResult = new BeanPropertyBindingResult(target, "request");
    bindingResult.addError(new FieldError("request", "resource", "must not be blank"));
    Method method = GlobalExceptionHandlerTest.class.getDeclaredMethod("sampleMethod", String.class);
    var parameter = new MethodParameter(method, 0);
    var ex = new MethodArgumentNotValidException(parameter, bindingResult);

    var response = handler.handleValidation(ex, request("/v1/authz/decisions"));

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().getTitle()).isEqualTo("Authorization request validation failed");
    assertThat(response.getBody().getProperties()).containsKey("violations");
  }

  @Test
  void mapsAccessDeniedToProblemDetail() {
    var response =
        handler.handleAccessDenied(new AccessDeniedException("denied"), request("/v1/authz/decisions"));

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().getTitle()).isEqualTo("Forbidden");
    assertThat(response.getBody().getDetail()).isEqualTo("Access denied");
  }

  @Test
  void problemExtensionsHelperBuildsMap() {
    assertThat(ProblemExtensions.of("key", "value")).containsEntry("key", "value");
  }

  @SuppressWarnings("unused")
  private void sampleMethod(String value) {}

  private static MockHttpServletRequest request(String path) {
    var request = new MockHttpServletRequest();
    request.setRequestURI(path);
    return request;
  }
}
