package com.pcis.authz.infrastructure.persistence;

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
  @Column(name = "crt_user", nullable = false, length = 10)
  private String crtUser;

  @CreatedDate
  @Column(name = "crt_timestamp", nullable = false)
  private Instant crtTimestamp;

  @LastModifiedBy
  @Column(name = "upd_user", nullable = false, length = 10)
  private String updUser;

  @LastModifiedDate
  @Column(name = "upd_timestamp", nullable = false)
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
