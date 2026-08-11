package com.pcis.audit.outbox;

/** Masks restricted-tier values before audit payload persistence. */
public interface AuditPayloadMasker {

  String mask(String value);
}
