package com.pcis.outbox;

public enum OutboxEventStatus {
  PENDING,
  PUBLISHED,
  DEAD_LETTER
}
