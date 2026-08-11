package com.pcis.authz.fixture.negative;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
class ReadOnlyGetMappingController {

  // ok: require-preauthorize-on-mutations
  @GetMapping("/roles/{id}")
  @PreAuthorize("hasAuthority('authz:role:read')")
  public RoleResponse getRole(@PathVariable String id) {
    return new RoleResponse();
  }

  // ok: require-preauthorize-on-mutations
  @GetMapping("/roles")
  public RoleListResponse listRoles() {
    return new RoleListResponse();
  }

  static class RoleResponse {}
  static class RoleListResponse {}
}
