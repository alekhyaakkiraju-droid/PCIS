package com.pcis.claims.domain.repository;

import com.pcis.claims.domain.ApprovalEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApprovalRepository extends JpaRepository<ApprovalEntity, Long> {
  List<ApprovalEntity> findByClaimClaimNbr(String claimNbr);

  Optional<ApprovalEntity> findByReserveReserveIdAndApprovalStatus(
      Long reserveId, String approvalStatus);

  boolean existsByReserveReserveIdAndApprovalStatus(Long reserveId, String approvalStatus);
}
