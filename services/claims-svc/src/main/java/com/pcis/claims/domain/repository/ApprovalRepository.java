package com.pcis.claims.domain.repository;

import com.pcis.claims.domain.ApprovalEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApprovalRepository extends JpaRepository<ApprovalEntity, Long> {
  List<ApprovalEntity> findByClaimClaimNbr(String claimNbr);
}
