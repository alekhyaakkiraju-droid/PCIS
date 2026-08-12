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
@Table(name = "INVOICE_T")
public class Invoice extends AuditableEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "INVOICE_ID")
  private Long invoiceId;

  @Column(name = "BILL_SCHED_ID")
  private Long billSchedId;

  @Column(name = "POL_NBR", length = 12)
  private String polNbr;

  @Column(name = "CUST_ID")
  private Integer custId;

  @Column(name = "INVOICE_DATE")
  private LocalDate invoiceDate;

  @Column(name = "INVOICE_DUE_DATE")
  private LocalDate invoiceDueDate;

  @Column(name = "INVOICE_AMT", precision = 11, scale = 2)
  private BigDecimal invoiceAmt;

  @Column(name = "INVOICE_STATUS", length = 4, columnDefinition = "char(4)")
  @JdbcTypeCode(SqlTypes.CHAR)
  private String invoiceStatus;

  public Long getInvoiceId() {
    return invoiceId;
  }

  public void setInvoiceId(Long invoiceId) {
    this.invoiceId = invoiceId;
  }

  public Long getBillSchedId() {
    return billSchedId;
  }

  public void setBillSchedId(Long billSchedId) {
    this.billSchedId = billSchedId;
  }

  public String getPolNbr() {
    return polNbr;
  }

  public void setPolNbr(String polNbr) {
    this.polNbr = polNbr;
  }

  public Integer getCustId() {
    return custId;
  }

  public void setCustId(Integer custId) {
    this.custId = custId;
  }

  public LocalDate getInvoiceDate() {
    return invoiceDate;
  }

  public void setInvoiceDate(LocalDate invoiceDate) {
    this.invoiceDate = invoiceDate;
  }

  public LocalDate getInvoiceDueDate() {
    return invoiceDueDate;
  }

  public void setInvoiceDueDate(LocalDate invoiceDueDate) {
    this.invoiceDueDate = invoiceDueDate;
  }

  public BigDecimal getInvoiceAmt() {
    return invoiceAmt;
  }

  public void setInvoiceAmt(BigDecimal invoiceAmt) {
    this.invoiceAmt = invoiceAmt;
  }

  public String getInvoiceStatus() {
    return invoiceStatus;
  }

  public void setInvoiceStatus(String invoiceStatus) {
    this.invoiceStatus = invoiceStatus;
  }
}
