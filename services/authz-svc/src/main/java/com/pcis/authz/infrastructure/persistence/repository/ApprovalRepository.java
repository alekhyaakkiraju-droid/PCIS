package com.pcis.authz.infrastructure.persistence.repository;

import com.pcis.authz.infrastructure.persistence.entity.ApprovalEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApprovalRepository extends JpaRepository<ApprovalEntity, Long> {

  Optional<ApprovalEntity> findFirstByClaimIdAndReserveHistIdAndApprovalStatus(
      String claimId, Long reserveHistId, String approvalStatus);
}
