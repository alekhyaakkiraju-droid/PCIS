package com.pcis.masking.scanner;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class LogOutputScannerTest {

  private final LogOutputScanner scanner = new LogOutputScanner();

  @Test
  void detectsUnmaskedPiiInLogLines() {
    var detections =
        scanner.scanLogLines(List.of("Customer email updated to bob@test.com", "status=ok"));

    assertThat(detections).hasSize(1);
    assertThat(detections.getFirst().location()).isEqualTo("line:1");
  }

  @Test
  void passesForMaskedLogLines() {
    var report = scanner.scanLogLinesReport(List.of("tax id ***-**-6789", "email ***@test.com"));

    assertThat(report.overallStatus()).isEqualTo(PiiScanReport.ScanStatus.PASS);
  }
}
