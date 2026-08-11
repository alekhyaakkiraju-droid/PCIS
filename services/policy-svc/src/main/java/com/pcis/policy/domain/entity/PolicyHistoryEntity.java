package com.pcis.policy.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "policy_history")
public class PolicyHistoryEntity extends AuditableEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "hist_id", nullable = false)
  private Long histId;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "pol_nbr", nullable = false)
  private PolicyEntity policy;

  @Column(name = "event_code", nullable = false, length = 10, columnDefinition = "char(10)")
  @JdbcTypeCode(SqlTypes.CHAR)
  private String eventCode;

  @Column(name = "event_date", nullable = false)
  private LocalDate eventDate;

  @Column(name = "event_desc", length = 100)
  private String eventDesc;

  public Long getHistId() { return histId; }
  public PolicyEntity getPolicy() { return policy; }
  public void setPolicy(PolicyEntity policy) { this.policy = policy; }
  public String getEventCode() { return eventCode; }
  public void setEventCode(String eventCode) { this.eventCode = eventCode; }
  public LocalDate getEventDate() { return eventDate; }
  public void setEventDate(LocalDate eventDate) { this.eventDate = eventDate; }
  public String getEventDesc() { return eventDesc; }
  public void setEventDesc(String eventDesc) { this.eventDesc = eventDesc; }
}
