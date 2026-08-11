package com.pcis.batch.audit.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "pcis.audit")
public class AuditArchiveProperties {

  private int retentionDays = 365;
  private int chunkSize = 1000;
  private String programName = "AUD002B";
  private TierRetention retention = new TierRetention();

  public int getRetentionDays() {
    return retentionDays;
  }

  public void setRetentionDays(int retentionDays) {
    this.retentionDays = retentionDays;
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

  public TierRetention getRetention() {
    return retention;
  }

  public void setRetention(TierRetention retention) {
    this.retention = retention;
  }

  public static class TierRetention {
    private int publicDays = 365;
    private int internalDays = 365;
    private int confidentialDays = 730;
    private int restrictedDays = 2555;

    public int getPublicDays() {
      return publicDays;
    }

    public void setPublicDays(int publicDays) {
      this.publicDays = publicDays;
    }

    public int getInternalDays() {
      return internalDays;
    }

    public void setInternalDays(int internalDays) {
      this.internalDays = internalDays;
    }

    public int getConfidentialDays() {
      return confidentialDays;
    }

    public void setConfidentialDays(int confidentialDays) {
      this.confidentialDays = confidentialDays;
    }

    public int getRestrictedDays() {
      return restrictedDays;
    }

    public void setRestrictedDays(int restrictedDays) {
      this.restrictedDays = restrictedDays;
    }

    public int[] allTierDays() {
      return new int[] {publicDays, internalDays, confidentialDays, restrictedDays};
    }
  }
}
