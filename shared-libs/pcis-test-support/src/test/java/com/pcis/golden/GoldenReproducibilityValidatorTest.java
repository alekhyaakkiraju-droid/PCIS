package com.pcis.golden;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class GoldenReproducibilityValidatorTest {

  @TempDir Path temp;

  @Test
  void identicalCapturesPass() throws Exception {
    GoldenReproducibilityValidator validator = GoldenReproducibilityValidator.triple(temp);
    byte[] payload = "{\"ok\":true}\n".getBytes(StandardCharsets.UTF_8);
    var result = validator.validate("BIL003B", "scenario-01", () -> payload);
    assertTrue(result.identical());
  }

  @Test
  void mismatchedCapturesWriteQuarantineReport() throws Exception {
    GoldenReproducibilityValidator validator = GoldenReproducibilityValidator.triple(temp);
    AtomicInteger n = new AtomicInteger();
    var result =
        validator.validate(
            "BIL003B",
            "scenario-01",
            () -> ("run-" + n.incrementAndGet() + "\n").getBytes(StandardCharsets.UTF_8));
    assertFalse(result.identical());
    assertTrue(Files.exists(result.quarantineReport()));
    String body = Files.readString(result.quarantineReport());
    assertTrue(body.contains("byte mismatch"));
    assertTrue(body.contains("\"scenario\""));
    assertTrue(
        result.quarantineReport().getFileName().toString().equals("scenario-01-quarantine.json"));
  }
}
