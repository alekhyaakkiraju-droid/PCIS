package com.pcis.classification;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/** Writes {@link CompletenessReport} JSON for CI artifacts. */
public final class ClassificationCompletenessReporter {

  private static final ObjectMapper MAPPER =
      new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

  private ClassificationCompletenessReporter() {}

  public static void writeReport(CompletenessReport report, Path outputPath) throws IOException {
    Files.createDirectories(outputPath.getParent());
    Map<String, Object> body = new LinkedHashMap<>();
    body.put("totalSchemaTables", report.totalSchemaTables());
    body.put("totalClassifiedTables", report.totalClassifiedTables());
    body.put("unclassifiedTables", report.unclassifiedTables());
    body.put("restrictedColumnsWithoutStrategy", report.restrictedColumnsWithoutStrategy());
    body.put("invalidMaskStrategies", report.invalidMaskStrategies());
    body.put("status", report.passed() ? "PASS" : "FAIL");
    MAPPER.writeValue(outputPath.toFile(), body);
  }
}
