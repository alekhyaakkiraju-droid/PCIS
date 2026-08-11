package com.pcis.billing.domain.repository;

import com.pcis.billing.domain.BillingPlan;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BillingPlanRepository extends JpaRepository<BillingPlan, Long> {

  List<BillingPlan> findByPolNbr(String polNbr);

  Optional<BillingPlan> findFirstByPolNbrOrderByBillPlanIdDesc(String polNbr);
}
