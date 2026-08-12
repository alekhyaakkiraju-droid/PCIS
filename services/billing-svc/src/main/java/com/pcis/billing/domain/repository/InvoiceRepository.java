package com.pcis.billing.domain.repository;

import com.pcis.billing.domain.Invoice;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InvoiceRepository extends JpaRepository<Invoice, Long> {

  List<Invoice> findByPolNbr(String polNbr);

  List<Invoice> findByBillSchedId(Long billSchedId);

  List<Invoice> findByCustId(Integer custId);
}
