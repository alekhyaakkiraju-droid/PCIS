package com.pcis.config.entity;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.Column;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Set;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class ConfigEntityValidationTest {

  private static Validator validator;

  @BeforeAll
  static void createValidator() {
    validator = Validation.buildDefaultValidatorFactory().getValidator();
  }

  @Test
  void configTunableEntityMapsDecimalColumnsWithElevenTwoPrecision() throws Exception {
    var column =
        ConfigTunableEntity.class.getDeclaredField("numericValue").getAnnotation(Column.class);
    assertThat(column.precision()).isEqualTo(11);
    assertThat(column.scale()).isEqualTo(2);
  }

  @Test
  void configTunableEntityVersionFieldIsOptimisticLock() throws Exception {
    assertThat(
            ConfigTunableEntity.class.getDeclaredField("versionNo").getAnnotation(
                jakarta.persistence.Version.class))
        .isNotNull();
  }

  @Test
  void validConfigTunableEntityPassesValidation() {
    ConfigTunableEntity entity = sampleTunable();
    Set<ConstraintViolation<ConfigTunableEntity>> violations = validator.validate(entity);
    assertThat(violations).isEmpty();
  }

  @Test
  void configTunableEntityRejectsBlankKey() {
    ConfigTunableEntity entity = sampleTunable();
    entity.setTunableKey("");
    assertThat(validator.validate(entity)).isNotEmpty();
  }

  @Test
  void configTunableHistoryEntityRequiresChangeEvidence() {
    ConfigTunableHistoryEntity entity = new ConfigTunableHistoryEntity();
    entity.setTunableKey("billing.leadDays");
    entity.setVersionNo(1);
    entity.setNewValue("20");
    entity.setChangedTimestamp(Instant.parse("2026-08-11T12:00:00Z"));

    Set<ConstraintViolation<ConfigTunableHistoryEntity>> violations = validator.validate(entity);
    assertThat(violations)
        .extracting(ConstraintViolation::getPropertyPath)
        .anyMatch(path -> path.toString().equals("changeReason"))
        .anyMatch(path -> path.toString().equals("changedBy"));
  }

  @Test
  void configRuleSetEntityRequiresPayloadAndDescription() {
    ConfigRuleSetEntity entity = new ConfigRuleSetEntity();
    entity.setRuleSetKey("billing-frequency-interval");
    entity.setVersionNo(1);
    entity.setEffectiveFrom(LocalDate.parse("2026-01-01"));
    entity.setStatusCd("A");

    assertThat(validator.validate(entity)).isNotEmpty();
  }

  @Test
  void configRuleSetEntityIdEqualityUsesCompositeKey() {
    var left = new ConfigRuleSetEntityId("billing-frequency-interval", 1);
    var right = new ConfigRuleSetEntityId("billing-frequency-interval", 1);
    var different = new ConfigRuleSetEntityId("billing-frequency-interval", 2);

    assertThat(left).isEqualTo(right).hasSameHashCodeAs(right).isNotEqualTo(different);
  }

  private static ConfigTunableEntity sampleTunable() {
    ConfigTunableEntity entity = new ConfigTunableEntity();
    entity.setTunableKey("billing.leadDays");
    entity.setDomainCd("BIL");
    entity.setValueType("I");
    entity.setNumericValue(new BigDecimal("15.00"));
    entity.setMinValue(new BigDecimal("1.00"));
    entity.setMaxValue(new BigDecimal("90.00"));
    entity.setUnitCd("days");
    entity.setDescription("Billing lead days");
    entity.setEffectiveFrom(LocalDate.parse("2026-01-01"));
    entity.setVersionNo(1);
    return entity;
  }
}
