package com.pcis.policy.batch.pol006b.config;

import jakarta.validation.constraints.Min;
import java.time.LocalDate;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "pcis.batch.policy-renewal")
public class PolicyRenewalProperties {

  @Min(1)
  private int chunkSize = 1;

  @Min(1)
  private int skipLimit = 100;

  private String programName = "POL006B";

  /** Optional override; defaults to {@code pcis.premium.renewal-window-days}. */
  private Integer renewalWindowDays;

  /** Optional fixed processing date for deterministic batch runs and tests. */
  private LocalDate referenceDate;

  public int getChunkSize() {
    return chunkSize;
  }

  public void setChunkSize(int chunkSize) {
    this.chunkSize = chunkSize;
  }

  public int getSkipLimit() {
    return skipLimit;
  }

  public void setSkipLimit(int skipLimit) {
    this.skipLimit = skipLimit;
  }

  public String getProgramName() {
    return programName;
  }

  public void setProgramName(String programName) {
    this.programName = programName;
  }

  public LocalDate getReferenceDate() {
    return referenceDate;
  }

  public void setReferenceDate(LocalDate referenceDate) {
    this.referenceDate = referenceDate;
  }

  public Integer getRenewalWindowDays() {
    return renewalWindowDays;
  }

  public void setRenewalWindowDays(Integer renewalWindowDays) {
    this.renewalWindowDays = renewalWindowDays;
  }

  public int resolveRenewalWindowDays(int premiumDefault) {
    return renewalWindowDays != null ? renewalWindowDays : premiumDefault;
  }
}
