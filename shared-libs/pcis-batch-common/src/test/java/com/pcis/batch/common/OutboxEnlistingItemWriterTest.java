package com.pcis.batch.common;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.jdbc.core.JdbcTemplate;

class OutboxEnlistingItemWriterTest {

  @Test
  void write_enlistsOutboxAfterDelegateSucceeds() throws Exception {
    @SuppressWarnings("unchecked")
    ItemWriter<String> delegate = mock(ItemWriter.class);
    JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
    OutboxEventWriter outboxEventWriter = new OutboxEventWriter(jdbcTemplate, new ObjectMapper(), "TEST");
    OutboxEnlistingItemWriter<String> writer =
        new OutboxEnlistingItemWriter<>(
            delegate,
            outboxEventWriter,
            item ->
                new OutboxEventSpec(
                    "demo", item, "Created", Map.of("value", item), UUID.randomUUID()));

    writer.write(new Chunk<>(List.of("alpha")));

    verify(delegate).write(any());
    verify(jdbcTemplate).update(any(String.class), any(), any(), any(), any(), any(), any(), any(), any());
  }

  @Test
  void write_rollsBackWhenDelegateFails() {
    @SuppressWarnings("unchecked")
    ItemWriter<String> delegate = mock(ItemWriter.class);
    JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
    OutboxEventWriter outboxEventWriter = new OutboxEventWriter(jdbcTemplate, new ObjectMapper(), "TEST");
    OutboxEnlistingItemWriter<String> writer =
        new OutboxEnlistingItemWriter<>(
            delegate,
            outboxEventWriter,
            item -> new OutboxEventSpec("demo", item, "Created", Map.of(), UUID.randomUUID()));

    assertThatThrownBy(() -> {
          doThrow(new IllegalStateException("delegate failed")).when(delegate).write(any());
          writer.write(new Chunk<>(List.of("alpha")));
        })
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("delegate failed");
  }

  @Test
  void write_rollsBackWhenOutboxInsertFails() throws Exception {
    @SuppressWarnings("unchecked")
    ItemWriter<String> delegate = mock(ItemWriter.class);
    JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
    doThrow(new RuntimeException("outbox insert failed"))
        .when(jdbcTemplate)
        .update(any(String.class), any(), any(), any(), any(), any(), any(), any(), any());
    OutboxEventWriter outboxEventWriter = new OutboxEventWriter(jdbcTemplate, new ObjectMapper(), "TEST");
    OutboxEnlistingItemWriter<String> writer =
        new OutboxEnlistingItemWriter<>(
            delegate,
            outboxEventWriter,
            item -> new OutboxEventSpec("demo", item, "Created", Map.of(), UUID.randomUUID()));

    assertThatThrownBy(() -> writer.write(new Chunk<>(List.of("alpha"))))
        .isInstanceOf(RuntimeException.class)
        .hasMessageContaining("outbox insert failed");
  }
}
