package com.pcis.sync.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Positive;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "pcis.sync")
public class SyncAgentProperties {

  private boolean enabled = true;

  @Positive private long pollIntervalMs = 60_000;

  @Positive private int chunkSize = 1000;

  @Valid private SourceProperties source = new SourceProperties();

  @NotEmpty @Valid private Map<String, DomainProperties> domains = new LinkedHashMap<>();

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public long getPollIntervalMs() {
    return pollIntervalMs;
  }

  public void setPollIntervalMs(long pollIntervalMs) {
    this.pollIntervalMs = pollIntervalMs;
  }

  public int getChunkSize() {
    return chunkSize;
  }

  public void setChunkSize(int chunkSize) {
    this.chunkSize = chunkSize;
  }

  public SourceProperties getSource() {
    return source;
  }

  public void setSource(SourceProperties source) {
    this.source = source;
  }

  public Map<String, DomainProperties> getDomains() {
    return domains;
  }

  public void setDomains(Map<String, DomainProperties> domains) {
    this.domains = domains;
  }

  @Validated
  public static class SourceProperties {

    @NotBlank private String url;
    @NotBlank private String username;
    private String password = "";
    @NotBlank private String driverClassName;

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

    public String getDriverClassName() {
      return driverClassName;
    }

    public void setDriverClassName(String driverClassName) {
      this.driverClassName = driverClassName;
    }
  }

  @Validated
  public static class DomainProperties {

    @NotBlank private String sourceTable;
    @NotBlank private String targetTable;
    @NotBlank private String watermarkColumn;
    @NotBlank private String primaryKey;
    @NotEmpty private List<@NotBlank String> columns;

    public String getSourceTable() {
      return sourceTable;
    }

    public void setSourceTable(String sourceTable) {
      this.sourceTable = sourceTable;
    }

    public String getTargetTable() {
      return targetTable;
    }

    public void setTargetTable(String targetTable) {
      this.targetTable = targetTable;
    }

    public String getWatermarkColumn() {
      return watermarkColumn;
    }

    public void setWatermarkColumn(String watermarkColumn) {
      this.watermarkColumn = watermarkColumn;
    }

    public String getPrimaryKey() {
      return primaryKey;
    }

    public void setPrimaryKey(String primaryKey) {
      this.primaryKey = primaryKey;
    }

    public List<String> getColumns() {
      return columns;
    }

    public void setColumns(List<String> columns) {
      this.columns = columns;
    }
  }
}
