package com.pcis.authz.fixture.positive;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
class UnannotatedPostMappingController {

  // ruleid: require-preauthorize-on-mutations
  @PostMapping("/roles")
  public RoleResponse createRole(@RequestBody CreateRoleRequest request) {
    return new RoleResponse();
  }

  static class CreateRoleRequest {}
  static class RoleResponse {}
}
