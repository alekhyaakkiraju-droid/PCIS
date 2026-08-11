package com.pcis.config;

import com.pcis.error.ReasonCode;
import com.pcis.error.TerminalPcisException;
import java.math.BigDecimal;

public final class TunableOutOfRangeException extends TerminalPcisException {
  public TunableOutOfRangeException(String key, BigDecimal value, BigDecimal min, BigDecimal max) {
    super(
        ReasonCode.CFG_TUNABLE_OUT_OF_RANGE,
        "Tunable out of range: " + key + " value=" + value + " bounds=[" + min + "," + max + "]",
        "system",
        "config/tunable/" + key,
        "resolve");
  }
}
