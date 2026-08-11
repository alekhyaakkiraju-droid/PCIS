package com.pcis.batch.policy.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.LocalDate;

@ConfigurationProperties(prefix = "pcis.policy.renewal")
public class PolicyRenewalProperties {

  private int chunkSize = 1;
  private String programName = "POL006B";
  private int renewalWindowDays = 60;
  private String premiumSvcUrl = "http://localhost:8085";
  /** Optional fixed processing date for deterministic batch runs and tests. */
  private LocalDate referenceDate;

  public LocalDate getReferenceDate() {
    return referenceDate;
  }

  public void setReferenceDate(LocalDate referenceDate) {
    this.referenceDate = referenceDate;
  }

  public int getChunkSize() {
    return chunkSize;
  }

  public void setChunkSize(int chunkSize) {
    this.chunkSize = chunkSize;
  }

  public String getProgramName() {
    return programName;
  }

  public void setProgramName(String programName) {
    this.programName = programName;
  }

  public int getRenewalWindowDays() {
    return renewalWindowDays;
  }

  public void setRenewalWindowDays(int renewalWindowDays) {
    this.renewalWindowDays = renewalWindowDays;
  }

  public String getPremiumSvcUrl() {
    return premiumSvcUrl;
  }

  public void setPremiumSvcUrl(String premiumSvcUrl) {
    this.premiumSvcUrl = premiumSvcUrl;
  }
}
