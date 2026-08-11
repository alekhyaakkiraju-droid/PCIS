package com.pcis.audit.config;

import static org.assertj.core.api.Assertions.assertThat;

import io.swagger.v3.oas.models.OpenAPI;
import org.junit.jupiter.api.Test;

class OpenApiConfigTest {

  @Test
  void publishesOpenApi31WithBearerSecurity() {
    OpenAPI openApi = new OpenApiConfig().auditOpenApi();

    assertThat(openApi.getInfo().getTitle()).isEqualTo("PCIS Audit Service");
    assertThat(openApi.getSecurity()).isNotEmpty();
    assertThat(openApi.getComponents()).isNotNull();
    assertThat(openApi.getComponents().getSecuritySchemes()).containsKey("bearerAuth");
  }
}
