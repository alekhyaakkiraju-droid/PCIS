package com.pcis.masking.logback;

import com.pcis.classification.DataClassificationRegistry;
import com.pcis.masking.MaskingService;

public final class LogbackMaskingBridge {

  private static final LogMessageMasker FALLBACK = LogMessageMasker.withDefaultRegistry();
  private static volatile LogMessageMasker activeMasker = FALLBACK;

  private LogbackMaskingBridge() {}

  public static void initialize(MaskingService maskingService, DataClassificationRegistry registry) {
    activeMasker = new LogMessageMasker(maskingService, registry);
  }

  public static void reset() {
    activeMasker = FALLBACK;
  }

  public static String mask(String input) {
    return activeMasker.mask(input);
  }

  public static String maskMdcValue(String key, String value) {
    return activeMasker.maskMdcValue(key, value);
  }
}
