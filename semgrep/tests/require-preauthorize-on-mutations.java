// Consolidated Semgrep test target for require-preauthorize-on-mutations.
// Individual fixtures also live under semgrep/tests/positive/ and negative/.

package com.pcis.authz.fixture;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
class PositiveFixtures {

  // ruleid: require-preauthorize-on-mutations
  @PostMapping("/roles")
  public RoleResponse createRole(@RequestBody CreateRoleRequest request) {
    return new RoleResponse();
  }

  // ruleid: require-preauthorize-on-mutations
  @PutMapping("/permissions/{id}")
  public PermissionResponse updatePermission(
      @PathVariable String id, @RequestBody UpdatePermissionRequest request) {
    return new PermissionResponse();
  }

  // ruleid: require-preauthorize-on-mutations
  @DeleteMapping("/user-roles/{userId}/{roleId}")
  public void revokeRole(@PathVariable String userId, @PathVariable String roleId) {}

  // ruleid: require-preauthorize-on-mutations
  @PatchMapping("/roles/{id}/permissions")
  public RoleResponse patchRolePermissions(
      @PathVariable String id, @RequestBody PatchPermissionsRequest request) {
    return new RoleResponse();
  }

  static class CreateRoleRequest {}
  static class UpdatePermissionRequest {}
  static class PatchPermissionsRequest {}
  static class RoleResponse {}
  static class PermissionResponse {}
}

@RestController
class NegativeFixtures {

  // ok: require-preauthorize-on-mutations
  @PostMapping("/events")
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize("hasAuthority('audit:write')")
  public AuditEventResponse recordEvent(@RequestBody AuditEventRequest request) {
    return new AuditEventResponse();
  }

  // ok: require-preauthorize-on-mutations
  @PutMapping("/roles/{id}")
  @PreAuthorize("hasAuthority('authz:role:write')")
  public RoleResponse updateRole(
      @PathVariable String id, @RequestBody UpdateRoleRequest request) {
    return new RoleResponse();
  }

  // ok: require-preauthorize-on-mutations
  @DeleteMapping("/permissions/{id}")
  @PreAuthorize("hasAuthority('authz:permission:delete')")
  public void deletePermission(@PathVariable String id) {}

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

  static class AuditEventRequest {}
  static class AuditEventResponse {}
  static class UpdateRoleRequest {}
  static class RoleResponse {}
  static class RoleListResponse {}
}
