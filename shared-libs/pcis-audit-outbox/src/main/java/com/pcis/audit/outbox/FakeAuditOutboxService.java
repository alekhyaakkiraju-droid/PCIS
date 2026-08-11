package com.pcis.audit.outbox;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * In-memory {@link AuditOutboxService} for unit and integration tests.
 *
 * <p>Does not enforce mandatory transactions — use only when the database outbox is not under test.
 */
public class FakeAuditOutboxService implements AuditOutboxService {

  private final List<AuditEvent> writtenEvents = new CopyOnWriteArrayList<>();
  private volatile boolean failOnNextWrite;

  @Override
  public void write(AuditEvent event) {
    if (failOnNextWrite) {
      failOnNextWrite = false;
      throw new IllegalStateException("simulated audit outbox write failure");
    }
    writtenEvents.add(event);
  }

  public List<AuditEvent> getWrittenEvents() {
    return Collections.unmodifiableList(new ArrayList<>(writtenEvents));
  }

  public void clear() {
    writtenEvents.clear();
  }

  public void setFailOnNextWrite(boolean failOnNextWrite) {
    this.failOnNextWrite = failOnNextWrite;
  }
}
