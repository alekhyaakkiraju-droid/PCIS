package com.pcis.audit.contract;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.UUID;

/**
 * Unified v1 audit event request — nine persisted payload fields plus optional correlation id.
 *
 * <p>Widths follow {@code contracts/audlog01-v1-contract.yaml} unified_v1_schema superset.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record AuditEventRequest(
    @JsonProperty("action") String action,
    @JsonProperty("old_value") String oldValue,
    @JsonProperty("new_value") String newValue,
    @JsonProperty("key") String key,
    @JsonProperty("service") String service,
    @JsonProperty("program") String program,
    @JsonProperty("actor") String actor,
    @JsonProperty("resource") String resource,
    @JsonProperty("field_name") String fieldName,
    @JsonProperty("correlation_id") UUID correlationId) {}
