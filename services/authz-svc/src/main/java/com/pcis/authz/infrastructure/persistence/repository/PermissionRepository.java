package com.pcis.authz.infrastructure.persistence.repository;

import com.pcis.authz.infrastructure.persistence.entity.PermissionEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PermissionRepository extends JpaRepository<PermissionEntity, Long> {

  Optional<PermissionEntity> findByPermissionCode(String permissionCode);
}
