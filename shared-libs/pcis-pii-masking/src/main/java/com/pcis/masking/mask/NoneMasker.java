package com.pcis.masking.mask;

import com.pcis.masking.MaskingConstants;

/** Pass-through masker for {@link com.pcis.classification.MaskStrategy#NONE}. */
public final class NoneMasker implements ValueMasker {

  @Override
  public String mask(String value) {
    return value;
  }
}
