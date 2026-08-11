package com.pcis.masking.jackson;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.BeanPropertyWriter;
import com.pcis.classification.MaskStrategy;
import com.pcis.masking.Classified;
import com.pcis.masking.MaskingService;
import java.lang.reflect.Field;
import java.util.Objects;

/** Bean property writer that masks {@link Classified} string fields before emission. */
final class MaskingBeanPropertyWriter extends BeanPropertyWriter {

  private final Classified classified;
  private final MaskingService maskingService;

  MaskingBeanPropertyWriter(
      BeanPropertyWriter base, Classified classified, MaskingService maskingService) {
    super(base);
    this.classified = classified;
    this.maskingService = maskingService;
  }

  @Override
  public void serializeAsField(Object bean, JsonGenerator gen, SerializerProvider prov)
      throws Exception {
    Object value = get(bean);
    if (value == null) {
      if (_nullSerializer != null) {
        gen.writeFieldName(_name);
        _nullSerializer.serialize(null, gen, prov);
      }
      return;
    }
    if (!(value instanceof String stringValue)) {
      super.serializeAsField(bean, gen, prov);
      return;
    }

    String masked = maskValue(bean, stringValue);
    gen.writeStringField(_name.getValue(), masked);
  }

  private String maskValue(Object bean, String value) {
    MaskStrategy override = classified.mask();
    if (override != null && override != MaskStrategy.NONE) {
      return maskingService.maskByClassification(value, override);
    }

    String column =
        classified.column().isBlank() ? _name.getValue() : classified.column();
    String discriminator = readDiscriminator(bean);
    return maskingService.mask(classified.entity(), column, value, discriminator);
  }

  private String readDiscriminator(Object bean) {
    String discriminatorField = classified.discriminatorField();
    if (discriminatorField.isBlank() || bean == null) {
      return null;
    }
    try {
      Field field = bean.getClass().getDeclaredField(discriminatorField);
      field.setAccessible(true);
      Object raw = field.get(bean);
      return raw == null ? null : Objects.toString(raw, null);
    } catch (ReflectiveOperationException ex) {
      return null;
    }
  }
}
