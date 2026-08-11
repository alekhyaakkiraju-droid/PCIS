package com.pcis.customer.api;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.pcis.customer.api.dto.CustomerResponse;
import com.pcis.customer.domain.CustomerEntity;
import com.pcis.customer.domain.CustomerDomainService;
import com.pcis.customer.api.dto.CustomerResponseMapper;
import com.pcis.customer.support.TestJwtGenerator;
import com.pcis.customer.support.TestSecurityConfig;
import java.util.List;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestSecurityConfig.class)
class Customer360WireMockTest {

  private static WireMockServer policyServer;
  private static WireMockServer billingServer;
  private static WireMockServer claimsServer;

  @Autowired private MockMvc mockMvc;

  @MockBean private CustomerDomainService customerDomainService;

  @MockBean private CustomerResponseMapper customerResponseMapper;

  @BeforeAll
  static void startWireMock() {
    policyServer = new WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort());
    billingServer = new WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort());
    claimsServer = new WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort());
    policyServer.start();
    billingServer.start();
    claimsServer.start();

    policyServer.stubFor(
        WireMock.get(urlEqualTo("/api/v1/customers/10/policies/summary"))
            .willReturn(
                aResponse()
                    .withHeader("Content-Type", "application/json")
                    .withBody(
                        """
                        {
                          "activeCount": 2,
                          "items": [
                            {
                              "policyId": "POL-001",
                              "policyType": "AUTO",
                              "status": "A",
                              "premium": 1200.00
                            }
                          ]
                        }
                        """)));

    billingServer.stubFor(
        WireMock.get(urlEqualTo("/api/v1/customers/10/billing/summary"))
            .willReturn(
                aResponse()
                    .withHeader("Content-Type", "application/json")
                    .withBody(
                        """
                        {
                          "balanceDue": 350.50,
                          "openInvoiceCount": 1
                        }
                        """)));

    claimsServer.stubFor(
        WireMock.get(urlEqualTo("/api/v1/customers/10/claims/summary"))
            .willReturn(
                aResponse()
                    .withHeader("Content-Type", "application/json")
                    .withBody(
                        """
                        {
                          "openClaimCount": 1,
                          "items": [
                            {
                              "claimId": "CLM-001",
                              "status": "OPEN",
                              "reserveAmount": 5000.00
                            }
                          ]
                        }
                        """)));
  }

  @AfterAll
  static void stopWireMock() {
    if (policyServer != null) {
      policyServer.stop();
    }
    if (billingServer != null) {
      billingServer.stop();
    }
    if (claimsServer != null) {
      claimsServer.stop();
    }
  }

  @DynamicPropertySource
  static void registerFeignUrls(DynamicPropertyRegistry registry) {
    registry.add("pcis.policy-svc.url", () -> "http://localhost:" + policyServer.port());
    registry.add("pcis.billing-svc.url", () -> "http://localhost:" + billingServer.port());
    registry.add("pcis.claims-svc.url", () -> "http://localhost:" + claimsServer.port());
    registry.add("spring.datasource.url", () -> "jdbc:h2:mem:customer360;MODE=PostgreSQL;DB_CLOSE_DELAY=-1");
    registry.add("spring.datasource.driver-class-name", () -> "org.h2.Driver");
    registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
    registry.add("spring.flyway.enabled", () -> false);
  }

  @Test
  void customer360AggregatesWireMockSections() throws Exception {
    CustomerEntity customer = new CustomerEntity();
    customer.setCustId(10);
    customer.setTaxId("111223333");
    customer.setCustName("Jane Doe");
    customer.setCustType("I");
    customer.setCustStatus("A");

    when(customerDomainService.findById(10)).thenReturn(customer);
    when(customerResponseMapper.toResponse(customer))
        .thenReturn(
            new CustomerResponse(10, "*****3333", "Jane Doe", "I", "A", List.of(), List.of()));

    mockMvc
        .perform(
            MockMvcRequestBuilders.get("/api/v1/customers/10/360")
                .with(
                    jwt()
                        .jwt(TestJwtGenerator.customerReader("agent-1"))
                        .authorities(new SimpleGrantedAuthority("customer:read"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.custId").value(10))
        .andExpect(jsonPath("$.profile.status").value("AVAILABLE"))
        .andExpect(jsonPath("$.profile.data.custName").value("Jane Doe"))
        .andExpect(jsonPath("$.policies.status").value("AVAILABLE"))
        .andExpect(jsonPath("$.policies.data.activeCount").value(2))
        .andExpect(jsonPath("$.billing.status").value("AVAILABLE"))
        .andExpect(jsonPath("$.billing.data.balanceDue").value(350.50))
        .andExpect(jsonPath("$.claims.status").value("AVAILABLE"))
        .andExpect(jsonPath("$.claims.data.openClaimCount").value(1));
  }

  @Test
  void customer360MarksUnavailableWhenWireMockReturns404() throws Exception {
    policyServer.stubFor(
        WireMock.get(urlEqualTo("/api/v1/customers/99/policies/summary"))
            .willReturn(aResponse().withStatus(404)));

    CustomerEntity customer = new CustomerEntity();
    customer.setCustId(99);
    customer.setTaxId("999887777");
    customer.setCustName("No Policies");
    customer.setCustType("I");
    customer.setCustStatus("A");

    when(customerDomainService.findById(99)).thenReturn(customer);
    when(customerResponseMapper.toResponse(customer))
        .thenReturn(
            new CustomerResponse(99, "*****7777", "No Policies", "I", "A", List.of(), List.of()));

    mockMvc
        .perform(
            MockMvcRequestBuilders.get("/api/v1/customers/99/360")
                .with(
                    jwt()
                        .jwt(TestJwtGenerator.customerReader("agent-1"))
                        .authorities(new SimpleGrantedAuthority("customer:read"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.profile.status").value("AVAILABLE"))
        .andExpect(jsonPath("$.policies.status").value("UNAVAILABLE"));
  }
}
