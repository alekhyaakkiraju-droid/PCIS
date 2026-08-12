package com.pcis.configsvc.application;

import com.pcis.config.TunableOutOfRangeException;
import com.pcis.config.TunableResolver;
import com.pcis.config.entity.ConfigTunableEntity;
import com.pcis.config.entity.ConfigTunableHistoryEntity;
import com.pcis.config.repository.ConfigTunableHistoryRepository;
import com.pcis.config.repository.ConfigTunableRepository;
import com.pcis.configsvc.api.dto.TunableHistoryResponse;
import com.pcis.configsvc.api.dto.TunableResponse;
import com.pcis.configsvc.api.dto.UpdateTunableRequest;
import com.pcis.configsvc.outbox.ConfigOutboxWriter;
import com.pcis.error.ConflictException;
import com.pcis.error.ResourceNotFoundException;
import com.pcis.error.ValidationException;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminTunableService {

  private final ConfigTunableRepository tunableRepository;
  private final ConfigTunableHistoryRepository historyRepository;
  private final ConfigOutboxWriter outboxWriter;
  private final TunableResolver tunableResolver;

  public AdminTunableService(
      ConfigTunableRepository tunableRepository,
      ConfigTunableHistoryRepository historyRepository,
      ConfigOutboxWriter outboxWriter,
      TunableResolver tunableResolver) {
    this.tunableRepository = tunableRepository;
    this.historyRepository = historyRepository;
    this.outboxWriter = outboxWriter;
    this.tunableResolver = tunableResolver;
  }

  public Page<TunableResponse> listCurrent(Pageable pageable) {
    Map<String, ConfigTunableEntity> effective = new LinkedHashMap<>();
    LocalDate today = LocalDate.now();
    for (ConfigTunableEntity row : tunableRepository.findAll()) {
      if (!isEffectiveOn(row, today)) {
        continue;
      }
      effective.merge(
          row.getTunableKey(),
          row,
          (left, right) -> left.getVersionNo() >= right.getVersionNo() ? left : right);
    }
    List<TunableResponse> sorted =
        effective.values().stream()
            .map(this::toResponse)
            .sorted(Comparator.comparing(TunableResponse::key))
            .toList();
    int start = (int) pageable.getOffset();
    if (start >= sorted.size()) {
      return new PageImpl<>(List.of(), pageable, sorted.size());
    }
    int end = Math.min(start + pageable.getPageSize(), sorted.size());
    return new PageImpl<>(sorted.subList(start, end), pageable, sorted.size());
  }

  public List<TunableHistoryResponse> history(String tunableKey) {
    ensureExists(tunableKey);
    return historyRepository.findByTunableKeyOrderByChangedTimestampDesc(tunableKey).stream()
        .map(this::toHistoryResponse)
        .toList();
  }

  @Transactional
  public TunableResponse update(String tunableKey, UpdateTunableRequest request) {
    ConfigTunableEntity current =
        tunableRepository
            .findByTunableKeyAndVersionNo(tunableKey, request.expectedVersion())
            .orElseThrow(
                () ->
                    new ResourceNotFoundException(
                        "Tunable not found for key and version",
                        actor(),
                        "config/tunable/" + tunableKey,
                        "update"));

    if (!current.getVersionNo().equals(request.expectedVersion())) {
      throw new ConflictException(
          "Expected version does not match current tunable version",
          actor(),
          "config/tunable/" + tunableKey,
          "update");
    }

    validateValue(current, request);

    String oldValue = formatValue(current);
    closeEffectiveRow(current, request.effectiveFrom());

    int newVersion = current.getVersionNo() + 1;
    ConfigTunableEntity next = copyWithNewVersion(current, request, newVersion);
    tunableRepository.save(next);

    ConfigTunableHistoryEntity history = new ConfigTunableHistoryEntity();
    history.setTunableKey(tunableKey);
    history.setVersionNo(newVersion);
    history.setOldValue(oldValue);
    history.setNewValue(formatValue(next));
    history.setChangeReason(request.changeReason());
    history.setChangedBy(actor());
    history.setChangedTimestamp(Instant.now());
    historyRepository.save(history);

    outboxWriter.writeTunableChangedEvent(
        tunableKey,
        Map.of(
            "tunableKey", tunableKey,
            "oldValue", oldValue,
            "newValue", formatValue(next),
            "version", newVersion,
            "changeReason", request.changeReason(),
            "changedBy", actor()),
        UUID.randomUUID());

    tunableResolver.refresh(tunableKey);
    return toResponse(next);
  }

  private void validateValue(ConfigTunableEntity current, UpdateTunableRequest request) {
    String type = current.getValueType();
    if ("S".equals(type) || "B".equals(type)) {
      if (request.numericValue() != null) {
        throw new ValidationException(
            "String tunables require valueText, not numericValue",
            actor(),
            "config/tunable/" + current.getTunableKey(),
            "validate-type");
      }
      return;
    }
    if (request.numericValue() == null) {
      throw new ValidationException(
          "Numeric tunable value is required",
          actor(),
          "config/tunable/" + current.getTunableKey(),
          "validate-type");
    }
    BigDecimal min = current.getMinValue();
    BigDecimal max = current.getMaxValue();
    if (min != null && request.numericValue().compareTo(min) < 0
        || max != null && request.numericValue().compareTo(max) > 0) {
      throw new TunableOutOfRangeException(
          current.getTunableKey(), request.numericValue(), min, max);
    }
  }

  private void closeEffectiveRow(ConfigTunableEntity current, LocalDate newEffectiveFrom) {
    if (current.getEffectiveTo() != null && !current.getEffectiveTo().isAfter(newEffectiveFrom)) {
      return;
    }
    if (newEffectiveFrom.isAfter(current.getEffectiveFrom())) {
      current.setEffectiveTo(newEffectiveFrom.minusDays(1));
    }
    current.setUpdUser(actor());
    current.setUpdTimestamp(Instant.now());
    tunableRepository.saveAndFlush(current);
  }

  private static ConfigTunableEntity copyWithNewVersion(
      ConfigTunableEntity current, UpdateTunableRequest request, int newVersion) {
    ConfigTunableEntity next = new ConfigTunableEntity();
    next.setTunableKey(current.getTunableKey());
    next.setDomainCd(current.getDomainCd());
    next.setValueType(current.getValueType());
    next.setMinValue(current.getMinValue());
    next.setMaxValue(current.getMaxValue());
    next.setUnitCd(current.getUnitCd());
    next.setDescription(current.getDescription());
    next.setEffectiveFrom(request.effectiveFrom());
    next.setEffectiveTo(null);
    next.setVersionNo(newVersion);
    next.setCrtUser(actor());
    next.setCrtTimestamp(Instant.now());
    if ("S".equals(current.getValueType()) || "B".equals(current.getValueType())) {
      next.setValueText(current.getValueText());
    } else {
      next.setNumericValue(request.numericValue());
    }
    return next;
  }

  private void ensureExists(String tunableKey) {
    if (tunableRepository.findByTunableKeyOrderByVersionNoDesc(tunableKey).isEmpty()) {
      throw new ResourceNotFoundException(
          "Tunable not found", actor(), "config/tunable/" + tunableKey, "read");
    }
  }

  private static boolean isEffectiveOn(ConfigTunableEntity row, LocalDate date) {
    if (row.getEffectiveFrom().isAfter(date)) {
      return false;
    }
    return row.getEffectiveTo() == null || !row.getEffectiveTo().isBefore(date);
  }

  private static String formatValue(ConfigTunableEntity entity) {
    if (entity.getNumericValue() != null) {
      return entity.getNumericValue().toPlainString();
    }
    return entity.getValueText();
  }

  private TunableResponse toResponse(ConfigTunableEntity entity) {
    return new TunableResponse(
        entity.getTunableKey(),
        entity.getDomainCd(),
        entity.getValueType(),
        entity.getValueText(),
        entity.getNumericValue(),
        entity.getMinValue(),
        entity.getMaxValue(),
        entity.getUnitCd(),
        entity.getDescription(),
        entity.getEffectiveFrom(),
        entity.getEffectiveTo(),
        entity.getVersionNo());
  }

  private TunableHistoryResponse toHistoryResponse(ConfigTunableHistoryEntity entity) {
    return new TunableHistoryResponse(
        entity.getVersionNo(),
        entity.getChangedBy(),
        entity.getOldValue(),
        entity.getNewValue(),
        entity.getChangeReason(),
        entity.getChangedTimestamp());
  }

  private static String actor() {
    var auth = SecurityContextHolder.getContext().getAuthentication();
    return auth != null && auth.getName() != null ? auth.getName() : "SYSTEM";
  }
}
