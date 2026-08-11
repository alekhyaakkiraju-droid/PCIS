package com.pcis.error;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootTest(classes = PcisExceptionHandlerTest.TestApplication.class)
@AutoConfigureMockMvc
class PcisExceptionHandlerTest {

  @Autowired private MockMvc mockMvc;

  @Test
  void mapsPcisExceptionToProblemDetail() throws Exception {
    mockMvc
        .perform(get("/test/pcis-error"))
        .andExpect(status().isNotImplemented())
        .andExpect(jsonPath("$.code").value("PRM_NOT_IMPLEMENTED"))
        .andExpect(jsonPath("$.correlation_id").exists())
        .andExpect(jsonPath("$.title").value("Premium rating not implemented"));
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
  }

  @SpringBootApplication
  @Import({PcisExceptionHandler.class, TestController.class})
  static class TestApplication {}

  @RestController
  static class TestController {

    @GetMapping(value = "/test/pcis-error", produces = MediaType.APPLICATION_PROBLEM_JSON_VALUE)
    void pcisError() {
      throw new TerminalPcisException(
          ReasonCode.PRM_NOT_IMPLEMENTED,
          "Premium rating not implemented",
          "test-user",
          "premium/calculation",
          "create");
    }

    @GetMapping("/test/unexpected")
    void unexpected() {
      throw new RuntimeException("secret internal detail");
    }
  }
}
