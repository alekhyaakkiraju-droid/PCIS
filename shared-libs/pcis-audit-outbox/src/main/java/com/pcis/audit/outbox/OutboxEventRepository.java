package com.pcis.audit.outbox;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, Long> {

  Optional<OutboxEvent> findByIdempotencyKey(UUID idempotencyKey);

  @Query(
      """
      SELECT e FROM OutboxEvent e
      WHERE e.status = com.pcis.audit.outbox.OutboxStatus.PENDING
      ORDER BY e.createdAt ASC
      """)
  List<OutboxEvent> findPendingOrderByCreatedAtAsc(Pageable pageable);

  @Query(
      value =
          """
          SELECT *
          FROM audit_outbox
          WHERE STATUS = 'PENDING'
            AND (NEXT_ATTEMPT_AT IS NULL OR NEXT_ATTEMPT_AT <= CURRENT_TIMESTAMP)
          ORDER BY CREATED_AT ASC
          LIMIT :limit
          FOR UPDATE SKIP LOCKED
          """,
      nativeQuery = true)
  List<OutboxEvent> findPendingForRelaySkipLocked(@Param("limit") int limit);
}
