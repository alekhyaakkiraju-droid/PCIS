package com.pcis.customer.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.pcis.customer.api.dto.Customer360Response;
import com.pcis.customer.api.dto.CustomerResponse;
import com.pcis.customer.api.dto.CustomerResponseMapper;
import com.pcis.customer.api.dto.SectionWrapper;
import com.pcis.customer.application.Customer360Service;
import com.pcis.customer.application.CustomerApplicationService;
import com.pcis.customer.config.SecurityConfig;
import com.pcis.customer.domain.CustomerEntity;
import com.pcis.customer.domain.DuplicateCandidate;
import com.pcis.customer.domain.exception.CustomerNotFoundException;
import com.pcis.customer.domain.exception.DuplicateTaxIdException;
import com.pcis.customer.support.TestJwtGenerator;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
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
  @MockBean private Customer360Service customer360Service;
  @MockBean private CustomerResponseMapper customerResponseMapper;
  @MockBean private JwtDecoder jwtDecoder;

  private CustomerEntity sampleCustomer;
  private CustomerResponse sampleResponse;

  @BeforeEach
  void setUp() {
    sampleCustomer = new CustomerEntity();
    sampleCustomer.setCustId(10);
    sampleCustomer.setTaxId("111223333");
    sampleCustomer.setCustName("Jane Doe");
    sampleCustomer.setCustType("I");
    sampleCustomer.setCustStatus("A");

    sampleResponse =
        new CustomerResponse(
            10, "*****3333", "Jane Doe", "I", "A", List.of(), List.of());
  }

  @Test
  void createReturns201WhenCustomerIsCreated() throws Exception {
    when(customerApplicationService.create(any())).thenReturn(sampleCustomer);
    when(customerResponseMapper.toResponse(sampleCustomer)).thenReturn(sampleResponse);

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
        .andExpect(jsonPath("$.custName").value("Jane Doe"));
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

    CustomerResponse overrideResponse =
        new CustomerResponse(42, "*****6789", "Override Customer", "I", "A", List.of(), List.of());

    when(customerApplicationService.createWithOverride(any(), any())).thenReturn(created);
    when(customerResponseMapper.toResponse(created)).thenReturn(overrideResponse);

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

  @Test
  void getCustomerReturns200WithReadScope() throws Exception {
    when(customerApplicationService.findById(10)).thenReturn(sampleCustomer);
    when(customerResponseMapper.toResponse(sampleCustomer)).thenReturn(sampleResponse);

    mockMvc
        .perform(
            get("/api/v1/customers/10")
                .with(
                    jwt()
                        .jwt(TestJwtGenerator.customerReader("agent-1"))
                        .authorities(new SimpleGrantedAuthority("customer:read"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.custId").value(10))
        .andExpect(jsonPath("$.taxId").value("*****3333"));
  }

  @Test
  void getCustomerReturns403WithoutReadScope() throws Exception {
    mockMvc
        .perform(
            get("/api/v1/customers/10")
                .with(
                    jwt()
                        .jwt(TestJwtGenerator.customerWriter("agent-1"))
                        .authorities(new SimpleGrantedAuthority("customer:write"))))
        .andExpect(status().isForbidden());
  }

  @Test
  void getCustomerReturns404WhenNotFound() throws Exception {
    when(customerApplicationService.findById(99))
        .thenThrow(new CustomerNotFoundException(99));

    mockMvc
        .perform(
            get("/api/v1/customers/99")
                .with(
                    jwt()
                        .jwt(TestJwtGenerator.customerReader("agent-1"))
                        .authorities(new SimpleGrantedAuthority("customer:read"))))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("CUSTOMER_NOT_FOUND"));
  }

  @Test
  void updateCustomerReturns200WithWriteScope() throws Exception {
    CustomerEntity updated = new CustomerEntity();
    updated.setCustId(10);
    updated.setCustName("Updated Name");
    updated.setCustType("I");
    updated.setCustStatus("A");

    CustomerResponse updatedResponse =
        new CustomerResponse(10, "*****3333", "Updated Name", "I", "A", List.of(), List.of());

    when(customerApplicationService.update(any())).thenReturn(updated);
    when(customerResponseMapper.toResponse(updated)).thenReturn(updatedResponse);

    mockMvc
        .perform(
            put("/api/v1/customers/10")
                .with(
                    jwt()
                        .jwt(TestJwtGenerator.customerWriter("agent-1"))
                        .authorities(new SimpleGrantedAuthority("customer:write")))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "custName": "Updated Name"
                    }
                    """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.custName").value("Updated Name"));
  }

  @Test
  void updateCustomerReturns403WithoutWriteScope() throws Exception {
    mockMvc
        .perform(
            put("/api/v1/customers/10")
                .with(
                    jwt()
                        .jwt(TestJwtGenerator.customerReader("agent-1"))
                        .authorities(new SimpleGrantedAuthority("customer:read")))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"custName\": \"Updated Name\"}"))
        .andExpect(status().isForbidden());
  }

  @Test
  void searchCustomersReturnsMatches() throws Exception {
    when(customerApplicationService.search("Jane")).thenReturn(List.of(sampleCustomer));
    when(customerResponseMapper.toResponse(sampleCustomer)).thenReturn(sampleResponse);

    mockMvc
        .perform(
            get("/api/v1/customers/search")
                .param("q", "Jane")
                .with(
                    jwt()
                        .jwt(TestJwtGenerator.customerReader("agent-1"))
                        .authorities(new SimpleGrantedAuthority("customer:read"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].custName").value("Jane Doe"));
  }

  @Test
  void duplicateCheckReturnsCandidateWhenFound() throws Exception {
    DuplicateCandidate existing = new DuplicateCandidate(5, "Other Customer", "A");
    when(customerApplicationService.duplicateCheck(10)).thenReturn(Optional.of(existing));

    mockMvc
        .perform(
            get("/api/v1/customers/10/duplicate-check")
                .with(
                    jwt()
                        .jwt(TestJwtGenerator.customerReader("agent-1"))
                        .authorities(new SimpleGrantedAuthority("customer:read"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.duplicateFound").value(true))
        .andExpect(jsonPath("$.existingCustomer.custId").value(5));
  }

  @Test
  void duplicateCheckReturnsFalseWhenUnique() throws Exception {
    when(customerApplicationService.duplicateCheck(10)).thenReturn(Optional.empty());

    mockMvc
        .perform(
            get("/api/v1/customers/10/duplicate-check")
                .with(
                    jwt()
                        .jwt(TestJwtGenerator.customerReader("agent-1"))
                        .authorities(new SimpleGrantedAuthority("customer:read"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.duplicateFound").value(false));
  }

  @Test
  void getCustomer360ReturnsAggregatedSections() throws Exception {
    Customer360Response response =
        new Customer360Response(
            10,
            SectionWrapper.available(sampleResponse),
            SectionWrapper.unavailable("policies service unavailable"),
            SectionWrapper.unavailable("billing service unavailable"),
            SectionWrapper.unavailable("claims service unavailable"));

    when(customer360Service.getCustomer360(10)).thenReturn(response);

    mockMvc
        .perform(
            get("/api/v1/customers/10/360")
                .with(
                    jwt()
                        .jwt(TestJwtGenerator.customerReader("agent-1"))
                        .authorities(new SimpleGrantedAuthority("customer:read"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.custId").value(10))
        .andExpect(jsonPath("$.profile.status").value("AVAILABLE"))
        .andExpect(jsonPath("$.policies.status").value("UNAVAILABLE"))
        .andExpect(jsonPath("$.billing.status").value("UNAVAILABLE"))
        .andExpect(jsonPath("$.claims.status").value("UNAVAILABLE"));
  }
}
