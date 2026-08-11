package com.pcis.billing.domain.repository;

import com.pcis.billing.domain.InvoiceLine;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InvoiceLineRepository extends JpaRepository<InvoiceLine, Long> {

  List<InvoiceLine> findByInvoiceIdOrderByLineNbrAsc(Long invoiceId);
}
