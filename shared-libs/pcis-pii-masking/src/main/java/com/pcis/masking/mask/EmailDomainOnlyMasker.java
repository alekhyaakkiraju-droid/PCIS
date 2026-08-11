package com.pcis.masking.mask;

import com.pcis.masking.MaskingConstants;

/** Keeps the email domain only (portion after {@code @}). */
public final class EmailDomainOnlyMasker implements ValueMasker {

  @Override
  public String mask(String value) {
    if (value == null || value.isEmpty()) {
      return value;
    }
    int atIndex = value.indexOf('@');
    if (atIndex < 0) {
      if (value.contains(".") && !value.contains(" ")) {
        return value;
      }
      return MaskingConstants.FULL_REDACT_TOKEN;
    }
    String domain = value.substring(atIndex + 1);
    if (domain.isBlank()) {
      return MaskingConstants.FULL_REDACT_TOKEN;
    }
    return domain;
  }
}
