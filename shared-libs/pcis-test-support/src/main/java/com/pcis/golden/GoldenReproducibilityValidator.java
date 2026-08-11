package com.pcis.golden;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * Runs N capture iterations and asserts byte-identity. Failures are written to
 * {@code golden/quarantine/{program}/{scenario}-quarantine.json}.
 */
public final class GoldenReproducibilityValidator {

  private static final ObjectMapper MAPPER =
      new ObjectMapper()
          .enable(SerializationFeature.INDENT_OUTPUT)
          .enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS);

  private final int iterations;
  private final Path quarantineRoot;

  public GoldenReproducibilityValidator(int iterations, Path quarantineRoot) {
    if (iterations < 2) {
      throw new IllegalArgumentException("iterations must be >= 2");
    }
    this.iterations = iterations;
    this.quarantineRoot = Objects.requireNonNull(quarantineRoot, "quarantineRoot");
  }

  public static GoldenReproducibilityValidator triple(Path quarantineRoot) {
    return new GoldenReproducibilityValidator(3, quarantineRoot);
  }

  public record Result(boolean identical, Path quarantineReport, List<String> reasons) {}

  /**
   * Invokes {@code captureSupplier} {@code iterations} times and compares
   * canonical bytes.
   */
  public Result validate(
      String program, String scenario, Supplier<byte[]> captureSupplier) throws IOException {
    List<byte[]> captures = new ArrayList<>(iterations);
    for (int i = 0; i < iterations; i++) {
      captures.add(captureSupplier.get());
    }

    List<String> reasons = new ArrayList<>();
    byte[] baseline = captures.get(0);
    for (int i = 1; i < captures.size(); i++) {
      if (!Arrays.equals(baseline, captures.get(i))) {
        reasons.add(
            "byte mismatch: run-1 vs run-"
                + (i + 1)
                + " (len "
                + baseline.length
                + " vs "
                + captures.get(i).length
                + ")");
      }
    }

    if (reasons.isEmpty()) {
      return new Result(true, null, List.of());
    }

    Path report =
        quarantineRoot
            .resolve(program.toLowerCase())
            .resolve(scenario + "-quarantine.json");
    Files.createDirectories(report.getParent());

    Map<String, Object> body = new LinkedHashMap<>();
    body.put("program", program);
    body.put("scenario", scenario);
    body.put("iterations", iterations);
    body.put("reasons", reasons);
    List<Map<String, Object>> runs = new ArrayList<>();
    for (int i = 0; i < captures.size(); i++) {
      Map<String, Object> run = new LinkedHashMap<>();
      run.put("run", i + 1);
      run.put("sha256", sha256(captures.get(i)));
      run.put("bytes", captures.get(i).length);
      run.put("preview", preview(captures.get(i)));
      runs.add(run);
    }
    body.put("runs", runs);
    Files.writeString(report, MAPPER.writeValueAsString(body) + "\n", StandardCharsets.UTF_8);
    return new Result(false, report, List.copyOf(reasons));
  }

  private static String preview(byte[] bytes) {
    String text = new String(bytes, StandardCharsets.UTF_8);
    return text.length() <= 400 ? text : text.substring(0, 400) + "…";
  }

  private static String sha256(byte[] bytes) {
    try {
      java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
      byte[] dig = md.digest(bytes);
      StringBuilder sb = new StringBuilder();
      for (byte b : dig) {
        sb.append(String.format("%02x", b));
      }
      return sb.toString();
    } catch (Exception e) {
      throw new ConfigurationException("SHA-256 unavailable", e);
    }
  }
}
