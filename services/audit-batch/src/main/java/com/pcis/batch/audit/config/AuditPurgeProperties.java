package com.pcis.batch.audit.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "pcis.audit.purge")
public class AuditPurgeProperties {

  private boolean enabled = true;
  private String programName = "AUDPURGE";
  private int kmsWaitingPeriodDays = 7;
  private boolean kmsEnabled = false;
  private long advisoryLockKey = 171_001L;

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public String getProgramName() {
    return programName;
  }

  public void setProgramName(String programName) {
    this.programName = programName;
  }

  public int getKmsWaitingPeriodDays() {
    return kmsWaitingPeriodDays;
  }

  public void setKmsWaitingPeriodDays(int kmsWaitingPeriodDays) {
    this.kmsWaitingPeriodDays = kmsWaitingPeriodDays;
  }

  public boolean isKmsEnabled() {
    return kmsEnabled;
  }

  public void setKmsEnabled(boolean kmsEnabled) {
    this.kmsEnabled = kmsEnabled;
  }

  public long getAdvisoryLockKey() {
    return advisoryLockKey;
  }

  public void setAdvisoryLockKey(long advisoryLockKey) {
    this.advisoryLockKey = advisoryLockKey;
  }
}
