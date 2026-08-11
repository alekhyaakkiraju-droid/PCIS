package com.pcis.billing.domain.repository;

import com.pcis.billing.domain.BillingSchedule;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BillingScheduleRepository extends JpaRepository<BillingSchedule, Long> {

  List<BillingSchedule> findByPolNbr(String polNbr);

  List<BillingSchedule> findByPolNbrOrderByInstallmentNbrAsc(String polNbr);

  @Query(
      """
      SELECT bs FROM BillingSchedule bs
      WHERE bs.polNbr = :polNbr AND bs.schedStatus IN ('O', 'L')
      ORDER BY bs.dueDate ASC, bs.billSchedId ASC
      """)
  List<BillingSchedule> findOpenInstallmentsByPolNbr(@Param("polNbr") String polNbr);
}
