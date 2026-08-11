package com.pcis.policy.domain.repository;

import com.pcis.policy.domain.entity.PolicyEntity;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PolicyRepository extends JpaRepository<PolicyEntity, String> {

  @Query(
      """
      SELECT DISTINCT p FROM PolicyEntity p
      LEFT JOIN FETCH p.coverages c
      LEFT JOIN FETCH c.deductibles
      LEFT JOIN FETCH p.billingPlan
      LEFT JOIN FETCH p.properties
      LEFT JOIN FETCH p.vehicles
      LEFT JOIN FETCH p.endorsements
      LEFT JOIN FETCH p.history
      WHERE p.polNbr = :polNbr
      """)
  Optional<PolicyEntity> findWithDetailsByPolNbr(@Param("polNbr") String polNbr);

  @Query(
      """
      SELECT p FROM PolicyEntity p
      WHERE (:custId IS NULL OR p.custId = :custId)
        AND (:status IS NULL OR p.polStatus = :status)
      """)
  Page<PolicyEntity> findByFilters(
      @Param("custId") Integer custId, @Param("status") String status, Pageable pageable);

  @Query(value = "SELECT nextval('seq_policy_nbr')", nativeQuery = true)
  long nextPolicyNumberSequence();

  @Query(value = "SELECT nextval('seq_coverage_id')", nativeQuery = true)
  long nextCoverageIdSequence();
}
