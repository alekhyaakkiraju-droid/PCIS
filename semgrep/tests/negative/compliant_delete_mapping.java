package com.pcis.authz.fixture.negative;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
class CompliantDeleteMappingController {

  // ok: require-preauthorize-on-mutations
  @DeleteMapping("/permissions/{id}")
  @PreAuthorize("hasAuthority('authz:permission:delete')")
  public void deletePermission(@PathVariable String id) {}
}
