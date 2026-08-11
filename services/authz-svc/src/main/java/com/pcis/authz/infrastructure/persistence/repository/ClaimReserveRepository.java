package com.pcis.authz.infrastructure.persistence.repository;

import com.pcis.authz.infrastructure.persistence.entity.ClaimReserveEntity;
import com.pcis.authz.infrastructure.persistence.projection.ReservePaidToDateProjection;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ClaimReserveRepository extends JpaRepository<ClaimReserveEntity, Long> {

  @Query(
      """
      select r.reserveHistId as reserveHistId, r.claimId as claimId, r.paidToDate as paidToDate
      from ClaimReserveEntity r
      where r.reserveHistId = :reserveHistId and r.claimId = :claimId
      """)
  Optional<ReservePaidToDateProjection> findByReserveHistIdAndClaimId(
      @Param("reserveHistId") Long reserveHistId, @Param("claimId") String claimId);
}
