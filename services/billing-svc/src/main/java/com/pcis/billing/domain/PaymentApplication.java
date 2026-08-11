package com.pcis.billing.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "PAYMENT_APPLICATION_T")
public class PaymentApplication extends AuditableEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "PAYMENT_APP_ID")
  private Long paymentAppId;

  @Column(name = "PAYMENT_ID", nullable = false)
  private Long paymentId;

  @Column(name = "INVOICE_ID", nullable = false)
  private Long invoiceId;

  @Column(name = "APPLIED_AMT", nullable = false, precision = 11, scale = 2)
  private BigDecimal appliedAmt;

  @Column(name = "APPLIED_DATE")
  private LocalDate appliedDate;

  public Long getPaymentAppId() {
    return paymentAppId;
  }

  public void setPaymentAppId(Long paymentAppId) {
    this.paymentAppId = paymentAppId;
  }

  public Long getPaymentId() {
    return paymentId;
  }

  public void setPaymentId(Long paymentId) {
    this.paymentId = paymentId;
  }

  public Long getInvoiceId() {
    return invoiceId;
  }

  public void setInvoiceId(Long invoiceId) {
    this.invoiceId = invoiceId;
  }

  public BigDecimal getAppliedAmt() {
    return appliedAmt;
  }

  public void setAppliedAmt(BigDecimal appliedAmt) {
    this.appliedAmt = appliedAmt;
  }

  public LocalDate getAppliedDate() {
    return appliedDate;
  }

  public void setAppliedDate(LocalDate appliedDate) {
    this.appliedDate = appliedDate;
  }
}
