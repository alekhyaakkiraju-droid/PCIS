package com.pcis.billing.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.pcis.billing.api.BillingScheduleController;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = BillingScheduleController.class)
@Import(SecurityConfig.class)
class SecurityConfigTest {

  @Autowired private MockMvc mockMvc;

  @MockBean private JwtDecoder jwtDecoder;

  @Test
  void healthEndpointPermittedWithoutAuthentication() throws Exception {
    mockMvc
        .perform(get("/actuator/health"))
        .andExpect(result -> assertThat(result.getResponse().getStatus()).isNotIn(401, 403));
  }

  @Test
  void scheduleEndpointReturns401WithoutJwt() throws Exception {
    mockMvc.perform(get("/v1/billing/schedules/POL123")).andExpect(status().isUnauthorized());
  }

  @Test
  void scheduleEndpointPassesAuthWithValidJwt() throws Exception {
    mockMvc
        .perform(
            get("/v1/billing/schedules/POL123")
                .with(
                    jwt()
                        .jwt(
                            Jwt.withTokenValue("test")
                                .header("alg", "none")
                                .subject("billing-user")
                                .claim("scope", List.of("billing:read"))
                                .build())
                        .authorities(new SimpleGrantedAuthority("billing:read"))))
        .andExpect(status().isNotImplemented());
  }

  @Test
  void createScheduleReturns401WithoutJwt() throws Exception {
    mockMvc.perform(post("/v1/billing/schedules")).andExpect(status().isUnauthorized());
  }
}
