package com.pcis.batch.audit.infrastructure;

import com.pcis.batch.audit.domain.PurgeType;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;

/** SHA-256 evidence hash for tamper detection (WO-171). */
public final class PurgeEvidenceHasher {

  private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_INSTANT;

  private PurgeEvidenceHasher() {}

  public static String computeHash(
      PurgeType purgeType,
      String targetIdentifier,
      String tier,
      int retentionDays,
      Instant purgeTimestamp,
      String actor) {
    String payload =
        purgeType.name()
            + '|'
            + targetIdentifier
            + '|'
            + tier
            + '|'
            + retentionDays
            + '|'
            + ISO.format(purgeTimestamp)
            + '|'
            + actor;
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] hash = digest.digest(payload.getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(hash);
    } catch (NoSuchAlgorithmException ex) {
      throw new IllegalStateException("SHA-256 not available", ex);
    }
  }
}
