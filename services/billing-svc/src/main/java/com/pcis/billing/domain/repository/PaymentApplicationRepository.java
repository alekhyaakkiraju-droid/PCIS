package com.pcis.billing.domain.repository;

import com.pcis.billing.domain.PaymentApplication;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentApplicationRepository extends JpaRepository<PaymentApplication, Long> {

  List<PaymentApplication> findByPaymentId(Long paymentId);

  List<PaymentApplication> findByInvoiceId(Long invoiceId);
}
