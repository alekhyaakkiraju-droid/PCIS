package com.pcis.error;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootTest(classes = PcisExceptionHandlerTest.TestApplication.class)
@AutoConfigureMockMvc
class PcisExceptionHandlerTest {

  @Autowired private MockMvc mockMvc;

  private ListAppender<ILoggingEvent> logAppender;
  private Logger handlerLogger;

  @BeforeEach
  void attachLogAppender() {
    handlerLogger = (Logger) LoggerFactory.getLogger(PcisExceptionHandler.class);
    logAppender = new ListAppender<>();
    logAppender.start();
    handlerLogger.addAppender(logAppender);
  }

  @AfterEach
  void detachLogAppender() {
    if (handlerLogger != null && logAppender != null) {
      handlerLogger.detachAppender(logAppender);
    }
  }

  @Test
  void mapsPcisExceptionToProblemDetail() throws Exception {
    mockMvc
        .perform(get("/test/not-implemented"))
        .andExpect(status().isNotImplemented())
        .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.code").value("PRM_NOT_IMPLEMENTED"))
        .andExpect(jsonPath("$.correlation_id").exists())
        .andExpect(jsonPath("$.title").value("Premium rating not implemented"));
  }

  @ParameterizedTest
  @CsvSource({
    "/test/not-found,404,SYS_NOT_FOUND",
    "/test/validation,400,SYS_VALIDATION",
    "/test/authorization-denied,403,AUTHZ_DENIED_NO_APPROVAL",
    "/test/conflict,409,SYS_CONFLICT",
    "/test/audit-write,500,AUD_WRITE_FAILURE"
  })
  void mapsPcisSubclassesToProblemDetail(String path, int status, String code) throws Exception {
    mockMvc
        .perform(get(path))
        .andExpect(status().is(status))
        .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.code").value(code))
        .andExpect(jsonPath("$.correlation_id").exists())
        .andExpect(jsonPath("$.status").value(status));
  }

  @Test
  void mapsAccessDeniedExceptionTo403() throws Exception {
    mockMvc
        .perform(get("/test/access-denied"))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("SYS_FORBIDDEN"))
        .andExpect(jsonPath("$.detail").value("Access denied"));
  }

  @Test
  void mapsAuthenticationExceptionTo401() throws Exception {
    mockMvc
        .perform(get("/test/authentication"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("SYS_UNAUTHORIZED"))
        .andExpect(jsonPath("$.detail").value("Unauthenticated"));
  }

  @Test
  void mapsDataIntegrityViolationTo409() throws Exception {
    String body =
        mockMvc
            .perform(get("/test/data-integrity"))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("SYS_CONFLICT"))
            .andExpect(jsonPath("$.detail").value("Resource conflict"))
            .andReturn()
            .getResponse()
            .getContentAsString();
    assertThat(body).doesNotContain("SQL");
    assertThat(body).doesNotContain("duplicate key");
  }

  @Test
  void unexpectedExceptionDoesNotLeakStackTrace() throws Exception {
    String body =
        mockMvc
            .perform(get("/test/unexpected"))
            .andExpect(status().isInternalServerError())
            .andExpect(jsonPath("$.code").value("SYS_UNEXPECTED"))
            .andReturn()
            .getResponse()
            .getContentAsString();
    assertThat(body).doesNotContain("RuntimeException");
    assertThat(body).doesNotContain("at com.pcis");
    assertThat(body).doesNotContain("secret internal detail");
  }

  @Test
  void logsWarnFor4xxAndErrorFor5xx() throws Exception {
    mockMvc.perform(get("/test/not-found")).andExpect(status().isNotFound());
    mockMvc.perform(get("/test/unexpected")).andExpect(status().isInternalServerError());

    assertThat(logAppender.list).hasSizeGreaterThanOrEqualTo(2);
    assertThat(logAppender.list.get(0).getLevel()).isEqualTo(Level.WARN);
    assertThat(logAppender.list.get(0).getFormattedMessage()).contains("SYS_NOT_FOUND");
    assertThat(logAppender.list.get(1).getLevel()).isEqualTo(Level.ERROR);
    assertThat(logAppender.list.get(1).getFormattedMessage()).contains("SYS_UNEXPECTED");
  }

  @SpringBootApplication
  @Import({PcisExceptionHandler.class, TestController.class})
  static class TestApplication {}

  @RestController
  static class TestController {

    @GetMapping("/test/not-implemented")
    void notImplemented() {
      TestExceptions.throwNotImplemented();
    }

    @GetMapping("/test/not-found")
    void notFound() {
      TestExceptions.throwResourceNotFound();
    }

    @GetMapping("/test/validation")
    void validation() {
      TestExceptions.throwValidation();
    }

    @GetMapping("/test/authorization-denied")
    void authorizationDenied() {
      TestExceptions.throwAuthorizationDenied();
    }

    @GetMapping("/test/conflict")
    void conflict() {
      TestExceptions.throwConflict();
    }

    @GetMapping("/test/audit-write")
    void auditWrite() {
      TestExceptions.throwAuditWrite();
    }

    @GetMapping("/test/access-denied")
    void accessDenied() {
      throw new AccessDeniedException("Forbidden");
    }

    @GetMapping("/test/authentication")
    void authentication() {
      throw new BadCredentialsException("Bad credentials");
    }

    @GetMapping("/test/data-integrity")
    void dataIntegrity() {
      throw new DataIntegrityViolationException("duplicate key value violates unique constraint");
    }

    @GetMapping("/test/unexpected")
    void unexpected() {
      TestExceptions.throwUnexpected();
    }
  }
}
