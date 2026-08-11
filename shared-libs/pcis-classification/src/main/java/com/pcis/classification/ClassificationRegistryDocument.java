package com.pcis.classification;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** YAML binding for config/pcis-data-classification.yaml. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ClassificationRegistryDocument(
    @JsonProperty("registry_version") String registryVersion,
    @JsonProperty("tier_handling") Map<String, TierHandlingRule> tierHandling,
    List<EntityDefinition> entities) {

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record TierHandlingRule(
      @JsonProperty("retention_days") int retentionDays,
      @JsonProperty("storage_encryption") String storageEncryption,
      @JsonProperty("access_control") String accessControl,
      @JsonProperty("log_emission") String logEmission) {}

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record EntityDefinition(
      String entity, String domain, String tier, List<ColumnDefinition> columns) {}

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record ColumnDefinition(
      String column,
      String tier,
      boolean pii,
      @JsonProperty("mask_strategy") String maskStrategy,
      @JsonProperty("discriminator_column") String discriminatorColumn,
      String rationale) {}

  public Map<DataTier, Integer> retentionDaysByTier() {
    Map<DataTier, Integer> map = new LinkedHashMap<>();
    if (tierHandling != null) {
      tierHandling.forEach(
          (tierName, rule) -> map.put(DataTier.fromYaml(tierName), rule.retentionDays()));
    }
    return map;
  }
}
