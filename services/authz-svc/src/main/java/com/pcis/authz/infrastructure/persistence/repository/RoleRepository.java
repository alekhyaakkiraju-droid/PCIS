package com.pcis.authz.infrastructure.persistence.repository;

import com.pcis.authz.infrastructure.persistence.entity.RoleEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoleRepository extends JpaRepository<RoleEntity, Long> {

  Optional<RoleEntity> findByRoleCode(String roleCode);
}
