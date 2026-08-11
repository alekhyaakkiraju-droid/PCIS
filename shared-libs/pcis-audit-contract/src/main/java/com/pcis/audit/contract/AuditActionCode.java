package com.pcis.audit.contract;

import java.util.Locale;
import java.util.Map;

/**
 * Explicit audit action domain covering batch 3-character codes and interactive 1-character codes.
 *
 * <p>Mapping follows {@code contracts/audlog01-v1-contract.yaml} action_code_rules.
 */
public enum AuditActionCode {
  ADD(AuditOperation.CREATE),
  UPD(AuditOperation.UPDATE),
  DEL(AuditOperation.DELETE),
  PAY(AuditOperation.PAY),
  REN(AuditOperation.RENEW),
  A(AuditOperation.CREATE),
  C(AuditOperation.UPDATE),
  D(AuditOperation.DELETE),
  INSERT(AuditOperation.CREATE),
  UPDATE(AuditOperation.UPDATE),
  DELETE(AuditOperation.DELETE),
  BILL(AuditOperation.BILL),
  INIT(AuditOperation.INIT),
  FINALIZE(AuditOperation.FINALIZE),
  RENEW(AuditOperation.RENEW),
  U(AuditOperation.UPDATE);

  private static final Map<String, AuditActionCode> LEGACY_LOOKUP =
      Map.ofEntries(
          Map.entry("ADD", ADD),
          Map.entry("UPD", UPD),
          Map.entry("DEL", DEL),
          Map.entry("PAY", PAY),
          Map.entry("REN", REN),
          Map.entry("A", A),
          Map.entry("C", C),
          Map.entry("D", D),
          Map.entry("INSERT", INSERT),
          Map.entry("UPDATE", UPDATE),
          Map.entry("DELETE", DELETE),
          Map.entry("BILL", BILL),
          Map.entry("INIT", INIT),
          Map.entry("FINALIZE", FINALIZE),
          Map.entry("RENEW", RENEW),
          Map.entry("U", U));

  private final AuditOperation operation;

  AuditActionCode(AuditOperation operation) {
    this.operation = operation;
  }

  public AuditOperation operation() {
    return operation;
  }

  /**
   * Resolves a legacy action code from batch (ADD, UPD, DEL, PAY, REN) or interactive (A, C, D)
   * callers, plus observed COBOL spellings (INSERT, UPDATE, …).
   *
   * @throws UnknownAuditActionException when the code is absent from the mapping table
   */
  public static AuditActionCode fromLegacy(String legacyAction) {
    if (legacyAction == null || legacyAction.isBlank()) {
      throw new UnknownAuditActionException(legacyAction);
    }
    String normalized = legacyAction.trim().toUpperCase(Locale.ROOT);
    AuditActionCode code = LEGACY_LOOKUP.get(normalized);
    if (code == null) {
      throw new UnknownAuditActionException(legacyAction);
    }
    return code;
  }
}
