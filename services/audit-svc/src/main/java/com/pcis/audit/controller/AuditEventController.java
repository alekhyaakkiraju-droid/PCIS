package com.pcis.audit.controller;

import com.pcis.audit.application.AuditEventService;
import com.pcis.audit.contract.AuditEventRequest;
import com.pcis.audit.contract.AuditEventResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/audit")
@Tag(name = "Audit Events", description = "Immutable audit trail ingestion (AUDLOG01 replacement)")
@SecurityRequirement(name = "bearerAuth")
public class AuditEventController {

  private final AuditEventService auditEventService;

  public AuditEventController(AuditEventService auditEventService) {
    this.auditEventService = auditEventService;
  }

  @PostMapping("/events")
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize("hasAuthority('audit:write')")
  @Operation(
      summary = "Record an audit event",
      description =
          "Accepts the unified v1 nine-field audit payload. Values are masked before persistence.")
  @ApiResponse(responseCode = "201", description = "Audit event persisted")
  @ApiResponse(responseCode = "400", description = "Validation failure (RFC 9457 problem detail)")
  @ApiResponse(responseCode = "401", description = "Unauthenticated")
  @ApiResponse(responseCode = "403", description = "Forbidden")
  public AuditEventResponse recordEvent(@RequestBody AuditEventRequest request) {
    return auditEventService.recordEvent(request);
  }
}
