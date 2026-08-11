package com.pcis.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TunableResolverTest {

  private TunableRepository repository;
  private PcisTunableProperties properties;
  private TunableResolver resolver;

  @BeforeEach
  void setUp() {
    repository = mock(TunableRepository.class);
    properties = new PcisTunableProperties();
    resolver = new TunableResolver(repository, properties, new SimpleMeterRegistry());
  }

  @Test
  void resolvesNumericValueFromDatabase() {
    when(repository.findEffective(TunableKey.BILLING_LEAD_DAYS.key()))
        .thenReturn(
            new TunableRow(
                TunableKey.BILLING_LEAD_DAYS.key(),
                "I",
                null,
                BigDecimal.valueOf(15),
                BigDecimal.ONE,
                BigDecimal.valueOf(90),
                java.time.LocalDate.now().minusDays(1),
                null));

    assertThat(resolver.getInt(TunableKey.BILLING_LEAD_DAYS)).isEqualTo(15);
  }

  @Test
  void failsFastWhenRequiredTunableMissing() {
    when(repository.findEffective(TunableKey.BILLING_LEAD_DAYS.key())).thenReturn(null);

    assertThatThrownBy(() -> resolver.getInt(TunableKey.BILLING_LEAD_DAYS))
        .isInstanceOf(TunableNotFoundException.class);
  }

  @Test
  void rejectsOutOfRangeValue() {
    when(repository.findEffective(TunableKey.BILLING_LEAD_DAYS.key()))
        .thenReturn(
            new TunableRow(
                TunableKey.BILLING_LEAD_DAYS.key(),
                "I",
                null,
                BigDecimal.valueOf(999),
                BigDecimal.ONE,
                BigDecimal.valueOf(90),
                java.time.LocalDate.now().minusDays(1),
                null));

    assertThatThrownBy(() -> resolver.getInt(TunableKey.BILLING_LEAD_DAYS))
        .isInstanceOf(TunableOutOfRangeException.class);
  }

  @Test
  void refreshMakesUpdatedDatabaseValueVisible() {
    when(repository.findEffective(TunableKey.PREMIUM_GRACE_DAYS.key()))
        .thenReturn(
            new TunableRow(
                TunableKey.PREMIUM_GRACE_DAYS.key(),
                "I",
                null,
                BigDecimal.TEN,
                BigDecimal.ZERO,
                BigDecimal.valueOf(60),
                java.time.LocalDate.now().minusDays(1),
                null))
        .thenReturn(
            new TunableRow(
                TunableKey.PREMIUM_GRACE_DAYS.key(),
                "I",
                null,
                BigDecimal.valueOf(20),
                BigDecimal.ZERO,
                BigDecimal.valueOf(60),
                java.time.LocalDate.now().minusDays(1),
                null));

    assertThat(resolver.getInt(TunableKey.PREMIUM_GRACE_DAYS)).isEqualTo(10);
    resolver.refresh(TunableKey.PREMIUM_GRACE_DAYS.key());
    assertThat(resolver.getInt(TunableKey.PREMIUM_GRACE_DAYS)).isEqualTo(20);
  }
}
