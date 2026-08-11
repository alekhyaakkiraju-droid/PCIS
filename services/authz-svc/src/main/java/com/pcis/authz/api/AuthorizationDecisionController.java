package com.pcis.authz.api;

import com.pcis.authz.application.AuthorizationDecisionService;
import com.pcis.authz.contract.AuthorizationRequest;
import com.pcis.authz.contract.AuthorizationResponse;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/authz")
public class AuthorizationDecisionController {

  private final AuthorizationDecisionService authorizationDecisionService;

  public AuthorizationDecisionController(AuthorizationDecisionService authorizationDecisionService) {
    this.authorizationDecisionService = authorizationDecisionService;
  }

  @PostMapping("/decisions")
  public AuthorizationResponse decide(
      @Valid @RequestBody AuthorizationRequest request,
      @AuthenticationPrincipal Jwt jwt,
      @RequestHeader(value = "X-Correlation-Id", required = false) String correlationIdHeader) {
    String principalId = jwt.getSubject();
    String correlationId =
        correlationIdHeader == null || correlationIdHeader.isBlank()
            ? UUID.randomUUID().toString()
            : correlationIdHeader;
    return authorizationDecisionService.decide(principalId, request, correlationId);
  }
}
