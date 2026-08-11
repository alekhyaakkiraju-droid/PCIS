package com.pcis.policy.domain.repository;

import com.pcis.policy.domain.entity.PolicyEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PolicyRepository extends JpaRepository<PolicyEntity, String> {

  @Query(
      """
      SELECT DISTINCT p FROM PolicyEntity p
      LEFT JOIN FETCH p.coverages
      LEFT JOIN FETCH p.properties
      LEFT JOIN FETCH p.vehicles
      LEFT JOIN FETCH p.endorsements
      LEFT JOIN FETCH p.history
      WHERE p.polNbr = :polNbr
      """)
  Optional<PolicyEntity> findWithDetailsByPolNbr(@Param("polNbr") String polNbr);
}
