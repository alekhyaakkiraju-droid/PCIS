package com.pcis.authz.application;

import com.pcis.authz.infrastructure.persistence.entity.RoleEntity;
import com.pcis.authz.infrastructure.persistence.repository.RolePermissionRepository;
import com.pcis.authz.infrastructure.persistence.repository.RoleRepository;
import com.pcis.authz.infrastructure.persistence.repository.UserRoleRepository;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Resolves effective permission codes for a principal from role assignments. */
@Service
public class PermissionResolver {

  private final UserRoleRepository userRoleRepository;
  private final RoleRepository roleRepository;
  private final RolePermissionRepository rolePermissionRepository;

  public PermissionResolver(
      UserRoleRepository userRoleRepository,
      RoleRepository roleRepository,
      RolePermissionRepository rolePermissionRepository) {
    this.userRoleRepository = userRoleRepository;
    this.roleRepository = roleRepository;
    this.rolePermissionRepository = rolePermissionRepository;
  }

  /** Role codes assigned to the principal (active roles only). */
  @Transactional(readOnly = true)
  public List<String> resolveRoleCodes(String principalId) {
    Set<String> roleCodes = new LinkedHashSet<>();
    for (var assignment : userRoleRepository.findByPrincipalId(principalId)) {
      roleRepository
          .findById(assignment.getRoleId())
          .filter(RoleEntity::isActive)
          .ifPresent(role -> roleCodes.add(role.getRoleCode()));
    }
    return List.copyOf(roleCodes);
  }

  @Transactional(readOnly = true)
  public List<String> resolvePermissionCodes(String principalId) {
    Set<String> permissionCodes = new LinkedHashSet<>();
    var roleAssignments = userRoleRepository.findByPrincipalId(principalId);

    for (var assignment : roleAssignments) {
      roleRepository
          .findById(assignment.getRoleId())
          .filter(role -> role.isActive())
          .ifPresent(
              role ->
                  rolePermissionRepository
                      .findByRoleIdWithPermissions(role.getRoleId())
                      .forEach(
                          mapping ->
                              permissionCodes.add(
                                  mapping.getPermission().getPermissionCode())));
    }

    return List.copyOf(permissionCodes);
  }
}
