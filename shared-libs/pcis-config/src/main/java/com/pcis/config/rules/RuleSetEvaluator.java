package com.pcis.config.rules;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.pcis.config.PcisCodeTableProperties;
import com.pcis.config.UnknownCodeValueException;
import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Resolves versioned rule sets from CONFIG_RULE_SET_T with Caffeine cache. */
public class RuleSetEvaluator {

  private static final Logger log = LoggerFactory.getLogger(RuleSetEvaluator.class);
  private static final ObjectMapper MAPPER = new ObjectMapper();

  private final RuleSetRepository repository;
  private final PcisCodeTableProperties properties;
  private final Cache<String, Object> cache;

  public RuleSetEvaluator(RuleSetRepository repository, PcisCodeTableProperties properties) {
    this.repository = repository;
    this.properties = properties;
    this.cache =
        Caffeine.newBuilder()
            .maximumSize(properties.getCache().getMaxSize())
            .expireAfterWrite(Duration.ofSeconds(properties.getCache().getTtlSeconds()))
            .build();
  }

  public BillingFrequencyIntervalRuleSet billingFrequencyIntervalRuleSet() {
    return billingFrequencyIntervalRuleSet(RuleSetKey.BILLING_FREQUENCY_INTERVAL, 1);
  }

  public BillingFrequencyIntervalRuleSet billingFrequencyIntervalRuleSet(
      RuleSetKey ruleSetKey, int versionNo) {
    return (BillingFrequencyIntervalRuleSet)
        cache.get(
            cacheKey(ruleSetKey.key(), versionNo),
            key -> loadBillingFrequencyIntervalRuleSet(ruleSetKey.key(), versionNo));
  }

  public DelinquencyStatusTransitionRuleSet delinquencyStatusTransitionRuleSet() {
    return delinquencyStatusTransitionRuleSet(RuleSetKey.DELINQUENCY_STATUS_TRANSITION, 1);
  }

  public DelinquencyStatusTransitionRuleSet delinquencyStatusTransitionRuleSet(
      RuleSetKey ruleSetKey, int versionNo) {
    return (DelinquencyStatusTransitionRuleSet)
        cache.get(
            cacheKey(ruleSetKey.key(), versionNo),
            key -> loadDelinquencyTransitionRuleSet(ruleSetKey.key(), versionNo));
  }

  /**
   * Resolves billing frequency interval months. Unknown frequencies fall back to the rule-set
   * default (legacy one-year) while emitting a structured exception record.
   */
  public IntervalResolution resolveBillingIntervalMonths(String frequency) {
    BillingFrequencyIntervalRuleSet ruleSet = billingFrequencyIntervalRuleSet();
    Integer interval = ruleSet.intervalMonthsByFrequency().get(frequency);
    if (interval != null) {
      return new IntervalResolution(interval, false, frequency);
    }
    log.warn(
        "actor=system resource=config/rule-set/billing-frequency-interval operation=resolve-fallback "
            + "frequency={} defaultIntervalMonths={} reason=unknown_frequency",
        frequency,
        ruleSet.defaultIntervalMonths());
    return new IntervalResolution(ruleSet.defaultIntervalMonths(), true, frequency);
  }

  public void refresh(RuleSetKey ruleSetKey, int versionNo) {
    cache.invalidate(cacheKey(ruleSetKey.key(), versionNo));
    log.info(
        "Refreshed rule-set cache actor=system resource=config/rule-set/{} operation=refresh version={}",
        ruleSetKey.key(),
        versionNo);
  }

  public void refreshAll() {
    cache.invalidateAll();
    log.info("Refreshed all rule-set cache entries actor=system resource=config/rule-set operation=refresh-all");
  }

  private BillingFrequencyIntervalRuleSet loadBillingFrequencyIntervalRuleSet(
      String ruleSetKey, int versionNo) {
    JsonNode payload = loadPayload(ruleSetKey, versionNo);
    List<BillingFrequencyIntervalRuleSet.FrequencyMapping> mappings = new ArrayList<>();
    for (JsonNode mapping : payload.get("mappings")) {
      mappings.add(
          new BillingFrequencyIntervalRuleSet.FrequencyMapping(
              mapping.get("frequency").asText(), mapping.get("intervalMonths").asInt()));
    }
    int defaultIntervalMonths = payload.get("defaultIntervalMonths").asInt();
    return BillingFrequencyIntervalRuleSet.fromPayload(mappings, defaultIntervalMonths);
  }

  private DelinquencyStatusTransitionRuleSet loadDelinquencyTransitionRuleSet(
      String ruleSetKey, int versionNo) {
    JsonNode payload = loadPayload(ruleSetKey, versionNo);
    List<DelinquencyStatusTransitionRuleSet.Transition> transitions = new ArrayList<>();
    for (JsonNode transition : payload.get("transitions")) {
      transitions.add(
          new DelinquencyStatusTransitionRuleSet.Transition(
              transition.get("fromStatus").asText(),
              transition.get("event").asText(),
              transition.get("toStatus").asText()));
    }
    return DelinquencyStatusTransitionRuleSet.fromPayload(transitions);
  }

  private JsonNode loadPayload(String ruleSetKey, int versionNo) {
    RuleSetRow row = repository.findByKeyAndVersion(ruleSetKey, versionNo);
    if (row == null || !isEffective(row)) {
      throw new RuleSetNotFoundException(ruleSetKey);
    }
    try {
      return MAPPER.readTree(row.payload());
    } catch (Exception ex) {
      throw new RuleSetNotFoundException(ruleSetKey);
    }
  }

  private boolean isEffective(RuleSetRow row) {
    LocalDate today = LocalDate.now();
    if (!"A".equalsIgnoreCase(row.statusCd())) {
      return false;
    }
    if (row.effectiveFrom().isAfter(today)) {
      return false;
    }
    return row.effectiveTo() == null || !row.effectiveTo().isBefore(today);
  }

  private static String cacheKey(String ruleSetKey, int versionNo) {
    return ruleSetKey + ":" + versionNo;
  }

  public record IntervalResolution(int intervalMonths, boolean usedFallback, String frequency) {

    public void throwIfInactiveFrequencyRequired() {
      if (usedFallback) {
        throw new UnknownCodeValueException("BILL_FREQ", frequency);
      }
    }
  }
}
