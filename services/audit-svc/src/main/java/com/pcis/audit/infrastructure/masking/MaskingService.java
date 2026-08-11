package com.pcis.audit.infrastructure.masking;

import com.pcis.observability.logging.PiiMaskingConverter;
import org.springframework.stereotype.Service;

/** Masks Restricted-tier PII in audit old/new values before persistence. */
@Service
public class MaskingService {

  public String mask(String value) {
    if (value == null || value.isBlank()) {
      return value;
    }
    return PiiMaskingConverter.maskPii(value);
  }
}
