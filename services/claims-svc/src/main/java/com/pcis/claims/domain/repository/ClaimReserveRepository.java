package com.pcis.claims.domain.repository;

import com.pcis.claims.domain.ClaimReserveEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClaimReserveRepository extends JpaRepository<ClaimReserveEntity, Long> {
  List<ClaimReserveEntity> findByClaimClaimNbr(String claimNbr);
}
