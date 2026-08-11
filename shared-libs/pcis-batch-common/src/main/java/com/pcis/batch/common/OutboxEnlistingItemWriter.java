package com.pcis.batch.common;

import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.transaction.annotation.Transactional;

/**
 * Decorates a delegate {@link ItemWriter} and inserts matching outbox rows in the same transaction.
 */
public class OutboxEnlistingItemWriter<T> implements ItemWriter<T> {

  private final ItemWriter<T> delegate;
  private final OutboxEventWriter outboxEventWriter;
  private final OutboxEventMapper<T> mapper;

  public OutboxEnlistingItemWriter(
      ItemWriter<T> delegate, OutboxEventWriter outboxEventWriter, OutboxEventMapper<T> mapper) {
    this.delegate = delegate;
    this.outboxEventWriter = outboxEventWriter;
    this.mapper = mapper;
  }

  @Override
  @Transactional
  public void write(Chunk<? extends T> chunk) throws Exception {
    delegate.write(chunk);
    for (T item : chunk.getItems()) {
      OutboxEventSpec spec = mapper.map(item);
      outboxEventWriter.write(
          spec.aggregateType(),
          spec.aggregateId(),
          spec.eventType(),
          spec.payload(),
          spec.idempotencyKey());
    }
  }
}
