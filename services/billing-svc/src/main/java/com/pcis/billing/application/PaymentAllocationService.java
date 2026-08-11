package com.pcis.billing.application;

import com.pcis.batch.common.OutboxEventWriter;
import com.pcis.billing.api.dto.PaymentAllocationDetail;
import com.pcis.billing.api.dto.PaymentRequest;
import com.pcis.billing.api.dto.PaymentResponse;
import com.pcis.billing.domain.BillingSchedule;
import com.pcis.billing.domain.Invoice;
import com.pcis.billing.domain.Payment;
import com.pcis.billing.domain.PaymentApplication;
import com.pcis.billing.domain.exception.DuplicatePaymentException;
import com.pcis.billing.domain.exception.NoOutstandingBalanceException;
import com.pcis.billing.domain.exception.OverApplicationException;
import com.pcis.billing.domain.exception.PolicyNotFoundException;
import com.pcis.billing.domain.repository.BillingScheduleRepository;
import com.pcis.billing.domain.repository.InvoiceRepository;
import com.pcis.billing.domain.repository.PaymentApplicationRepository;
import com.pcis.billing.domain.repository.PaymentRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentAllocationService {

  private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

  private final PaymentRepository paymentRepository;
  private final PaymentApplicationRepository paymentApplicationRepository;
  private final BillingScheduleRepository billingScheduleRepository;
  private final InvoiceRepository invoiceRepository;
  private final JdbcTemplate jdbcTemplate;
  private final OutboxEventWriter paymentOutboxEventWriter;

  public PaymentAllocationService(
      PaymentRepository paymentRepository,
      PaymentApplicationRepository paymentApplicationRepository,
      BillingScheduleRepository billingScheduleRepository,
      InvoiceRepository invoiceRepository,
      JdbcTemplate jdbcTemplate,
      @Qualifier("paymentOutboxEventWriter") OutboxEventWriter paymentOutboxEventWriter) {
    this.paymentRepository = paymentRepository;
    this.paymentApplicationRepository = paymentApplicationRepository;
    this.billingScheduleRepository = billingScheduleRepository;
    this.invoiceRepository = invoiceRepository;
    this.jdbcTemplate = jdbcTemplate;
    this.paymentOutboxEventWriter = paymentOutboxEventWriter;
  }

  @Transactional
  public PaymentResponse applyPayment(PaymentRequest request) {
    paymentRepository
        .findByPaymentToken(request.paymentToken())
        .ifPresent(existing -> {
          throw new DuplicatePaymentException(existing.getPaymentRef());
        });

    assertPolicyExists(request.polNbr());

    BigDecimal paymentAmt = new BigDecimal(request.paymentAmt()).setScale(2, RoundingMode.HALF_UP);
    List<BillingSchedule> installments =
        billingScheduleRepository.findOpenInstallmentsByPolNbr(request.polNbr());
    if (installments.isEmpty()) {
      throw new NoOutstandingBalanceException(request.polNbr());
    }

    BigDecimal totalOutstanding = computeTotalOutstanding(installments);
    if (totalOutstanding.compareTo(ZERO) <= 0) {
      throw new NoOutstandingBalanceException(request.polNbr());
    }
    if (paymentAmt.compareTo(totalOutstanding) > 0) {
      throw new OverApplicationException(totalOutstanding);
    }

    List<AllocationPlan> plans = buildAllocationPlan(installments, paymentAmt);
    if (plans.isEmpty()) {
      throw new NoOutstandingBalanceException(request.polNbr());
    }
    String paymentRef = generatePaymentRef();

    Payment payment = new Payment();
    payment.setPaymentRef(paymentRef);
    payment.setPolNbr(request.polNbr());
    payment.setPaymentDate(request.paymentDate());
    payment.setPaymentAmt(paymentAmt);
    payment.setPaymentMethod(request.paymentMethod());
    payment.setPaymentToken(request.paymentToken());
    payment.setPaymentStatus("POST");
    payment.setInvoiceId(plans.get(0).invoiceId());

    try {
      payment = paymentRepository.saveAndFlush(payment);
    } catch (DataIntegrityViolationException ex) {
      paymentRepository
          .findByPaymentToken(request.paymentToken())
          .ifPresent(existing -> {
            throw new DuplicatePaymentException(existing.getPaymentRef());
          });
      throw ex;
    }

    List<PaymentAllocationDetail> allocations = new ArrayList<>();
    for (AllocationPlan plan : plans) {
      BillingSchedule schedule = plan.schedule();
      schedule.setAmtPaid(plan.newPaid());
      schedule.setSchedStatus(plan.newStatus());
      billingScheduleRepository.save(schedule);

      PaymentApplication application = new PaymentApplication();
      application.setPaymentId(payment.getPaymentId());
      application.setInvoiceId(plan.invoiceId());
      application.setAppliedAmt(plan.appliedAmt());
      application.setAppliedDate(request.paymentDate());
      paymentApplicationRepository.save(application);

      allocations.add(
          new PaymentAllocationDetail(
              schedule.getBillSchedId(),
              plan.appliedAmt(),
              plan.newBalance(),
              plan.newStatus().trim()));
    }

    writeOutbox(paymentRef, request, paymentAmt, allocations);
    return new PaymentResponse(
        paymentRef,
        request.polNbr(),
        paymentAmt,
        request.paymentDate(),
        payment.getPaymentStatus().trim(),
        allocations);
  }

  private List<AllocationPlan> buildAllocationPlan(
      List<BillingSchedule> installments, BigDecimal paymentAmt) {
    BigDecimal remaining = paymentAmt;
    List<AllocationPlan> plans = new ArrayList<>();

    for (BillingSchedule schedule : installments) {
      if (remaining.compareTo(ZERO) <= 0) {
        break;
      }
      BigDecimal balance = installmentBalance(schedule);
      if (balance.compareTo(ZERO) <= 0) {
        continue;
      }
      BigDecimal applied = remaining.min(balance).setScale(2, RoundingMode.HALF_UP);
      BigDecimal newPaid = nullToZero(schedule.getAmtPaid()).add(applied).setScale(2, RoundingMode.HALF_UP);
      String newStatus = newPaid.compareTo(schedule.getAmtDue()) >= 0 ? "P" : schedule.getSchedStatus();
      BigDecimal newBalance = schedule.getAmtDue().subtract(newPaid).setScale(2, RoundingMode.HALF_UP);
      Invoice invoice = resolveInvoice(schedule);
      plans.add(new AllocationPlan(schedule, invoice.getInvoiceId(), applied, newPaid, newStatus, newBalance));
      remaining = remaining.subtract(applied).setScale(2, RoundingMode.HALF_UP);
    }
    return plans;
  }

  private void assertPolicyExists(String polNbr) {
    Integer count =
        jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM POLICY_T WHERE POL_NBR = ? AND POL_STATUS = 'ACTV'",
            Integer.class,
            polNbr);
    if (count == null || count == 0) {
      throw new PolicyNotFoundException(polNbr);
    }
  }

  private BigDecimal computeTotalOutstanding(List<BillingSchedule> installments) {
    BigDecimal total = ZERO;
    for (BillingSchedule schedule : installments) {
      total = total.add(installmentBalance(schedule));
    }
    return total.setScale(2, RoundingMode.HALF_UP);
  }

  private BigDecimal installmentBalance(BillingSchedule schedule) {
    return schedule
        .getAmtDue()
        .subtract(nullToZero(schedule.getAmtPaid()))
        .setScale(2, RoundingMode.HALF_UP);
  }

  private BigDecimal nullToZero(BigDecimal value) {
    return value == null ? ZERO : value.setScale(2, RoundingMode.HALF_UP);
  }

  private Invoice resolveInvoice(BillingSchedule schedule) {
    return invoiceRepository.findByBillSchedId(schedule.getBillSchedId()).stream()
        .findFirst()
        .orElseThrow(
            () ->
                new IllegalStateException(
                    "Missing invoice for schedule " + schedule.getBillSchedId()));
  }

  private String generatePaymentRef() {
    Long seq = jdbcTemplate.queryForObject("SELECT nextval('seq_payment_id')", Long.class);
    return "PAY" + String.format("%011d", seq);
  }

  private void writeOutbox(
      String paymentRef,
      PaymentRequest request,
      BigDecimal paymentAmt,
      List<PaymentAllocationDetail> allocations) {
    Map<String, Object> payload = new HashMap<>();
    payload.put("paymentId", paymentRef);
    payload.put("polNbr", request.polNbr());
    payload.put("custId", request.custId());
    payload.put("paymentAmt", paymentAmt.toPlainString());
    payload.put("paymentMethod", request.paymentMethod());
    payload.put("paymentDate", request.paymentDate().toString());
    payload.put(
        "allocations",
        allocations.stream()
            .map(
                detail ->
                    Map.of(
                        "billSchedId",
                        detail.billSchedId(),
                        "appliedAmt",
                        detail.appliedAmt().toPlainString(),
                        "newBalance",
                        detail.newBalance().toPlainString(),
                        "newStatus",
                        detail.newStatus()))
            .toList());

    paymentOutboxEventWriter.write(
        "Payment",
        paymentRef,
        "PaymentApplied",
        payload,
        UUID.nameUUIDFromBytes(request.paymentToken().getBytes()));
  }

  private record AllocationPlan(
      BillingSchedule schedule,
      Long invoiceId,
      BigDecimal appliedAmt,
      BigDecimal newPaid,
      String newStatus,
      BigDecimal newBalance) {}
}
