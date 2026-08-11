package com.pcis.batch.auth.support;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/** Builds unsigned JWT access tokens for tests. */
public final class TestJwtFactory {

  private TestJwtFactory() {}

  public static String tokenWithSubject(String subject) {
    String header =
        Base64.getUrlEncoder()
            .withoutPadding()
            .encodeToString("{\"alg\":\"none\",\"typ\":\"JWT\"}".getBytes(StandardCharsets.UTF_8));
    String payload =
        Base64.getUrlEncoder()
            .withoutPadding()
            .encodeToString(
                ("{\"sub\":\"" + subject + "\",\"exp\":" + (System.currentTimeMillis() / 1000 + 3600) + "}")
                    .getBytes(StandardCharsets.UTF_8));
    return header + "." + payload + ".signature";
  }
}
