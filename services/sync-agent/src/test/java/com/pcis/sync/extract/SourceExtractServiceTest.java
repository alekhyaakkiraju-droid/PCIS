package com.pcis.sync.extract;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class SourceExtractServiceTest {

  private final SourceExtractService service = new SourceExtractService(null);

  @Test
  void maxWatermarkFromBatchReturnsHighestValue() {
    List<Map<String, Object>> rows =
        List.of(
            row("2024-01-01 10:00:00"),
            row("2024-03-01 10:00:00"),
            row("2024-02-01 10:00:00"));

    String max = service.maxWatermarkFromBatch(rows, "UPD_TIMESTAMP", "2024-01-01 00:00:00");

    assertThat(max).isEqualTo("2024-03-01 10:00:00");
  }

  @Test
  void maxWatermarkFromBatchRetainsCurrentWhenBatchEmpty() {
    String max = service.maxWatermarkFromBatch(List.of(), "UPD_TIMESTAMP", "2024-05-01 00:00:00");

    assertThat(max).isEqualTo("2024-05-01 00:00:00");
  }

  private Map<String, Object> row(String timestamp) {
    Map<String, Object> row = new LinkedHashMap<>();
    row.put("UPD_TIMESTAMP", timestamp);
    return row;
  }
}
