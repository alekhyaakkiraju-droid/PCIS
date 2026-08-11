package com.pcis.audit.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

  @Bean
  OpenAPI auditOpenApi() {
    return new OpenAPI()
        .info(
            new Info()
                .title("PCIS Audit Service")
                .version("v1")
                .description(
                    "Immutable audit trail API replacing AUDLOG01. "
                        + "Contract: contracts/audlog01-v1-contract.yaml"))
        .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
        .schemaRequirement(
            "bearerAuth",
            new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .description("OAuth2 bearer token with audit:write scope"));
  }
}
