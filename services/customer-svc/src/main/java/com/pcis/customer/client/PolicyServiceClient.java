package com.pcis.customer.client;

import com.pcis.customer.api.dto.Customer360Response;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(
    name = "policy-svc",
    url = "${pcis.policy-svc.url}",
    fallback = PolicyServiceClient.UnavailableFallback.class)
public interface PolicyServiceClient {

  @GetMapping("/api/v1/customers/{custId}/policies/summary")
  Customer360Response.PolicySection getPolicySummary(@PathVariable("custId") Integer custId);

  @Component
  class UnavailableFallback implements PolicyServiceClient {

    @Override
    public Customer360Response.PolicySection getPolicySummary(Integer custId) {
      return null;
    }
  }
}
