package com.pcis.authz.contract;

import com.pcis.authz.domain.decision.AuthorizationDecision;
import com.pcis.authz.domain.decision.ReasonCode;
import java.util.List;

/** Outbound policy decision response. */
public record AuthorizationResponse(
    AuthorizationDecision decision,
    ReasonCode reasonCode,
    List<String> evaluatedPermissions,
    String correlationId) {}
