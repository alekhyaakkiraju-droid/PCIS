package com.pcis.billing.domain;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import java.time.Instant;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class AuditableEntity {

  @CreatedBy
  @Column(name = "CRT_USER", length = 10, updatable = false)
  private String crtUser;

  @CreatedDate
  @Column(name = "CRT_TIMESTAMP", updatable = false)
  private Instant crtTimestamp;

  @LastModifiedBy
  @Column(name = "UPD_USER", length = 10)
  private String updUser;

  @LastModifiedDate
  @Column(name = "UPD_TIMESTAMP")
  private Instant updTimestamp;

  public String getCrtUser() {
    return crtUser;
  }

  public Instant getCrtTimestamp() {
    return crtTimestamp;
  }

  public String getUpdUser() {
    return updUser;
  }

  public Instant getUpdTimestamp() {
    return updTimestamp;
  }
}
