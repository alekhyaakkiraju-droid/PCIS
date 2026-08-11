package com.pcis.config.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinColumns;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;

@Entity
@Table(name = "config_tunable_history_t")
public class ConfigTunableHistoryEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "history_id", nullable = false, updatable = false)
  private Long historyId;

  @NotBlank
  @Size(max = 60)
  @Column(name = "tunable_key", nullable = false, length = 60)
  private String tunableKey;

  @NotNull
  @Column(name = "version_no", nullable = false)
  private Integer versionNo;

  @Size(max = 200)
  @Column(name = "old_value", length = 200)
  private String oldValue;

  @NotBlank
  @Size(max = 200)
  @Column(name = "new_value", nullable = false, length = 200)
  private String newValue;

  @NotBlank
  @Size(max = 200)
  @Column(name = "change_reason", nullable = false, length = 200)
  private String changeReason;

  @NotBlank
  @Size(max = 50)
  @Column(name = "changed_by", nullable = false, length = 50)
  private String changedBy;

  @NotNull
  @Column(name = "changed_timestamp", nullable = false)
  private Instant changedTimestamp;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumns({
    @JoinColumn(name = "tunable_key", referencedColumnName = "tunable_key", insertable = false, updatable = false),
    @JoinColumn(name = "version_no", referencedColumnName = "version_no", insertable = false, updatable = false)
  })
  private ConfigTunableEntity tunable;

  public Long getHistoryId() {
    return historyId;
  }

  public void setHistoryId(Long historyId) {
    this.historyId = historyId;
  }

  public String getTunableKey() {
    return tunableKey;
  }

  public void setTunableKey(String tunableKey) {
    this.tunableKey = tunableKey;
  }

  public Integer getVersionNo() {
    return versionNo;
  }

  public void setVersionNo(Integer versionNo) {
    this.versionNo = versionNo;
  }

  public String getOldValue() {
    return oldValue;
  }

  public void setOldValue(String oldValue) {
    this.oldValue = oldValue;
  }

  public String getNewValue() {
    return newValue;
  }

  public void setNewValue(String newValue) {
    this.newValue = newValue;
  }

  public String getChangeReason() {
    return changeReason;
  }

  public void setChangeReason(String changeReason) {
    this.changeReason = changeReason;
  }

  public String getChangedBy() {
    return changedBy;
  }

  public void setChangedBy(String changedBy) {
    this.changedBy = changedBy;
  }

  public Instant getChangedTimestamp() {
    return changedTimestamp;
  }

  public void setChangedTimestamp(Instant changedTimestamp) {
    this.changedTimestamp = changedTimestamp;
  }

  public ConfigTunableEntity getTunable() {
    return tunable;
  }

  public void setTunable(ConfigTunableEntity tunable) {
    this.tunable = tunable;
  }
}
