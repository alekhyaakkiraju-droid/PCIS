package com.pcis.claims.domain.repository;

import com.pcis.claims.domain.ClaimEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClaimRepository extends JpaRepository<ClaimEntity, String> {
  List<ClaimEntity> findByPolNbr(String polNbr);

  List<ClaimEntity> findByCustId(Integer custId);

  List<ClaimEntity> findByClaimStatus(String claimStatus);
}
