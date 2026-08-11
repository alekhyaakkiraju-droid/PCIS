package com.pcis.batch.audit.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;

class AuditArchivePropertiesTest {

  @Test
  void bindsRetentionAndChunkDefaults() {
    AuditArchiveProperties properties =
        bind(Map.of("pcis.audit.retention-days", "180", "pcis.audit.chunk-size", "500"));

    assertThat(properties.getRetentionDays()).isEqualTo(180);
    assertThat(properties.getChunkSize()).isEqualTo(500);
    assertThat(properties.getProgramName()).isEqualTo("AUD002B");
  }

  @Test
  void defaultsMatchLegacyCobol() {
    AuditArchiveProperties properties = bind(Map.of());

    assertThat(properties.getRetentionDays()).isEqualTo(365);
    assertThat(properties.getChunkSize()).isEqualTo(1000);
  }

  private static AuditArchiveProperties bind(Map<String, String> values) {
    return new Binder(new MapConfigurationPropertySource(values))
        .bind("pcis.audit", Bindable.of(AuditArchiveProperties.class))
        .orElseGet(AuditArchiveProperties::new);
  }
}
