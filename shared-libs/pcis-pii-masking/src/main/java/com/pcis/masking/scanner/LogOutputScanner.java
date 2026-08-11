package com.pcis.masking.scanner;

import java.util.ArrayList;
import java.util.List;

/** Scans captured log output line-by-line for unmasked PII. */
public final class LogOutputScanner {

  private final PiiScanner textScanner = new PiiScanner();

  public List<PiiDetection> scanLogLines(List<String> lines) {
    if (lines == null || lines.isEmpty()) {
      return List.of();
    }
    List<PiiDetection> detections = new ArrayList<>();
    for (int i = 0; i < lines.size(); i++) {
      String line = lines.get(i);
      detections.addAll(
          textScanner.scanText("log", "line:" + (i + 1), String.valueOf(i + 1), line));
    }
    return detections;
  }

  public PiiScanReport scanLogLinesReport(List<String> lines) {
    List<PiiDetection> detections = scanLogLines(lines);
    long lineCount = lines == null ? 0 : lines.size();
    return PiiScanReport.of(0, 0, lineCount, detections);
  }
}
