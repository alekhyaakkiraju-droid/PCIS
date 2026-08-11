package com.pcis.authz.fixture.positive;

import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
class UnannotatedPatchMappingController {

  // ruleid: require-preauthorize-on-mutations
  @PatchMapping("/roles/{id}/permissions")
  public RoleResponse patchRolePermissions(
      @PathVariable String id, @RequestBody PatchPermissionsRequest request) {
    return new RoleResponse();
  }

  static class PatchPermissionsRequest {}
  static class RoleResponse {}
}
