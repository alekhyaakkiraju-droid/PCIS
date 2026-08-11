package com.pcis.masking.jackson;

import com.fasterxml.jackson.databind.module.SimpleModule;
import com.pcis.masking.MaskingService;

/** Jackson module registering {@link ClassifiedFieldSerializerModifier}. */
public final class PcisJacksonMaskingModule extends SimpleModule {

  public PcisJacksonMaskingModule(MaskingService maskingService) {
    super("PcisJacksonMaskingModule");
    setSerializerModifier(new ClassifiedFieldSerializerModifier(maskingService));
  }
}
