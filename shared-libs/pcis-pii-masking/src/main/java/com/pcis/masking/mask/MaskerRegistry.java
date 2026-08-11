package com.pcis.masking.mask;

import com.pcis.classification.MaskStrategy;
import java.util.EnumMap;
import java.util.Map;

/** Resolves {@link MaskStrategy} tokens to concrete {@link ValueMasker} implementations. */
public final class MaskerRegistry {

  private final Map<MaskStrategy, ValueMasker> maskers;

  public MaskerRegistry() {
    this(defaultMaskers());
  }

  public MaskerRegistry(Map<MaskStrategy, ValueMasker> maskers) {
    this.maskers = Map.copyOf(maskers);
  }

  public ValueMasker get(MaskStrategy strategy) {
    ValueMasker masker = maskers.get(strategy);
    if (masker == null) {
      return maskers.get(MaskStrategy.FULL_REDACT);
    }
    return masker;
  }

  private static Map<MaskStrategy, ValueMasker> defaultMaskers() {
    Map<MaskStrategy, ValueMasker> map = new EnumMap<>(MaskStrategy.class);
    map.put(MaskStrategy.NONE, new NoneMasker());
    map.put(MaskStrategy.LAST_FOUR, new LastFourMasker());
    map.put(MaskStrategy.EMAIL_DOMAIN_ONLY, new EmailDomainOnlyMasker());
    map.put(MaskStrategy.PHONE_LAST_FOUR, new PhoneLastFourMasker());
    map.put(MaskStrategy.DATE_YEAR_ONLY, new DateYearOnlyMasker());
    map.put(MaskStrategy.FULL_REDACT, new FullRedactMasker());
    return map;
  }
}
