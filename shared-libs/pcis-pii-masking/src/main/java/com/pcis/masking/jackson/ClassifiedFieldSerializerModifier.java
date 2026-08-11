package com.pcis.masking.jackson;

import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.ser.BeanPropertyWriter;
import com.fasterxml.jackson.databind.ser.BeanSerializerModifier;
import com.pcis.masking.Classified;
import com.pcis.masking.MaskingService;
import java.util.ArrayList;
import java.util.List;

/** Wraps {@link Classified} bean properties with runtime PII masking during JSON serialization. */
public final class ClassifiedFieldSerializerModifier extends BeanSerializerModifier {

  private final MaskingService maskingService;

  public ClassifiedFieldSerializerModifier(MaskingService maskingService) {
    this.maskingService = maskingService;
  }

  @Override
  public List<BeanPropertyWriter> changeProperties(
      SerializationConfig config, BeanDescription beanDesc, List<BeanPropertyWriter> beanProperties) {
    List<BeanPropertyWriter> writers = new ArrayList<>(beanProperties.size());
    for (BeanPropertyWriter writer : beanProperties) {
      Classified classified = writer.getAnnotation(Classified.class);
      if (classified != null) {
        writers.add(new MaskingBeanPropertyWriter(writer, classified, maskingService));
      } else {
        writers.add(writer);
      }
    }
    return writers;
  }
}
