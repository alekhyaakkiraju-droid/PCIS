package com.pcis.billing.domain.repository;

import com.pcis.billing.domain.Payment;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

  Optional<Payment> findByPaymentId(Long paymentId);

  Optional<Payment> findByPaymentRef(String paymentRef);

  List<Payment> findByInvoiceId(Long invoiceId);
}
