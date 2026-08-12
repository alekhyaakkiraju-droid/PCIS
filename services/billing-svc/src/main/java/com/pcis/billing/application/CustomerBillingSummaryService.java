package com.pcis.billing.application;

import com.pcis.billing.domain.Invoice;
import com.pcis.billing.domain.repository.InvoiceRepository;
import com.pcis.billing.dto.CustomerBillingSummaryResponse;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CustomerBillingSummaryService {

  private static final List<String> OPEN_STATUSES = List.of("OPEN", "O   ", "O");

  private final InvoiceRepository invoiceRepository;

  public CustomerBillingSummaryService(InvoiceRepository invoiceRepository) {
    this.invoiceRepository = invoiceRepository;
  }

  @Transactional(readOnly = true)
  public CustomerBillingSummaryResponse getSummary(Integer custId) {
    List<Invoice> openInvoices = invoiceRepository.findByCustId(custId).stream()
        .filter(invoice -> isOpen(invoice.getInvoiceStatus()))
        .toList();

    BigDecimal balanceDue =
        openInvoices.stream()
            .map(Invoice::getInvoiceAmt)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

    return new CustomerBillingSummaryResponse(balanceDue, openInvoices.size());
  }

  private static boolean isOpen(String invoiceStatus) {
    if (invoiceStatus == null) {
      return false;
    }
    String trimmed = invoiceStatus.trim();
    return "OPEN".equalsIgnoreCase(trimmed) || "O".equalsIgnoreCase(trimmed);
  }
}
