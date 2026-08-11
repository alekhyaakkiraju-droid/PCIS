package com.pcis.audit.contract;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class AuditEventValidatorTest {

  @Test
  void acceptsWidestLegacyFieldWidths() {
    String hundredCharValue = "X".repeat(100);
    String fortyCharKey = "K".repeat(40);

    ValidatedAuditEvent validated =
        AuditEventValidator.validate(
            new AuditEventRequest(
                "UPD",
                hundredCharValue,
                hundredCharValue,
                fortyCharKey,
                "customer-svc",
                "CUS001A",
                "user001",
                "CUSTOMER_T",
                "TAX_ID",
                UUID.fromString("550e8400-e29b-41d4-a716-446655440000")));

    assertThat(validated.oldValue()).hasSize(100);
    assertThat(validated.newValue()).hasSize(100);
    assertThat(validated.key()).hasSize(40);
    assertThat(validated.operation()).isEqualTo(AuditOperation.UPDATE);
  }

  @Test
  void rejectsOldValueExceedingMaxWidth() {
    assertThatThrownBy(
            () ->
                AuditEventValidator.validate(
                    new AuditEventRequest(
                        "A",
                        "X".repeat(101),
                        null,
                        "key",
                        "policy-svc",
                        "POL001A",
                        "user001",
                        "POLICY_T",
                        "STATUS",
                        null)))
        .isInstanceOf(AuditValidationException.class)
        .satisfies(
            ex ->
                assertThat(((AuditValidationException) ex).violations())
                    .anyMatch(v -> v.contains("old_value exceeds maximum length")));
  }

  @Test
  void rejectsUnknownActionWith400StyleViolations() {
    assertThatThrownBy(
            () ->
                AuditEventValidator.validate(
                    new AuditEventRequest(
                        "UNKNOWN",
                        null,
                        null,
                        "key",
                        "billing-svc",
                        "BIL003B",
                        "batch",
                        "BILLING_SCHEDULE_T",
                        null,
                        null)))
        .isInstanceOf(AuditValidationException.class)
        .satisfies(
            ex ->
                assertThat(((AuditValidationException) ex).violations())
                    .anyMatch(v -> v.contains("unknown action code")));
  }

  @Test
  void generatesCorrelationIdWhenAbsent() {
    ValidatedAuditEvent validated =
        AuditEventValidator.validate(
            new AuditEventRequest(
                "PAY", null, null, "CLM-001", "claims-svc", "CLM006B", "batchclm", "CLAIM_T", null, null));

    assertThat(validated.correlationId()).isNotNull();
  }

  @Test
  void rejectsMissingRequiredFields() {
    assertThatThrownBy(
            () ->
                AuditEventValidator.validate(
                    new AuditEventRequest(null, null, null, null, null, null, null, null, null, null)))
        .isInstanceOf(AuditValidationException.class)
        .satisfies(
            ex -> assertThat(((AuditValidationException) ex).violations()).hasSizeGreaterThan(3));
  }
}
