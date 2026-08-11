package com.pcis.authz.infrastructure.persistence.repository;

import com.pcis.authz.infrastructure.persistence.entity.RolePermissionEntity;
import com.pcis.authz.infrastructure.persistence.entity.RolePermissionId;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RolePermissionRepository
    extends JpaRepository<RolePermissionEntity, RolePermissionId> {

  List<RolePermissionEntity> findByRoleId(Long roleId);
}
