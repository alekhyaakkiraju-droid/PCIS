package com.pcis.masking.logback;

import ch.qos.logback.classic.pattern.ClassicConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;

public class ClassificationLogMaskingConverter extends ClassicConverter {

  @Override
  public String convert(ILoggingEvent event) {
    if (event == null) {
      return "";
    }
    String message = event.getFormattedMessage();
    if (message == null) {
      return "";
    }
    try {
      return maskPii(message);
    } catch (RuntimeException ex) {
      try {
        addWarn("PII log masking failed; emitting original message: " + ex.getMessage());
      } catch (RuntimeException ignored) {
        // Context may be unavailable in unit tests
      }
      return message;
    }
  }

  public static String maskPii(String input) {
    return LogbackMaskingBridge.mask(input);
  }
}
