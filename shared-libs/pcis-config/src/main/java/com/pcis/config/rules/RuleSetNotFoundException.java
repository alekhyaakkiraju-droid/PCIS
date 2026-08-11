package com.pcis.config.rules;

import com.pcis.error.ReasonCode;
import com.pcis.error.TerminalPcisException;

public final class RuleSetNotFoundException extends TerminalPcisException {

  public RuleSetNotFoundException(String ruleSetKey) {
    super(
        ReasonCode.CFG_RULE_SET_NOT_FOUND,
        "Required rule set not found: " + ruleSetKey,
        "system",
        "config/rule-set/" + ruleSetKey,
        "resolve");
  }
}
