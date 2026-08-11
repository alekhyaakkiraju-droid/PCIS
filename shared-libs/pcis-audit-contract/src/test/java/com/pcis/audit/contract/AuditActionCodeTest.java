package com.pcis.audit.contract;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class AuditActionCodeTest {

  @ParameterizedTest
  @CsvSource({
    "ADD, CREATE",
    "UPD, UPDATE",
    "DEL, DELETE",
    "PAY, PAY",
    "REN, RENEW",
    "A, CREATE",
    "C, UPDATE",
    "D, DELETE",
    "INSERT, CREATE",
    "UPDATE, UPDATE",
    "DELETE, DELETE",
    "INIT, INIT",
    "FINALIZE, FINALIZE"
  })
  void fromLegacyMapsBatchAndInteractiveCodes(String legacy, AuditOperation expected) {
    assertThat(AuditActionCode.fromLegacy(legacy).operation()).isEqualTo(expected);
  }

  @Test
  void fromLegacyIsCaseInsensitive() {
    assertThat(AuditActionCode.fromLegacy("add").operation()).isEqualTo(AuditOperation.CREATE);
  }

  @Test
  void fromLegacyTrimsWhitespace() {
    assertThat(AuditActionCode.fromLegacy("  PAY  ").operation()).isEqualTo(AuditOperation.PAY);
  }

  @Test
  void fromLegacyRejectsUnknownCode() {
    assertThatThrownBy(() -> AuditActionCode.fromLegacy("ZZZ"))
        .isInstanceOf(UnknownAuditActionException.class)
        .hasMessageContaining("ZZZ");
  }

  @Test
  void fromLegacyRejectsBlankCode() {
    assertThatThrownBy(() -> AuditActionCode.fromLegacy(" "))
        .isInstanceOf(UnknownAuditActionException.class);
  }
}
