package com.pcis.premium.contract;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.pcis.premium.application.PremiumRatingService;
import com.pcis.premium.support.PremiumTestSecurityConfig;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.parser.OpenAPIV3Parser;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = com.pcis.premium.controller.PremiumRatingController.class)
@Import({
  com.pcis.premium.config.SecurityConfig.class,
  com.pcis.premium.controller.PremiumRatingController.class,
  PremiumTestSecurityConfig.class
})
class PremiumRatingProviderContractTest {

  @Autowired private MockMvc mockMvc;

  @MockBean private PremiumRatingService ratingService;

  @Test
  void providerExposesAllContractPaths() {
    OpenAPI contract =
        new OpenAPIV3Parser()
            .readContents(PremiumRatingContractSupport.contractYaml(), null, null)
            .getOpenAPI();

    assertThat(contract.getPaths()).containsKeys("/calculations", "/calculations/{calculationId}");
    assertThat(contract.getPaths().get("/calculations").readOperationsMap())
        .containsKey(PathItem.HttpMethod.POST);
    assertThat(contract.getPaths().get("/calculations/{calculationId}").readOperationsMap())
        .containsKey(PathItem.HttpMethod.GET);
  }

  @Test
  void providerPostCalculationReturnsContractProblemDetailWhenNotImplemented() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/premium/calculations")
                .with(user("rater").authorities(new SimpleGrantedAuthority("ROLE_premium:rate")))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"policyType":"HOME","coverageType":"HO3","territory":"TX","state":"TX","limit":"250000.00"}
                    """))
        .andExpect(status().isNotImplemented())
        .andExpect(jsonPath("$.type").exists())
        .andExpect(jsonPath("$.title").exists())
        .andExpect(jsonPath("$.status").value(501))
        .andExpect(jsonPath("$.detail").exists())
        .andExpect(jsonPath("$.code").value("PRM_NOT_IMPLEMENTED"))
        .andExpect(jsonPath("$.correlation_id").exists());
  }
}
