package com.pcis.config.entity;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import java.time.Instant;

@MappedSuperclass
public abstract class AuditableEntity {

  @Column(name = "crt_user", length = 10)
  private String crtUser;

  @Column(name = "crt_timestamp")
  private Instant crtTimestamp;

  @Column(name = "upd_user", length = 10)
  private String updUser;

  @Column(name = "upd_timestamp")
  private Instant updTimestamp;

  public String getCrtUser() {
    return crtUser;
  }

  public void setCrtUser(String crtUser) {
    this.crtUser = crtUser;
  }

  public Instant getCrtTimestamp() {
    return crtTimestamp;
  }

  public void setCrtTimestamp(Instant crtTimestamp) {
    this.crtTimestamp = crtTimestamp;
  }

  public String getUpdUser() {
    return updUser;
  }

  public void setUpdUser(String updUser) {
    this.updUser = updUser;
  }

  public Instant getUpdTimestamp() {
    return updTimestamp;
  }

  public void setUpdTimestamp(Instant updTimestamp) {
    this.updTimestamp = updTimestamp;
  }
}
