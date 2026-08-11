package com.pcis.billing.config;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import java.util.Set;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "pcis.billing")
public class BillingConfigProperties {

  private static final Set<String> ALLOWED_FREQUENCIES = Set.of("M", "Q", "S", "A");

  @Min(0)
  @Max(90)
  private int leadDays = 15;

  @Min(0)
  @Max(90)
  private int graceDays = 10;

  @NotEmpty
  private List<String> frequencies = List.of("M", "Q", "S", "A");

  @Min(1)
  @Max(1000)
  private int chunkSize = 1;

  @Min(0)
  @Max(100000)
  private int errorThreshold = 100;

  public int getLeadDays() {
    return leadDays;
  }

  public void setLeadDays(int leadDays) {
    this.leadDays = leadDays;
  }

  public int getGraceDays() {
    return graceDays;
  }

  public void setGraceDays(int graceDays) {
    this.graceDays = graceDays;
  }

  public List<String> getFrequencies() {
    return frequencies;
  }

  public void setFrequencies(List<String> frequencies) {
    this.frequencies = frequencies;
  }

  public int getChunkSize() {
    return chunkSize;
  }

  public void setChunkSize(int chunkSize) {
    this.chunkSize = chunkSize;
  }

  public int getErrorThreshold() {
    return errorThreshold;
  }

  public void setErrorThreshold(int errorThreshold) {
    this.errorThreshold = errorThreshold;
  }

  @AssertTrue(message = "frequencies must be a non-empty subset of M, Q, S, A")
  boolean isFrequenciesValid() {
    return frequencies != null
        && !frequencies.isEmpty()
        && ALLOWED_FREQUENCIES.containsAll(frequencies);
  }
}
