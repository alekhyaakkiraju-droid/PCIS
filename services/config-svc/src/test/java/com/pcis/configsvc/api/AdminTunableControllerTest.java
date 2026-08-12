package com.pcis.configsvc.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pcis.configsvc.api.dto.TunableResponse;
import com.pcis.configsvc.api.dto.UpdateTunableRequest;
import com.pcis.configsvc.application.AdminTunableService;
import com.pcis.configsvc.config.SecurityConfig;
import com.pcis.configsvc.support.TestJwtGenerator;
import com.pcis.error.PcisExceptionHandler;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = AdminTunableController.class)
@Import({SecurityConfig.class, PcisExceptionHandler.class})
class AdminTunableControllerTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;

  @MockBean private AdminTunableService adminTunableService;
  @MockBean private JwtDecoder jwtDecoder;

  @Test
  void listRequiresConfigurationAdmin() throws Exception {
    mockMvc.perform(get("/api/v1/admin/tunables")).andExpect(status().isUnauthorized());
  }

  @Test
  void listForbiddenWithoutAdminAuthority() throws Exception {
    mockMvc
        .perform(
            get("/api/v1/admin/tunables")
                .with(
                    SecurityMockMvcRequestPostProcessors.jwt()
                        .jwt(TestJwtGenerator.unauthorizedReader("csr-1"))
                        .authorities(new SimpleGrantedAuthority("customer:read"))))
        .andExpect(status().isForbidden());
  }

  @Test
  void listReturnsTunablesForAdmin() throws Exception {
    TunableResponse row =
        new TunableResponse(
            "billing.leadDays",
            "BIL",
            "I",
            null,
            new BigDecimal("15.00"),
            new BigDecimal("1.00"),
            new BigDecimal("90.00"),
            "days",
            "Billing lead days",
            LocalDate.parse("2026-01-01"),
            null,
            1);
    when(adminTunableService.listCurrent(any(Pageable.class)))
        .thenReturn(new PageImpl<>(List.of(row)));

    mockMvc
        .perform(
            get("/api/v1/admin/tunables")
                .with(
                    SecurityMockMvcRequestPostProcessors.jwt()
                        .jwt(TestJwtGenerator.configAdmin("admin-1"))
                        .authorities(new SimpleGrantedAuthority("configuration-admin"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[0].key").value("billing.leadDays"))
        .andExpect(jsonPath("$.content[0].version").value(1));
  }

  @Test
  void updateReturnsNewVersion() throws Exception {
    TunableResponse updated =
        new TunableResponse(
            "billing.leadDays",
            "BIL",
            "I",
            null,
            new BigDecimal("20.00"),
            new BigDecimal("1.00"),
            new BigDecimal("90.00"),
            "days",
            "Billing lead days",
            LocalDate.parse("2026-08-11"),
            null,
            2);
    when(adminTunableService.update(eq("billing.leadDays"), any(UpdateTunableRequest.class)))
        .thenReturn(updated);

    UpdateTunableRequest body =
        new UpdateTunableRequest(
            new BigDecimal("20.00"),
            null,
            LocalDate.parse("2026-08-11"),
            1,
            "Regulatory adjustment for billing lead time");

    mockMvc
        .perform(
            put("/api/v1/admin/tunables/billing.leadDays")
                .with(
                    SecurityMockMvcRequestPostProcessors.jwt()
                        .jwt(TestJwtGenerator.configAdmin("admin-1"))
                        .authorities(new SimpleGrantedAuthority("configuration-admin")))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.version").value(2))
        .andExpect(jsonPath("$.numericValue").value(20.00));

    verify(adminTunableService).update(eq("billing.leadDays"), any(UpdateTunableRequest.class));
  }
}
