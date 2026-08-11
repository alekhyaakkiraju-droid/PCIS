package com.pcis.customer.application;

import com.pcis.customer.api.dto.Customer360Response;
import com.pcis.customer.api.dto.CustomerResponse;
import com.pcis.customer.api.dto.CustomerResponseMapper;
import com.pcis.customer.api.dto.SectionWrapper;
import com.pcis.customer.client.BillingServiceClient;
import com.pcis.customer.client.ClaimsServiceClient;
import com.pcis.customer.client.PolicyServiceClient;
import com.pcis.customer.domain.CustomerDomainService;
import com.pcis.customer.domain.CustomerEntity;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class Customer360Service {

  private static final Logger log = LoggerFactory.getLogger(Customer360Service.class);
  private static final long SECTION_TIMEOUT_SECONDS = 2;

  private final CustomerDomainService customerDomainService;
  private final CustomerResponseMapper customerResponseMapper;
  private final PolicyServiceClient policyServiceClient;
  private final BillingServiceClient billingServiceClient;
  private final ClaimsServiceClient claimsServiceClient;

  public Customer360Service(
      CustomerDomainService customerDomainService,
      CustomerResponseMapper customerResponseMapper,
      PolicyServiceClient policyServiceClient,
      BillingServiceClient billingServiceClient,
      ClaimsServiceClient claimsServiceClient) {
    this.customerDomainService = customerDomainService;
    this.customerResponseMapper = customerResponseMapper;
    this.policyServiceClient = policyServiceClient;
    this.billingServiceClient = billingServiceClient;
    this.claimsServiceClient = claimsServiceClient;
  }

  @Transactional(readOnly = true)
  public Customer360Response getCustomer360(Integer custId) {
    CustomerEntity customer = customerDomainService.findById(custId);
    CustomerResponse profile = customerResponseMapper.toResponse(customer);

    CompletableFuture<SectionWrapper<Customer360Response.PolicySection>> policiesFuture =
        fetchSection(() -> policyServiceClient.getPolicySummary(custId), "policies");
    CompletableFuture<SectionWrapper<Customer360Response.BillingSection>> billingFuture =
        fetchSection(() -> billingServiceClient.getBillingSummary(custId), "billing");
    CompletableFuture<SectionWrapper<Customer360Response.ClaimsSection>> claimsFuture =
        fetchSection(() -> claimsServiceClient.getClaimsSummary(custId), "claims");

    return new Customer360Response(
        custId,
        SectionWrapper.available(profile),
        policiesFuture.join(),
        billingFuture.join(),
        claimsFuture.join());
  }

  private <T> CompletableFuture<SectionWrapper<T>> fetchSection(
      java.util.function.Supplier<T> supplier, String sectionName) {
    return CompletableFuture.supplyAsync(
            () -> {
              T data = supplier.get();
              if (data == null) {
                return SectionWrapper.<T>unavailable(sectionName + " service unavailable");
              }
              return SectionWrapper.available(data);
            })
        .orTimeout(SECTION_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .exceptionally(
            ex -> {
              log.warn("Customer 360 section {} failed: {}", sectionName, ex.toString());
              return SectionWrapper.unavailable(sectionName + " service unavailable");
            });
  }
}
