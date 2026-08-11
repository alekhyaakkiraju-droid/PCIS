package com.pcis.customer.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.pcis.customer.api.dto.CustomerResponse;
import com.pcis.customer.api.dto.CustomerResponseMapper;
import com.pcis.customer.api.dto.SectionWrapper;
import com.pcis.customer.client.BillingServiceClient;
import com.pcis.customer.client.ClaimsServiceClient;
import com.pcis.customer.client.PolicyServiceClient;
import com.pcis.customer.domain.CustomerDomainService;
import com.pcis.customer.domain.CustomerEntity;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class Customer360ServiceTest {

  @Mock private CustomerDomainService customerDomainService;
  @Mock private CustomerResponseMapper customerResponseMapper;
  @Mock private PolicyServiceClient policyServiceClient;
  @Mock private BillingServiceClient billingServiceClient;
  @Mock private ClaimsServiceClient claimsServiceClient;

  @InjectMocks private Customer360Service customer360Service;

  @Test
  void getCustomer360AggregatesAvailableSections() {
    CustomerEntity customer = new CustomerEntity();
    customer.setCustId(10);
    CustomerResponse profile =
        new CustomerResponse(10, "*****3333", "Jane", "I", "A", List.of(), List.of());

    when(customerDomainService.findById(10)).thenReturn(customer);
    when(customerResponseMapper.toResponse(customer)).thenReturn(profile);
    when(policyServiceClient.getPolicySummary(10))
        .thenReturn(new com.pcis.customer.api.dto.Customer360Response.PolicySection(1, List.of()));
    when(billingServiceClient.getBillingSummary(10))
        .thenReturn(new com.pcis.customer.api.dto.Customer360Response.BillingSection(
            BigDecimal.TEN, 1));
    when(claimsServiceClient.getClaimsSummary(10))
        .thenReturn(new com.pcis.customer.api.dto.Customer360Response.ClaimsSection(0, List.of()));

    var response = customer360Service.getCustomer360(10);

    assertThat(response.custId()).isEqualTo(10);
    assertThat(response.profile().status()).isEqualTo(SectionWrapper.SectionStatus.AVAILABLE);
    assertThat(response.policies().status()).isEqualTo(SectionWrapper.SectionStatus.AVAILABLE);
    assertThat(response.billing().status()).isEqualTo(SectionWrapper.SectionStatus.AVAILABLE);
    assertThat(response.claims().status()).isEqualTo(SectionWrapper.SectionStatus.AVAILABLE);
  }

  @Test
  void getCustomer360MarksUnavailableWhenFallbackReturnsNull() {
    CustomerEntity customer = new CustomerEntity();
    customer.setCustId(10);
    CustomerResponse profile =
        new CustomerResponse(10, "*****3333", "Jane", "I", "A", List.of(), List.of());

    when(customerDomainService.findById(10)).thenReturn(customer);
    when(customerResponseMapper.toResponse(customer)).thenReturn(profile);
    when(policyServiceClient.getPolicySummary(10)).thenReturn(null);
    when(billingServiceClient.getBillingSummary(10)).thenReturn(null);
    when(claimsServiceClient.getClaimsSummary(10)).thenReturn(null);

    var response = customer360Service.getCustomer360(10);

    assertThat(response.policies().status()).isEqualTo(SectionWrapper.SectionStatus.UNAVAILABLE);
    assertThat(response.billing().status()).isEqualTo(SectionWrapper.SectionStatus.UNAVAILABLE);
    assertThat(response.claims().status()).isEqualTo(SectionWrapper.SectionStatus.UNAVAILABLE);
  }
}
