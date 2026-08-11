package com.pcis.customer.client;

import com.pcis.customer.api.dto.Customer360Response;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(
    name = "billing-svc",
    url = "${pcis.billing-svc.url}",
    fallback = BillingServiceClient.UnavailableFallback.class)
public interface BillingServiceClient {

  @GetMapping("/api/v1/customers/{custId}/billing/summary")
  Customer360Response.BillingSection getBillingSummary(@PathVariable("custId") Integer custId);

  @Component
  class UnavailableFallback implements BillingServiceClient {

    @Override
    public Customer360Response.BillingSection getBillingSummary(Integer custId) {
      return null;
    }
  }
}
