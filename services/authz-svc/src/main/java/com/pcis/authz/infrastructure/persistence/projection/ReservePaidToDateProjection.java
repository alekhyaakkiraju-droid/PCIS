package com.pcis.authz.infrastructure.persistence.projection;

import java.math.BigDecimal;

/** Reserve paid-to-date snapshot for cumulative authority evaluation. */
public interface ReservePaidToDateProjection {

  Long getReserveHistId();

  String getClaimId();

  BigDecimal getPaidToDate();
}
