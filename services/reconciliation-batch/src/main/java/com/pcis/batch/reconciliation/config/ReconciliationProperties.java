package com.pcis.batch.reconciliation.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "pcis.reconciliation")
public class ReconciliationProperties {

  private String programName = "RECON001";
  private String businessDate;
  private int minimumCleanDays = 30;
  private ReadReplicaProperties readReplica = new ReadReplicaProperties();
  private RollbackProperties rollback = new RollbackProperties();

  public String getProgramName() {
    return programName;
  }

  public void setProgramName(String programName) {
    this.programName = programName;
  }

  public String getBusinessDate() {
    return businessDate;
  }

  public void setBusinessDate(String businessDate) {
    this.businessDate = businessDate;
  }

  public int getMinimumCleanDays() {
    return minimumCleanDays;
  }

  public void setMinimumCleanDays(int minimumCleanDays) {
    this.minimumCleanDays = minimumCleanDays;
  }

  public ReadReplicaProperties getReadReplica() {
    return readReplica;
  }

  public void setReadReplica(ReadReplicaProperties readReplica) {
    this.readReplica = readReplica;
  }

  public RollbackProperties getRollback() {
    return rollback;
  }

  public void setRollback(RollbackProperties rollback) {
    this.rollback = rollback;
  }

  public static class ReadReplicaProperties {
    private String url;
    private String username;
    private String password;

    public String getUrl() {
      return url;
    }

    public void setUrl(String url) {
      this.url = url;
    }

    public String getUsername() {
      return username;
    }

    public void setUsername(String username) {
      this.username = username;
    }

    public String getPassword() {
      return password;
    }

    public void setPassword(String password) {
      this.password = password;
    }
  }

  public static class RollbackProperties {
    private String domain;

    public String getDomain() {
      return domain;
    }

    public void setDomain(String domain) {
      this.domain = domain;
    }
  }
}
