package com.pcis.config;

/** Typed registry of code-table domains referenced by domain services (build-gate source of truth). */
public enum CodeDomain {
  BILL_FREQ("BILL_FREQ"),
  BILL_FREQ_INTERVAL("BILL_FREQ_INTERVAL"),
  BILL_SCHED_STATUS("BILL_SCHED_STATUS"),
  RESERVE_STATUS("RESERVE_STATUS"),
  CLAIM_TYPE("CLAIM_TYPE"),
  CANCEL_REASON("CANCEL_REASON");

  private final String domainCode;

  CodeDomain(String domainCode) {
    this.domainCode = domainCode;
  }

  public String domainCode() {
    return domainCode;
  }

  public static CodeDomain fromDomainCode(String domainCode) {
    for (CodeDomain domain : values()) {
      if (domain.domainCode().equals(domainCode)) {
        return domain;
      }
    }
    throw new IllegalArgumentException("Unknown code domain: " + domainCode);
  }
}
