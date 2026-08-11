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
@Table(name = "REFUND_T")
public class Refund extends AuditableEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "REFUND_ID")
  private Long refundId;

  @Column(name = "POL_NBR", length = 12)
  private String polNbr;

  @Column(name = "REFUND_DATE")
  private LocalDate refundDate;

  @Column(name = "REFUND_AMT", precision = 11, scale = 2)
  private BigDecimal refundAmt;

  @Column(name = "REFUND_REASON", length = 4, columnDefinition = "char(4)")
  @JdbcTypeCode(SqlTypes.CHAR)
  private String refundReason;

  public Long getRefundId() {
    return refundId;
  }

  public void setRefundId(Long refundId) {
    this.refundId = refundId;
  }

  public String getPolNbr() {
    return polNbr;
  }

  public void setPolNbr(String polNbr) {
    this.polNbr = polNbr;
  }

  public LocalDate getRefundDate() {
    return refundDate;
  }

  public void setRefundDate(LocalDate refundDate) {
    this.refundDate = refundDate;
  }

  public BigDecimal getRefundAmt() {
    return refundAmt;
  }

  public void setRefundAmt(BigDecimal refundAmt) {
    this.refundAmt = refundAmt;
  }

  public String getRefundReason() {
    return refundReason;
  }

  public void setRefundReason(String refundReason) {
    this.refundReason = refundReason;
  }
}
