package com.pcis.policy.domain.repository;

import com.pcis.policy.domain.entity.BillingPlanEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BillingPlanRepository extends JpaRepository<BillingPlanEntity, Long> {

  Optional<BillingPlanEntity> findByPolicy_PolNbr(String polNbr);
}
