package com.pcis.observability.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for PCIS observability defaults.
 */
@ConfigurationProperties(prefix = "pcis.observability")
public class ObservabilityProperties {

  /**
   * Trace sample rate for successful requests (0.0–1.0). Default 0.1 (10%).
   * Errors should still be sampled at a higher rate via OTel parent/error policies.
   */
  private double traceSampleRate = 0.1d;

  /**
   * HTTP header used for inbound/outbound correlation identifiers.
   */
  private String correlationHeader = "X-Correlation-ID";

  public double getTraceSampleRate() {
    return traceSampleRate;
  }

  public void setTraceSampleRate(double traceSampleRate) {
    if (traceSampleRate < 0.0d || traceSampleRate > 1.0d) {
      throw new IllegalArgumentException("pcis.observability.trace-sample-rate must be between 0.0 and 1.0");
    }
    this.traceSampleRate = traceSampleRate;
  }

  public String getCorrelationHeader() {
    return correlationHeader;
  }

  public void setCorrelationHeader(String correlationHeader) {
    this.correlationHeader = correlationHeader;
  }
}
