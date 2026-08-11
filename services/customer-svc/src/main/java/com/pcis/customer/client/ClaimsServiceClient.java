package com.pcis.customer.client;

import com.pcis.customer.api.dto.Customer360Response;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(
    name = "claims-svc",
    url = "${pcis.claims-svc.url}",
    fallback = ClaimsServiceClient.UnavailableFallback.class)
public interface ClaimsServiceClient {

  @GetMapping("/api/v1/customers/{custId}/claims/summary")
  Customer360Response.ClaimsSection getClaimsSummary(@PathVariable("custId") Integer custId);

  @Component
  class UnavailableFallback implements ClaimsServiceClient {

    @Override
    public Customer360Response.ClaimsSection getClaimsSummary(Integer custId) {
      return null;
    }
  }
}
