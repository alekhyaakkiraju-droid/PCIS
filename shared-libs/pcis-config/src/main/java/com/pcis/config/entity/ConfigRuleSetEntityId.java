package com.pcis.config.entity;

import java.io.Serializable;
import java.util.Objects;

public class ConfigRuleSetEntityId implements Serializable {

  private String ruleSetKey;
  private Integer versionNo;

  public ConfigRuleSetEntityId() {}

  public ConfigRuleSetEntityId(String ruleSetKey, Integer versionNo) {
    this.ruleSetKey = ruleSetKey;
    this.versionNo = versionNo;
  }

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

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof ConfigRuleSetEntityId that)) {
      return false;
    }
    return Objects.equals(ruleSetKey, that.ruleSetKey) && Objects.equals(versionNo, that.versionNo);
  }

  @Override
  public int hashCode() {
    return Objects.hash(ruleSetKey, versionNo);
  }
}
