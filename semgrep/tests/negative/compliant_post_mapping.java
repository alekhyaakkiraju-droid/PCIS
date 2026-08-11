package com.pcis.authz.fixture.negative;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
class CompliantPostMappingController {

  // ok: require-preauthorize-on-mutations
  @PostMapping("/events")
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize("hasAuthority('audit:write')")
  public AuditEventResponse recordEvent(@RequestBody AuditEventRequest request) {
    return new AuditEventResponse();
  }

  static class AuditEventRequest {}
  static class AuditEventResponse {}
}
