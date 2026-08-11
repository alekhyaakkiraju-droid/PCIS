package com.pcis.observability.logging;

import ch.qos.logback.classic.pattern.ClassicConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;
import com.pcis.masking.logback.ClassificationLogMaskingConverter;

/** Backward-compatible alias for {@link ClassificationLogMaskingConverter}. */
@Deprecated
public class PiiMaskingConverter extends ClassicConverter {

  private final ClassificationLogMaskingConverter delegate = new ClassificationLogMaskingConverter();

  @Override
  public String convert(ILoggingEvent event) {
    return delegate.convert(event);
  }

  public static String maskPii(String input) {
    return ClassificationLogMaskingConverter.maskPii(input);
  }
}
