package com.pcis.authz.fixture.positive;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
class UnannotatedDeleteMappingController {

  // ruleid: require-preauthorize-on-mutations
  @DeleteMapping("/user-roles/{userId}/{roleId}")
  public void revokeRole(@PathVariable String userId, @PathVariable String roleId) {}
}
