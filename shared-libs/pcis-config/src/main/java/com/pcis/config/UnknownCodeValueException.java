package com.pcis.config;

import com.pcis.error.ReasonCode;
import com.pcis.error.TerminalPcisException;

public final class UnknownCodeValueException extends TerminalPcisException {

  private final String domainCode;
  private final String codeValue;

  public UnknownCodeValueException(String domainCode, String codeValue) {
    super(
        ReasonCode.CFG_UNKNOWN_CODE_VALUE,
        "Unknown or inactive code value: domain=" + domainCode + " code=" + codeValue,
        "system",
        "config/code-table/" + domainCode + "/" + codeValue,
        "lookup");
    this.domainCode = domainCode;
    this.codeValue = codeValue;
  }

  public String domainCode() {
    return domainCode;
  }

  public String codeValue() {
    return codeValue;
  }
}
