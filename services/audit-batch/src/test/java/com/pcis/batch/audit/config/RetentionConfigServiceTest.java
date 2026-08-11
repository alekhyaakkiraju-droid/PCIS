package com.pcis.batch.audit.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.pcis.config.TunableKey;
import com.pcis.config.TunableNotFoundException;
import com.pcis.config.TunableResolver;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class RetentionConfigServiceTest {

  @Test
  void resolvesRetentionDaysFromTunableResolver() {
    TunableResolver resolver = Mockito.mock(TunableResolver.class);
    Mockito.when(resolver.getInt(TunableKey.AUDIT_RETENTION_DAYS)).thenReturn(730);
    AuditArchiveProperties properties = new AuditArchiveProperties();

    RetentionConfigService service = new RetentionConfigService(resolver, properties);

    assertThat(service.getRetentionDays()).isEqualTo(730);
  }

  @Test
  void fallsBackToPropertiesWhenTunableMissing() {
    TunableResolver resolver = Mockito.mock(TunableResolver.class);
    Mockito.when(resolver.getInt(TunableKey.AUDIT_RETENTION_DAYS))
        .thenThrow(new TunableNotFoundException("audit.retention.days"));
    AuditArchiveProperties properties = new AuditArchiveProperties();
    properties.setRetentionDays(400);

    RetentionConfigService service = new RetentionConfigService(resolver, properties);

    assertThat(service.getRetentionDays()).isEqualTo(400);
  }

  @Test
  void rejectsRetentionBelowPolicyMinimum() {
    TunableResolver resolver = Mockito.mock(TunableResolver.class);
    Mockito.when(resolver.getInt(TunableKey.AUDIT_RETENTION_DAYS)).thenReturn(180);
    AuditArchiveProperties properties = new AuditArchiveProperties();

    RetentionConfigService service = new RetentionConfigService(resolver, properties);

    assertThatThrownBy(service::getRetentionDays)
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("365");
  }

  @Test
  void resolvesTierRetentionDays() {
    TunableResolver resolver = Mockito.mock(TunableResolver.class);
    Mockito.when(resolver.getInt(TunableKey.AUDIT_RETENTION_DAYS))
        .thenThrow(new TunableNotFoundException("audit.retention.days"));
    AuditArchiveProperties properties = new AuditArchiveProperties();

    RetentionConfigService service = new RetentionConfigService(resolver, properties);

    assertThat(service.getRetentionDaysForTier("CONFIDENTIAL")).isEqualTo(730);
    assertThat(service.getRetentionDaysForTier("RESTRICTED")).isEqualTo(2555);
  }

  @Test
  void rejectsTierRetentionBelowPolicyMinimum() {
    TunableResolver resolver = Mockito.mock(TunableResolver.class);
    AuditArchiveProperties properties = new AuditArchiveProperties();
    properties.getRetention().setPublicDays(180);

    RetentionConfigService service = new RetentionConfigService(resolver, properties);

    assertThatThrownBy(() -> service.getRetentionDaysForTier("PUBLIC"))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("365");
  }
}
