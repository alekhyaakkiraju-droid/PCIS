package com.pcis.claims.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "claim_reserve_ledger")
public class ClaimReserveLedgerEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "ledger_id")
  private Long ledgerId;

  @ManyToOne(optional = false)
  @JoinColumn(name = "claim_nbr", nullable = false)
  private ClaimEntity claim;

  @ManyToOne
  @JoinColumn(name = "reserve_id")
  private ClaimReserveEntity reserve;

  @Column(name = "event_date", nullable = false)
  private LocalDate eventDate;

  @Column(name = "reason", nullable = false, length = 200)
  private String reason;

  @Column(name = "amount", nullable = false, precision = 11, scale = 2)
  private BigDecimal amount;

  @Column(name = "balance_after", nullable = false, precision = 11, scale = 2)
  private BigDecimal balanceAfter;

  @Column(name = "actor_id", nullable = false, length = 10)
  private String actorId;

  @Column(name = "event_type", nullable = false, length = 4)
  private String eventType;

  @Column(name = "crt_timestamp", nullable = false)
  private Instant crtTimestamp = Instant.now();

  public Long getLedgerId() {
    return ledgerId;
  }

  public ClaimEntity getClaim() {
    return claim;
  }

  public void setClaim(ClaimEntity claim) {
    this.claim = claim;
  }

  public ClaimReserveEntity getReserve() {
    return reserve;
  }

  public void setReserve(ClaimReserveEntity reserve) {
    this.reserve = reserve;
  }

  public LocalDate getEventDate() {
    return eventDate;
  }

  public void setEventDate(LocalDate eventDate) {
    this.eventDate = eventDate;
  }

  public String getReason() {
    return reason;
  }

  public void setReason(String reason) {
    this.reason = reason;
  }

  public BigDecimal getAmount() {
    return amount;
  }

  public void setAmount(BigDecimal amount) {
    this.amount = amount;
  }

  public BigDecimal getBalanceAfter() {
    return balanceAfter;
  }

  public void setBalanceAfter(BigDecimal balanceAfter) {
    this.balanceAfter = balanceAfter;
  }

  public String getActorId() {
    return actorId;
  }

  public void setActorId(String actorId) {
    this.actorId = actorId;
  }

  public String getEventType() {
    return eventType;
  }

  public void setEventType(String eventType) {
    this.eventType = eventType;
  }

  public Instant getCrtTimestamp() {
    return crtTimestamp;
  }
}
