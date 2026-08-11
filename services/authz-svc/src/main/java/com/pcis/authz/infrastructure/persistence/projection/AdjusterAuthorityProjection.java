package com.pcis.authz.infrastructure.persistence.projection;

import java.math.BigDecimal;

/** Read-only adjuster authority limit for payment checks. */
public interface AdjusterAuthorityProjection {

  String getAdjusterId();

  BigDecimal getAuthorityLimit();
}
