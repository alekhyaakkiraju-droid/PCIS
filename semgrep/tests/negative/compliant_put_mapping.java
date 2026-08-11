package com.pcis.authz.fixture.negative;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
class CompliantPutMappingController {

  // ok: require-preauthorize-on-mutations
  @PutMapping("/roles/{id}")
  @PreAuthorize("hasAuthority('authz:role:write')")
  public RoleResponse updateRole(
      @PathVariable String id, @RequestBody UpdateRoleRequest request) {
    return new RoleResponse();
  }

  static class UpdateRoleRequest {}
  static class RoleResponse {}
}
