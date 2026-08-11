package com.pcis.config;

import com.pcis.error.ReasonCode;
import com.pcis.error.TerminalPcisException;

public final class TunableNotFoundException extends TerminalPcisException {
  public TunableNotFoundException(String key) {
    super(
        ReasonCode.CFG_TUNABLE_NOT_FOUND,
        "Required tunable not found: " + key,
        "system",
        "config/tunable/" + key,
        "resolve");
  }
}
