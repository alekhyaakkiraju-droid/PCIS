package com.pcis.premium.domain;

/** Raised when no effective rate table exists for the requested policy type and territory. */
public class RateLookupNotFoundException extends RuntimeException {

  private final String policyType;
  private final String territory;

  public RateLookupNotFoundException(String policyType, String territory) {
    super("No rate table found for policyType=" + policyType + ", territory=" + territory);
    this.policyType = policyType;
    this.territory = territory;
  }

  public String policyType() {
    return policyType;
  }

  public String territory() {
    return territory;
  }
}
