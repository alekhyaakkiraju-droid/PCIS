package com.pcis.masking.logback;

import com.pcis.classification.DataClassificationRegistry;
import com.pcis.masking.MaskingService;
import org.springframework.beans.factory.InitializingBean;

public final class LogbackMaskingInitializer implements InitializingBean {

  private final MaskingService maskingService;
  private final DataClassificationRegistry registry;

  public LogbackMaskingInitializer(
      MaskingService maskingService, DataClassificationRegistry registry) {
    this.maskingService = maskingService;
    this.registry = registry;
  }

  @Override
  public void afterPropertiesSet() {
    LogbackMaskingBridge.initialize(maskingService, registry);
  }
}
