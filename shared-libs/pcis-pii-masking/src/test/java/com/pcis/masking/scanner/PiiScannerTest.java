package com.pcis.masking.scanner;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class PiiScannerTest {

  private final PiiScanner scanner = new PiiScanner();

  @Test
  void detectsDashedSsn() {
    var detections = scanner.scanText("AUDIT_LOG_T", "OLD_VALUE", "1", "tax=123-45-6789");

    assertThat(detections).hasSize(1);
    assertThat(detections.getFirst().patternType()).isEqualTo(PiiPattern.SSN_DASHED);
    assertThat(detections.getFirst().snippet()).isEqualTo("12...89");
  }

  @Test
  void ignoresMaskedSsn() {
    var detections = scanner.scanText("AUDIT_LOG_T", "OLD_VALUE", "1", "***-**-6789");

    assertThat(detections).isEmpty();
  }

  @Test
  void detectsEmailAddress() {
    var detections =
        scanner.scanText("AUDIT_LOG_T", "NEW_VALUE", "2", "contact alice@example.com please");

    assertThat(detections).anyMatch(d -> d.patternType() == PiiPattern.EMAIL);
  }

  @Test
  void ignoresMaskedEmail() {
    var detections = scanner.scanText("AUDIT_LOG_T", "NEW_VALUE", "2", "***@example.com");

    assertThat(detections).isEmpty();
  }

  @Test
  void detectsVin() {
    var detections =
        scanner.scanText("AUDIT_LOG_T", "RECORD_KEY", "3", "VIN 1HGCM82633A004352 stored");

    assertThat(detections).anyMatch(d -> d.patternType() == PiiPattern.VIN);
  }

  @Test
  void handlesNullText() {
    assertThat(scanner.scanText("AUDIT_LOG_T", "OLD_VALUE", "4", null)).isEmpty();
  }
}
