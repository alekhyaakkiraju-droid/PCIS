package com.pcis.claims.domain.repository;

import com.pcis.claims.domain.ClaimPaymentEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClaimPaymentRepository extends JpaRepository<ClaimPaymentEntity, Long> {
  List<ClaimPaymentEntity> findByClaimClaimNbrOrderByPaymentIdAsc(String claimNbr);
}
