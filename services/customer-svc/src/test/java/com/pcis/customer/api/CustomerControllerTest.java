package com.pcis.customer.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.pcis.customer.config.SecurityConfig;
import com.pcis.customer.application.CustomerApplicationService;
import com.pcis.customer.domain.CustomerEntity;
import com.pcis.customer.domain.exception.DuplicateTaxIdException;
import com.pcis.customer.domain.DuplicateCandidate;
import com.pcis.customer.support.TestJwtGenerator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = CustomerController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class CustomerControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockBean private CustomerApplicationService customerApplicationService;
  @MockBean private JwtDecoder jwtDecoder;

  @Test
  void createReturns201WhenCustomerIsCreated() throws Exception {
    CustomerEntity created = new CustomerEntity();
    created.setCustId(10);
    created.setTaxId("111223333");
    created.setCustName("New Customer");
    created.setCustType("I");
    created.setCustStatus("A");

    when(customerApplicationService.create(any())).thenReturn(created);

    mockMvc
        .perform(
            post("/api/v1/customers")
                .with(
                    jwt()
                        .jwt(TestJwtGenerator.customerWriter("agent-1"))
                        .authorities(new SimpleGrantedAuthority("customer:write")))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "taxId": "111223333",
                      "custName": "New Customer",
                      "custType": "I"
                    }
                    """))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.custId").value(10))
        .andExpect(jsonPath("$.custName").value("New Customer"));
  }

  @Test
  void createReturns409WithDuplicateReasonCode() throws Exception {
    DuplicateCandidate existing = new DuplicateCandidate(1, "Jane Doe", "A");
    when(customerApplicationService.create(any()))
        .thenThrow(new DuplicateTaxIdException("123456789", existing));

    mockMvc
        .perform(
            post("/api/v1/customers")
                .with(
                    jwt()
                        .jwt(TestJwtGenerator.customerWriter("agent-1"))
                        .authorities(new SimpleGrantedAuthority("customer:write")))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "taxId": "123456789",
                      "custName": "Duplicate Attempt",
                      "custType": "I"
                    }
                    """))
        .andExpect(status().isConflict())
        .andExpect(header().string("Content-Type", MediaType.APPLICATION_PROBLEM_JSON_VALUE))
        .andExpect(jsonPath("$.code").value("DUPLICATE_TAX_ID"))
        .andExpect(jsonPath("$.existingCustId").value(1))
        .andExpect(jsonPath("$.existingCustName").value("Jane Doe"));
  }

  @Test
  void duplicateOverrideRequiresPermission() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/customers/duplicate-overrides")
                .with(
                    jwt()
                        .jwt(TestJwtGenerator.customerWriter("agent-1"))
                        .authorities(new SimpleGrantedAuthority("customer:write")))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "overrideReason": "Supervisor approved duplicate for branch merge",
                      "customer": {
                        "taxId": "123456789",
                        "custName": "Override Customer",
                        "custType": "I"
                      }
                    }
                    """))
        .andExpect(status().isForbidden());
  }

  @Test
  void duplicateOverrideReturns201WithPermission() throws Exception {
    CustomerEntity created = new CustomerEntity();
    created.setCustId(42);
    created.setTaxId("123456789");
    created.setCustName("Override Customer");
    created.setCustType("I");
    created.setCustStatus("A");

    when(customerApplicationService.createWithOverride(any(), any())).thenReturn(created);

    mockMvc
        .perform(
            post("/api/v1/customers/duplicate-overrides")
                .with(
                    jwt()
                        .jwt(TestJwtGenerator.duplicateOverride("supervisor-1"))
                        .authorities(
                            new SimpleGrantedAuthority("customer:write"),
                            new SimpleGrantedAuthority("customer:duplicate-override")))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "overrideReason": "Supervisor approved duplicate for branch merge",
                      "customer": {
                        "taxId": "123456789",
                        "custName": "Override Customer",
                        "custType": "I"
                      }
                    }
                    """))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.custId").value(42));
  }
}
