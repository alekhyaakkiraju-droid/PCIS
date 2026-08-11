package com.pcis.config.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "config_rule_set_t")
@IdClass(ConfigRuleSetEntityId.class)
public class ConfigRuleSetEntity extends AuditableEntity {

  @Id
  @NotBlank
  @Size(max = 60)
  @Column(name = "rule_set_key", nullable = false, length = 60)
  private String ruleSetKey;

  @Id
  @NotNull
  @Column(name = "version_no", nullable = false)
  private Integer versionNo;

  @NotBlank
  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "payload", nullable = false, columnDefinition = "jsonb")
  private String payload;

  @NotBlank
  @Size(max = 200)
  @Column(name = "description", nullable = false, length = 200)
  private String description;

  @NotNull
  @Column(name = "effective_from", nullable = false)
  private LocalDate effectiveFrom;

  @Column(name = "effective_to")
  private LocalDate effectiveTo;

  @NotBlank
  @Size(min = 1, max = 1)
  @Column(name = "status_cd", nullable = false, length = 1)
  private String statusCd;

  public String getRuleSetKey() {
    return ruleSetKey;
  }

  public void setRuleSetKey(String ruleSetKey) {
    this.ruleSetKey = ruleSetKey;
  }

  public Integer getVersionNo() {
    return versionNo;
  }

  public void setVersionNo(Integer versionNo) {
    this.versionNo = versionNo;
  }

  public String getPayload() {
    return payload;
  }

  public void setPayload(String payload) {
    this.payload = payload;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public LocalDate getEffectiveFrom() {
    return effectiveFrom;
  }

  public void setEffectiveFrom(LocalDate effectiveFrom) {
    this.effectiveFrom = effectiveFrom;
  }

  public LocalDate getEffectiveTo() {
    return effectiveTo;
  }

  public void setEffectiveTo(LocalDate effectiveTo) {
    this.effectiveTo = effectiveTo;
  }

  public String getStatusCd() {
    return statusCd;
  }

  public void setStatusCd(String statusCd) {
    this.statusCd = statusCd;
  }
}
