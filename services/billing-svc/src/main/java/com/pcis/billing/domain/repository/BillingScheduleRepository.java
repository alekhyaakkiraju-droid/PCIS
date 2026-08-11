package com.pcis.billing.domain.repository;

import com.pcis.billing.domain.BillingSchedule;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BillingScheduleRepository extends JpaRepository<BillingSchedule, Long> {

  List<BillingSchedule> findByPolNbr(String polNbr);

  List<BillingSchedule> findByPolNbrOrderByInstallmentNbrAsc(String polNbr);
}
