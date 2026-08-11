package com.pcis.billing.batch.bil003b;

import static org.assertj.core.api.Assertions.assertThat;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.pcis.billing.batch.bil003b.domain.FrequencyIntervalMapper;
import java.time.LocalDate;
import java.time.Period;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

class FrequencyIntervalMapperTest {

  private ListAppender<ILoggingEvent> logAppender;

  @BeforeEach
  void setUp() {
    Logger logger = (Logger) LoggerFactory.getLogger(FrequencyIntervalMapper.class);
    logAppender = new ListAppender<>();
    logAppender.start();
    logger.addAppender(logAppender);
  }

  @AfterEach
  void tearDown() {
    Logger logger = (Logger) LoggerFactory.getLogger(FrequencyIntervalMapper.class);
    logger.detachAppender(logAppender);
  }

  @Test
  void mapsKnownFrequencies() {
    assertThat(FrequencyIntervalMapper.toPeriod("M")).isEqualTo(Period.ofMonths(1));
    assertThat(FrequencyIntervalMapper.toPeriod("Q")).isEqualTo(Period.ofMonths(3));
    assertThat(FrequencyIntervalMapper.toPeriod("S")).isEqualTo(Period.ofMonths(6));
    assertThat(FrequencyIntervalMapper.toPeriod("A")).isEqualTo(Period.ofYears(1));
  }

  @Test
  void unknownFrequencyDefaultsToAnnualWithWarning() {
    assertThat(FrequencyIntervalMapper.toPeriod("X")).isEqualTo(Period.ofYears(1));
    assertThat(logAppender.list)
        .anyMatch(
            event ->
                event.getLevel() == Level.WARN
                    && event.getFormattedMessage().contains("Unknown billing frequency"));
  }

  @Test
  void nullAndBlankDefaultToAnnual() {
    assertThat(FrequencyIntervalMapper.toPeriod(null)).isEqualTo(Period.ofYears(1));
    assertThat(FrequencyIntervalMapper.toPeriod("")).isEqualTo(Period.ofYears(1));
  }

  @Test
  void nextDueDateUsesReferenceDateForFirstInstallment() {
    LocalDate ref = LocalDate.parse("2024-06-15");
    assertThat(FrequencyIntervalMapper.nextDueDate(null, "M", ref)).isEqualTo(ref);
  }

  @Test
  void nextDueDateAdvancesByFrequency() {
    LocalDate last = LocalDate.parse("2024-01-31");
    assertThat(FrequencyIntervalMapper.nextDueDate(last, "M", last))
        .isEqualTo(LocalDate.parse("2024-02-29"));
    assertThat(FrequencyIntervalMapper.nextDueDate(last, "Q", last))
        .isEqualTo(LocalDate.parse("2024-04-30"));
  }
}
