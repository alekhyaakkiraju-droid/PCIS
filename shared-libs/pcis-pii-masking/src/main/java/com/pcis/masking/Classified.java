package com.pcis.masking;

import com.pcis.classification.DataTier;
import com.pcis.classification.MaskStrategy;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a field for metadata-driven PII masking at serialization and emission time.
 *
 * <p>Primary mask resolution uses {@code entity} plus {@code column} against the classification
 * registry. Optional {@code mask} and {@code tier} attributes override registry defaults when set.
 */
@Documented
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Classified {

  /** Registry entity (table) name, for example {@code CUSTOMER_T}. */
  String entity();

  /** Registry column name, for example {@code TAX_ID}. Defaults to the Java field name. */
  String column() default "";

  /** Optional tier override when not resolved from the registry. */
  DataTier tier() default DataTier.PUBLIC;

  /** Optional mask strategy override; {@link MaskStrategy#NONE} means use the registry. */
  MaskStrategy mask() default MaskStrategy.NONE;

  /**
   * Sibling field holding a polymorphic discriminator value (for example {@code contactType} for
   * {@code CUSTOMER_CONTACT_T.CONTACT_VALUE}).
   */
  String discriminatorField() default "";
}
