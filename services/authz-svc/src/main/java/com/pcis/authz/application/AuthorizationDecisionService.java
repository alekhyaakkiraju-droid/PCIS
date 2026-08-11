package com.pcis.authz.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pcis.authz.contract.AuthorizationRequest;
import com.pcis.authz.contract.AuthorizationResponse;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Orchestrates policy evaluation, structured audit logging, and outbox enlistment. */
@Service
public class AuthorizationDecisionService {

  private static final Logger log = LoggerFactory.getLogger(AuthorizationDecisionService.class);

  private final PolicyDecisionService policyDecisionService;
  private final AuthorizationDecisionOutboxWriter outboxWriter;
  private final ObjectMapper objectMapper;

  public AuthorizationDecisionService(
      PolicyDecisionService policyDecisionService,
      AuthorizationDecisionOutboxWriter outboxWriter,
      ObjectMapper objectMapper) {
    this.policyDecisionService = policyDecisionService;
    this.outboxWriter = outboxWriter;
    this.objectMapper = objectMapper;
  }

  @Transactional
  public AuthorizationResponse decide(
      String principalId, AuthorizationRequest request, String correlationId) {
    AuthorizationResponse response =
        policyDecisionService.evaluate(principalId, request, correlationId);
    outboxWriter.write(principalId, request, response);
    logStructuredDecision(principalId, request, response);
    return response;
  }

  private void logStructuredDecision(
      String principalId, AuthorizationRequest request, AuthorizationResponse response) {
    Map<String, Object> entry = new LinkedHashMap<>();
    entry.put("actor", principalId);
    entry.put("resource", request.resource());
    entry.put("operation", request.operation());
    entry.put("decision", response.decision().name());
    entry.put("reasonCode", response.reasonCode().name());
    entry.put("correlationId", response.correlationId());
    try {
      log.info("authorization_decision {}", objectMapper.writeValueAsString(entry));
    } catch (JsonProcessingException ex) {
      log.warn("Unable to serialize authorization decision log entry", ex);
    }
  }
}
