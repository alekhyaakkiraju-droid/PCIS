package com.pcis.billing.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;

@Entity
@Table(name = "INVOICE_LINE_T")
public class InvoiceLine extends AuditableEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "INVOICE_LINE_ID")
  private Long invoiceLineId;

  @Column(name = "INVOICE_ID", nullable = false)
  private Long invoiceId;

  @Column(name = "LINE_NBR", nullable = false)
  private Integer lineNbr;

  @Column(name = "LINE_DESC", length = 60)
  private String lineDesc;

  @Column(name = "LINE_AMT", nullable = false, precision = 11, scale = 2)
  private BigDecimal lineAmt;

  public Long getInvoiceLineId() {
    return invoiceLineId;
  }

  public void setInvoiceLineId(Long invoiceLineId) {
    this.invoiceLineId = invoiceLineId;
  }

  public Long getInvoiceId() {
    return invoiceId;
  }

  public void setInvoiceId(Long invoiceId) {
    this.invoiceId = invoiceId;
  }

  public Integer getLineNbr() {
    return lineNbr;
  }

  public void setLineNbr(Integer lineNbr) {
    this.lineNbr = lineNbr;
  }

  public String getLineDesc() {
    return lineDesc;
  }

  public void setLineDesc(String lineDesc) {
    this.lineDesc = lineDesc;
  }

  public BigDecimal getLineAmt() {
    return lineAmt;
  }

  public void setLineAmt(BigDecimal lineAmt) {
    this.lineAmt = lineAmt;
  }
}
