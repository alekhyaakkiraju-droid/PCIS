package com.pcis.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class DefaultBatchPrincipalProvider implements BatchPrincipalProvider {

  private final String principal;

  public DefaultBatchPrincipalProvider(
      @Value("${pcis.batch.principal:svc-batch-workload}") String principal) {
    this.principal = principal;
  }

  @Override
  public String principal() {
    return principal;
  }
}
