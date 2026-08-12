package com.pcis.claims.domain.repository;

import com.pcis.claims.domain.ClaimPaymentEntity;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ClaimPaymentRepository extends JpaRepository<ClaimPaymentEntity, Long> {
  List<ClaimPaymentEntity> findByClaimClaimNbrOrderByPaymentIdAsc(String claimNbr);

  @Query(
      """
      SELECT COALESCE(SUM(p.paymentAmt), 0)
      FROM ClaimPaymentEntity p
      WHERE p.claim.claimNbr = :claimNbr
        AND p.adjuster.adjusterId = :adjusterId
      """)
  BigDecimal sumPaymentAmtByClaimNbrAndAdjusterId(
      @Param("claimNbr") String claimNbr, @Param("adjusterId") String adjusterId);
}
