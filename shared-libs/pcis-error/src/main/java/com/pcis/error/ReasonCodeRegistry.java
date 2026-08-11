package com.pcis.error;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class ReasonCodeRegistry {

  private static final Map<String, ReasonCode> BY_CODE =
      Arrays.stream(ReasonCode.values())
          .collect(Collectors.toUnmodifiableMap(ReasonCode::code, Function.identity()));

  private ReasonCodeRegistry() {}

  public static ReasonCode require(String code) {
    ReasonCode reason = BY_CODE.get(code);
    if (reason == null) {
      throw new IllegalArgumentException("Unknown reason code: " + code);
    }
    return reason;
  }

  public static boolean isRegistered(String code) {
    return BY_CODE.containsKey(code);
  }

  public static int size() {
    return BY_CODE.size();
  }
}
