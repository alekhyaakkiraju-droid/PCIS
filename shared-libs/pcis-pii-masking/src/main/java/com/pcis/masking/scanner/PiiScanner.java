package com.pcis.masking.scanner;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;

/** Scans free-text values for unmasked Restricted-tier PII patterns. */
public final class PiiScanner {

  public List<PiiDetection> scanText(
      String source, String location, String rowId, String text) {
    if (text == null || text.isBlank()) {
      return List.of();
    }
    List<PiiDetection> detections = new ArrayList<>();
    for (PiiPattern pattern : PiiPattern.values()) {
      Matcher matcher = pattern.pattern().matcher(text);
      while (matcher.find()) {
        String matched = matcher.group();
        if (MaskPatternExclusion.isMaskedValue(matched)) {
          continue;
        }
        if (pattern == PiiPattern.EMAIL && matched.startsWith("***@")) {
          continue;
        }
        detections.add(
            new PiiDetection(
                source,
                location,
                rowId,
                pattern,
                PiiDetection.snippetFor(matched)));
      }
    }
    return detections;
  }
}
