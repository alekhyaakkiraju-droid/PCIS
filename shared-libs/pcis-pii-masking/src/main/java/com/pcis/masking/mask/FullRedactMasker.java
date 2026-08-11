package com.pcis.masking.mask;

import com.pcis.masking.MaskingConstants;

/** Replaces any value with the fixed redaction token. */
public final class FullRedactMasker implements ValueMasker {

  @Override
  public String mask(String value) {
    if (value == null || value.isEmpty()) {
      return value;
    }
    if (MaskingConstants.FULL_REDACT_TOKEN.equals(value)) {
      return value;
    }
    return MaskingConstants.FULL_REDACT_TOKEN;
  }
}
