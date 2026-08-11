package com.pcis.premium.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.pcis.premium.application.PremiumRatingService;
import com.pcis.premium.config.SecurityConfig;
import com.pcis.premium.dto.PremiumCalculationResponse;
import com.pcis.premium.exception.GlobalExceptionHandler;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = PremiumRatingController.class)
@Import({SecurityConfig.class, PremiumRatingController.class, GlobalExceptionHandler.class})
class PremiumRatingControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockBean private PremiumRatingService ratingService;

  @Test
  void postCalculationReturnsBreakdownForAuthorizedRater() throws Exception {
    when(ratingService.createCalculation(any()))
        .thenReturn(
            new PremiumCalculationResponse(
                "calc-1",
                "00",
                "APPROVE",
                new BigDecimal("35.0000"),
                "B",
                new BigDecimal("1200.00"),
                new BigDecimal("1.0500"),
                new BigDecimal("1260.00"),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                new BigDecimal("1323.00"),
                null,
                null,
                List.of("1323.00")));

    mockMvc
        .perform(
            post("/api/v1/premium/calculations")
                .with(user("rater").authorities(new SimpleGrantedAuthority("ROLE_premium:rate")))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"policyType\":\"HOME\",\"state\":\"TX\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.calculationId").value("calc-1"))
        .andExpect(jsonPath("$.returnCode").value("00"))
        .andExpect(jsonPath("$.finalPremium").value("1323.00"));
  }

  @Test
  void postCalculationReturns400ForMissingPolicyType() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/premium/calculations")
                .with(user("rater").authorities(new SimpleGrantedAuthority("ROLE_premium:rate")))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"state\":\"TX\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("SYS_VALIDATION"));
  }
}
