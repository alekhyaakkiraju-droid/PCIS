package com.pcis.authz.fixture.positive;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
class UnannotatedPutMappingController {

  // ruleid: require-preauthorize-on-mutations
  @PutMapping("/permissions/{id}")
  public PermissionResponse updatePermission(
      @PathVariable String id, @RequestBody UpdatePermissionRequest request) {
    return new PermissionResponse();
  }

  static class UpdatePermissionRequest {}
  static class PermissionResponse {}
}
