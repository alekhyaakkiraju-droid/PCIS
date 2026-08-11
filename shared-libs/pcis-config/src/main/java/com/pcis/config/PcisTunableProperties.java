package com.pcis.config;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "pcis.tunables")
public class PcisTunableProperties {

  @NotNull private DurationProperties cache = new DurationProperties();

  private Map<String, BigDecimal> numericDefaults = new HashMap<>();
  private Map<String, String> textDefaults = new HashMap<>();

  public DurationProperties getCache() {
    return cache;
  }

  public void setCache(DurationProperties cache) {
    this.cache = cache;
  }

  public Map<String, BigDecimal> getNumericDefaults() {
    return numericDefaults;
  }

  public void setNumericDefaults(Map<String, BigDecimal> numericDefaults) {
    this.numericDefaults = numericDefaults;
  }

  public Map<String, String> getTextDefaults() {
    return textDefaults;
  }

  public void setTextDefaults(Map<String, String> textDefaults) {
    this.textDefaults = textDefaults;
  }

  public static class DurationProperties {
    @Min(1)
    @Max(3600)
    private int ttlSeconds = 300;

    @Min(10)
    @Max(10000)
    private int maxSize = 128;

    public int getTtlSeconds() {
      return ttlSeconds;
    }

    public void setTtlSeconds(int ttlSeconds) {
      this.ttlSeconds = ttlSeconds;
    }

    public int getMaxSize() {
      return maxSize;
    }

    public void setMaxSize(int maxSize) {
      this.maxSize = maxSize;
    }
  }
}
