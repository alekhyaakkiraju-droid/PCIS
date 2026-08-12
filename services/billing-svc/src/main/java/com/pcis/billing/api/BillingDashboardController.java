package com.pcis.billing.api;

import com.pcis.billing.domain.BillingSchedule;
import com.pcis.billing.domain.Invoice;
import com.pcis.billing.domain.repository.BillingScheduleRepository;
import com.pcis.billing.domain.repository.InvoiceRepository;
import com.pcis.billing.dto.AgingBucketResponse;
import com.pcis.billing.dto.InstallmentResponse;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/billing")
public class BillingDashboardController {

  private static final List<String> OPEN_INVOICE_STATUSES = List.of("OPEN", "O   ", "O");

  private final BillingScheduleRepository billingScheduleRepository;
  private final InvoiceRepository invoiceRepository;

  public BillingDashboardController(
      BillingScheduleRepository billingScheduleRepository,
      InvoiceRepository invoiceRepository) {
    this.billingScheduleRepository = billingScheduleRepository;
    this.invoiceRepository = invoiceRepository;
  }

  @GetMapping("/installments")
  @PreAuthorize("hasAuthority('billing:read')")
  @Transactional(readOnly = true)
  public List<InstallmentResponse> listInstallments() {
    return billingScheduleRepository.findAll().stream()
        .sorted(
            (left, right) -> {
              int byDue =
                  nullSafeDate(left.getDueDate()).compareTo(nullSafeDate(right.getDueDate()));
              if (byDue != 0) {
                return byDue;
              }
              return Long.compare(
                  nullSafeId(left.getBillSchedId()), nullSafeId(right.getBillSchedId()));
            })
        .map(this::toInstallmentResponse)
        .toList();
  }

  @GetMapping("/aging")
  @PreAuthorize("hasAuthority('billing:read')")
  @Transactional(readOnly = true)
  public List<AgingBucketResponse> listAgingBuckets() {
    LocalDate today = LocalDate.now();
    Map<String, AgingAccumulator> buckets = new LinkedHashMap<>();
    buckets.put("Current", new AgingAccumulator());
    buckets.put("1-30 days", new AgingAccumulator());
    buckets.put("31-60 days", new AgingAccumulator());
    buckets.put("61-90 days", new AgingAccumulator());
    buckets.put("90+ days", new AgingAccumulator());

    for (Invoice invoice : invoiceRepository.findAll()) {
      if (!isOpenInvoice(invoice.getInvoiceStatus())) {
        continue;
      }
      long daysPastDue =
          ChronoUnit.DAYS.between(nullSafeDate(invoice.getInvoiceDueDate()), today);
      String bucketKey =
          daysPastDue <= 0
              ? "Current"
              : daysPastDue <= 30
                  ? "1-30 days"
                  : daysPastDue <= 60
                      ? "31-60 days"
                      : daysPastDue <= 90 ? "61-90 days" : "90+ days";
      buckets.get(bucketKey).add(invoice.getInvoiceAmt());
    }

    List<AgingBucketResponse> response = new ArrayList<>();
    for (Map.Entry<String, AgingAccumulator> entry : buckets.entrySet()) {
      AgingAccumulator bucket = entry.getValue();
      response.add(
          new AgingBucketResponse(entry.getKey(), bucket.invoiceCount(), bucket.amountDue()));
    }
    return response;
  }

  private InstallmentResponse toInstallmentResponse(BillingSchedule schedule) {
    return new InstallmentResponse(
        "INST-" + schedule.getBillSchedId(),
        schedule.getPolNbr(),
        schedule.getDueDate(),
        schedule.getAmtDue(),
        mapScheduleStatus(schedule.getSchedStatus()));
  }

  private static String mapScheduleStatus(String schedStatus) {
    if (schedStatus == null) {
      return "Unknown";
    }
    return switch (schedStatus.trim()) {
      case "O" -> "Open";
      case "P" -> "Paid";
      case "L" -> "Late";
      default -> schedStatus.trim();
    };
  }

  private static boolean isOpenInvoice(String invoiceStatus) {
    if (invoiceStatus == null) {
      return false;
    }
    String trimmed = invoiceStatus.trim();
    return OPEN_INVOICE_STATUSES.stream().anyMatch(status -> status.trim().equalsIgnoreCase(trimmed));
  }

  private static LocalDate nullSafeDate(LocalDate date) {
    return date != null ? date : LocalDate.MIN;
  }

  private static long nullSafeId(Long id) {
    return id != null ? id : 0L;
  }

  private static final class AgingAccumulator {
    private int invoiceCount;
    private BigDecimal amountDue = BigDecimal.ZERO;

    void add(BigDecimal amount) {
      invoiceCount++;
      amountDue = amountDue.add(amount != null ? amount : BigDecimal.ZERO);
    }

    int invoiceCount() {
      return invoiceCount;
    }

    BigDecimal amountDue() {
      return amountDue;
    }
  }
}
