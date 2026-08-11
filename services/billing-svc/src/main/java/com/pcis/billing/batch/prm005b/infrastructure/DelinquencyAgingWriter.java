package com.pcis.billing.batch.prm005b.infrastructure;

import com.pcis.batch.common.BatchJobExecutionListener;
import com.pcis.batch.common.OutboxEventWriter;
import com.pcis.billing.batch.bil003b.exception.AuditFailureException;
import com.pcis.billing.batch.prm005b.config.DelinquencyAgingProperties;
import com.pcis.billing.batch.prm005b.domain.DelinquencyUpdate;
import com.pcis.billing.domain.BillingSchedule;
import com.pcis.billing.domain.repository.BillingScheduleRepository;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.transaction.annotation.Transactional;

public class DelinquencyAgingWriter implements ItemWriter<DelinquencyUpdate> {

  public static final String TRANSITIONED_TO_P_KEY = "delinquencyTransitionedToP";
  public static final String TRANSITIONED_TO_L_KEY = "delinquencyTransitionedToL";

  private final BillingScheduleRepository billingScheduleRepository;
  private final OutboxEventWriter outboxEventWriter;
  private final DelinquencyAgingProperties properties;
  private StepExecution stepExecution;

  public DelinquencyAgingWriter(
      BillingScheduleRepository billingScheduleRepository,
      OutboxEventWriter delinquencyAgingOutboxEventWriter,
      DelinquencyAgingProperties properties) {
    this.billingScheduleRepository = billingScheduleRepository;
    this.outboxEventWriter = delinquencyAgingOutboxEventWriter;
    this.properties = properties;
  }

  @BeforeStep
  public void beforeStep(StepExecution stepExecution) {
    this.stepExecution = stepExecution;
  }

  @Override
  @Transactional
  public void write(Chunk<? extends DelinquencyUpdate> chunk) {
    List<? extends DelinquencyUpdate> items = chunk.getItems();
    if (items.isEmpty()) {
      return;
    }

    int toP = 0;
    int toL = 0;

    for (DelinquencyUpdate update : items) {
      BillingSchedule schedule =
          billingScheduleRepository
              .findById(update.candidate().billSchedId())
              .orElseThrow(
                  () ->
                      new IllegalStateException(
                          "Schedule not found: " + update.candidate().billSchedId()));

      if (schedule.getVersion() != null
          && schedule.getVersion().longValue() != update.candidate().version()) {
        throw new OptimisticLockingFailureException(
            "Concurrent modification on schedule " + update.candidate().billSchedId());
      }

      int delinquencyCount =
          schedule.getRecDelinquent() == null ? 0 : schedule.getRecDelinquent();
      if (update.transition().incrementDelinquencyCounter()) {
        delinquencyCount++;
        schedule.setRecDelinquent(delinquencyCount);
      }

      schedule.setSchedStatus(update.transition().newStatus());
      billingScheduleRepository.save(schedule);

      writeOutbox(update, delinquencyCount);

      if ("P".equals(update.transition().newStatus())) {
        toP++;
      } else if ("L".equals(update.transition().newStatus())) {
        toL++;
      }
    }

    incrementCounter(TRANSITIONED_TO_P_KEY, toP);
    incrementCounter(TRANSITIONED_TO_L_KEY, toL);
  }

  private void writeOutbox(DelinquencyUpdate update, int delinquencyCount) {
    Map<String, Object> payload = new HashMap<>();
    payload.put("program", properties.getProgramName());
    payload.put("billSchedId", update.candidate().billSchedId());
    payload.put("polNbr", update.candidate().polNbr());
    payload.put("oldStatus", update.transition().oldStatus());
    payload.put("newStatus", update.transition().newStatus());
    payload.put("daysPastDue", update.transition().daysPastDue());
    payload.put("delinquencyCount", delinquencyCount);

    try {
      outboxEventWriter.write(
          "billing-schedule",
          String.valueOf(update.candidate().billSchedId()),
          "DelinquencyStatusChanged",
          payload,
          UUID.randomUUID());
    } catch (DataAccessException ex) {
      markOutboxFailure();
      throw new AuditFailureException(
          "DelinquencyStatusChanged outbox write failed for schedule "
              + update.candidate().billSchedId(),
          ex);
    }
  }

  private void markOutboxFailure() {
    if (stepExecution != null) {
      stepExecution
          .getJobExecution()
          .getExecutionContext()
          .put(BatchJobExecutionListener.OUTBOX_WRITE_FAILED_KEY, Boolean.TRUE);
    }
  }

  private void incrementCounter(String key, int delta) {
    if (stepExecution == null || delta == 0) {
      return;
    }
    var jobContext = stepExecution.getJobExecution().getExecutionContext();
    long current = jobContext.getLong(key, 0L);
    jobContext.putLong(key, current + delta);
  }
}
