package com.pcis.audit.outbox;

public enum OutboxStatus {
  PENDING,
  PUBLISHED,
  DEAD_LETTER
}
