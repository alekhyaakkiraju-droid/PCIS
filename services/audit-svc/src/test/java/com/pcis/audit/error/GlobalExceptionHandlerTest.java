package com.pcis.audit.error;

import static org.assertj.core.api.Assertions.assertThat;

import com.pcis.audit.contract.AuditValidationException;
import com.pcis.audit.contract.UnknownAuditActionException;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.access.AccessDeniedException;

class GlobalExceptionHandlerTest {

  private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

  @Test
  void mapsValidationExceptionToProblemDetail() {
    var request = new MockHttpServletRequest("POST", "/v1/audit/events");
    var response =
        handler.handleValidation(
            new AuditValidationException(List.of("action is required")), request);

    assertThat(response.getStatusCode().value()).isEqualTo(400);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().getTitle()).isEqualTo("Audit event validation failed");
    assertThat(response.getBody().getProperties()).containsKey("violations");
  }

  @Test
  void mapsUnknownActionToProblemDetail() {
    var request = new MockHttpServletRequest("POST", "/v1/audit/events");
    var response =
        handler.handleUnknownAction(new UnknownAuditActionException("ZZZ"), request);

    assertThat(response.getStatusCode().value()).isEqualTo(400);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().getProperties()).containsEntry("legacy_action", "ZZZ");
  }

  @Test
  void mapsAccessDeniedToProblemDetail() {
    var request = new MockHttpServletRequest("POST", "/v1/audit/events");
    var response = handler.handleAccessDenied(new AccessDeniedException("nope"), request);

    assertThat(response.getStatusCode().value()).isEqualTo(403);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().getTitle()).isEqualTo("Forbidden");
  }

  @Test
  void problemExtensionsHelperBuildsMap() {
    assertThat(GlobalExceptionHandler.ProblemExtensions.of("key", "value"))
        .isEqualTo(Map.of("key", "value"));
  }
}
