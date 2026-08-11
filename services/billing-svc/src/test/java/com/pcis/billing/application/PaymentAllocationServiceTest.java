package com.pcis.billing.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pcis.batch.common.OutboxEventWriter;
import com.pcis.billing.api.dto.PaymentRequest;
import com.pcis.billing.api.dto.PaymentResponse;
import com.pcis.billing.domain.BillingSchedule;
import com.pcis.billing.domain.Invoice;
import com.pcis.billing.domain.Payment;
import com.pcis.billing.domain.exception.OverApplicationException;
import com.pcis.billing.domain.repository.BillingScheduleRepository;
import com.pcis.billing.domain.repository.InvoiceRepository;
import com.pcis.billing.domain.repository.PaymentApplicationRepository;
import com.pcis.billing.domain.repository.PaymentRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

@ExtendWith(MockitoExtension.class)
class PaymentAllocationServiceTest {

  @Mock private PaymentRepository paymentRepository;
  @Mock private PaymentApplicationRepository paymentApplicationRepository;
  @Mock private BillingScheduleRepository billingScheduleRepository;
  @Mock private InvoiceRepository invoiceRepository;
  @Mock private JdbcTemplate jdbcTemplate;
  @Mock private OutboxEventWriter outboxEventWriter;

  private PaymentAllocationService service;

  @BeforeEach
  void setUp() {
    service =
        new PaymentAllocationService(
            paymentRepository,
            paymentApplicationRepository,
            billingScheduleRepository,
            invoiceRepository,
            jdbcTemplate,
            outboxEventWriter);
  }

  @Test
  void fullSingleInstallmentPayment() {
    stubPolicyExists();
    BillingSchedule schedule = schedule(1L, "500.00", "0.00", "O", LocalDate.parse("2024-01-01"));
    when(billingScheduleRepository.findOpenInstallmentsByPolNbr("POL001")).thenReturn(List.of(schedule));
    when(invoiceRepository.findByBillSchedId(1L)).thenReturn(List.of(invoice(10L, 1L)));
    when(jdbcTemplate.queryForObject("SELECT nextval('seq_payment_id')", Long.class)).thenReturn(42L);
    Payment saved = savedPayment(100L, "PAY00000000042");
    when(paymentRepository.saveAndFlush(any(Payment.class))).thenReturn(saved);

    PaymentResponse response =
        service.applyPayment(
            request("500.00", "token-full"));

    assertThat(response.paymentAmt()).isEqualByComparingTo("500.00");
    assertThat(response.allocations()).hasSize(1);
    assertThat(response.allocations().get(0).appliedAmt()).isEqualByComparingTo("500.00");
    assertThat(response.allocations().get(0).newStatus()).isEqualTo("P");
    assertThat(schedule.getAmtPaid()).isEqualByComparingTo("500.00");
    verify(outboxEventWriter).write(any(), any(), any(), any(), any());
  }

  @Test
  void partialPaymentLeavesBalance() {
    stubPolicyExists();
    BillingSchedule schedule = schedule(1L, "500.00", "0.00", "O", LocalDate.parse("2024-01-01"));
    when(billingScheduleRepository.findOpenInstallmentsByPolNbr("POL001")).thenReturn(List.of(schedule));
    when(invoiceRepository.findByBillSchedId(1L)).thenReturn(List.of(invoice(10L, 1L)));
    when(jdbcTemplate.queryForObject("SELECT nextval('seq_payment_id')", Long.class)).thenReturn(43L);
    when(paymentRepository.saveAndFlush(any(Payment.class)))
        .thenReturn(savedPayment(101L, "PAY00000000043"));

    PaymentResponse response = service.applyPayment(request("200.00", "token-partial"));

    assertThat(response.allocations().get(0).newBalance()).isEqualByComparingTo("300.00");
    assertThat(response.allocations().get(0).newStatus()).isEqualTo("O");
  }

  @Test
  void multiInstallmentAllocationAcrossThreeInstallments() {
    stubPolicyExists();
    BillingSchedule first = schedule(1L, "300.00", "0.00", "O", LocalDate.parse("2024-01-01"));
    BillingSchedule second = schedule(2L, "300.00", "0.00", "O", LocalDate.parse("2024-02-01"));
    BillingSchedule third = schedule(3L, "300.00", "0.00", "O", LocalDate.parse("2024-03-01"));
    when(billingScheduleRepository.findOpenInstallmentsByPolNbr("POL001"))
        .thenReturn(List.of(first, second, third));
    when(invoiceRepository.findByBillSchedId(1L)).thenReturn(List.of(invoice(11L, 1L)));
    when(invoiceRepository.findByBillSchedId(2L)).thenReturn(List.of(invoice(12L, 2L)));
    when(invoiceRepository.findByBillSchedId(3L)).thenReturn(List.of(invoice(13L, 3L)));
    when(jdbcTemplate.queryForObject("SELECT nextval('seq_payment_id')", Long.class)).thenReturn(44L);
    when(paymentRepository.saveAndFlush(any(Payment.class)))
        .thenReturn(savedPayment(102L, "PAY00000000044"));

    PaymentResponse response = service.applyPayment(request("800.00", "token-multi"));

    assertThat(response.allocations()).hasSize(3);
    assertThat(response.allocations().get(0).appliedAmt()).isEqualByComparingTo("300.00");
    assertThat(response.allocations().get(1).appliedAmt()).isEqualByComparingTo("300.00");
    assertThat(response.allocations().get(2).appliedAmt()).isEqualByComparingTo("200.00");
  }

  @Test
  void overApplicationRejected() {
    stubPolicyExists();
    BillingSchedule schedule = schedule(1L, "500.00", "0.00", "O", LocalDate.parse("2024-01-01"));
    when(billingScheduleRepository.findOpenInstallmentsByPolNbr("POL001")).thenReturn(List.of(schedule));

    assertThatThrownBy(() -> service.applyPayment(request("600.00", "token-over")))
        .isInstanceOf(OverApplicationException.class)
        .satisfies(
            ex ->
                assertThat(((OverApplicationException) ex).getMaxApplicableAmount())
                    .isEqualByComparingTo("500.00"));
  }

  @Test
  void zeroBalanceInstallmentSkipped() {
    stubPolicyExists();
    BillingSchedule paid = schedule(1L, "100.00", "100.00", "O", LocalDate.parse("2024-01-01"));
    BillingSchedule open = schedule(2L, "200.00", "0.00", "O", LocalDate.parse("2024-02-01"));
    when(billingScheduleRepository.findOpenInstallmentsByPolNbr("POL001"))
        .thenReturn(List.of(paid, open));
    when(invoiceRepository.findByBillSchedId(2L)).thenReturn(List.of(invoice(20L, 2L)));
    when(jdbcTemplate.queryForObject("SELECT nextval('seq_payment_id')", Long.class)).thenReturn(45L);
    when(paymentRepository.saveAndFlush(any(Payment.class)))
        .thenReturn(savedPayment(103L, "PAY00000000045"));

    PaymentResponse response = service.applyPayment(request("50.00", "token-skip"));

    assertThat(response.allocations()).hasSize(1);
    assertThat(response.allocations().get(0).billSchedId()).isEqualTo(2L);
  }

  @Test
  void lateInstallmentIncluded() {
    stubPolicyExists();
    BillingSchedule late = schedule(4L, "150.00", "0.00", "L", LocalDate.parse("2024-01-15"));
    when(billingScheduleRepository.findOpenInstallmentsByPolNbr("POL001")).thenReturn(List.of(late));
    when(invoiceRepository.findByBillSchedId(4L)).thenReturn(List.of(invoice(30L, 4L)));
    when(jdbcTemplate.queryForObject("SELECT nextval('seq_payment_id')", Long.class)).thenReturn(46L);
    when(paymentRepository.saveAndFlush(any(Payment.class)))
        .thenReturn(savedPayment(104L, "PAY00000000046"));

    PaymentResponse response = service.applyPayment(request("150.00", "token-late"));

    ArgumentCaptor<BillingSchedule> captor = ArgumentCaptor.forClass(BillingSchedule.class);
    verify(billingScheduleRepository).save(captor.capture());
    assertThat(captor.getValue().getSchedStatus()).isEqualTo("P");
    assertThat(response.allocations().get(0).newStatus()).isEqualTo("P");
  }

  private void stubPolicyExists() {
    when(jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM POLICY_T WHERE POL_NBR = ? AND POL_STATUS = 'ACTV'",
            Integer.class,
            "POL001"))
        .thenReturn(1);
    when(paymentRepository.findByPaymentToken(any())).thenReturn(Optional.empty());
  }

  private PaymentRequest request(String amount, String token) {
    return new PaymentRequest(
        "POL001", "CUST001", amount, "CH", LocalDate.parse("2024-06-15"), token);
  }

  private BillingSchedule schedule(
      long id, String due, String paid, String status, LocalDate dueDate) {
    BillingSchedule schedule = new BillingSchedule();
    schedule.setBillSchedId(id);
    schedule.setPolNbr("POL001");
    schedule.setBillPlanId(1L);
    schedule.setInstallmentNbr(1);
    schedule.setDueDate(dueDate);
    schedule.setAmtDue(new BigDecimal(due));
    schedule.setAmtPaid(new BigDecimal(paid));
    schedule.setSchedStatus(status);
    return schedule;
  }

  private Invoice invoice(long invoiceId, long billSchedId) {
    Invoice invoice = new Invoice();
    invoice.setInvoiceId(invoiceId);
    invoice.setBillSchedId(billSchedId);
    invoice.setPolNbr("POL001");
    return invoice;
  }

  private Payment savedPayment(long id, String ref) {
    Payment payment = new Payment();
    payment.setPaymentId(id);
    payment.setPaymentRef(ref);
    payment.setPaymentStatus("POST");
    return payment;
  }
}
