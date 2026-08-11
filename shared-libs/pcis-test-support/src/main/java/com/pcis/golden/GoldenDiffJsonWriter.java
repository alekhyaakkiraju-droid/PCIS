package com.pcis.golden;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Serializes {@link GoldenDiff} to JSON for CI artifact publishing. */
public final class GoldenDiffJsonWriter {

  private static final ObjectMapper MAPPER =
      new ObjectMapper()
          .enable(SerializationFeature.INDENT_OUTPUT)
          .enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS);

  private GoldenDiffJsonWriter() {}

  public static String render(GoldenDiff diff) {
    try {
      return MAPPER.writeValueAsString(toMap(diff));
    } catch (IOException e) {
      throw new ConfigurationException("Failed to serialize golden diff to JSON", e);
    }
  }

  public static void write(GoldenDiff diff, Path dest) throws IOException {
    Files.createDirectories(dest.getParent());
    Files.writeString(dest, render(diff) + "\n");
  }

  public static void write(GoldenDiff diff, OutputStream out) throws IOException {
    MAPPER.writeValue(out, toMap(diff));
  }

  private static Map<String, Object> toMap(GoldenDiff diff) {
    Map<String, Object> root = new LinkedHashMap<>();
    root.put("scenarioId", diff.getScenarioId());
    root.put("match", diff.isMatch());
    root.put("totalDiffCount", diff.getTotalDiffCount());
    root.put("truncated", diff.isTruncated());
    root.put("countsByCategory", diff.getCountsByCategory());
    root.put(
        "entries",
        diff.getEntries().stream().map(GoldenDiffJsonWriter::entryMap).toList());
    return root;
  }

  private static Map<String, Object> entryMap(GoldenDiffEntry entry) {
    Map<String, Object> map = new LinkedHashMap<>();
    map.put("table", entry.getTable());
    map.put("businessKey", entry.getBusinessKey());
    map.put("column", entry.getColumn());
    map.put("expectedValue", entry.getExpectedValue());
    map.put("actualValue", entry.getActualValue());
    map.put("signedDelta", entry.getSignedDelta());
    map.put("category", entry.getCategory().name());
    return map;
  }
}
