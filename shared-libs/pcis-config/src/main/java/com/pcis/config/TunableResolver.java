package com.pcis.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;

public class TunableResolver {

  private static final Logger log = LoggerFactory.getLogger(TunableResolver.class);

  private final TunableRepository repository;
  private final PcisTunableProperties properties;
  private final Cache<String, Object> cache;
  private final ConcurrentMap<String, Double> lastResolved = new ConcurrentHashMap<>();

  public TunableResolver(
      TunableRepository repository, PcisTunableProperties properties, MeterRegistry meterRegistry) {
    this.repository = repository;
    this.properties = properties;
    this.cache =
        Caffeine.newBuilder()
            .maximumSize(properties.getCache().getMaxSize())
            .expireAfterWrite(Duration.ofSeconds(properties.getCache().getTtlSeconds()))
            .build();
    for (TunableKey key : TunableKey.values()) {
      Gauge.builder("pcis.tunable.resolved", lastResolved, map -> map.getOrDefault(key.key(), 0.0))
          .tag("key", key.key())
          .register(meterRegistry);
    }
  }

  @EventListener(ApplicationReadyEvent.class)
  void validateRequiredTunablesOnStartup() {
    for (TunableKey key : TunableKey.values()) {
      if (key.required()) {
        resolveNumeric(key);
      }
    }
  }

  public int getInt(TunableKey key) {
    return resolveNumeric(key).intValueExact();
  }

  public long getLong(TunableKey key) {
    return resolveNumeric(key).longValueExact();
  }

  public BigDecimal getBigDecimal(TunableKey key) {
    return resolveNumeric(key);
  }

  public boolean getBoolean(TunableKey key) {
    Object cached = cache.get(key.key(), k -> loadValue(key));
    if (cached instanceof Boolean bool) {
      return bool;
    }
    if (cached instanceof BigDecimal decimal) {
      return decimal.compareTo(BigDecimal.ZERO) != 0;
    }
    if (cached instanceof String text) {
      return Boolean.parseBoolean(text);
    }
    throw new TunableNotFoundException(key.key());
  }

  public String getString(TunableKey key) {
    Object cached = cache.get(key.key(), k -> loadValue(key));
    if (cached instanceof String text) {
      return text;
    }
    if (cached instanceof BigDecimal decimal) {
      return decimal.toPlainString();
    }
    throw new TunableNotFoundException(key.key());
  }

  public void refresh(String key) {
    cache.invalidate(key);
    log.info("Refreshed tunable cache actor=system resource=config/tunable/{} operation=refresh", key);
  }

  public void refreshAll() {
    cache.invalidateAll();
    log.info("Refreshed all tunable cache entries actor=system resource=config/tunable operation=refresh-all");
  }

  private BigDecimal resolveNumeric(TunableKey key) {
    Object cached = cache.get(key.key(), k -> loadValue(key));
    if (!(cached instanceof BigDecimal decimal)) {
      throw new TunableNotFoundException(key.key());
    }
    lastResolved.put(key.key(), decimal.doubleValue());
    return decimal;
  }

  private Object loadValue(TunableKey key) {
    TunableRow row = repository.findEffective(key.key());
    if (row != null) {
      return materializeRow(row);
    }
    BigDecimal propertyDefault = properties.getNumericDefaults().get(key.key());
    if (propertyDefault != null) {
      return propertyDefault;
    }
    String textDefault = properties.getTextDefaults().get(key.key());
    if (textDefault != null) {
      return textDefault;
    }
    throw new TunableNotFoundException(key.key());
  }

  private Object materializeRow(TunableRow row) {
    return switch (row.valueType()) {
      case "I", "M" -> {
        BigDecimal value = row.numericValue();
        if (value == null) {
          throw new TunableNotFoundException(row.key());
        }
        if (row.minValue() != null && value.compareTo(row.minValue()) < 0
            || row.maxValue() != null && value.compareTo(row.maxValue()) > 0) {
          throw new TunableOutOfRangeException(row.key(), value, row.minValue(), row.maxValue());
        }
        yield value;
      }
      case "B" -> Boolean.parseBoolean(row.valueText());
      default -> {
        if (row.valueText() == null) {
          throw new TunableNotFoundException(row.key());
        }
        yield row.valueText();
      }
    };
  }
}
