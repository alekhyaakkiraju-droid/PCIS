package com.pcis.reporting.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "pcis.reporting.datasource")
public record ReportingDataSourceProperties(
    String url, String username, String password, int maximumPoolSize, long connectionTimeoutMs, boolean readOnly) {
  public ReportingDataSourceProperties {
    if (maximumPoolSize <= 0) maximumPoolSize = 5;
    if (connectionTimeoutMs <= 0) connectionTimeoutMs = 30_000L;
  }
}
