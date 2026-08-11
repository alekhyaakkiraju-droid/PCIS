package com.pcis.premium.controller;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.pcis.premium.application.PremiumRatingService;
import com.pcis.premium.config.SecurityConfig;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;

@WebMvcTest(controllers = PremiumRatingController.class)
@Import({SecurityConfig.class, PremiumRatingController.class})
class PremiumRatingControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockBean private PremiumRatingService ratingService;

  @Test
  void postCalculationReturnsNotImplementedProblemDetailForAuthorizedRater() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/premium/calculations")
                .with(user("rater").authorities(new SimpleGrantedAuthority("ROLE_premium:rate")))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"policyType\":\"HOME\",\"state\":\"TX\"}"))
        .andExpect(status().isNotImplemented())
        .andExpect(jsonPath("$.code").value("PRM_NOT_IMPLEMENTED"));
  }

  @Test
  void getCalculationRequiresReadAuthorityAnnotation() throws NoSuchMethodException {
    var method =
        PremiumRatingController.class.getMethod(
            "getCalculation", String.class, HttpServletRequest.class);
    PreAuthorize annotation = method.getAnnotation(PreAuthorize.class);
    assertThat(annotation).isNotNull();
    assertThat(annotation.value()).contains("ROLE_premium:read");
  }
}
