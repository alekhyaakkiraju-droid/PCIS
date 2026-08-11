package com.pcis.billing.domain.repository;

import com.pcis.billing.domain.Refund;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RefundRepository extends JpaRepository<Refund, Long> {

  List<Refund> findByPolNbr(String polNbr);
}
