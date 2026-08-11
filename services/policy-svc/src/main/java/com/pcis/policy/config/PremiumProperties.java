package com.pcis.policy.config;

import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "pcis.premium")
public class PremiumProperties {

  @Min(1)
  private int renewalWindowDays = 60;

  private String svcUrl = "http://premium-svc:8085";

  public int getRenewalWindowDays() {
    return renewalWindowDays;
  }

  public void setRenewalWindowDays(int renewalWindowDays) {
    this.renewalWindowDays = renewalWindowDays;
  }

  public String getSvcUrl() {
    return svcUrl;
  }

  public void setSvcUrl(String svcUrl) {
    this.svcUrl = svcUrl;
  }
}
