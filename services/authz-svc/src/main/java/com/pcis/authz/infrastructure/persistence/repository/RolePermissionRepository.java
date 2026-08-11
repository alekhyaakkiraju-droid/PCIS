package com.pcis.authz.infrastructure.persistence.repository;

import com.pcis.authz.infrastructure.persistence.entity.RolePermissionEntity;
import com.pcis.authz.infrastructure.persistence.entity.RolePermissionId;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RolePermissionRepository
    extends JpaRepository<RolePermissionEntity, RolePermissionId> {

  List<RolePermissionEntity> findByRoleId(Long roleId);

  @Query(
      """
      SELECT rp FROM RolePermissionEntity rp
      JOIN FETCH rp.permission
      WHERE rp.roleId = :roleId
      """)
  List<RolePermissionEntity> findByRoleIdWithPermissions(@Param("roleId") Long roleId);
}
