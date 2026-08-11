package com.pcis.masking.scanner;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class PiiScanReportTest {

  @Test
  void serializesToJsonWithPassStatus() {
    var report = PiiScanReport.of(1, 10, 0, java.util.List.of());

    assertThat(report.overallStatus()).isEqualTo(PiiScanReport.ScanStatus.PASS);
    assertThat(report.toJson()).contains("\"overallStatus\" : \"PASS\"");
  }

  @Test
  void snippetTruncatesMatchedValue() {
    assertThat(PiiDetection.snippetFor("123-45-6789")).isEqualTo("12...89");
  }
}
