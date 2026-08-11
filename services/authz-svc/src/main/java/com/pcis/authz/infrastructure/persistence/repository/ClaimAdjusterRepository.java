package com.pcis.authz.infrastructure.persistence.repository;

import com.pcis.authz.infrastructure.persistence.entity.ClaimAdjusterEntity;
import com.pcis.authz.infrastructure.persistence.projection.AdjusterAuthorityProjection;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ClaimAdjusterRepository extends JpaRepository<ClaimAdjusterEntity, String> {

  @Query(
      """
      select a.adjusterId as adjusterId, a.authorityLimit as authorityLimit
      from ClaimAdjusterEntity a
      where a.adjusterId = :adjusterId
      """)
  Optional<AdjusterAuthorityProjection> findAuthorityByAdjusterId(
      @Param("adjusterId") String adjusterId);
}
