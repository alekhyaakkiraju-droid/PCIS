package com.pcis.billing.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "PAYMENT_T")
public class Payment extends AuditableEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "PAYMENT_ID")
  private Long paymentId;

  @Column(name = "PAYMENT_REF", length = 12)
  private String paymentRef;

  @Column(name = "INVOICE_ID")
  private Long invoiceId;

  @Column(name = "PAYMENT_DATE")
  private LocalDate paymentDate;

  @Column(name = "PAYMENT_AMT", precision = 11, scale = 2)
  private BigDecimal paymentAmt;

  @Column(name = "PAYMENT_TOKEN", length = 40)
  private String paymentToken;

  @Column(name = "PAYMENT_STATUS", length = 4, columnDefinition = "char(4)")
  @JdbcTypeCode(SqlTypes.CHAR)
  private String paymentStatus;

  public Long getPaymentId() {
    return paymentId;
  }

  public void setPaymentId(Long paymentId) {
    this.paymentId = paymentId;
  }

  public String getPaymentRef() {
    return paymentRef;
  }

  public void setPaymentRef(String paymentRef) {
    this.paymentRef = paymentRef;
  }

  public Long getInvoiceId() {
    return invoiceId;
  }

  public void setInvoiceId(Long invoiceId) {
    this.invoiceId = invoiceId;
  }

  public LocalDate getPaymentDate() {
    return paymentDate;
  }

  public void setPaymentDate(LocalDate paymentDate) {
    this.paymentDate = paymentDate;
  }

  public BigDecimal getPaymentAmt() {
    return paymentAmt;
  }

  public void setPaymentAmt(BigDecimal paymentAmt) {
    this.paymentAmt = paymentAmt;
  }

  public String getPaymentToken() {
    return paymentToken;
  }

  public void setPaymentToken(String paymentToken) {
    this.paymentToken = paymentToken;
  }

  public String getPaymentStatus() {
    return paymentStatus;
  }

  public void setPaymentStatus(String paymentStatus) {
    this.paymentStatus = paymentStatus;
  }
}
