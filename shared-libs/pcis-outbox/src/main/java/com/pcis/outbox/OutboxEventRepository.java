package com.pcis.outbox;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, Long> {

  /**
   * Returns unpublished events ordered by creation time (maps {@code STATUS <> 'PUBLISHED'}).
   */
  @Query(
      """
      SELECT e FROM OutboxEvent e
      WHERE e.status <> com.pcis.outbox.OutboxEventStatus.PUBLISHED
      ORDER BY e.createdAt ASC
      """)
  List<OutboxEvent> findByPublishedFalseOrderByCreatedAtAsc(Pageable pageable);

  /**
   * Claims a batch of pending events for relay using PostgreSQL {@code FOR UPDATE SKIP LOCKED}.
   */
  @Query(
      value =
          """
          SELECT *
          FROM outbox_events
          WHERE STATUS = 'PENDING'
            AND (NEXT_ATTEMPT_AT IS NULL OR NEXT_ATTEMPT_AT <= CURRENT_TIMESTAMP)
          ORDER BY CRT_TIMESTAMP ASC
          LIMIT :limit
          FOR UPDATE SKIP LOCKED
          """,
      nativeQuery = true)
  List<OutboxEvent> findPendingForRelaySkipLocked(@Param("limit") int limit);

  Optional<OutboxEvent> findByIdempotencyKey(UUID idempotencyKey);
}
