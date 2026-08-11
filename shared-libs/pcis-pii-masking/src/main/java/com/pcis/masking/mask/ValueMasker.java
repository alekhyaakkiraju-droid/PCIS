package com.pcis.masking.mask;

/** Applies a single {@link com.pcis.classification.MaskStrategy} to a string value. */
public interface ValueMasker {

  String mask(String value);
}
