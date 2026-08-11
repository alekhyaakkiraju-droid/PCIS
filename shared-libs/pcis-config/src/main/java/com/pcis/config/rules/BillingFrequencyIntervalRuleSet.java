package com.pcis.config.rules;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** Immutable view of BIL003B billing frequency interval mapping. */
public record BillingFrequencyIntervalRuleSet(
    Map<String, Integer> intervalMonthsByFrequency, int defaultIntervalMonths) {

  public BillingFrequencyIntervalRuleSet {
    intervalMonthsByFrequency = Map.copyOf(intervalMonthsByFrequency);
  }

  public static BillingFrequencyIntervalRuleSet fromPayload(
      List<FrequencyMapping> mappings, int defaultIntervalMonths) {
    Map<String, Integer> byFrequency =
        mappings.stream()
            .collect(
                Collectors.toUnmodifiableMap(
                    FrequencyMapping::frequency, FrequencyMapping::intervalMonths));
    return new BillingFrequencyIntervalRuleSet(byFrequency, defaultIntervalMonths);
  }

  public record FrequencyMapping(String frequency, int intervalMonths) {}
}
