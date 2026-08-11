package com.pcis.authz.infrastructure.persistence.repository;

import com.pcis.authz.infrastructure.persistence.entity.ClaimPaymentEntity;
import java.math.BigDecimal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ClaimPaymentRepository extends JpaRepository<ClaimPaymentEntity, Long> {

  @Query(
      """
      select coalesce(sum(p.paymentAmt), 0)
      from ClaimPaymentEntity p
      where p.claimId = :claimId and p.adjusterId = :adjusterId
      """)
  BigDecimal sumPaymentAmountByClaimIdAndAdjusterId(
      @Param("claimId") String claimId, @Param("adjusterId") String adjusterId);
}
