package com.pcis.masking.scanner;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.Instant;
import java.util.List;

/** Structured PII scan report emitted to CI and production logs. */
public record PiiScanReport(
    Instant scanTimestamp,
    int tablesScanned,
    long rowsScanned,
    long logLinesScanned,
    List<PiiDetection> detections,
    ScanStatus overallStatus) {

  public enum ScanStatus {
    PASS,
    FAIL
  }

  private static final ObjectMapper MAPPER =
      new ObjectMapper()
          .registerModule(new JavaTimeModule())
          .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

  public static PiiScanReport of(
      int tablesScanned,
      long rowsScanned,
      long logLinesScanned,
      List<PiiDetection> detections) {
    ScanStatus status = detections.isEmpty() ? ScanStatus.PASS : ScanStatus.FAIL;
    return new PiiScanReport(
        Instant.now(), tablesScanned, rowsScanned, logLinesScanned, List.copyOf(detections), status);
  }

  public String toJson() {
    try {
      return MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(this);
    } catch (JsonProcessingException ex) {
      throw new IllegalStateException("Unable to serialize PiiScanReport", ex);
    }
  }
}
