package com.pcis.config.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "config_tunable_t")
public class ConfigTunableEntity extends AuditableEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "tunable_row_id", nullable = false, updatable = false)
  private Long tunableRowId;

  @NotBlank
  @Size(max = 60)
  @Column(name = "tunable_key", nullable = false, length = 60)
  private String tunableKey;

  @NotBlank
  @Size(min = 3, max = 3)
  @Column(name = "domain_cd", nullable = false, length = 3)
  private String domainCd;

  @NotBlank
  @Size(min = 1, max = 1)
  @Column(name = "value_type", nullable = false, length = 1)
  private String valueType;

  @Size(max = 200)
  @Column(name = "value_text", length = 200)
  private String valueText;

  @Column(name = "numeric_value", precision = 11, scale = 2)
  private BigDecimal numericValue;

  @Column(name = "min_value", precision = 11, scale = 2)
  private BigDecimal minValue;

  @Column(name = "max_value", precision = 11, scale = 2)
  private BigDecimal maxValue;

  @Size(max = 10)
  @Column(name = "unit_cd", length = 10)
  private String unitCd;

  @NotBlank
  @Size(max = 200)
  @Column(name = "description", nullable = false, length = 200)
  private String description;

  @NotNull
  @Column(name = "effective_from", nullable = false)
  private LocalDate effectiveFrom;

  @Column(name = "effective_to")
  private LocalDate effectiveTo;

  @NotNull
  @Version
  @Column(name = "version_no", nullable = false)
  private Integer versionNo;

  public Long getTunableRowId() {
    return tunableRowId;
  }

  public void setTunableRowId(Long tunableRowId) {
    this.tunableRowId = tunableRowId;
  }

  public String getTunableKey() {
    return tunableKey;
  }

  public void setTunableKey(String tunableKey) {
    this.tunableKey = tunableKey;
  }

  public String getDomainCd() {
    return domainCd;
  }

  public void setDomainCd(String domainCd) {
    this.domainCd = domainCd;
  }

  public String getValueType() {
    return valueType;
  }

  public void setValueType(String valueType) {
    this.valueType = valueType;
  }

  public String getValueText() {
    return valueText;
  }

  public void setValueText(String valueText) {
    this.valueText = valueText;
  }

  public BigDecimal getNumericValue() {
    return numericValue;
  }

  public void setNumericValue(BigDecimal numericValue) {
    this.numericValue = numericValue;
  }

  public BigDecimal getMinValue() {
    return minValue;
  }

  public void setMinValue(BigDecimal minValue) {
    this.minValue = minValue;
  }

  public BigDecimal getMaxValue() {
    return maxValue;
  }

  public void setMaxValue(BigDecimal maxValue) {
    this.maxValue = maxValue;
  }

  public String getUnitCd() {
    return unitCd;
  }

  public void setUnitCd(String unitCd) {
    this.unitCd = unitCd;
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

  public Integer getVersionNo() {
    return versionNo;
  }

  public void setVersionNo(Integer versionNo) {
    this.versionNo = versionNo;
  }
}
