package com.pcis.batch.common;

import java.util.Map;
import java.util.UUID;

public record OutboxEventSpec(
    String aggregateType, String aggregateId, String eventType, Map<String, Object> payload, UUID idempotencyKey) {}
